package com.dealfaro.android.doinbackground;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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
	     // Makes the progress bar invisible.
	     ProgressBar pgb = (ProgressBar) findViewById(R.id.progressBar1);
	     pgb.setVisibility(View.GONE);
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
		m.put("userid", myId);
		m.put("username", username);
		spec.setParams(m);
		spec.context = getApplication();
		// Makes the progress bar visible.
		ProgressBar pgb = (ProgressBar) findViewById(R.id.progressBar1);
		pgb.setVisibility(View.VISIBLE);
		// Initiates server call.
		downloader = new ServerCall();
		downloader.execute(spec);
		
	}
	
	
	// Sends a message.
	class SendSpec extends ServerCallSpec {
		@Override
		public void useResult(Context context, String r) {
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
				// Makes the progress bar visible.
				ProgressBar pgb = (ProgressBar) findViewById(R.id.progressBar1);
				pgb.setVisibility(View.GONE);
				// Makes a toast for failure.
				Toast toast = Toast.makeText(context, "Network problem: the message has not been sent.", Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
}
