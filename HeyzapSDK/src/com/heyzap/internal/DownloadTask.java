package com.heyzap.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;

public class DownloadTask extends AsyncTask<String, Integer, String> {

    private Context context;
    public StateListener stateListener;
    public long startTime;
    public URL url;
    
    public DownloadTask(Context context) {
        this.context = context;
    }
    
    public DownloadTask(Context context, StateListener listener) {
    	this.context = context;
    	this.stateListener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
    	if (params.length == 0) {
    		return null;
    	}
    	
    	URL url = null;
    	
    	String urlString = params[0];
    	String outputPathString = params[1];
    	
        // take CPU lock to prevent CPU from going off if the user 
        // presses the power button during download
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//             getClass().getName());
//        wl.acquire();

        try {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            
            try {
                this.url = new URL(urlString);
                connection = (HttpURLConnection) this.url.openConnection();
                connection.connect();
                
                this.startTime = System.currentTimeMillis();

                // expect HTTP 200 OK, so we don't mistakenly save error report 
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                	if (this.stateListener != null) {
                		this.stateListener.onError(this, this.url, new Throwable("HTTP " + connection.getResponseCode() 
                         + " " + connection.getResponseMessage()));
                	}
                    
                	return "Server returned HTTP " + connection.getResponseCode() 
                         + " " + connection.getResponseMessage();
                }
                
                if (this.stateListener != null) {
                	this.stateListener.onStarted(this.url);
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                
                File file = new File(this.context.getCacheDir() + "/" + outputPathString);
                
                // Clear existing file and re-open
                if (file.exists()) {
                	file.delete();
                	file = new File(this.context.getCacheDir() + "/" + outputPathString);
                }
                
                output = new FileOutputStream(file, false);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                    	if (this.stateListener != null) {
                    		this.stateListener.onCancelled(this.url);
                    	}
                    	
                    	input.close();
                    	output.close();
                    	// is this correct?
                    	throw new Exception("cancelled");
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                
            } catch (Exception e) {

            	if (!e.getMessage().equals("cancelled")) {
                	e.printStackTrace();
                	
            		if (this.stateListener != null) {
            			this.stateListener.onError(this, this.url, e);
            		}
            	}
            	
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } 
                catch (IOException ignored) { }
                
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	
        } finally {
        	
        }
        
        if (this.stateListener != null) {
        	this.stateListener.onComplete(this.url, outputPathString, System.currentTimeMillis() - this.startTime);
        }
        
        return null;
    }
    
    @Override
    protected void onProgressUpdate(Integer... progress) {
    	if (this.stateListener != null) {
    		this.stateListener.onProgress(progress[0]);
    	}
    }
    
    @Override
    protected void onPostExecute(String result) {
    	
    }
    
    @Override
    protected void onCancelled() {
    	if (this.stateListener != null) {
    		this.stateListener.onCancelled(this.url);
    	}
    }
    
    public interface StateListener {
    	public void onStarted(URL url);
    	public void onComplete(URL url, String pathName, long downloadTime);
    	public void onError(DownloadTask task, URL url, Throwable e);
    	public void onCancelled(URL url);
    	public void onProgress(Integer progress);
    }
}