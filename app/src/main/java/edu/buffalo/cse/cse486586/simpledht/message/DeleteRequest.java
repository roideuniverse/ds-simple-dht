package edu.buffalo.cse.cse486586.simpledht.message;

public class DeleteRequest implements IMessage {
    private int mDestination;
    private String mNodeId;
    private String mNodeName;
    private int mHost;
    private String mDeleteKey;
    
    public DeleteRequest() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public MessageType getType() {
        return MessageType.VALUE_DELETE_REQUEST;
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
    
    public void setDeleteKey(String key) {
        mDeleteKey = key;
    }
    
    public String getDeleteKey() {
        return mDeleteKey;
    }

}
