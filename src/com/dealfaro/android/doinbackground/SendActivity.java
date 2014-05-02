package com.dealfaro.android.doinbackground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

public class SendActivity extends Activity {

	static final private String LOG_TAG = "SendActivity";
	String username;
	String myId;
	
	// Background downloader.
	private ServerCall downloader = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send);
	     SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	     myId = settings.getString(MainActivity.PREF_MY_ID, null);
	     username = settings.getString(MainActivity.PREF_USERNAME, "");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send, menu);
		return true;
	}
	
	public void clickSend(View v) {
		EditText v_dest = (EditText) findViewById(R.id.editText1);
		String dest = v_dest.getText().toString();
		EditText v_msg = (EditText) findViewById(R.id.editText2);
		String msg = v_msg.getText().toString();
		SendSpec spec = new SendSpec();
		spec.url = MainActivity.SERVER_URL_PREFIX + "send.json";
		HashMap<String,String> m = new HashMap<String,String>();
		m.put("dest", dest);
		m.put("msg", msg);
		m.put("secret", MainActivity.MY_SECRET);
		m.put("id", myId);
		m.put("username", username);
		spec.setParams(m);
		spec.activity = getApplication();
		// Initiates server call.
		downloader = new ServerCall();
		downloader.execute(spec);
		
	}
	
	
	// Checking for username uniqueness.
	class SendSpec extends ServerCallSpec {
		@Override
		public void useResult(Application context, String r) {
			// If we get a null result, the server is down.
			// Go to the Network configuration.
			boolean success = false;
			if (r != null) {
				// Decodes the Json
				Gson gson = new Gson();
				OkResult result = gson.fromJson(r, OkResult.class);
				success = result.result;
			}
			if (success) {
				finish();
			} else {
				Toast toast = new Toast(getApplicationContext());
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setText("Not sent.");
				toast.show();
			}
		}
	}
	
}
