package com.heyzap.sdk.ads;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;

import com.heyzap.http.RequestParams;
import com.heyzap.internal.APIClient;
import com.heyzap.internal.APIResponseHandler;
import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

abstract class AdModel {
	
	public static String FORMAT = null;
	
	// Impression
	private String strategy;
	private String gamePackage;
	private String impressionId;
	private Boolean sentImpression = false;
	private Boolean sentIncentiveComplete = false;
	protected String creativeType;
	public String userIdentifier;
	private Integer creativeId;

	// Click
	public String actionUrl;
	private Boolean sentClick = false;
	
	// Tag
	private String tag;
	private int adUnit;
    public static String DEFAULT_TAG_NAME = "default";
    
	
	// Expiration
	private long ttl = 0;
	private long fetchTime = System.currentTimeMillis();
	
	// Orientation
	protected int requiredOrientation = Configuration.ORIENTATION_UNDEFINED;
	
	
	public AdModel(JSONObject response) throws Exception,JSONException {
        // Get the ad data

        this.strategy = (String) response.optString("ad_strategy", this.strategy);
        this.gamePackage = (String) response.optString("promoted_game_package", "");
        this.impressionId = (String) response.getString("impression_id");
        this.actionUrl = (String) response.optString("click_url", null);
        this.ttl = response.optLong("refresh_time", 0) * 1000L;
        this.creativeId = response.optInt("creative_id", 0);
	}
	
	public void onClick() { onClick(null); };
	
	public Boolean onClick(String customGamePackage) {
		
        // Double click checking
		if (this.sentClick) {
            Logger.log("Already sent click successfully.");
            return true;
        }
        
        // Check we haven't received this event twice in close proximity	
        if (System.currentTimeMillis() - Manager.getInstance().lastClickedTime < Manager.maxClickDifference) {
            return false;
        }
		
        RequestParams params = new RequestParams();
        params.put("ad_strategy", this.strategy);
        params.put("promoted_game_package", this.gamePackage);
        if (customGamePackage != null) {
            params.put("custom_game_package", customGamePackage);
        }
        params.put("impression_id", this.impressionId);

        if (this.tag != null) {
            params.put("tag", AdModel.normalizeTag(this.tag));
        }

        APIClient.post(Manager.applicationContext, Manager.EVENT_SERVER + "/register_click", params, new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                        Logger.format("(CLICK) %s", AdModel.this);
                        AdModel.this.sentClick = true;
                    }
                } catch (JSONException e) {
                	
                }
            }
        });
        
        return true;
	}
	
	public void onImpression() {
        if (this.sentImpression == true) {
            Logger.log("Already sent impression successfully.");
            return;
        }

        RequestParams params = new RequestParams();
        params.put("impression_id", impressionId);
        params.put("promoted_android_package", this.gamePackage);

        if (this.tag != null) {
            params.put("tag", AdModel.normalizeTag(this.tag));
        }

        APIClient.post(Manager.applicationContext, Manager.EVENT_SERVER + "/register_impression", params, new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                        Logger.format("(IMPRESSION) %s", AdModel.this);
                        AdModel.this.sentImpression = true;
                    }
                } catch (JSONException e) {
                	e.printStackTrace();
                }
            }
        });
	}
	
	public void onIncentiveComplete() {
		if (this.sentIncentiveComplete == true) {
            Logger.log("Already sent completion successfully.");
            return;
		}
		
        RequestParams params = new RequestParams();
        params.put("impression_id", getImpressionId());
        params.put("promoted_android_package", this.gamePackage);
        
        APIClient.post(Manager.applicationContext, Manager.EVENT_SERVER + "/event/register_incentive_complete", params, new APIResponseHandler() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    if (response.getInt("status") == 200) {
                        Logger.format("(COMPLETE) %s", AdModel.this);
                        AdModel.this.sentIncentiveComplete = true;
                    }
                } catch (JSONException e) {
                	e.printStackTrace();
                }
            }
        });
	}
	
	/* Cleanup */
	public abstract void cleanup() throws Exception;
	
	/* Post Fetch Actions */
	public abstract void doPostFetchActions(Context context, ModelPostFetchCompleteListener listener);
	
	/* Getters & Setters */
	
	public void setAdUnit(int adUnit) {
		this.adUnit = adUnit;
	}
	
	public int getAdUnit() {
		return this.adUnit;
	}
	
	public String getTag() {
		return this.tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getImpressionId() {
		return this.impressionId;
	}
	
	public String getGamePackage() {
		return this.gamePackage;
	}
	
	public String getFormat() {
		return this.creativeType;
	}
	
	public int getCreativeId() {
		return this.creativeId;
	}
	
	public String getCreativeType() {
		return this.creativeType;
	}
	
	public Boolean hasSentImpression() {
		return this.sentImpression;
	}
	
	public Boolean isExpired() {
		if (this.ttl > 0) {
			return System.currentTimeMillis() > (this.fetchTime + this.ttl);
		}
		
		return false;
	}
	
	public Boolean isInstalled(Context context) {
		return Utils.packageIsInstalled(this.gamePackage, context);
	}
	
	public Boolean supportsCurrentOrientation(Context context) {
		int orientation = context.getResources().getConfiguration().orientation;
		return (this.requiredOrientation == Configuration.ORIENTATION_UNDEFINED)
			   || (orientation == this.requiredOrientation);
	}
	
	public int getRequiredOrientation() {
		return this.requiredOrientation;
	}
	
	public interface ModelPostFetchCompleteListener {
	    public void onComplete(AdModel model, Throwable e);
	}
	
	/* Static */
	public static String normalizeTag(String tag) {
        if (tag == null || tag.trim().equals("")) {
            tag = AdModel.DEFAULT_TAG_NAME;
        }
        
        return tag.trim();
	}
	
	/* String */
	
	public String toString() {
		return String.format("<%s T:%s I:%s CID: %s>", this.getClass().getName(), this.getCreativeType(), this.getImpressionId(), String.valueOf(this.creativeId));
	}
	
}
