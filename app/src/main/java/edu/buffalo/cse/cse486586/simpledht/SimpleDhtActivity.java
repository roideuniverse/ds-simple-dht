package edu.buffalo.cse.cse486586.simpledht;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {
    
    Button mBtGet;
    Button mBtPut;
    EditText mEtKey;
    EditText mEtVal;
    
    ContentResolver mContentResolver;
    Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        
        mContentResolver = getContentResolver();
        
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
        
        mBtGet = (Button) findViewById(R.id.bt_get);
        mBtPut = (Button) findViewById(R.id.bt_put);
        mEtKey = (EditText) findViewById(R.id.et_key_id);
        mEtVal = (EditText) findViewById(R.id.et_value_id);
        
        mBtGet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = mEtKey.getText().toString();
                new Task().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,new String("GET"), new String(key));
            }
        });
        
        mBtPut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = mEtKey.getText().toString();
                String val = mEtVal.getText().toString();
                new Task().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,new String("PUT"), new String(key), new String(val));
            }
        });
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }
    
    private class Task extends AsyncTask<String, String, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.d("TAASK", "doInBackground");
            String type = params[0];
            if(type.equals("GET")) {
                String key = params[1];
                mContentResolver.query(mUri, null, key, null,null);
            } else if( type.equals( "PUT")) {
                ContentValues cv = new ContentValues();
                String key = params[1];
                String value = params[2];
                cv.put(Constants.KEY, key);
                cv.put(Constants.VALUE, value);
                mContentResolver.insert(mUri, cv);
            }
            return null;
        }
        
        protected void onProgressUpdate(String...strings) {
            return;
        }
    }

}
