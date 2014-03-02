package com.heyzap.sdk.ads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.heyzap.http.RequestParams;
import com.heyzap.internal.Connectivity;
import com.heyzap.internal.Logger;

class FetchRequest {
    
    private JSONObject          response              = null;
    private int                 remainingTries        = 3;
    private int                 adUnit;
    private String              tag;
    private String              host                  = "ads.heyzap.com";
    private String              endpoint              = "/in_game_api/ads/fetch_ad";
    private Boolean             secure                = false;
    private String              rejectedImpressionId  = null;
    private Map<String, String> additionalParams      = new HashMap<String, String>();
    private ArrayList<String>   creativeTypes;
    private Integer             creativeId            = 0;
    private Integer             campaignId            = 0;
    private Boolean             debugEnabled          = false;
    private Boolean             randomStrategyEnabled = false;
    
    public FetchRequest(int adUnit, String tag, ArrayList<String> creativeTypes) {
        this.adUnit = adUnit;
        this.tag = tag;
        this.creativeTypes = creativeTypes;
    }
    
    public Boolean isValid() {
        return (this.remainingTries > 0);
    }
    
    public RequestParams getParams(Context context) {
        
        final RequestParams requestParams = new RequestParams(
                this.additionalParams);
        
        // Framework Params
        if (HeyzapAds.mediator != null) {
            requestParams.put("sdk_mediator", HeyzapAds.mediator);
        }
        
        if (HeyzapAds.framework != null) {
            requestParams.put("sdk_framework", HeyzapAds.framework);
        }        
        
        // Ad Context
        switch (this.adUnit) {
            case Manager.AD_UNIT_INCENTIVIZED:
                requestParams.put("ad_unit", "incentivized");
                break;
            case Manager.AD_UNIT_VIDEO:
                requestParams.put("ad_unit", "video");
                break;
            case Manager.AD_UNIT_INTERSTITIAL:
            default:
                requestParams.put("ad_unit", "interstitial");
                break;
        }
        
        // Creative Types
        requestParams.put("creative_type",
                TextUtils.join(",", this.creativeTypes));
        
        // Device Specific
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        requestParams.put("connection_type",
                Connectivity.connectionType(context));
        requestParams.put("device_dpi", Float.toString(dm.density));
        
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                    .getPath());
            long bytesAvailable = (long) stat.getBlockSize()
                    * (long) stat.getAvailableBlocks();
            requestParams.put("device_free_bytes",
                    Long.toString(bytesAvailable));
        } catch (Exception ex1) {
            requestParams.put("device_free_bytes", "0");
        }
        
        // Dimension Metrics
        
        int height = dm.heightPixels;
        int width = dm.widthPixels;
        
        if (!requestParams.containsKey("orientation")) {
            String orientationString = (dm.widthPixels > dm.heightPixels) ? "landscape"
                    : "portrait";
            requestParams.put("orientation", orientationString);
        } else {
            String reportedOrientation = requestParams.get("orientation");
            if (reportedOrientation.equals("landscape") && height > width) {
                width = dm.heightPixels;
                height = dm.widthPixels;
            }
        }
        
        requestParams.put("device_width", width);
        requestParams.put("device_height", height);
        
        // Supported features
        requestParams.put("supported_features",
                "chromeless,js_visibility_callback");
        
        // View specifics
        requestParams.put("ad_chrome", "true");
        
        // Tag
        if (tag != null) {
            requestParams.put("tag", AdModel.normalizeTag(this.tag));
        } else {
            requestParams.put("tag", AdModel.DEFAULT_TAG_NAME);
        }
        
        // Rejected Impression IDs
        if (this.rejectedImpressionId != null) {
            requestParams.put("rejected_impression_id",
                    this.rejectedImpressionId);
        }
        
        // Creative ID
        if (this.creativeId > 0) {
            requestParams.put("creative_id", this.creativeId);
        }
        
        // Campaign ID
        if (this.campaignId > 0) {
            requestParams.put("campaign_id", this.campaignId);
        }
        
        // Debug
        if (this.debugEnabled) {
            requestParams.put("debug", "1");
        }
        
        // Random
        if (this.randomStrategyEnabled) {
            requestParams.put("use_random_strategy_v2", "1");
        }
        
        return requestParams;
    }
    
    public void incrementTries() {
        this.remainingTries = this.remainingTries - 1;
    }
    
    public void setRejectedImpressionId(String impressionId) {
        this.rejectedImpressionId = impressionId;
    }
    
    public void setAdditionalParams(Map<String, String> params) {
        this.additionalParams = params;
    }
    
    public void setCreativeId(Integer creativeId) {
        this.creativeId = creativeId;
    }
    
    public Integer getCreativeId() {
        return this.creativeId;
    }
    
    public void setDebugEnabled(Boolean enabled) {
        this.debugEnabled = enabled;
    }
    
    public Boolean getDebugEnabled() {
        return this.debugEnabled;
    }
    
    public void setRandomStrategyEnabled(Boolean enabled) {
        this.randomStrategyEnabled = enabled;
    }
    
    public Boolean getRandomStrategyEnabled() {
        return this.randomStrategyEnabled;
    }
    
    public void setCampaignId(Integer campaignId) {
        this.campaignId = campaignId;
    }
    
    public Integer getCampaignId() {
        return this.campaignId;
    }
    
    public String getTag() {
        return this.tag;
    }
    
    public int getAdUnit() {
        return this.adUnit;
    }
    
    public String getUrl() {
        String schema = this.secure ? "https" : "http";
        return String.format("%s://%s%s", schema, this.host, this.endpoint);
    }
}
