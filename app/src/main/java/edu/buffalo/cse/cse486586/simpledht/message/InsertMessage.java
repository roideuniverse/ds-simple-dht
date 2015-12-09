package edu.buffalo.cse.cse486586.simpledht.message;

import java.util.HashMap;
import java.util.Map;

public class InsertMessage implements IMessage {
    private int mDestination;
    private String mNodeId;
    private String mNodeName;
    private int mHost;
    private HashMap<String, String> mPayload;

    public InsertMessage() {
        mPayload = new HashMap<String,String>();
    }
    
    @Override
    public MessageType getType() {
        return MessageType.VALUE_INSERT_REQUEST;
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
    
    public void setPayload(Map<String,String> values) {
        mPayload.putAll(values);
    }
    
    public Map<String,String> getPayload() {
        return mPayload;
    }
}
