/**
 * 
 */
package edu.buffalo.cse.cse486586.simpledht;

import java.util.HashMap;

/**
 * @author roide
 *
 */
public class Constants {

    /**
     * 
     */
    private Constants() {
    }
    
    public static final String MASTER_NODE_ID = "5554";
    public static final String NODE_NAME1 = "5554";
    public static final String NODE_NAME2 = "5556";
    public static final String NODE_NAME3 = "5558";
    public static final String NODE_NAME4 = "5560";
    public static final String NODE_NAME5 = "5562";
    
    public static final int SERVER_PORT = 10000;
    public static final int MASTER_NODE_PORT=11108;
    
    public static final int RESULT_SUCCESS = 101;
    public static final int RESULT_FAILURE = 100;
    
    public static final HashMap<String,Integer> MAP_NODENAME_PORT = new HashMap<String, Integer>();
    
    public static final String KEY = "key";
    public static final String VALUE = "value";
    
    static {
        MAP_NODENAME_PORT.put(NODE_NAME1, 11108);
        MAP_NODENAME_PORT.put(NODE_NAME2, 11112);
        MAP_NODENAME_PORT.put(NODE_NAME3, 11116);
        MAP_NODENAME_PORT.put(NODE_NAME4, 11120);
        MAP_NODENAME_PORT.put(NODE_NAME5, 11124);
    }
    

}
