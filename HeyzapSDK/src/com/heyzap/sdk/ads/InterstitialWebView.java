package com.heyzap.sdk.ads;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.heyzap.internal.ClickableToast;
import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

class InterstitialWebView extends FrameLayout {

	private WrapperView wrapperView;
	private AbstractActivity.AdActionListener listener;
	private Boolean globalTouchEnabled = false;
	
    private static final float MAX_SIZE_PERCENT = 0.98f;
    private static final int MAX_SIZE_DP_WIDTH = 360;
    private static final int MAX_SIZE_DP_HEIGHT = 360;
	
	public InterstitialWebView(Context context, AbstractActivity.AdActionListener listener) {
		super(context);
		this.listener = listener;
		this.wrapperView = new WrapperView(context);
		this.addView(wrapperView);
		
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
		setupWebview();
        setTouchListener();
    }
    
    public void render(InterstitialModel ad) {
    	this.render(ad.getHtmlData(), ad.getWidth(), ad.getHeight(), ad.getBackgroundOverlayColor());
    }
    
    public void render(VideoModel ad) {
    	this.render(ad.getHtmlData(), ad.getInterstitialWidth(), ad.getInterstitialHeight(), ad.getInterstitialBackgroundOverlayColor());
    }
    
    public void render(final String htmlData, final int width, final int height, final Integer backgroundOverlayColor) {
    	final Activity activity = (Activity)this.wrapperView.getContext();
    	activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
		    	InterstitialWebView.this.setWidths(activity, width, height);
		    	setBackgroundColor(backgroundOverlayColor);
				InterstitialWebView.this.wrapperView.webview.loadDataWithBaseURL(null, htmlData, "text/html", null, null);
			}
    	});
    }
    
    public void clear() {
    	this.wrapperView.webview.loadDataWithBaseURL(null, "<html></html>", "text/html", null, null);
    }
    
    public void hide(final Boolean animated, final Boolean doCallback) {
        final Activity activity = (Activity)this.wrapperView.getContext();
        activity.runOnUiThread(new Runnable() {
        	@Override
			public void run() {
		    	if (!animated) {
		    		if (doCallback) {
		    			InterstitialWebView.this.viewDidHide(animated);
		    		}
		    		
		    		//WebViewPopup.super.hide();
		    	} else {
		    		Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
		    		fadeOut.setDuration(150);
		    		fadeOut.setInterpolator(new AccelerateInterpolator());
		    		fadeOut.setAnimationListener(new AnimationListener() {
    	
    	                @Override
    	                public void onAnimationEnd(Animation arg0) {
    	                	if (doCallback) {
    	                		InterstitialWebView.this.viewDidHide(animated);
    	                	}
      	                	//WebViewPopup.super.hide();
    	                }
    	
    	                @Override
    	                public void onAnimationRepeat(Animation arg0) {}
    	
    	                @Override
    	                public void onAnimationStart(Animation arg0) {}
    	
    	            });
    	
    	            InterstitialWebView.this.wrapperView.startAnimation(fadeOut);
            	}
    	}
        });

    }
    
    public void hide(Boolean animated) {
    	hide(animated, true);
    }
    
//    @Override
//    public void hide() {
//    	hide(true);
//    }
//    
//    public void silentHide() {
//    	super.hide();
//    }
    
    public void show(Boolean animated) {
    	show(animated, true);
    }
    
    public void show(Boolean animated, final Boolean doCallback) {

        final Activity activity = (Activity) this.getContext();
        
        if (!animated) {
        	//super.show();
        	if (doCallback) {
        		this.viewDidShow(false);
        	}
        } else {

            final AnimationSet set = new AnimationSet(true);

            // Fake in web view
            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(200); //200
            set.addAnimation(animation);

            // Zoom in web view
            animation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, (float) 0.5, Animation.RELATIVE_TO_SELF, (float) 0.5);
            animation.setDuration(300);
            animation.setInterpolator(new AccelerateInterpolator());
            set.addAnimation(animation);
            
            animation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationStart(Animation animation) {
					if (doCallback) {
						InterstitialWebView.this.viewDidShow(true);
					}
				}
            	
            });

            setTouchListener();
            
            activity.runOnUiThread(new Runnable() {
            	@Override
				public void run() {
                    InterstitialWebView.this.wrapperView.startAnimation(set);
            	}
            });
            
            //if (!this.isShown()) {
            	//super.show();
            //}
        }
    }
//    
//    @Override
//    public void show() {
//    	show(true);
//    }
    
    private void viewDidShow(Boolean animated) {
		this.wrapperView.webview.loadUrl("javascript: try{adViewShown();}catch(e){}");
		if (this.listener != null) {
			this.listener.show();
		}
    }
    
    private void viewDidHide(Boolean animated) {
		this.wrapperView.webview.loadUrl("javascript: try{adViewHidden();} catch(e) {}");
		if (this.listener != null) {
			this.listener.hide();
		}
    }
    
    private void setupWebview() {
        this.wrapperView.webview.getSettings().setJavaScriptEnabled(true);
        this.wrapperView.webview.getSettings().setLoadsImagesAutomatically(true);
        this.wrapperView.webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        WebViewClient customWebViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {            	
            	if (InterstitialWebView.this.listener != null) {
                    if (url.contains("Heyzap.close")) {
                    	InterstitialWebView.this.listener.hide();
                    } else if (url.contains("Heyzap.restart")) {
                    	InterstitialWebView.this.listener.restart();
                    } else if (url.contains("Heyzap.installHeyzap")) {
                    	InterstitialWebView.this.listener.installHeyzap();
                    } else if (url.contains("Heyzap.clickAd")) {
                    	InterstitialWebView.this.listener.click();
                    } else if (url.contains("Heyzap.clickManualAdUrl=")) {
                        int urlStart = url.indexOf("Heyzap.clickManualAdUrl=") + 24;
                        int separator = url.indexOf(":::");
                        int packageStart = separator + 3;
                        String adUrl = url.substring(urlStart, separator);
                        String customGamePackage = url.substring(packageStart);
                        InterstitialWebView.this.listener.clickUrl(adUrl, customGamePackage);
                    }
                }

                return true;
            }
        };

        WebChromeClient customWebChromeClient = new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Logger.log("Console Message", message, lineNumber, sourceID);
            }
        };

        this.wrapperView.webview.setWebViewClient(customWebViewClient);
        this.wrapperView.webview.setWebChromeClient(customWebChromeClient);
    }
    
    private void setWidths(final Context context, int width, int height) {
        final Activity activity = (Activity) context;
        
        if (width == 0 && height == 0) {
        	width = Math.round(activity.getWindowManager().getDefaultDisplay().getWidth() * MAX_SIZE_PERCENT);
        	height = Math.round(activity.getWindowManager().getDefaultDisplay().getHeight() * MAX_SIZE_PERCENT);
        	
            width = Math.min(Utils.getScaledSize(context, MAX_SIZE_DP_WIDTH), width);
            height = Math.min(Utils.getScaledSize(context, MAX_SIZE_DP_HEIGHT), height);

            width = Math.min(width, height);
            height = Math.min(width, height);
        }
        
        int dp = Utils.dpToPx(context, 10);
        
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.wrapperView.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;

        layoutParams.width = width;
        layoutParams.height = height;

        this.setLayoutParams(layoutParams);
    }
    
    private void setTouchListener() {
        this.wrapperView.webview.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	if (InterstitialWebView.this.globalTouchEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
            		if (InterstitialWebView.this.listener != null) {
            			InterstitialWebView.this.listener.click();
            		}
            	} else {
            		return false;
            	}
            	
            	return true;
            }
        });
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	if (InterstitialWebView.this.listener != null) {
        		InterstitialWebView.this.listener.hide();
        	}
        	
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    
    /* Classes */
    
    private class CustomWebView extends WebView {
        
        public CustomWebView(Context context) {
            super(context);
            this.setBackgroundColor(0);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
        	return InterstitialWebView.this.onKeyDown(keyCode, event);
        }
    }
    
	private class WrapperView extends RelativeLayout {
		
		public FrameLayout container;
		public CustomWebView webview;
		
		private static final int OVERLAY_PADDING = 10;
		
    	public WrapperView(Context context) {
    		super(context);
    		
    		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    		this.setLayoutParams(params);
    		this.setGravity(Gravity.CENTER);
    		
    		this.container = new FrameLayout(context);
    		
    		int scaledPadding = Utils.getScaledSize(context, OVERLAY_PADDING);
    		
    		RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
    		containerParams.addRule(ALIGN_PARENT_LEFT);
    		containerParams.addRule(ALIGN_PARENT_TOP);
    		
    		this.addView(this.container, containerParams);
    		
    		this.webview = new CustomWebView(context);
            this.webview.setVisibility(View.VISIBLE);
            this.webview.setVerticalScrollBarEnabled(false);
            this.webview.setHorizontalScrollBarEnabled(false);
            this.webview.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
            this.webview.setBackgroundColor(0x00000000);
            
            FrameLayout.LayoutParams webviewParams = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            this.container.addView(this.webview, webviewParams);
    	}
    }
}
