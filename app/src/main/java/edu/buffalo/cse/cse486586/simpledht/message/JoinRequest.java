package edu.buffalo.cse.cse486586.simpledht.message;

public class JoinRequest implements IMessage {
    
    private int mDestination;
    private String mNodeId;
    private String mNodeName;
    private int mHost;

    @Override
    public MessageType getType() {
        return MessageType.NODE_JOIN_REQUEST;
    }

    @Override
    public int getDestination() {
        return mDestination;
    }

    @Override
    public void setDestination(int destination) {
        mDestination = destination;
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
    public void setHost(int host) {
        mHost = host;
    }

    @Override
    public int getHost() {
        return mHost;
    }
    
    @Override
    public void setNodeName(String name) {
        mNodeName = name;
    }

    @Override
    public String getNodeName() {
        return mNodeName;
    }

}
