package com.heyzap.sdk.ads;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import com.heyzap.internal.Connectivity;
import com.heyzap.internal.Logger;

public class InterstitialAd {
    private static volatile HashMap<String, String>ads = new HashMap<String, String>();
    private static volatile HashMap<String, Boolean>loadingAds = new HashMap<String, Boolean>();
    final private static ArrayList<String> CREATIVE_TYPES = new ArrayList<String>(Arrays.asList("interstitial", "full_screen_interstitial", "video", "interstitial_video"));
    final private static ArrayList<String> NO_VIDEO_CREATIVE_TYPES = new ArrayList<String>(Arrays.asList("interstitial", "full_screen_interstitial"));
    
    private static Integer creativeId = 0;
    private static Integer campaignId = 0;
    private static String creativeType = null;
    private static Boolean debugEnabled = false;
    
    private static int AD_UNIT = Manager.AD_UNIT_INTERSTITIAL;
    private static long lastDisplayTimeMillis = 0;
    private static long minimumDisplayIntervalMillis = 5000;
    private static HashMap<String, DisplayRequest> displayRequests = new HashMap<String,DisplayRequest>();
    
    private InterstitialAd() {
    	// Prevent this class from being instantiated
    }
    
    /**
     * Fetch an ad from the ad server. An ad will be not be fetched if there
     * is no internet connection, an ad has already been fetched for the specified tag,
     * or if forced is false.
     * @param tag          the tagged ad to fetch. The fetch will fail of the tag is turned off.
     * @param orientation  an orientation from Configuration (only Configuration.ORIENTATION_LANDSCAPE or Configuration.ORIENTATION_PORTRAIT are accepted).
     * @see HeyzapAds.OnStatusListener
     */
    public static void fetch(String tag, int orientation) {    	
    	if (AdCache.getInstance().has(AD_UNIT, tag)) {
    		return;
    	}
    	
		FetchRequest request = createRequest(tag, true, orientation);
		FetchManager.getInstance().fetch(Manager.applicationContext, request, new FetchManager.OnFetchResponse() {
			@Override
			public void onFetchResponse(AdModel model, String tag, Throwable e) {
				
			}
		});
    }
    
    
    /**
     * Fetch an ad from the ad server. An ad will be not be fetched if there
     * is no internet connection, an ad has already been fetched for the specified tag,
     * or if forced is false.
     * 
     * Unless you set the flag {@link HeyzapAds#DISABLE_AUTOMATIC_FETCH} when starting Heyzap, you do not need to
     * fetch before an ad is shown.
     *
     * @param forced	whether to force a fetch in the case when an ad is already available.
     * @param tag       the tagged ad to fetch. The fetch will fail of the tag is turned off.
     * @see       HeyzapAds.OnStatusListener
     */
    @Deprecated
    public static void fetch(boolean forced, String tag) {
    	fetch(tag, 0);
    }
    /**
     * Fetch an ad from the ad server. An ad will be not be fetched if there
     * is no internet connection or an ad has already been fetched.
     * 
     * Unless you set the flag {@link HeyzapAds#DISABLE_AUTOMATIC_FETCH} when starting Heyzap, you do not need to
     * fetch before an ad is shown.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onAvailable(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFetchFailed(String)}
     */
    public static void fetch() {
    	fetch(AdModel.DEFAULT_TAG_NAME, 0);
    }
    
    /**
     * Fetch an ad from the ad server. An ad will be not be fetched if there
     * is no internet connection or an ad has already been fetched.
     * 
     * Unless you set the flag {@link HeyzapAds#DISABLE_AUTOMATIC_FETCH} when starting Heyzap, you do not need to
     * fetch before an ad is shown.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onAvailable(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFetchFailed(String)}
     */
    public static void fetch(String tag) {
    	fetch(tag, 0);
    }
    
    /**
     * Check if an ad is immediately available to show.
     * @return    <code>true</code> if the ad is completely loaded and ready to be shown.
     * @see       {@link HeyzapAds.OnStatusListener#onAvailable(String)}
     */
    public static Boolean isAvailable() {
    	return isAvailable(AdModel.DEFAULT_TAG_NAME);
    }
    
    /**
     * Checks if an ad is available. If an ad is immediately available to show for the specified tag.
     * @param tag The tag used to fetch the ad.
     * @return    <code>true</code> if an ad is completely loaded and ready to be shown.
     * @see       {@link HeyzapAds.OnStatusListener#onAvailable(String)}
     */
    public static Boolean isAvailable(String tag) {
    	if (Connectivity.isConnected(Manager.applicationContext)) {
    		return AdCache.getInstance().has(AD_UNIT, tag);
    	}
        
        return false;
    }
    
    /**
     * Display an ad. If the ad has not been fetched, one will be fetched for the specified tag. 
     * Please be warned that unless the ad has been pre-fetched, there will be some delay between
     * calling display and the ad showing on the screen.
     * 
     * If the tag has been turned off on the Heyzap dashboard, you will receive both a
     * onFetchFailed and a onFailedToShow callback.
     * 
     * @param activity The currently visible Activity.
     * @param tag The tag used to fetch the ad.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFailedToShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFetchFailed(String)}
     */
	public static void display(final Activity activity, final String tag) {
		// No display with no internet
		if (!Connectivity.isConnected(activity)) {
			if (Manager.displayListener != null) Manager.displayListener.onFailedToShow(tag);
			return;
		}
        
        // Start HeyzapAds if it hasn't been started yet
        if (!HeyzapAds.hasStarted()) {
        	HeyzapAds.start(activity, HeyzapAds.DISABLE_FIRST_AUTOMATIC_FETCH);
        }
    	
    	if ((System.currentTimeMillis() - lastDisplayTimeMillis) < minimumDisplayIntervalMillis) {
    		return;
    	} else {
    		lastDisplayTimeMillis = System.currentTimeMillis();
    	}
    	
    	final AdModel ad = AdCache.getInstance().peek(AD_UNIT, tag);
    	
    	if (ad == null) {
    		FetchRequest request = createRequest(tag, false, Configuration.ORIENTATION_UNDEFINED);
    		FetchManager.getInstance().fetch(Manager.applicationContext, request, new FetchManager.OnFetchResponse() {
				
				@Override
				public void onFetchResponse(AdModel model, String tag, Throwable e) {
					if (e != null) {
						if (Manager.displayListener != null) Manager.displayListener.onFailedToShow(tag);
					} else {
						lastDisplayTimeMillis = 0;
						InterstitialAd.display(activity, tag);
					}
				}
			});
    		
    	} else {
			final Class klass;
    		if (ad.getFormat() == VideoModel.FORMAT) {
    			klass = HeyzapVideoActivity.class;
    		} else {
    			klass = HeyzapInterstitialActivity.class;
    		}
    		
	    	((Activity)activity).runOnUiThread(new Runnable() {
				@Override
				public void run() {
		    		Intent intent = new Intent(activity, klass);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    		intent.putExtra(AbstractActivity.ACTIVITY_INTENT_IMPRESSION_KEY, ad.getImpressionId());
		    		intent.putExtra(AbstractActivity.ACTIVITY_INTENT_CONTEXT_KEY, AD_UNIT);
		    		intent.putExtra(AbstractActivity.ACTIVITY_INTENT_ACTION_KEY, AbstractActivity.ACTIVITY_ACTION_SHOW);
		    		intent.putExtra(AbstractActivity.ACTIVITY_INTENT_ORIGINAL_ORIENTATION, activity.getResources().getConfiguration().orientation);
		    		activity.startActivity(intent);
				}
	    	});
    	}
    }

    /**
     * Display an ad. If the ad has not been fetched, one will be fetched. Please be warned that
     * unless the ad has been pre-fetched, there will be some delay between
     * calling display and the ad showing on the screen.
     * 
     * @param activity The currently visible Activity.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onShow(String tag)}
     * @see       {@link HeyzapAds.OnStatusListener#onFailedToShow(String tag)}
     */
    public static void display(final Activity activity) {
        display(activity, AdModel.DEFAULT_TAG_NAME);
    }

    /**
     * Dismiss a currently visible ad.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onHide(String)}
     */
    public static void dismiss() {
    	if (Manager.lastActivity != null) {
    		Manager.lastActivity.onHide();
    	}
    }
    
    /**
     * Display an ad. If the ad has not been fetched, one will be fetched. Please be warned that
     * unless the ad has been pre-fetched, there will be some delay between
     * calling display and the ad showing on the screen.
     * 
     * @param context The currently visible Activity.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFailedToShow(String)}
     */
    @Deprecated
    public static void display(Context context) {
    	display((Activity)context);
    }
    
    /**
     * Display an ad. If the ad has not been fetched, one will be fetched. Please be warned that
     * unless the ad has been pre-fetched, there will be some delay between
     * calling display and the ad showing on the screen.
     * 
     * If the tag has been turned off on the Heyzap dashboard, you will receive both a
     * {@link HeyzapAds.OnStatusListener#onFailedToFetch(String)} and a {@link HeyzapAds.OnStatusListener#onFailedToShow(String)} callback.
     * 
     * @param context The currently visible Activity.
     * @param tag     The tagged ad fetched or to be fetched.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFailedToShow(String)}
     */
    @Deprecated
    public static void display(Context context, String tag) {
    	display((Activity)context, tag);
    }
    
    /**
     * Register a status listener for all ads.
     * 
     * @param listener A class implementing the <code>HeyzapAds.OnStatusListener</code> interface.
     * 
     * @see       {@link HeyzapAds.OnStatusListener}
     * @see       {@link HeyzapAds#setOnStatusListener(com.heyzap.sdk.ads.HeyzapAds.OnStatusListener)}
     */
    
    @Deprecated
    public static void setDisplayListener(HeyzapAds.OnStatusListener listener) {
    	HeyzapAds.setOnStatusListener(listener);
    }
    
    /** @hide @exclude */
    private static FetchRequest createRequest(String tag, Boolean withVideo, int orientation) {
    	
    	ArrayList<String> creativeTypes;
    	
    	if (creativeType != null) {
    		creativeTypes = new ArrayList<String>(Arrays.asList(creativeType));
    	} else {
    		creativeTypes = withVideo ? CREATIVE_TYPES : NO_VIDEO_CREATIVE_TYPES;
    	}
    	
		FetchRequest request = new FetchRequest(AD_UNIT, tag, creativeTypes);
		if (debugEnabled) {
			request.setDebugEnabled(debugEnabled);
			request.setRandomStrategyEnabled(true);
		}
		
		if (orientation > Configuration.ORIENTATION_UNDEFINED) {
			HashMap<String, String> additionalParams = new HashMap<String, String>();
			switch(orientation) {
				case Configuration.ORIENTATION_LANDSCAPE:
					additionalParams.put("orientation", "landscape");
					break;
				case Configuration.ORIENTATION_PORTRAIT:
					additionalParams.put("orientation", "portrait");
					break;
			}
			
			request.setAdditionalParams(additionalParams);
		}
		
		request.setCreativeId(creativeId);
		request.setCampaignId(campaignId);
		
		return request;
    }
    
    /** @hide @exclude */
    public static void setCreativeId(int cid) {
    	creativeId = cid;
    }
    
    /** @hide @exclude */
    public static void setCampaignId(int cid) {
    	campaignId = cid;
    }

    /** @hide @exclude */
    public static void setTargetCreativeType(String type) {
        creativeType = type;
    }
    
    private static class DisplayRequest {
    	long validUntil;
    	WeakReference<Activity> activityRef;
    	String tag;
    	
    	public DisplayRequest(Activity context, String tag, int ttl) {
    		this.activityRef = new WeakReference<Activity>(context);
    		this.tag = tag;
    		this.validUntil = System.currentTimeMillis() + ttl;
    	}
    	
    	public void displayIfValid(){
    		Activity activity = activityRef.get();
    		if(activity != null && System.currentTimeMillis() < validUntil){
    			InterstitialAd.display(activity, tag);
    		}
    	}
    }
}