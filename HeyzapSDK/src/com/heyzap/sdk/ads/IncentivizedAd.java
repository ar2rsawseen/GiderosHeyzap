package com.heyzap.sdk.ads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;

import com.heyzap.internal.Connectivity;

public class IncentivizedAd {
    private static volatile HashMap<String, String>ads = new HashMap<String, String>();
    private static volatile HashMap<String, Boolean>loadingAds = new HashMap<String, Boolean>();
    final private static ArrayList<String> CREATIVE_TYPES = new ArrayList<String>(Arrays.asList("video", "interstitial_video"));
    
    private static String userIdentifier = null;
    private static Integer creativeId = 0;
    private static Integer campaignId = 0;
    private static String creativeType = null;
    private static Boolean debugEnabled = false;
    
    private static int AD_UNIT = Manager.AD_UNIT_INCENTIVIZED;
    private static long lastDisplayTimeMillis = 0;
    private static long minimumDisplayIntervalMillis = 5000;
    
    private IncentivizedAd() {
    	// Prevent this class from being instantiated
    }
    
    /**
     * Fetch an ad from the ad server. An ad will be not be fetched if there
     * is no internet connection, an ad has already been fetched for the specified tag,
     * or if forced is false.
     * 
     * Unlike {@link InterstitialAd}, you must fetch before showing an ad.
     *
     * @param forced	whether to force a fetch in the case when an ad is already available.
     * @see       HeyzapAds.OnStatusListener#onAvailable(String)
     * @see       HeyzapAds.OnStatusListener#onFetchFailed(String)
     */
    public static void fetch(boolean forced) {
    	
    	if (AdCache.getInstance().has(AD_UNIT, AdModel.DEFAULT_TAG_NAME)) return;
    	
		FetchRequest request = createRequest(AdModel.DEFAULT_TAG_NAME);
		
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
     * Unlike {@link InterstitialAd}, you must fetch before showing an ad.
     * 
     * @see       HeyzapAds.OnStatusListener#onAvailable(String)
     * @see       HeyzapAds.OnStatusListener#onFetchFailed(String)
     */
    public static void fetch() {
    	fetch(false);
    }

    /**
     * Check if an ad is immediately available to show.
     * @return    <code>true</code> if the ad is completely loaded and ready to be shown.
     * @see       {@link HeyzapAds.OnStatusListener#onAvailable(String)}
     */
    public static Boolean isAvailable() {
    	final String tag = AdModel.DEFAULT_TAG_NAME;
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
     * In addition to the normal the normal {@link HeyzapAds.OnStatusListener} callbacks, {@link HeyzapAds.OnIncentiveResultListener} should be used
     * to listen to callbacks for when an ad has been completed or is incomplete.
     * 
     * @param activity The currently visible Activity.
     * 
     * @see       {@link HeyzapAds.OnStatusListener#onShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFailedToShow(String)}
     * @see       {@link HeyzapAds.OnStatusListener#onFetchFailed(String)}
     * @see		  {@link HeyzapAds.OnIncentiveResultListener#onComplete}
     * @see 	  {@link HeyzapAds.OnIncentiveResultListener#onIncomplete}
     */
	public static void display(final Activity activity) {
		// No display with no internet
		if (!Connectivity.isConnected(activity)) {
			if (Manager.displayListener != null) Manager.displayListener.onFailedToShow(AdModel.DEFAULT_TAG_NAME);
			return;
		}
        
        // Start HeyzapAds if it hasn't been started yet
        if (!HeyzapAds.hasStarted()) {
        	HeyzapAds.start(activity);
        }
        
    	if ((System.currentTimeMillis() - lastDisplayTimeMillis) < minimumDisplayIntervalMillis) {
    		return;
    	} else {
    		lastDisplayTimeMillis = System.currentTimeMillis();
    	}
    	
    	final AdModel ad = AdCache.getInstance().peek(AD_UNIT, AdModel.DEFAULT_TAG_NAME);
    	if (ad != null) {
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
	    			activity.startActivity(intent);
				}
	    	});
    	} else if (Manager.displayListener != null) {
    		Manager.displayListener.onFailedToShow(AdModel.DEFAULT_TAG_NAME);
    	}
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
     * Set a unique user identifier. When this is included, it will be included as part of the server-to-server
     * callback (set on the Heyzap dashboard) which will inform you if a user has completed an incentive.
     * 
     * In the case where you are giving away virtual currency for successful completion of an ad, 
     * we recommend matching up the callback from {@link HeyzapAds.OnIncentiveResultListener#onComplete()} with
     * a server-to-server callback.
     * 
     */
    public static void setUserIdentifier(String identifier) {
    	if (identifier != null && identifier.trim().equals("")) {
    		identifier = null;
    	}
    	
    	IncentivizedAd.userIdentifier = identifier;
    }
    
    /** @hide @exclude */
    private static FetchRequest createRequest(String tag) {
    	
    	ArrayList<String> creativeTypes;
    	
    	if (creativeType != null) {
    		creativeTypes = new ArrayList<String>(Arrays.asList(creativeType));
    	} else {
    		creativeTypes = CREATIVE_TYPES;
    	}
    	
		FetchRequest request = new FetchRequest(AD_UNIT, tag, creativeTypes);
		
		if (debugEnabled) {
			request.setDebugEnabled(debugEnabled);
			request.setRandomStrategyEnabled(true);
		}
		
		request.setCreativeId(creativeId);
		request.setCampaignId(campaignId);
		
		HashMap<String, String> additionalParams = new HashMap<String, String>();
		additionalParams.put("orientation", "landscape");
		
		if (userIdentifier != null) {
			additionalParams.put("user_identifier", userIdentifier);

		}
		
		request.setAdditionalParams(additionalParams);
		
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
}