package edu.buffalo.cse.cse486586.simpledht.message;

public class JoinResponse implements IMessage {
    
    private int mDestination;
    private String mPredecessorId;
    private String mPredecessorName;
    private String mSuscessorId;
    private String mSuccessorName;
    private int mResultCode;
    private String mNodeId;
    private String mNodeName;
    private int mHost;

    @Override
    public MessageType getType() {
        return MessageType.NODE_JOIN_RESPONSE;
    }

    @Override
    public int getDestination() {
        return mDestination;
    }
    
    public void setDestination(int dest) {
        mDestination = dest;
    }
    
    public String getPredecessorId() {
        return mPredecessorId;
    }

    public void setPredecessorId(String predecessorId) {
        mPredecessorId = predecessorId;
    }

    public String getSuscessorId() {
        return mSuscessorId;
    }

    public void setSuscessorId(String suscessorId) {
        mSuscessorId = suscessorId;
    }
    
    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int resultCode) {
        mResultCode = resultCode;
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

    public String getSuccessorName() {
        return mSuccessorName;
    }

    public void setSuccessorName(String mSuccessorName) {
        this.mSuccessorName = mSuccessorName;
    }

    public String getPredecessorName() {
        return mPredecessorName;
    }

    public void setPredecessorName(String mPredecessorName) {
        this.mPredecessorName = mPredecessorName;
    }

}
