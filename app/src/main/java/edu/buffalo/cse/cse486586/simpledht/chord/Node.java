/**
 * 
 */
package edu.buffalo.cse.cse486586.simpledht.chord;

import edu.buffalo.cse.cse486586.simpledht.Constants;
import android.util.Log;

/**
 * @author roide
 *
 */
public class Node {

    private boolean mIsMaster;
    private String mNodeId;
    private String mNodeName;
    private static int mSize = 1;
    
    //private LinkedList<Node> mNodeList;
    private static final String TAG = Node.class.getSimpleName();
    
    /**
     * 
     */
    public Node(String nodeId, String nodeName, boolean isMaster) {
        mNodeId = nodeId;
        mNodeName = nodeName;
        mIsMaster = isMaster;
        if(mIsMaster)
            initMasterNode();
    }
    public Node(String nodeId, String nodeName) {
        mNodeId = nodeId;
        mNodeName = nodeName;
        mIsMaster = false;
    }
    
    public void setSuccessor(Node n) {
        mNext = n;
    }
    
    public Node getSuccessor() {
        return mNext;
    }
    
    public void setPredecessor(Node n) {
        mPrev = n;
    }
    
    public Node getPredecessor() {
        return mPrev;
    }
    
    public String getNodeId() {
        return mNodeId;
    }

    public String getNodeName() {
        return mNodeName;
    }

    public void setNodeName(String nodeName) {
        mNodeName = nodeName;
    }
    
    public int getPort() {
        return Constants.MAP_NODENAME_PORT.get(mNodeName);
    }
        
    /*
     * Variables and Functions for the Master Node
     */
    
    private Node mRoot;
    private Node mNext;
    private Node mPrev;
    
    private void initMasterNode() {
        Log.d(TAG, "initMasterNode");
        mRoot = this;
        mRoot.setSuccessor(this);
        mRoot.setPredecessor(this);
    }

    private boolean insert(Node curr, Node newNode) {
        //Log.d(TAG, "insert::curr:"+curr.getNodeId() +" new:"+newNode.getNodeId());
        //Log.d(TAG, "insert::currSucc::" + curr.getSuccessor().getNodeId());
        //Log.d(TAG, "insert::currPred::" + curr.getPredecessor().getNodeId());
        
        int comp = newNode.getNodeId().compareTo(curr.getNodeId());
        //Log.d(TAG, "comp="+comp);
        if(comp == 0) {
            //Log.d(TAG, "Collision?? -- This should not be happening");
            throw new IllegalStateException("Trying to add a node that already exists");
        } else if(comp<0) {
            //node is < curr node
            if(curr == mRoot) {
                //Log.d(TAG, "curr==root");
                newNode.setPredecessor(mRoot.getPredecessor());
                newNode.setSuccessor(mRoot);
                mRoot.getPredecessor().setSuccessor(newNode);
                mRoot.setPredecessor(newNode);
                //Log.d(TAG, "RootNode changed::old:"+ mRoot.getNodeId() );
                mRoot = newNode;
                //Log.d(TAG, "RootNode changed::new:"+ mRoot.getNodeId() );
            } else {
                //Log.d(TAG, "curr!=root");
                //Log.d(TAG, "curr: suc::" + curr.getSuccessor().getNodeId() + " pre:" + curr.getPredecessor().getNodeId());
                newNode.setSuccessor(curr);
                newNode.setPredecessor(curr.getPredecessor());
                curr.getPredecessor().setSuccessor(newNode);
                curr.setPredecessor(newNode);
                
                //Log.d(TAG, "new: suc" + newNode.getSuccessor().getNodeId() + " pre:" + newNode.getPredecessor().getNodeId());
            }
            return true;
        } else if(comp>0) {
            if(curr.getSuccessor() == mRoot) {
                curr.setSuccessor(newNode);
                newNode.setPredecessor(curr);
                newNode.setSuccessor(mRoot);
                mRoot.setPredecessor(newNode);
                return true;
            }
        }
        return false;
    }
    
    public Node add(String newNodeId, String nodeName) {
        Node newNode = new Node(newNodeId, nodeName,false);
        if(!mIsMaster) {
            Log.d(TAG, "Add not Permitted. Not a master node.");
            //throw new IllegalStateException("Add not Permitted. Not a master node.");
            return null;
        }
        Node curr = mRoot;
        
        if(insert(mRoot, newNode)) {
            Log.d(TAG, "Insert with root and New node Success. Size" + ++mSize);
            return newNode;
        }
        
        curr = mRoot.getSuccessor();
        while(curr != mRoot) {
            if(insert(curr, newNode)) {
                ++mSize;
                return newNode;
                //break;
            }
            curr = curr.getSuccessor();
        }
        return null;
    }   

}
