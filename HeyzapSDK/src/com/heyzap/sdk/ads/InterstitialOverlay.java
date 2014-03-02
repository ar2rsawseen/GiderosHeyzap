package com.heyzap.sdk.ads;

import android.app.Activity;
import android.content.Context;

@Deprecated
public class InterstitialOverlay {
	
	public static void fetch(boolean forced, String tag) {
		InterstitialAd.fetch(forced, tag);
	}
	
	public static void fetch() {
		InterstitialAd.fetch();
	}
	
	public static void fetch(String tag) {
		InterstitialAd.fetch(tag);
	}
	
	public static Boolean isAvailable() {
		return InterstitialAd.isAvailable();
	}
	
	public static Boolean isAvailable(String tag) {
		return InterstitialAd.isAvailable(tag);
	}
	
	public static void display(final Context context, final String tag) {
		InterstitialAd.display((Activity)context, tag);
	}
	
	public static void display(final Context context) {
		InterstitialAd.display((Activity)context);
	}
	
	public static void dismiss() {
		InterstitialAd.dismiss();
	}
	
	public static void setDisplayListener(HeyzapAds.OnStatusListener listener) {
		HeyzapAds.setOnStatusListener(listener);
	}
}
