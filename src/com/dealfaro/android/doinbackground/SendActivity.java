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
	private ServerConnect downloader = null;
	
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
		spec.params = m;
		spec.activity = getApplication();
		// Initiates server call.
		downloader = new ServerConnect();
		downloader.execute(spec);
		
	}
	
	
	// Specification for accessing the server.
	class Spec {
		Spec() {};
		
		public String url;
		public Application activity;
		public HashMap<String, String> params;
		public void useResult(Application context, String r) {
			Log.i(LOG_TAG, "Result: " + r);
		}
	}

	
	// Checking for username uniqueness.
	class SendSpec extends Spec {
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
	
	class WhoIsHereSpec extends Spec {
		@Override
		public void useResult(Application activity, String r) {
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
    		while (downloadedString == null && numTries < MainActivity.MAX_SETUP_DOWNLOAD_TRIES && !isCancelled()) {
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
    		instr.spec.useResult(instr.spec.activity, instr.result);
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
