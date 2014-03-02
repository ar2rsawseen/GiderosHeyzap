package com.heyzap.sdk.ads;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.view.ViewGroup.LayoutParams;

import com.heyzap.http.RequestParams;
import com.heyzap.internal.APIClient;
import com.heyzap.internal.APIResponseHandler;
import com.heyzap.internal.DownloadTask;
import com.heyzap.internal.Logger;

class VideoModel extends AdModel {
	
	public static String FORMAT = "video";
	private Context context;
	
	private Boolean sentVideoComplete = false;
	
	//Interstitial
	private String interstitialHtmlData;
	private Integer interstitialBackgroundOverlayColor = Color.TRANSPARENT;
	private int interstitialHeight = LayoutParams.MATCH_PARENT;
	private int interstitialWidth = LayoutParams.MATCH_PARENT;
	private Boolean manualSize;
	private Boolean disableGlobalTouch;
	
	//Video specific
	private ArrayList<String> staticUrls = new ArrayList<String>();
	private ArrayList<String> streamingUrls = new ArrayList<String>();
	private Integer videoWidth = 0;
	private Integer videoHeight = 0;
	private Integer videoLength = 0;
	private Integer lockoutTime = 0;
	private Boolean allowSkip = true;
	private Boolean allowHide = false;
	private Boolean allowClick = true;
	private Boolean postRollInterstitial = false;
	private String cachedVideoPath = null;
	private DownloadTask downloadTask = null;
	
	public VideoModel(JSONObject response) throws Exception, JSONException {
		super(response);
		this.context = context;
		this.creativeType = VideoModel.FORMAT;
		        
        if (response.has("interstitial")) {
        	JSONObject interstitialObj = response.getJSONObject("interstitial");
        	if (interstitialObj.has("meta")) {
        		JSONObject interstitialMeta = interstitialObj.getJSONObject("meta");
        		this.interstitialHeight = interstitialMeta.optInt("height", this.interstitialHeight);
        		this.interstitialWidth = interstitialMeta.optInt("width", this.interstitialWidth);
        	}
        	
        	this.interstitialHtmlData = interstitialObj.getString("html_data");
        	this.interstitialBackgroundOverlayColor = interstitialObj.optInt("background_color", this.interstitialBackgroundOverlayColor);
        }
        
        if (response.has("video")) {
        	JSONObject videoObj = response.getJSONObject("video");
        	if (videoObj.has("meta")) {
        		JSONObject videoMeta = videoObj.getJSONObject("meta");
        		this.videoWidth = videoMeta.optInt("width", this.videoWidth);
        		this.videoHeight = videoMeta.optInt("height", this.videoHeight);
        		this.videoLength = videoMeta.optInt("length", this.videoLength);
        	}
        	
    		this.lockoutTime = videoObj.optInt("lockout_time", 0);
    		this.allowSkip = videoObj.optBoolean("allow_skip", this.allowSkip);
    		this.allowHide = videoObj.optBoolean("allow_hide", this.allowHide);
    		this.allowClick = videoObj.optBoolean("allow_click", this.allowClick);
    		this.postRollInterstitial = videoObj.optBoolean("post_roll_interstitial", this.postRollInterstitial);
    		
    		if (videoObj.has("static_url")) {
    			JSONArray staticUrls = videoObj.getJSONArray("static_url");			
    			for (int i = 0; i < staticUrls.length(); i++) {
    				String url = staticUrls.getString(i);
    				this.staticUrls.add(url);
    			}
    		}
    		
    		if (videoObj.has("streaming_url")) {
    			JSONArray streamingUrls = videoObj.getJSONArray("streaming_url");
    			for (int i = 0; i < streamingUrls.length(); i++) {
    				String url = streamingUrls.getString(i);
    				this.streamingUrls.add(url);
    			}
    		}
    		
    		if (this.staticUrls.size() == 0 && this.streamingUrls.size() == 0) {
    			throw new Exception("No video URLs.");
    		}
        }
	}
	
	/* Post-Roll Interstitial */
	
	public String getHtmlData() {
		return this.interstitialHtmlData;
	}
	
	public int getInterstitialWidth() {
		return this.interstitialWidth;
	}
	
	public int getInterstitialHeight() {
		return this.interstitialHeight;
	}
	
	public int getInterstitialBackgroundOverlayColor() {
		return this.interstitialBackgroundOverlayColor;
	}
	
	/* Video */
	
	public Boolean allowSkip() {
		return this.allowSkip;
	}
	
	public Boolean allowHide() {
		return this.allowHide;
	}
	
	public Boolean allowClick() {
		return this.allowClick;
	}
	
	public int getSkipLockoutTime() {
		return this.lockoutTime;
	}
	
	public Boolean showPostRollInterstitial() {
		return this.postRollInterstitial;
	}
	
	/* Video Caching */
	
	public String getCachedPath() {
		return this.cachedVideoPath;
	}
	
	public Uri getStreamingUri() {
		if (this.streamingUrls.size() > 0) {
			return Uri.parse(this.streamingUrls.get(0));
		}
		
		return null;
	}
	
	public void downloadVideo(final Context context, final ModelPostFetchCompleteListener listener) {
		
		if (this.staticUrls.size() == 0 && this.streamingUrls.size() == 0) {
			//This is bad.
			if (listener != null) listener.onComplete(null, new Throwable("no_video"));
			return;
		} else if (this.staticUrls.size() == 0) {
			if (listener != null) listener.onComplete(this, null);
			return;
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();
				try {
					// No cache directory? Let's make it.
					File file = new File(Manager.applicationContext.getCacheDir() + "/heyzap");
					if (!file.exists()) {
						file.mkdirs();
					}
					
					final String videoPath = "heyzap/video-" + VideoModel.this.getImpressionId();
					
					VideoModel.this.downloadTask = new DownloadTask(context, new DownloadTask.StateListener() {
						@Override
						public void onProgress(Integer progress) {
							
						}
						
						@Override
						public void onStarted(URL url) {
							Logger.format("(DOWNLOADING) %s", VideoModel.this);
						}
						
						@Override
						public void onError(DownloadTask task, URL url, Throwable e) {
							Logger.format("(DOWNLOAD ERROR) Error: %s %s", e.toString(), VideoModel.this);
							
							// Just in case this is our fault, dump the cache when we get this error.
							if (e.getMessage().equals("No space left on device")) {
								Logger.log("Dumping caches.");
								Manager.getInstance().clearAndCreateFileCache();
							}
							
							if (listener != null) listener.onComplete(null, e);
							
							
							
							// Let's get rid of that static URL and try from the top
//							Video.this.staticUrls.remove(url);
//							if (Video.this.staticUrls.size() > 0) {
//								Video.this.cacheVideo();
//							} else {
//								// OK we have nothing!
//							}
						}
						
						@Override
						public void onComplete(URL url, String pathName, long timeInMillis) {
							int seconds = (int)(timeInMillis / 1000L);
							Logger.format("(CACHED) %s in %s seconds", VideoModel.this, String.valueOf(seconds));
							VideoModel.this.cachedVideoPath = pathName;
							
							File file = new File(context.getCacheDir() + "/" + pathName);
							if (file.exists()) {
								if (listener != null) listener.onComplete(VideoModel.this, null);
							} else {
								Exception exception = new Exception("File does not exist.");
								if (listener != null) listener.onComplete(null, exception);
							}
						}
						
						@Override
						public void onCancelled(URL url) {
							Logger.format("(CANCELLED) %s", VideoModel.this);
							
							File someFile = new File(context.getCacheDir() + "/" + videoPath);
							someFile.delete();
						}
					});
					
					if (VideoModel.this.staticUrls.size() > 0) {
						VideoModel.this.downloadTask.execute(VideoModel.this.staticUrls.get(0), videoPath);
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (listener != null) listener.onComplete(null, e);
				}
			}
		
		}).start();
	}
	
	public void cancelDownload() {
		if (this.downloadTask != null) {
			this.downloadTask.cancel(true);
		}
	}
	
	public void cleanup() throws Exception {
		Logger.log("(CLEANUP) " + this.getImpressionId());
		this.cancelDownload();
		
		File dir = Manager.applicationContext.getCacheDir();
		if (this.cachedVideoPath != null) {
			File file = new File(dir, this.cachedVideoPath);
			if (file.exists() && !file.delete()) {
				throw new Exception("Failed to delete file!");
			}
		}
	}
	
	
	public Boolean onComplete(Boolean incentivized, int totalTimeWatched, int totalVideoDuration, Boolean videoComplete) {
		if (this.sentVideoComplete == true) {
			Logger.log("Already sent video complete successfully");
			return false;
		}
		
        RequestParams params = new RequestParams();
        params.put("impression_id", getImpressionId());
        params.put("promoted_android_package", getGamePackage());
        params.put("promoted_game_package", getGamePackage());
        params.put("video_duration_seconds", totalVideoDuration);
        params.put("watched_duration_seconds", totalTimeWatched);
        
        String finished = videoComplete ? "true" : "false";
        params.put("video_finished", finished);
        
        int lockoutTimeSeconds = (int)(this.lockoutTime / 1000.0);
        params.put("lockout_time_seconds", lockoutTimeSeconds);
        
        params.put("tag", this.getTag());
        
        if (incentivized) {
        	params.put("incentivized", "true");
        }
		
        APIClient.post(Manager.applicationContext, Manager.EVENT_SERVER + "/event/video_impression_complete", params, new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                        Logger.format("(COMPLETE) %s", VideoModel.this);
                        VideoModel.this.sentVideoComplete = true;
                    }
                } catch (JSONException e) {
                	
                }
            }
        });
        
        return true;
	}

	@Override
	public void doPostFetchActions(Context context,
			ModelPostFetchCompleteListener listener) {
		this.downloadVideo(context, listener);
	}
}
