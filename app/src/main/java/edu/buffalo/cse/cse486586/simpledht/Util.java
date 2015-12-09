/**
 * 
 */
package edu.buffalo.cse.cse486586.simpledht;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * @author roide
 *
 */
public class Util {
    private static final String TAG = Util.class.getSimpleName();
    /**
     * 
     */
    private Util() {
    }
    
    public static String getEmulatorName(Context context) {
        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //Log.d(TAG, "line no=" + tel.getLine1Number());
        String emulatorNo = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        //Log.d(TAG, "emulatorNo=" + emulatorNo  );
        return emulatorNo;
    }
    
    public static Map<String,String> CursorToMap(Cursor cursor) {
        HashMap<String,String> map = new HashMap<String, String>();
        
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String key = cursor.getString(cursor.getColumnIndex(Constants.KEY));
                String val = cursor.getString(cursor.getColumnIndex(Constants.VALUE));
                map.put(key, val);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return map;
    }
    
    public static MatrixCursor MapToCursor(Map<String,String> map) {
        Log.d(TAG, "map:" + map);
        String[] columns = {
                Constants.KEY, Constants.VALUE
        };
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        Set<String> keySet = map.keySet();
        for(String key:keySet) {
            String val = map.get(key);
            matrixCursor.addRow(new String[] {
                    key, val
            });
        }
        return matrixCursor;
    }

}
