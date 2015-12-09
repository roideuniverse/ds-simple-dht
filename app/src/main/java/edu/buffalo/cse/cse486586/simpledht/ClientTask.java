package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.buffalo.cse.cse486586.simpledht.message.IMessage;
import android.os.AsyncTask;
import android.util.Log;

class ClientTask extends AsyncTask<IMessage, Void, Void> {
    private final String TAG = "ClientTask";
    @Override
    protected Void doInBackground(IMessage... msgArray) {
        Log.d(TAG, "doInBackground()");
        IMessage msgToSend = msgArray[0];
        Socket socket = null;
        
        try {
             socket = new Socket(InetAddress.getByAddress(new byte[] {
                    10, 0, 2, 2
            }), msgToSend.getDestination());

            //Log.d(TAG, "ClientTask : doInBackground::msg=" + msgToSend.getText());
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            oStream.writeObject(msgToSend);
            oStream.close();
            closeSocket(socket);
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, " exception:" + e);
            e.printStackTrace();
            closeSocket(socket);
        }
        return null;
    }
    
    private void closeSocket(Socket socket) {
        if(socket != null) {
            if(!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "exception::closeSocket::" + e);
                    e.printStackTrace();
                }
            }
        }
    }
}
