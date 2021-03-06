package com.dealfaro.android.doinbackground;

// Copyright 2013 Luca de Alfaro.  Released under CC BY License
// See http://creativecommons.org/licenses/

import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;


public class MainActivity extends Activity {

	private static final String LOG_TAG = "MainActivity";
	
	// Server URL
	public static final String SERVER_URL_PREFIX = "https://luca-ucsc.appspot.com/classexample/default/";
	private String myId;
	private String username;
	
	// Background downloader.
	private ServerCall downloader = null;

	public static final String PREF_MY_ID = "app_id";
	public static final String PREF_USERNAME = "username";
	
	public static final String MY_SECRET = "behappy";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	     // Initialize random id.
	     SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	     myId = settings.getString(PREF_MY_ID, null);
	     if (myId == null) {
	         Random rand = new Random();
	    	 Editor editor = settings.edit();
	    	 // Invents a random id.
	    	 myId = String.valueOf(rand.nextLong());
	         editor.putString(PREF_MY_ID, myId);
	         editor.commit();
	     }
	     username = settings.getString(PREF_USERNAME, "").trim();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(LOG_TAG, "Username: " + username);
		TextView uv = (TextView) findViewById(R.id.textViewUsername);
		uv.setText(username);
		if (username.equals("")) {
			setUsername();
		}
	}
	
	
	private void setUsername() {
		// We ask the user for a username, and we check that it is unique.
		// A dialog for entering information, from http://www.androidsnippets.com/prompt-user-input-with-an-alertdialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Welcome");
		builder.setMessage("Choose Your Username");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		builder.setView(input);

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				// Send value to server, checking for uniqueness.
				UsernameSetSpec spec = new UsernameSetSpec();
				spec.url = SERVER_URL_PREFIX + "set_username.json";
				HashMap<String,String> m = new HashMap<String,String>();
				m.put("username", value);
				m.put("secret", MY_SECRET);
				m.put("userid", myId);
				spec.setParams(m);
				spec.context = MainActivity.this;
				dialog.dismiss();

				// Initiates server call.
				downloader = new ServerCall();
				downloader.execute(spec);

			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		builder.show();
	}
	
	
	public void clickSettings(View v) {
		// Goes to the settings activity.
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
	}
	
	public void clickSend(View v) {
		Intent intent = new Intent(this, SendActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	
	// Checking for username uniqueness.
	class UsernameSetSpec extends ServerCallSpec {
		@Override
		public void useResult(Context context, String r) {
			// If we get a null result, the server is down.
			// Go to the Network configuration.
			if (r == null) {
						Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
						startActivity(intent);
			} else {
				// Decodes the Json
				Gson gson = new Gson();
				OkResult result = gson.fromJson(r, OkResult.class);
				Log.i(LOG_TAG, "Result is: " + result.result);
				if (result.result) {					
					// Store the username and display it.
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
							getApplicationContext());
					Editor editor = settings.edit();
					editor.putString(PREF_USERNAME, result.username);
					editor.commit();
					username = settings.getString(PREF_USERNAME, "");
					Log.i(LOG_TAG, "Username: " + username);
					TextView uv = (TextView) findViewById(R.id.textViewUsername);
					uv.setText(result.username);
				} else {
					// Show that there is a problem.
					Log.i(LOG_TAG, "Gotten into the case");
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					Log.i(LOG_TAG, "Built the builder");
					builder.setTitle("Problem");
					builder.setMessage("The username is already taken");

					builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							setUsername();
						}
					});
					Log.i(LOG_TAG, "About to show");
					builder.show();
				}
			}
		}
	}
	
		
    
    @Override
    // This stops the downloader as soon as possible.
    public void onStop() {
    	if (downloader != null) {
    		downloader.cancel(false);
    	}
    	super.onStop();
    }
    


}
