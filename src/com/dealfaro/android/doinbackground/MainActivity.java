package com.dealfaro.android.doinbackground;

// Copyright 2013 Luca de Alfaro.  Released under CC BY License
// See http://creativecommons.org/licenses/

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
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
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
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setUsername();
	}
	
	
	private void setUsername() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		username = settings.getString(PREF_USERNAME, "");
		Log.i(LOG_TAG, "Username: " + username);
		if (username.equals("")) {
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
					String value = input.getText().toString();
					// Send value to server, checking for uniqueness.
					UsernameSetSpec spec = new UsernameSetSpec();
					spec.url = SERVER_URL_PREFIX + "set_username.json";
					HashMap<String,String> m = new HashMap<String,String>();
					m.put("name", value);
					m.put("secret", MY_SECRET);
					m.put("id", myId);
					spec.setParams(m);
					spec.activity = getApplication();
					// What do we do next.
					
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
	}
	
	
	public void clickSettings(View v) {
		// Goes to the settings activity.
    	Intent intent = new Intent(this, SettingsActivity.class);
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
		public void useResult(Application context, String r) {
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
					if (false) {
						// Show that there is a problem.
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle("Problem");
						builder.setMessage("The username is already taken");

						builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								setUsername();
							}
						});
						builder.show();
					}
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
