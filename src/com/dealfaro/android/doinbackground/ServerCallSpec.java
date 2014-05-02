package com.dealfaro.android.doinbackground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import android.app.Application;
import android.util.Log;

public class ServerCallSpec {

	private static final String LOG_TAG = "ServerCallSpec";
	
	// Specification for accessing the server.
	ServerCallSpec() {};

	public String url;
	public Application activity;
	public UrlEncodedFormEntity form;
	public void useResult(Application context, String r) {}
	
	public boolean setParams(HashMap<String, String> params) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pair = it.next();
			nameValuePairs.add(new BasicNameValuePair(pair.getKey(), pair.getValue()));
		}
		try {
			form = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(LOG_TAG, "Encoding exception: " + e.toString());
			return false;
		}
		return true;
	}
	
}
