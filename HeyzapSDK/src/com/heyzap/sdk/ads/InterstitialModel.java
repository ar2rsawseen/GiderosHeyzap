package com.heyzap.sdk.ads;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.ViewGroup.LayoutParams;

import com.heyzap.internal.Logger;
import com.heyzap.sdk.ads.AdModel.ModelPostFetchCompleteListener;


class InterstitialModel extends AdModel {
	
	public static String FORMAT = "interstitial";
	private String htmlData;
	private Integer backgroundOverlay;

	private int height;
	private int width;
	private Boolean manualSize;
	private Boolean disableGlobalTouch;

	public InterstitialModel(JSONObject response) throws JSONException, Exception {
		super(response);
		
		this.creativeType = InterstitialModel.FORMAT;
        this.htmlData = (String) response.getString("ad_html");
        this.height = response.optInt("ad_height");
        this.width = response.optInt("ad_width");
        
        if (this.height == 0) {
            String h = response.optString("ad_height");
            if (h.equals("fill_parent")) {
                this.height = LayoutParams.MATCH_PARENT;
            }
        }

        if (this.width == 0) {
            String h = response.optString("ad_width");
            if (h.equals("fill_parent")) {
                this.width = LayoutParams.MATCH_PARENT;
            }
        }

        this.manualSize = this.height != 0 && this.width != 0;
        
        String orientation = response.optString("required_orientation", "portrait");
        Boolean hideOnOrientationChange = response.optBoolean("hide_on_orientation_change", true);
        if (hideOnOrientationChange) {
            if (orientation.equals("landscape")) {
            	this.requiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
            } else if (orientation.equals("portrait")) {
            	this.requiredOrientation = Configuration.ORIENTATION_PORTRAIT;
            } else {
            	this.requiredOrientation = Configuration.ORIENTATION_UNDEFINED;
            }
        }

        disableGlobalTouch = response.optBoolean("disable_global_touch", false);
        backgroundOverlay = response.optInt("background_overlay", -1);
	}
	
	public String getHtmlData() {
		return this.htmlData;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
	
    public Integer getBackgroundOverlayColor() {
        if (backgroundOverlay == -1) {
            return Color.TRANSPARENT;
        } else {
            return backgroundOverlay;
        }
    }
    
    public void cleanup() throws Exception {}

	@Override
	public void doPostFetchActions(Context context,
			ModelPostFetchCompleteListener listener) {
		if (listener != null) listener.onComplete(this, null);		
	}
}
