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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;


public class MainActivity extends Activity {

	private static final String LOG_TAG = "MainActivity";
	private static final int MAX_SETUP_DOWNLOAD_TRIES = 3;
	
	// Server URL
	private static final String SERVER_URL_PREFIX = "https://luca-ucsc.appspot.com/classexample/default/";
	private String myId;
	private String username;
	
	// Background downloader.
	private ServerConnect downloader = null;

	private static final String PREF_MY_ID = "app_id";
	private static final String PREF_USERNAME = "username";
	
	private static final String MY_SECRET = "behappy";
	
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
					spec.params = m;
					// What do we do next.
					
					// Initiates server call.
					downloader = new ServerConnect();
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
	
	// Specification for accessing the server.
	class Spec {
		Spec() {};
		
		public String url;
		public HashMap<String, String> params;
		public void useResult(String r) {
			Log.i(LOG_TAG, "Result: " + r);
		}
	}

	
	// Checking for username uniqueness.
	class UsernameSetSpec extends Spec {
		@Override
		public void useResult(String r) {
			// Decodes the Json
			Gson gson = new Gson();
			OkResult result = gson.fromJson(r, OkResult.class);
			if (result.equals("ok")) {
				// Toast to acknowledge, and store into settings.
				
			} else {
				// Show that there is a problem.
				AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
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
	
	class WhoIsHereSpec extends Spec {
		@Override
		public void useResult(String r) {
			// Here goes the Json decoding etc.
			Log.i(LOG_TAG, "Result to use: " + r);			
		}
	}
	
	class PostProcessPair {
		PostProcessPair() {};
		public Spec spec;
		public String result;
	}
	
		
    // This class downloads from the net the camera setup instructions.
    private class ServerConnect extends AsyncTask<Spec, String, PostProcessPair> {
    	
    	protected PostProcessPair doInBackground(Spec... specs) {
    		Log.d(LOG_TAG, "Starting the download.");
    		String downloadedString = null;
    		Spec spec = specs[0];
    		URI url = URI.create(spec.url);
    		int numTries = 0;
    		while (downloadedString == null && numTries < MAX_SETUP_DOWNLOAD_TRIES && !isCancelled()) {
    			numTries++;
    			HttpPost request = new HttpPost(url);
    			// We need to add the parameters.
    			UrlEncodedFormEntity form = urlEncodeHashMap(spec.params);
    			request.setEntity(form);
    	
    			DefaultHttpClient httpClient = new DefaultHttpClient();
    			HttpResponse response = null;
    			try {
    				response = httpClient.execute(request);
    			} catch (ClientProtocolException ex) {
    				Log.e(LOG_TAG, ex.toString());
    			} catch (IOException ex) {
    				Log.e(LOG_TAG, ex.toString());
    			}
    			if (response != null) {
    				// Checks the status code.
    				int statusCode = response.getStatusLine().getStatusCode();
    				Log.d(LOG_TAG, "Status code: " + statusCode);

    				if (statusCode == HttpURLConnection.HTTP_OK) {
    					// Correct response. Reads the real result.
    					// Extracts the string content of the response.
    					HttpEntity entity = response.getEntity();
    					InputStream iStream = null;
    					try {
    						iStream = entity.getContent();
    					} catch (IOException ex) {
    						Log.e(LOG_TAG, ex.toString());
    					}
    					if (iStream != null) {
    						downloadedString = ConvertStreamToString(iStream);
    						Log.d(LOG_TAG, "Received string: " + downloadedString);
    			    		// Postprocess the result.
    			    		PostProcessPair instr = new PostProcessPair();
    			    		instr.spec = spec;
    			    		instr.result = downloadedString;
    			    		return instr;
    					}
    				}
    			}
    		}
    		// Postprocess the result.
    		PostProcessPair instr = new PostProcessPair();
    		instr.spec = spec;
    		instr.result = downloadedString;
    		return instr;
    	}
    	
    	protected void onPostExecute(PostProcessPair instr) {
    		// This is executed in the UI thread.
    		instr.spec.useResult(instr.result);
    	}
    }
    
    public static UrlEncodedFormEntity urlEncodeHashMap(HashMap<String, String> params) {
    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    	Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<String, String> pair = it.next();
    		nameValuePairs.add(new BasicNameValuePair(pair.getKey(), pair.getValue()));
    	}
    	UrlEncodedFormEntity form;
    	try {
    		form = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
    	} catch (UnsupportedEncodingException e) {
    		Log.e(LOG_TAG, "Encoding exception: " + e.toString());
    		return null;
    	}
    	return form;
    }

    
    @Override
    // This stops the downloader as soon as possible.
    public void onStop() {
    	if (downloader != null) {
    		downloader.cancel(false);
    	}
    	super.onStop();
    }
    
    public static String ConvertStreamToString(InputStream is) {
    	
    	if (is == null) {
    		return null;
    	}
    	
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append((line + "\n"));
	        }
	    } catch (IOException e) {
	        Log.d(LOG_TAG, e.toString());
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            Log.d(LOG_TAG, e.toString());
	        }
	    }
	    return sb.toString();
	}



}
