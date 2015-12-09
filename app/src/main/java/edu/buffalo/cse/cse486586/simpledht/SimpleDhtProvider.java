
package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.cse486586.simpledht.chord.Node;
import edu.buffalo.cse.cse486586.simpledht.message.DeleteRequest;
import edu.buffalo.cse.cse486586.simpledht.message.IMessage;
import edu.buffalo.cse.cse486586.simpledht.message.InsertMessage;
import edu.buffalo.cse.cse486586.simpledht.message.JoinNotify;
import edu.buffalo.cse.cse486586.simpledht.message.JoinRequest;
import edu.buffalo.cse.cse486586.simpledht.message.JoinResponse;
import edu.buffalo.cse.cse486586.simpledht.message.Message;
import edu.buffalo.cse.cse486586.simpledht.message.MessageType;
import edu.buffalo.cse.cse486586.simpledht.message.QueryRequest;
import edu.buffalo.cse.cse486586.simpledht.message.QueryResponse;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private SimpleDhtDataBase mDatabase;
    private boolean mIsMasterNode;
    private Node mLocalNode;
    private ServerSocket mServerSocket;

    private static final String AUTHORITY = "edu.buffalo.cse.cse486586.simpledht.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    private final String TAG = SimpleDhtProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if(selection.equals("*")) {
            deleteDHTAll();
            return 0;
        }
        
        if(selection.equals("@")) {
            return mDatabase.delete(selection);
        }
        /*
         * Try deleting in local database. If failure send request to successor.
         */
        int count = mDatabase.delete(selection);
        if(count == 0 ) {
            Log.d(TAG, "Not deleted in LocalDb.");
            sendDeleteMsgToSuccessor(selection);
        }
        Log.d(TAG, "delete()::count::" + count);
        return 0;
    }
    
    private int deleteDHTAll() {
        int count = 0;
        count = count + mDatabase.delete("@");
        sendDeleteMsgToSuccessor("*");
        return count;
    }
    
    private void sendDeleteMsgToSuccessor(String key) {
        DeleteRequest req = new DeleteRequest();
        req.setDeleteKey(key);
        req.setDestination(mLocalNode.getSuccessor().getPort());
        req.setHost(mLocalNode.getPort());
        req.setNodeId(mLocalNode.getNodeId());
        req.setNodeName(mLocalNode.getNodeName());
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, req);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert()");
        String key = values.getAsString(Constants.KEY);
        Log.d(TAG, "insert::key::" + key);
        boolean sendToSuccessor = false;

        HashMap<String, String> payload = new HashMap<String, String>();

        String hKey = null;
        try {
            hKey = genHash(key);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Exception::insert()::" + e);
        }

        if (hKey == null) {
            return null;
        }

        if (mLocalNode == null) {
            Log.d(TAG, "Not Ready for insert. Should not be happening");
            return null;
        }

        if (mLocalNode.getSuccessor() == null) {
            Log.d(TAG, "successor null...inserting in local.");
            return mDatabase.putValue(createCV(key, values.getAsString(Constants.VALUE)));
        }

        /*
         * if successor and current is same, its the only node, master node
         */
        if (mLocalNode.getNodeId().equals(mLocalNode.getSuccessor().getNodeId())) {
            Log.d(TAG, "only master node in network::inserting in localdb.");
            return mDatabase.putValue(createCV(key, values.getAsString(Constants.VALUE)));
        }

        int compToCurr = hKey.compareTo(mLocalNode.getNodeId());
        int compToPred = hKey.compareTo(mLocalNode.getPredecessor().getNodeId());

        /*
         * key equal to node, so should be assigned here.
         */
        if (compToCurr == 0) {
            Log.d(TAG, "key equall to node::inserting local");
            return mDatabase.putValue(createCV(key, values.getAsString(Constants.VALUE)));
        }

        if (compToPred > 0 && compToCurr < 0) {
            Log.d(TAG, "key>pred & key < curr::assigning local");
            return mDatabase.putValue(createCV(key, values.getAsString(Constants.VALUE)));
        }
        /*
         * Since the assignment of the key is not local, add it to payload, to
         * be sent to successor
         */
        payload.put(key, values.getAsString(Constants.VALUE));
        sendToSuccessor = true;

        /*
         * We have values that need to be send to successor.
         */
        if (sendToSuccessor && !payload.isEmpty()) {
            InsertMessage iMsg = new InsertMessage();
            iMsg.setDestination(mLocalNode.getSuccessor().getPort());
            iMsg.setHost(mLocalNode.getPort());
            iMsg.setNodeId(mLocalNode.getNodeId());
            iMsg.setNodeName(mLocalNode.getNodeName());
            iMsg.setPayload(payload);

            Log.d(TAG, "sendingValues to Successor:" + mLocalNode.getSuccessor().getNodeId() + "::"
                    + payload);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, iMsg);
        }
        return null;
    }

    private ContentValues createCV(String key, String value) {
        ContentValues cVal = new ContentValues();
        cVal.put(Constants.KEY, key);
        cVal.put(Constants.VALUE, value);
        return cVal;
    }

    @Override
    public boolean onCreate() {
        mDatabase = new SimpleDhtDataBase(getContext());
        String emulatorName = Util.getEmulatorName(getContext());
        Log.d(TAG, "emulator:" + emulatorName);

        String nodeId = null;
        try {
            nodeId = genHash(emulatorName);
            Log.d(TAG, "nodeid:" + nodeId);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Exception::onCreate()::" + e);
        }

        if (nodeId == null) {
            Log.d(TAG, "NodeId:" + nodeId);
            return false;
        }

        try {
            if (genHash(Constants.MASTER_NODE_ID).equals(nodeId)) {
                mLocalNode = new Node(nodeId, emulatorName, true);
                mIsMasterNode = true;
            } else {
                mLocalNode = new Node(nodeId, emulatorName, false);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Exception::onCreate()::" + e);
            return false;
        }

        if (!mIsMasterNode) {
            sendJoinRequest();
        }

        startServer();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.d(TAG, "query=" + selection);
        if (selection.equals("*")) {
            return getDHTAllValues();
        }

        if (selection.equals("@")) {
            return mDatabase.getValue(selection);
        }

        Cursor c = mDatabase.getValue(selection);
        if (c.getCount() < 1) {
            Log.d(TAG, "Key::" + selection + "::Not_Present_Locally");
            QueryRequest qRes = new QueryRequest();
            qRes.setDestination(mLocalNode.getSuccessor().getPort());
            qRes.setHost(mLocalNode.getPort());
            qRes.setNodeId(mLocalNode.getNodeId());
            qRes.setNodeName(mLocalNode.getNodeName());
            qRes.setQueryString(selection);
            QueryResponse qResponse = sendQueryRequestToSucccessor(qRes);
            if (qResponse.hasPayload()) {
                Map<String, String> payload = qResponse.getPayload();
                MatrixCursor cur = Util.MapToCursor(payload);
                return cur;
            }
        }
        return mDatabase.getValue(selection);
    }

    private Cursor getDHTAllValues() {
        Log.d(TAG, "getDHTAllValues()::");
        String[] columns = {
                Constants.KEY, Constants.VALUE
        };
        MatrixCursor matrixCursor = new MatrixCursor(columns);

        Cursor c = mDatabase.getValue("@");
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                String key = c.getString(c.getColumnIndex(Constants.KEY));
                String val = c.getString(c.getColumnIndex(Constants.VALUE));
                matrixCursor.addRow(new String[] {
                        key, val
                });
                Log.d(TAG, "POSITION=" + matrixCursor.getPosition());
                c.moveToNext();
            }
        }
        c.close();

        if (mLocalNode.getSuccessor() == null) {
            return matrixCursor;
        }

        /*
         * if successor is same, just return.
         */
        if (mLocalNode.getNodeId().equals(mLocalNode.getSuccessor().getNodeId())) {
            return matrixCursor;
        }

        /*
         * send request to successor, synchronous request.
         */
        QueryRequest qMsg = new QueryRequest();
        qMsg.setDestination(mLocalNode.getSuccessor().getPort());
        qMsg.setHost(mLocalNode.getPort());
        qMsg.setNodeId(mLocalNode.getNodeId());
        qMsg.setNodeName(mLocalNode.getNodeName());
        qMsg.setQueryString("*");

        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByAddress(new byte[] {
                    10, 0, 2, 2
            }), qMsg.getDestination());

            /*
             * Write to socket
             */
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            oStream.writeObject(qMsg);
            oStream.flush();

            /*
             * Read from socket.
             */
            ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());
            IMessage msg = (IMessage) iStream.readObject();

            Log.d(TAG, "getDHTAllValues()::GotMsgInSocket::" + msg.getType());
            if (msg.getType() == MessageType.VALUE_QUERY_RESPONSE) {
                QueryResponse res = (QueryResponse) msg;
                Log.d(TAG, "hasPayload:" + res.hasPayload());
                Log.d(TAG, "payload::" + res.getPayload());
                if (res.hasPayload()) {
                    Map<String, String> payload = res.getPayload();
                    Set<String> keySet = payload.keySet();
                    for (String key : keySet) {
                        matrixCursor.addRow(new String[] {
                                key, payload.get(key)
                        });
                    }
                }
            }

            iStream.close();
            oStream.close();
            socket.close();
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, " exception:" + e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return matrixCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private void startServer() {
        Log.d(TAG, "startServer()::");
        try {
            mServerSocket = new ServerSocket(Constants.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mServerSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (mServerSocket != null)
            if (!mServerSocket.isClosed())
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    Log.d(TAG, "stopServer()::" + e.toString());
                }
    }

    private void sendJoinRequest() {
        JoinRequest msg = new JoinRequest();
        msg.setNodeId(mLocalNode.getNodeId());
        msg.setNodeName(mLocalNode.getNodeName());
        msg.setHost(mLocalNode.getPort());
        msg.setDestination(Constants.MASTER_NODE_PORT);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
    }

    /*
     * For a non master node. This function is called when a non master node
     * receives a result of the join request it sent to the master server
     */
    private void receivedJoinResponse(IMessage msg) {
        Log.d(TAG, "receivedJoinResponse()::");
        JoinResponse res = (JoinResponse) msg;
        if (res.getResultCode() == Constants.RESULT_SUCCESS) {
            mLocalNode.setSuccessor(new Node(res.getSuscessorId(), res.getSuccessorName()));
            mLocalNode.setPredecessor(new Node(res.getPredecessorId(), res.getPredecessorName()));
            /*
             * if both successor and predecessor are same, send only one req.
             */
            if (res.getSuscessorId().equals(res.getPredecessorId())) {
                notifyPredecessor();
            } else {
                notifyPredecessor();
                notifySuccessor();
            }
        }
        Log.d(TAG, "receivedJoinResponse()::" + res.getResultCode());
        Log.d(TAG, "predecessor:" + res.getPredecessorId());
        Log.d(TAG, "successor:" + res.getSuscessorId());
    }

    /*
     * For master node, when it receives a join request by a new node in the
     * network
     */
    private void receivedJoinRequest(IMessage msg) {
        Log.d(TAG, "receivedJoinRequest()::");
        if (!mIsMasterNode) {
            Log.d(TAG, "Received Join on a non-master node");
            return;
        }
        JoinRequest req = (JoinRequest) msg;
        JoinResponse resMsg = new JoinResponse();
        Log.d(TAG, "addRequestFrom::" + req.getNodeId());
        Node n = mLocalNode.add(req.getNodeId(), req.getNodeName());
        if (n != null) {
            // succsss
            resMsg.setResultCode(Constants.RESULT_SUCCESS);
            resMsg.setDestination(req.getHost());
            resMsg.setPredecessorId(n.getPredecessor().getNodeId());
            resMsg.setPredecessorName(n.getPredecessor().getNodeName());
            resMsg.setSuscessorId(n.getSuccessor().getNodeId());
            resMsg.setSuccessorName(n.getSuccessor().getNodeName());
        } else {
            // failure
            resMsg.setResultCode(Constants.RESULT_FAILURE);
            resMsg.setDestination(req.getHost());
        }
        Log.d(TAG, "NewNode_predecessor:" + n.getPredecessor().getNodeId());
        Log.d(TAG, "NewNode_successor:" + n.getSuccessor().getNodeId());

        // send response for join
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, resMsg);
    }

    private void notifyPredecessor() {
        Log.d(TAG, "notifyPredecessor()::");

        JoinNotify notifyMsg = new JoinNotify();
        notifyMsg.setNodeId(mLocalNode.getNodeId());
        notifyMsg.setNodeName(mLocalNode.getNodeName());
        notifyMsg.setDestination(mLocalNode.getPredecessor().getPort());
        notifyMsg.setIsSuccessor(true);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, notifyMsg);
    }

    private void notifySuccessor() {
        Log.d(TAG, "notifySuccessor");

        JoinNotify notifyMsg = new JoinNotify();
        notifyMsg.setNodeId(mLocalNode.getNodeId());
        notifyMsg.setNodeName(mLocalNode.getNodeName());
        notifyMsg.setDestination(mLocalNode.getSuccessor().getPort());
        notifyMsg.setIsSuccessor(false);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, notifyMsg);
    }

    /*
     * this function is called, when a new node has joined in the neighbourhood
     * and is trying to communicate of its presence.
     */
    private void receivedJoinNotifyMesage(IMessage msg) {
        Log.d(TAG, "receivedJoinNotifyMesage()");
        Log.d(TAG, "old:succ:" + mLocalNode.getSuccessor().getNodeId());
        Log.d(TAG, "old:pred:" + mLocalNode.getPredecessor().getNodeId());

        JoinNotify notifyMsg = (JoinNotify) msg;
        String nName = notifyMsg.getNodeName();
        String nId = notifyMsg.getNodeId();

        boolean prede = false;

        if (notifyMsg.isSuccessor()) {
            if (!mLocalNode.getSuccessor().getNodeId().equals(nId)) {
                mLocalNode.setSuccessor(new Node(nId, nName));
            }
        } else {
            if (!mLocalNode.getPredecessor().getNodeId().equals(nId)) {
                mLocalNode.setPredecessor(new Node(nId, nName));
            }
            prede = true;
        }

        Log.d(TAG, "new:succ:" + mLocalNode.getSuccessor().getNodeId());
        Log.d(TAG, "new:pred:" + mLocalNode.getPredecessor().getNodeId());

        if (prede) {
            Log.d(TAG, "The node joined as a predecessor, I might need to send it the payload.");
            // TODO:send insert value request
        }
    }

    private void receivedDeleteValueRequest(IMessage msg) {
        DeleteRequest req = (DeleteRequest) msg;
        String key = req.getDeleteKey();
        int count = 0;
        if(!key.equals("*")) {
            count = mDatabase.delete(key);
            if(count > 0) 
                return;
        }
        
        mDatabase.delete("@");
        if(req.getNodeId().equals(mLocalNode.getSuccessor().getNodeId())) {
            //next node in ring is sender of msg, so cannot fwd, so return.
            return;
        }
        
        DeleteRequest newReq = new DeleteRequest();
        newReq.setDeleteKey(req.getDeleteKey());
        newReq.setDestination(mLocalNode.getSuccessor().getPort());
        newReq.setHost(req.getHost());
        newReq.setNodeId(req.getNodeId());
        newReq.setNodeName(req.getNodeName());
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newReq);
        
    }
    private void receivedInsertValueRequest(IMessage msg) {
        Log.d(TAG, "receivedInsertValueRequest()::");
        InsertMessage recMsg = (InsertMessage) msg;
        Map<String, String> payloadReceived = recMsg.getPayload();
        Set<String> keySet = payloadReceived.keySet();

        boolean sendToSuccessor = false;
        HashMap<String, String> payloadToFwd = new HashMap<String, String>();

        for (String key : keySet) {
            String hKey = null;
            try {
                hKey = genHash(key);
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (hKey == null)
                continue;

            int compToCurr = hKey.compareTo(mLocalNode.getNodeId());
            int compToPred = hKey.compareTo(mLocalNode.getPredecessor().getNodeId());
            Log.d(TAG, "CurrNode:" + mLocalNode.getNodeId());
            Log.d(TAG, "PredNode:" + mLocalNode.getPredecessor().getNodeId());
            Log.d(TAG, "compToCurr:" + compToCurr + " compToPred:" + compToPred);

            /*
             * key equal to node, so should be assigned here.
             */
            if (compToCurr == 0) {
                Log.d(TAG, "key equall to node::inserting local");
                mDatabase.putValue(createCV(key, payloadReceived.get(key)));
                continue;
            }

            if (compToPred > 0 && compToCurr < 0) {
                Log.d(TAG, "key>pred & key < curr::assigning local");
                mDatabase.putValue(createCV(key, payloadReceived.get(key)));
                continue;
            }

            if (compToPred > 0 && compToCurr > 0) {
                int comp = mLocalNode.getPredecessor().getNodeId()
                        .compareTo(mLocalNode.getNodeId());
                if (comp > 0) {
                    Log.d(TAG, "key>pred & key>cur & pred>curr :: inserting local");
                    mDatabase.putValue(createCV(key, payloadReceived.get(key)));
                    continue;
                }
            }

            if (compToPred < 0 && compToCurr < 0) {
                int comp = mLocalNode.getPredecessor().getNodeId()
                        .compareTo(mLocalNode.getNodeId());
                if (comp > 0) {
                    Log.d(TAG, "key<pred & key<curr & pred>curr :: inserting local");
                    mDatabase.putValue(createCV(key, payloadReceived.get(key)));
                    continue;
                }
            }
            /*
             * Since the assignment of the key is not local, add it to payload,
             * to be sent to successor
             */
            payloadToFwd.put(key, payloadReceived.get(key));
            sendToSuccessor = true;
        }
        /*
         * We have values that need to be send to successor.
         */
        if (sendToSuccessor && !payloadToFwd.isEmpty()) {
            InsertMessage iMsg = new InsertMessage();
            iMsg.setDestination(mLocalNode.getSuccessor().getPort());
            iMsg.setHost(mLocalNode.getPort());
            iMsg.setNodeId(mLocalNode.getNodeId());
            iMsg.setNodeName(mLocalNode.getNodeName());
            iMsg.setPayload(payloadToFwd);

            Log.d(TAG, "sendingValues to Successor:" + mLocalNode.getSuccessor().getNodeId() + "::"
                    + payloadToFwd);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, iMsg);
        }

    }

    private MatrixCursor getAllDHTLocal() {
        String[] columns = {
                Constants.KEY, Constants.VALUE
        };
        MatrixCursor matrixCursor = new MatrixCursor(columns);

        Cursor c = mDatabase.getValue("@");
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                String key = c.getString(c.getColumnIndex(Constants.KEY));
                String val = c.getString(c.getColumnIndex(Constants.VALUE));
                matrixCursor.addRow(new String[] {
                        key, val
                });
                c.moveToNext();
            }
        }
        c.close();
        return matrixCursor;
    }

    private QueryResponse receivedQueryRequest(IMessage msg) {
        Log.d(TAG, "receivedQueryRequest()");
        QueryRequest qMsg = (QueryRequest) msg;
        String query = qMsg.getQueryKey();

        Log.d(TAG, "key:" + query);
        QueryResponse resMsg = new QueryResponse();

        if (query.equals("*")) {
            resMsg.setPayload(Util.CursorToMap(getAllDHTLocal()));
        } else {
            Cursor c = mDatabase.getValue(query);
            if (c.getCount() >= 1) {
                Log.d(TAG, "Key found in local db..");
                resMsg.setPayload(Util.CursorToMap(c));
            } else
                Log.d(TAG, "Key not found in localdb");
        }

        Log.d(TAG, "---**---");
        /*
         * check if we need to forward this request to successor. if(successorId
         * == nodeId where the msg originated, do not forward
         */
        if (mLocalNode.getSuccessor().getNodeId().equals(qMsg.getNodeId())) {
            Log.d(TAG, "Not sending to successor. Both successor and originator same");
            return resMsg;
        } else {
            // we need to send it to successor if query != * or there is no
            // payload
            if (!resMsg.hasPayload() || query.equals("*")) {
                Log.d(TAG, "Forwarding req to successor.");
                QueryResponse resSucc = sendQueryRequestToSucccessor(qMsg);
                Log.d(TAG, "");
                if (resSucc.hasPayload()) {
                    resMsg.addPayload(resSucc.getPayload());
                }
            }
        }

        Log.d(TAG, "receivedQueryRequest()::Returning::");
        return resMsg;
    }

    private QueryResponse sendQueryRequestToSucccessor(QueryRequest qMsg) {
        Log.d(TAG, "sendQueryRequestToSucccessor()");
        QueryResponse resQuery = null;
        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByAddress(new byte[] {
                    10, 0, 2, 2
            }), mLocalNode.getSuccessor().getPort());

            /*
             * Write to socket
             */
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            oStream.writeObject(qMsg);
            oStream.flush();

            /*
             * Read from socket.
             */
            ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());
            IMessage msg = (IMessage) iStream.readObject();

            Log.d(TAG, "sendQueryRequestToSucccessor()::GotMsgInSocket::" + msg.getType());
            if (msg.getType() == MessageType.VALUE_QUERY_RESPONSE) {
                QueryResponse rQuery = (QueryResponse) msg;

                Log.d(TAG, "hasPayload" + rQuery.hasPayload());
                Log.d(TAG, "payload:" + rQuery.getPayload());

                resQuery = new QueryResponse();
                HashMap<String, String> load = new HashMap<String, String>();
                load.putAll(rQuery.getPayload());
                resQuery.setPayload(load);
            }

            // iStream.close();
            // oStream.close();
            socket.close();
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, " exception:" + e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resQuery;
    }

    /**
     * @author roide
     */
    private class ServerTask extends AsyncTask<ServerSocket, IMessage, Void> {
        private static final String TAG = "ServerTask";

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.d(TAG, "ServerTask:doInBackground");
            ServerSocket serverSocket = sockets[0];
            try {
                while (true) {
                    Socket client = serverSocket.accept();
                    // Log.d(TAG, "Accepted?-->" + client);
                    ObjectInputStream iStream = new ObjectInputStream(client.getInputStream());
                    IMessage msg = (IMessage) iStream.readObject();
                    Log.d(TAG, "Received:" + msg.getType());

                    if (msg.getType() == MessageType.VALUE_QUERY_REQUEST) {
                        QueryResponse res = receivedQueryRequest(msg);
                        ObjectOutputStream oStream = new ObjectOutputStream(
                                client.getOutputStream());
                        oStream.writeObject(res);
                        oStream.flush();
                        oStream.close();
                        continue;
                    }

                    publishProgress(msg);
                }
            } catch (IOException e) {
                Log.d(TAG, "Exception:doInBackground:" + e.toString());
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "Exception:doInBackground:" + e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(IMessage... message) {
            IMessage msg = message[0];
            Log.d(TAG, "onProgressUpdate::" + msg.getType());
            if (msg.getType() == MessageType.NODE_JOIN_RESPONSE) {
                receivedJoinResponse(msg);
            } else if (msg.getType() == MessageType.NODE_JOIN_REQUEST) {
                receivedJoinRequest(msg);
            } else if (msg.getType() == MessageType.NODE_JOIN_NOTIFY) {
                receivedJoinNotifyMesage(msg);
            } else if (msg.getType() == MessageType.VALUE_INSERT_REQUEST) {
                receivedInsertValueRequest(msg);
            } else if(msg.getType() == MessageType.VALUE_DELETE_REQUEST) {
                receivedDeleteValueRequest(msg);
            }
        }
    }
}
