package com.heyzap.sdk.ads;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.DisplayMetrics;

import com.heyzap.http.RequestParams;
import com.heyzap.internal.APIClient;
import com.heyzap.internal.APIResponseHandler;
import com.heyzap.internal.Connectivity;
import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;
import com.heyzap.sdk.ads.HeyzapAds.OnIncentiveResultListener;
import com.heyzap.sdk.ads.HeyzapAds.OnStatusListener;


class Manager {
	public static Context applicationContext;
	
	public long lastClickedTime = 0;
	public static long maxClickDifference = 1000;
	
	public static String AD_SERVER = "http://ads.heyzap.com/in_game_api/ads";
	public static String EVENT_SERVER = "http://ads.heyzap.com/in_game_api/ads";
	public static final String ACTION_URL_PLACEHOLDER = "market://details?id=%s&referrer=%s";
	public static final String ACTION_URL_REFERRER = "utm_source%3Dheyzap%26utm_medium%3Dmobile%26utm_campaign%3Dheyzap_ad_network";

	public static final Handler handler = new Handler(Looper.getMainLooper());
	
	public static final String FIRST_RUN_KEY = "HeyzapAdsFirstRun";
	
	public static Boolean started = false;
	private int flags = 0;
    
    final private static String PREFERENCES_KEY = "com.heyzap.sdk.ads";  
    
    final public static int AD_UNIT_UNKNOWN      = 0;
    final public static int AD_UNIT_INTERSTITIAL = 1;
    final public static int AD_UNIT_INCENTIVIZED = 2;
    final public static int AD_UNIT_VIDEO        = 3;
    
    public static OnStatusListener displayListener;
    public static OnIncentiveResultListener incentiveListener;
    
    public static AbstractActivity lastActivity = null;
	
	// Standard Singleton Creation
	private Manager() {
		Logger.log("Heyzap Ad Manager started.");

		final SharedPreferences prefs = Manager.applicationContext.getSharedPreferences(Manager.PREFERENCES_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		final boolean firstRun = !prefs.getBoolean("ran_once", false);
		
		// If this is our first run then we need to get a list of all the
		// installed packages on the system
		if (firstRun) {
			Logger.log("Running first run tasks");
			this.registerSelfInstall();
			editor.putBoolean("ran_once", true);
			editor.commit();
		}
		
		this.clearAndCreateFileCache();
		
		Manager.started = true;
	};
	
	static public Boolean isStarted() {
		return Manager.started;
	}
	
	public void registerSelfInstall() {
		if (Manager.applicationContext == null) {
			return;
		}
		
		RequestParams reqParameters = new RequestParams();
		
		if (Utils.isAmazon()) {
		    reqParameters.put("platform", "amazon");
		} else {
		    reqParameters.put("platform", "android");
		}
		
		final String packageName = Utils.getPackageName(Manager.applicationContext);
		reqParameters.put("for_game_package", packageName);
		
		APIClient.post(Manager.applicationContext, EVENT_SERVER + "/register_new_game_install", reqParameters,  new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                    	Logger.log("(SELF INSTALL) Package: " + packageName);
                    } else {
                    	
                    }
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
        });
	}
	
	public void registerInstall(final String packageName) {
		if (Manager.applicationContext == null) return;
		
		// Tell ad server we did an install
		RequestParams reqParameters = new RequestParams();
		
		if (Utils.isAmazon()) {
		    reqParameters.put("platform", "amazon");
		} else {
		    reqParameters.put("platform", "android");
		}
		
		final String associatedImpressionId = getImpression(packageName);
		if (associatedImpressionId == null) {
			return;
		}
		
		reqParameters.put("impression_id", associatedImpressionId);

		APIClient.post(Manager.applicationContext, EVENT_SERVER + "/track_impression_event", reqParameters,  new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                    	Logger.log("(INSTALL) " + associatedImpressionId);
                    	Manager.this.removeImpression(packageName);
                    } else {
                    	
                    }
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            
            @Override
            public void onFailure(final Throwable e) {
            	Logger.log(e);
            }
        });
	}
	
	public List<String> getLocalPackages() {
		if (Manager.applicationContext == null) {
			return null;
		}
		
		List<PackageInfo> packages = Manager.applicationContext.getPackageManager()
				.getInstalledPackages(0);
		
		final List<String> packageNames = new ArrayList<String>();
		
		for (PackageInfo packageInfo : packages) {
			if (packageInfo.packageName.startsWith("android.") || 
				packageInfo.packageName.startsWith("com.google.android") ||
				packageInfo.packageName.startsWith("com.android") ||
				packageInfo.packageName.startsWith("com.htc") || 
				packageInfo.packageName.startsWith("com.samsung") || 
				packageInfo.packageName.startsWith("com.sec") ||
				packageInfo.packageName.startsWith("com.monotype") ||
				packageInfo.packageName.startsWith("com.verizon") ||
				packageInfo.packageName.startsWith("com.qualcomm") ||
				packageInfo.packageName.startsWith("com.vzw")) {
				continue;
			}
			
			packageNames.add(packageInfo.packageName);
		}
		
		return packageNames;
	}
	
	protected void installHeyzap(AdModel referringAdModel) {
		String gamePackage = referringAdModel.getGamePackage() == null ? "null" : referringAdModel.getGamePackage();
		installHeyzap(gamePackage);
	}
	
	protected void installHeyzap(String gamePackage) {
		// if a newish Heyzap is installed, launch game details.
		// if an older Heyzap is installed, just launch Heyzap.
		// if Heyzap isn't installed, open it in the market.
		
		if (Utils.heyzapIsInstalled(Manager.applicationContext)) {
			Intent i = new Intent(Intent.ACTION_MAIN); 
			i.setAction(Utils.HEYZAP_PACKAGE);
			i.putExtra("from_ad_for_game_package", gamePackage);
			i.putExtra("packageName", Manager.applicationContext.getPackageName());
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (gamePackage != null) {
				i.setComponent(new ComponentName(Utils.HEYZAP_PACKAGE, Utils.HEYZAP_PACKAGE + ".activity.GameDetails"));
				i.putExtra("game_package", gamePackage);
			} else {
				i.setComponent(new ComponentName(Utils.HEYZAP_PACKAGE, Utils.HEYZAP_PACKAGE + ".activity.CheckinHub"));
			}

			Manager.applicationContext.startActivity(i);
		} else {
			Utils.installHeyzap(Manager.applicationContext, String.format("action=ad_heyzap_logo&game_package=%s", gamePackage));
		}
	}
	
	protected void clearAndCreateFileCache() {
		String directoryPath = Manager.applicationContext.getCacheDir() + "/heyzap";
		try {
			File existingPath = new File(directoryPath);
			if (existingPath.exists()) {
				Utils.deleteDirectory(new File(directoryPath));
			}
			
			File file = new File(directoryPath);
			file.mkdirs();
			file.deleteOnExit();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Flags
	
	protected void setFlags(int flags) {
		this.flags = flags;
	}
	
	protected Boolean isFlagEnabled(int flag) {
		return (this.flags & flag) > 0;
	}
	
	// Impression Management
	
    public String getImpression(String gamePackage) {
        if (Manager.applicationContext == null) return null;
        if (gamePackage == null) return null;
        
        try {
	        String prefKey = "impression." + gamePackage;
	        final SharedPreferences prefs = Manager.applicationContext.getSharedPreferences(Manager.PREFERENCES_KEY, Context.MODE_PRIVATE);
	        
	        return prefs.getString(prefKey, null);
        } catch (Exception e) {
        	e.printStackTrace();
        	return null;
        }
    }
    
    public void removeImpression(String gamePackage) {
        if (Manager.applicationContext == null) return;
        if (gamePackage == null) return;
        
        try {
	        String prefKey = "impression." + gamePackage;
	        
	        final SharedPreferences prefs = Manager.applicationContext.getSharedPreferences(Manager.PREFERENCES_KEY, Context.MODE_PRIVATE);
	        SharedPreferences.Editor editor = prefs.edit();
	        
	        String existingImpressionId = prefs.getString(prefKey, null);
	        
	        editor.remove(prefKey);
	        editor.commit();
        } catch (Exception e) {
        	e.printStackTrace();
        	return;
        }
    }
    
	/* Singleton */

	protected synchronized static Manager getInstance() {
	    if (ref == null) {
	        ref = new Manager();
	    }
	    
	    return ref;
	}
		
	public Object clone() {
		return null;
	}
	
	private static volatile Manager ref;
}
