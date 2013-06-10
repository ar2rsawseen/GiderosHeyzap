package com.giderosmobile.android.plugins.heyzap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;

import android.app.Activity;
import android.util.SparseArray;
import android.view.Gravity;
import com.heyzap.sdk.HeyzapLib;
import com.heyzap.sdk.ads.BannerOverlay;
import com.heyzap.sdk.ads.HeyzapAds;
import com.heyzap.sdk.ads.InterstitialOverlay;
import com.heyzap.sdk.ads.OnAdDisplayListener;

public class GHeyzap implements OnAdDisplayListener{
	
	private static WeakReference<Activity> sActivity;
	private static long sData = 0;
	private static GHeyzap sInstance;
	private static Hashtable<String, Integer> alignVer;
	private static String curVerAlignment = "top";
	private static boolean hasInterstitial = false;
	private static boolean hasBanner = false;
	
	public static void onCreate(Activity activity)
	{
		sActivity =  new WeakReference<Activity>(activity);
		
		alignVer = new Hashtable<String, Integer>();
		alignVer.put("top", Gravity.TOP);
		alignVer.put("bottom", Gravity.BOTTOM);
		alignVer.put("center", Gravity.CENTER_VERTICAL);
	}
	
	//on destroy event
	public static void onDestroy()
	{
		cleanup();
	}
	
	
	public static void init(long data){
		sData = data;
		sInstance = new GHeyzap();
		InterstitialOverlay.setDisplayListener(sInstance);
		BannerOverlay.setDisplayListener(sInstance);
	}
	
	public static void cleanup()
	{
		sData = 0;
		sInstance = null;
		hideAd();
	}
	
	public static void initialize(boolean willUseAds){
		HeyzapLib.load(sActivity.get(), false);
		if(willUseAds){
			HeyzapAds.start(sActivity.get());
		}
	}
	
	//load an Ad
	public static void showAd(final Object parameters)
	{
		SparseArray<String> param = (SparseArray<String>)parameters;
		String type = param.get(0);
		if(type.equals("interstitial"))
		{
			if(hasInterstitial)
			{
				InterstitialOverlay.dismiss();
			}
			InterstitialOverlay.display(sActivity.get());
			hasInterstitial = true;
		}
		else if(type.equals("banner"))
		{
			curVerAlignment = param.get(1, "top");
			if(hasBanner)
			{
				BannerOverlay.dismiss();
			}
			BannerOverlay.display(sActivity.get(), alignVer.get(curVerAlignment));
			hasBanner = true;
		}
	}
	
	//remove ad
	public static void hideAd()
	{
		if(hasInterstitial)
		{
			InterstitialOverlay.dismiss();
			hasInterstitial = false;
		}
		if(hasBanner)
		{
			BannerOverlay.dismiss();
			hasBanner = false;
		}
	}
	
	public static void submitScore(String score, String displayScore, String level){
		HeyzapLib.submitScore(sActivity.get(), score, displayScore, level);
	}
	
	public static void showLeaderboard(){
			HeyzapLib.showLeaderboards(sActivity.get());
	}
	
	public static void showLeaderboard(String levelId){
			HeyzapLib.showLeaderboards(sActivity.get(), levelId);
	}
	
	public static void unlockAchievement(String achievement){
		ArrayList<String> achievementsUnlocked = new ArrayList<String>();
		achievementsUnlocked.add(achievement);
		HeyzapLib.unlockAchievement(sActivity.get(), achievementsUnlocked);
	}
	
	public static void showAchievements(){
		HeyzapLib.showAchievements(sActivity.get());
	}
	
	public static void checkin(){
		HeyzapLib.checkin(sActivity.get());
	}
	
	public static void checkin(String message){
		HeyzapLib.checkin(sActivity.get(), message);
	}
	
	private static native void onAdReceived(long data);
	private static native void onAdFailed(long data);
	private static native void onAdActionBegin(long data);
	private static native void onAdActionEnd(long data);
	private static native void onAdDismissed(long data);

	@Override
	public void onShow() {
		if (sData != 0)
			onAdReceived(sData);
	}

	@Override
	public void onClick() {
		if (sData != 0)
			onAdActionBegin(sData);
	}

	@Override
	public void onHide() {
		if (sData != 0)
			onAdDismissed(sData);
	}

	@Override
	public void onFailedToShow() {
		if (sData != 0)
			onAdFailed(sData);
	}

	@Override
	public void onAvailable() {
	}

	@Override
	public void onFailedToFetch() {
		if (sData != 0)
			onAdFailed(sData);
	}
}
