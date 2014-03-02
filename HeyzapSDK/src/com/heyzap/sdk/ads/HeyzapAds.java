package com.heyzap.sdk.ads;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.heyzap.internal.Analytics;
import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

public class HeyzapAds {
	private static boolean enabled = false;
	
	/**
	 * @hide
	 * {@hide} 
	 **/
    public static String framework = null;
	/**
	 * @hide
	 * {@hide} 
	 **/
    public static String mediator = null;

	/**
	 * @hide
	 * {@hide} 
	 **/
    public static String store = "google";
	
    /**
     * Flag to indicate no special options are being used.
    */
    public static final int NONE = 0 << 0;
    /**
    Flag to disable automatic fetching for interstitial ads. Useful for cases
    where there are resource constraints or for cases where a mediation or waterfall
    is being used.
    */
	public static final int DISABLE_AUTOMATIC_FETCH = 1 << 0; // 1
	@Deprecated
	public static final int ADVERTISER_ONLY = 1 << 1; //2
    /**
     * Flag only to be used for tracking app installs by an advertiser. Showing of ads is disabled.
    */
	public static final int INSTALL_TRACKING_ONLY = 1 << 1; //2
    /**
     * Flag to indicate app is being distributed with the Amazon Appstore.
    */
	public static final int AMAZON = 1 << 2; //3
	/*
	 * Disable first auto-fetch
	 */
	public static final int DISABLE_FIRST_AUTOMATIC_FETCH = 1 << 3; //4
	
	/**
	 * Start Heyzap Ads. This needs to be called as early as possible in the application lifecycle.
	 * 
	 * @param activity The current activity.
	 * @param flags A set of flags to modify Heyzap ads.
	 * <ul>
	 * <li>{@link #NONE}
     * <li>{@link #DISABLE_AUTOMATIC_FETCH}
     * <li>{@link #INSTALL_TRACKING_ONLY}
     * <li>{@link #AMAZON}
     * </ul>
	 * @param listener A class which implements the {@link com.heyzap.ads.HeyzapAds.OnStatusListener} interface.
	 * 
	 * 
	 * @see HeyzapAds.OnStatusListener
	 */
	public static void start(final Activity context, int flags, final HeyzapAds.OnStatusListener listener) {
		if (hasStarted()) {
			return;
		}
		
		// Get the flags all setup
		Manager.applicationContext = context.getApplicationContext();
		setOnStatusListener(listener);
		Manager.getInstance(); //instantiate it
		
		if (flags > 0) {
			Manager.getInstance().setFlags(flags);
		}
		
		enabled = true;
		
		// Send an Analytics Event
		Analytics.trackEvent(context, "heyzap-start");
		
		if ((flags & AMAZON) == AMAZON || Utils.isAmazon()) {
		    HeyzapAds.store = "amazon";
		}
		
		if (!Manager.getInstance().isFlagEnabled(DISABLE_FIRST_AUTOMATIC_FETCH) && !Manager.getInstance().isFlagEnabled(DISABLE_AUTOMATIC_FETCH) && !Manager.getInstance().isFlagEnabled(INSTALL_TRACKING_ONLY)) {
			InterstitialAd.fetch();
		}
	}
	
	/**
	 * Start Heyzap Ads. This needs to be called as early as possible in the application lifecycle.
	 * 
	 * @param activity The current activity.
	 * @param listener A class which implements the {@link com.heyzap.ads.HeyzapAds.OnStatusListener} interface.
	 * 
	 * @see HeyzapAds.OnStatusListener
	 */
	public static void start(final Activity activity, final OnStatusListener listener) {
		start(activity, 0, listener);
	}
	
	/**
	 * Enable Heyzap Ads. This needs to be called as early as possible in the application lifecycle.
	 * 
	 * @param activity The current activity.
	 * @param flags Flags which change certain behaviors of Heyzap Ads.
	 */
	public static void start(final Activity activity, int flags) {
		start(activity, flags, null);
	}
	
	/**
	 * Start Heyzap Ads. This needs to be called as early as possible in the application lifecycle.
	 * 
	 * @param activity The current activity.
	 */
	public static void start(final Activity activity) {
		start(activity, 0, null);
	}
	
	@Deprecated
	public static void start(Context context, int flags, OnStatusListener listener) { start((Activity)context, NONE, listener); };
	
	@Deprecated
	public static void start(Context context, OnStatusListener listener) { start((Activity)context, NONE, listener); };
	
	@Deprecated
	public static void start(Context context) { start((Activity)context, NONE, null); };
	
	@Deprecated
	public static void start(Context context, int flags) { start((Activity)context, flags, null); };
	
	/**
	 * Checks if Heyzap has been started.
	 * 
	 * @return true if Heyzap has been started.
	 */
	public static Boolean hasStarted() {
		return enabled && Manager.isStarted();
	}
	
	/**
	 * Register an ad status callback. The listener needs to implement the {@link HeyzapAds.OnStatusListener#} interface. 
	 * 
	 * @param listener The listener which implements {@link HeyzapAds.OnStatusListener#}.
	 * @see {@link HeyzapAds.OnStatusListener }
	 */
	public static void setOnStatusListener(HeyzapAds.OnStatusListener listener) {
		Manager.displayListener = listener;
	}
	
	/**
	 * Register an incentivized ad result callback. This listener is used in connection with {@link IncentivizedAd} to send
	 * a callback when an incentivized ad has been completed.
	 * 
	 * @param listener The listener which implements {@link HeyzapAds.OnIncentiveResultListener#}.
	 * @see {@link HeyzapAds.OnIncentiveResultListener }
	 */
	public static void setOnIncentiveResultListener(HeyzapAds.OnIncentiveResultListener listener) {
		Manager.incentiveListener = listener;
	}
	
	public static void changeServer(String url) {
	    Manager.AD_SERVER = url;
	}
	
	public interface OnStatusListener {
		/**
		 * Called when an ad is shown to the user.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onShow(String tag);
		/**
		 * Call when an ad is clicked on by the user. Expect the application to
		 * go into the background shortly after this callback is fired.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onClick(String tag);
		/**
		 * Called when an ad has been hidden.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onHide(String tag);
		/**
		 * Called when an ad failed to be shown. This can be due to a variety of reasons, such as
		 * an internal error in Heyzap or no internet connection. May be called in connection with
		 * {@link #onFailedToFetch(String)}.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onFailedToShow(String tag);
		/**
		 * Called when an ad has been successfully fetched and is available to show.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onAvailable(String tag);
		/**
		 * Called when an ad could not be fetched from the ad server. This could be for a variety
		 * of reasons, such as poor internet connectivity, the ad server choosing not to fill the ad,
		 * or a tag being disabled.
		 * 
		 * @param tag The tag associated with the ad.
		 */
		public void onFailedToFetch(String tag);
		/**
		 * Called when an ad being shown will be using audio. This callback is ideal in the case
		 * where you want to shut off the app's background music. 
		 */
		public void onAudioStarted();
		/*
		 * Called when an ad being shown has finished using audio. This callback is ideal in the case where you want to
		 * restart background music after having shut it off.
		 */
		public void onAudioFinished();
	}
	
	public interface OnIncentiveResultListener {
		/**
		 * Called when an incentivized ad using {@link IncentivizedAd} has been completed.
		 */
		public void onComplete();
		/**
		 * Called when an incentivized ad using {@link IncentivizedAd} was not completed.
		 */
		public void onIncomplete();
	}
}
