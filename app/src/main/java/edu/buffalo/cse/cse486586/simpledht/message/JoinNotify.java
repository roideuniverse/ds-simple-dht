/**
 * 
 */
package edu.buffalo.cse.cse486586.simpledht.message;

/**
 * @author roide
 *
 */
public class JoinNotify implements IMessage {
    private int mDestination;
    private String mNodeId;
    private String mNodeName;
    private int mHost;
    private boolean mHasPayLoad;
    private boolean mIsSuccessor;

    @Override
    public MessageType getType() {
        return MessageType.NODE_JOIN_NOTIFY;
    }

    @Override
    public void setDestination(int destination) {
        mDestination = destination;
    }

    @Override
    public int getDestination() {
        return mDestination;
    }

    @Override
    public void setNodeId(String nodeId) {
        mNodeId = nodeId;
     }
    @Override
    public String getNodeId() {
        return mNodeId;
    }

    @Override
    public void setNodeName(String name) {
       mNodeName = name;
    }

    @Override
    public String getNodeName() {
        return mNodeName;
    }

    @Override
    public void setHost(int host) {
        mHost = host;
    }

    @Override
    public int getHost() {
        return mHost;
    }
    
    public boolean hasPayload() {
        return mHasPayLoad;
    }
    
    public void setHasPayloadTrue() {
        mHasPayLoad = true;
    }

    public boolean isSuccessor() {
        return mIsSuccessor;
    }

    public void setIsSuccessor(boolean mIsSuccessor) {
        this.mIsSuccessor = mIsSuccessor;
    }

}
