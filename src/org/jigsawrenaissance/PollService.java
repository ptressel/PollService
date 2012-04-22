package org.jigsawrenaissance;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Read commands from server at specified intervals. Make commands available to
 * command processing service.
 *  
 * Send sensor and status data to server at specified intervals.
 * 
 * Using examples from:
 * http://developer.android.com/guide/topics/fundamentals/services.html
 * http://w3mentor.com/learn/java/android-development/android-http-services/example-of-http-get-request-using-httpclient-in-android/
 * RHoK #3 SAARAA situation reporting app (uncompleted).
 * @author Joe McCarthy
 * @author Pat Tressel
 */

public class PollService extends IntentService {

	public static final int BUFSIZ = 1024;

	/** 
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public PollService() {
		super("PollService");
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(Constants.TAG, "About to start polling loop.");
		while (true) {
			if (Constants.GET_COMMAND) {
			    // Read one command from the server.
				String command = tryGet(intent);
		        Log.i(Constants.TAG, "Received command: " + command);
				// Send command to board.
				// @ToDo: Queue this up and process it in a separate thread.
				if (command.length() > 0) {
					boolean resultCommand = executeCommand(intent, command);
				}
			}
			if (Constants.SEND_DATA) {
				// Check for staged files and attempt to send them. trySend returns
				// false if there were files to send, but it was unable to do so.
				boolean resultSend = trySend(intent);
			}
			// Sleep for the poll interval.
			long endTime = System.currentTimeMillis() + Constants.POLL_TIME;
	        Log.i(Constants.TAG, "Sleeping for: " + Constants.POLL_TIME);
			while (System.currentTimeMillis() < endTime) {
				synchronized (this) {
					try {
						wait(endTime - System.currentTimeMillis());
					} catch (Exception e) {
					}
				}
			}
		}
		// Not sure if this is the right place to do this, but the doc says Services must manage 
		// their own life cycle.
		// @ToDo: Currently we have no mechanism for telling the service to exit.
		//stopSelf();
	}
	/**
	 * Using onStart() vs. onStartCommand() in Android 1.6
	 * onStart() does not have an int flags param
	 * onStart() does not return an int value
	 */
	@Override
	public void onStart(Intent intent, int startId) {
        Log.d(Constants.TAG, "Polling service onStart called.");
		super.onStart(intent,startId);
	}

	/**
	 * Read one command from the server.
	 * @param intent
	 * @return the command string
	 */
	public String tryGet(Intent intent) {
		// Ask the server for one command.
		// @ToDo: Fetch multiple commands at once, up to a limit.
        Log.i(Constants.TAG, "Requesting command from server.");
		HttpClient httpclient = new DefaultHttpClient();
		//HttpGet httpget = new HttpGet("http://demo.eden.sahanafoundation.org/eden/supply/item/10");
		HttpGet httpget = new HttpGet("http://108.229.97.42:8990/new.html");
    	HttpResponse response = null;
		try {
			response = httpclient.execute(httpget);
		} catch (UnsupportedEncodingException e) {
			// @ToDo error handling
			return "";
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		
		if (response != null) {
			int status = response.getStatusLine().getStatusCode();
			if (status == 200) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
	            StringBuffer sb = new StringBuffer("");
	            String line = "";
	            try {
					while ((line = in.readLine()) != null) {
					            sb.append(line);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return "";
				}
	            try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
	            return new String(sb);
			}
		}
		return "";
	}
	
	/**
	 * Send one command to the Arduino.
	 */
	public boolean executeCommand(Intent intent, String command) {
		return true;
	}
	
	/**
	 * If there are any staged files to send, make the attempt.
	 * @return true if all staged files were sent.
	 */
	public boolean trySend(Intent intent) {
        Log.i(Constants.TAG, "Sending data to server.");
        // Look for work to do. (Reopen directory on each pass in case
        // new work has come in.)
        File staging = new File("/sdcard/" + Constants.STAGING_DIRECTORY);
        File[] files = staging.listFiles();
        if (files == null) {
        	// No staged files.
        	return true;
        }

        // Check network connectivity. (This does not detect a downstream
        // network disruption -- just whether the phone has a local
        // network connection.)
        if (!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
        	return false;
        }

        // Currently we just send each file separately - we don't assemble
        // multiple files for one request. If we identify images and videos
        // in some manner in their files, the server can aggregate them.
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        // @ToDo: Change server.
        HttpPost httppost = new HttpPost("http://108.229.97.42:8990");
        for (File file : files) {
        	try {
        		BufferedInputStream fstream = new BufferedInputStream(new FileInputStream(file));
        		byte[] barray = new byte[Constants.BUFSIZ];
        		StringBuffer sbuffer = new StringBuffer();
        		while (fstream.read(barray, 0, Constants.BUFSIZ) != -1) {
        			sbuffer.append(barray);
        		}
        		fstream.close();
        		StringEntity se = new StringEntity(new String(sbuffer));  
        		httppost.setEntity(se);

        		// Execute HTTP Post Request
        		HttpResponse response = httpclient.execute(httppost);
        		if (response.getStatusLine().getStatusCode() != 200) {
        			return false;
        			// @ToDo: handle errors
        		}
        	} catch (FileNotFoundException e) {
        		throw new RuntimeException(e);
        	} catch (SecurityException e) {
        		throw new RuntimeException(e);
        	} catch (ClientProtocolException e) {
        		throw new RuntimeException(e);
        	} catch (IOException e) {
        		throw new RuntimeException(e);
        	}
        }
        return true;
	}
}
