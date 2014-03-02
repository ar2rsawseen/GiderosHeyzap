package com.heyzap.sdk.ads;

import java.util.Timer;

import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

abstract class AbstractActivity extends Activity {
    
    final public static String ACTIVITY_INTENT_IMPRESSION_KEY       = "impression_id";
    final public static String ACTIVITY_INTENT_CONTEXT_KEY          = "ad_context";
    final public static String ACTIVITY_INTENT_ACTION_KEY           = "action";
    final public static String ACTIVITY_INTENT_ORIGINAL_ORIENTATION = "original_orientation";
    
    final public static int    ACTIVITY_ACTION_SHOW                 = 1;
    final public static int    ACTIVITY_ACTION_HIDE                 = 2;
    
    protected AdModel          currentAd;
    protected String           currentAdTag                         = null;
    protected String           currentAdImpressionId                = null;
    protected int              currentAdContext                     = Manager.AD_UNIT_UNKNOWN;
    private AtomicBoolean      marketIntentLaunched                 = new AtomicBoolean(
                                                                            false);
    protected Boolean          currentAdComplete                    = false;
    private int                originalOrientation                  = Configuration.ORIENTATION_UNDEFINED;
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        
        if (Utils.getSdkVersion() >= 11) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        
        super.overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
        
        if (!HeyzapAds.hasStarted()) {
            finish();
            return;
        }
        
        Intent intent = getIntent();
        handleIntent(intent);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    protected void handleIntent(Intent intent) {
        if (intent.getExtras() == null) {
            finish();
        }
        
        if (!intent.getExtras().containsKey(ACTIVITY_INTENT_ACTION_KEY)) {
            finish();
        }
        
        if (intent.getExtras()
                .containsKey(ACTIVITY_INTENT_ORIGINAL_ORIENTATION)) {
            this.originalOrientation = intent.getExtras().getInt(
                    ACTIVITY_INTENT_ORIGINAL_ORIENTATION);
        }
        
        if (intent.getExtras().containsKey(ACTIVITY_INTENT_ACTION_KEY)) {
            switch (intent.getExtras().getInt(ACTIVITY_INTENT_ACTION_KEY)) {
                case ACTIVITY_ACTION_HIDE:
                    if (currentAd == null) {
                        finish();
                    } else {
                        onHide();
                    }
                    break;
                case ACTIVITY_ACTION_SHOW:
                default:
                    currentAdImpressionId = intent
                            .getStringExtra(ACTIVITY_INTENT_IMPRESSION_KEY);
                    currentAdContext = intent.getIntExtra(
                            ACTIVITY_INTENT_CONTEXT_KEY,
                            Manager.AD_UNIT_UNKNOWN);
                    currentAd = AdCache.getInstance().peek(
                            currentAdImpressionId);
                    
                    if (currentAd == null || currentAd.isExpired()) {
                        if (Manager.displayListener != null) {
                            Manager.displayListener.onFailedToShow(null);
                        }
                        
                        finish();
                        return;
                    }
                    
                    currentAdTag = currentAd.getTag();
                    
                    lockCurrentAdOrientation();
                    
                    if (this.onPrepared()) {
                        setContentView(getContentView());
                        this.onShow();
                    }
                    
                    Manager.lastActivity = this;
                    
                    break;
            }
        }
    }
    
    public abstract Boolean onPrepared();
    
    public abstract View getContentView();
    
    @SuppressLint("InlinedApi")
    private void lockCurrentAdOrientation() {
        
        int orientation = currentAd.getRequiredOrientation();
        if (orientation == Configuration.ORIENTATION_UNDEFINED) return;
        
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (Utils.getSdkVersion() > 8) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (Utils.getSdkVersion() > 8) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;
        }
    }
    
    public void onHide() {
        
        // Fixes race-condition on two hides in quick succession
        if (this.currentAd == null) {
            return;
        }
        
        Logger.format("(HIDE) %s", this.currentAd);
        
        if (Manager.displayListener != null) {
            Manager.displayListener.onHide(this.currentAd.getTag());
        }
        
        try {
            this.currentAd.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Say goodbye to activity
        this.finish();
        
        // Get rid of that ad!
        AdCache.getInstance().pop(currentAdImpressionId);
        
        switch (this.currentAdContext) {
            case Manager.AD_UNIT_INCENTIVIZED:
                break;
            case Manager.AD_UNIT_INTERSTITIAL:
                if (!Manager.getInstance().isFlagEnabled(
                        HeyzapAds.DISABLE_AUTOMATIC_FETCH)) {
                    InterstitialAd.fetch(this.currentAdTag,
                            this.originalOrientation);
                }
                break;
        }
        
        // Clear meta
        
        this.currentAd = null;
        this.currentAdImpressionId = null;
        this.currentAdTag = null;
        
        Manager.lastActivity = null;
    }
    
    public void finish() {
        super.finish();
    }
    
    public void onClick() {
        onClick(this.currentAd.actionUrl, null);
    }
    
    public void onClick(String adUrl, String customPackageName) {
        if (this.currentAd.onClick(customPackageName)) {
            // Show a spinner
            try {
                final ProgressDialog marketSpinner = ProgressDialog.show(this,
                        "", "Loading...", true);
                
                Manager.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            marketSpinner.dismiss();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }, 3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Send off to listener
            if (Manager.displayListener != null) {
                Manager.displayListener.onClick(this.currentAd.getTag());
            }
            
            gotoMarket(adUrl);
        }
    }
    
    public void onShow() {
        this.currentAd.onImpression();
        
        // Send off to listener
        if (Manager.displayListener != null) {
            Manager.displayListener.onShow(this.currentAd.getTag());
        }
    }
    
    private void launchMarketIntent(Context context, String intentUrl) {
        if (this.marketIntentLaunched.compareAndSet(false, true)) {
            // Fire the intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(intent);
        } else {
            
        }
    }
    
    private boolean launchIfMarket(Context context, String adUrl) {
        if (Utils.isAmazon()) {
            if (adUrl.startsWith("amzn")) {
                launchMarketIntent(context, adUrl);
                return true;
            }
            
            if (adUrl.contains("amazon.com/gp/mas/dl/android?")) {
                // Turn it into an amzn:// link so the user won't get the
                // "open with browser or market" prompt
                int i = adUrl.indexOf("android?");
                String marketUrl = "amzn://apps/" + adUrl.substring(i);
                launchMarketIntent(context, marketUrl);
                return true;
            }
        } else {
            if (adUrl.startsWith("market")) {
                launchMarketIntent(context, adUrl);
                return true;
            }
            
            if (adUrl.contains("play.google")) {
                // Turn it into a market:// link so the user won't get the
                // "open with browser or market" prompt
                int i = adUrl.indexOf("details?");
                if (i == -1) {
                    // Doesn't look to be the kind of play.google URL we expect,
                    // but let's still launch it
                    launchMarketIntent(context, adUrl);
                } else {
                    String marketUrl = "market://" + adUrl.substring(i);
                    launchMarketIntent(context, marketUrl);
                }
                return true;
            }
        }
        
        return false;
    }
    
    protected void gotoMarket(String url) {
        final String adUrl = url;
        this.marketIntentLaunched.set(false);
        
        if (launchIfMarket(this, adUrl)) {
            return;
        }
        
        final WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                return super.shouldOverrideUrlLoading(wView, url);
            }
            
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (launchIfMarket(AbstractActivity.this, url)) {
                    view.stopLoading();
                    return;
                }
            }
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                
                if (launchIfMarket(AbstractActivity.this, url)) {
                    view.stopLoading();
                    return;
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
        
        webView.getSettings().setJavaScriptEnabled(true);
        
        Timer timer = new Timer();
        
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                AbstractActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(adUrl);
                    }
                });
            }
        }, 250);
        
        webView.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                if (!AbstractActivity.this.marketIntentLaunched.get()) {
                    AbstractActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl(adUrl);
                        }
                    });
                } else {
                }
            }
        }, 750);
        
        webView.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                if (!AbstractActivity.this.marketIntentLaunched.get()) {
                    launchMarketIntent(AbstractActivity.this, adUrl);
                } else {
                }
            }
        }, 1250);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    public interface AdActionListener {
        public void show();
        
        public void hide();
        
        public void click();
        
        public void clickUrl(String url, String extraData);
        
        public void installHeyzap();
        
        public void completed();
        
        public void error();
        
        public void restart();
    }
}
