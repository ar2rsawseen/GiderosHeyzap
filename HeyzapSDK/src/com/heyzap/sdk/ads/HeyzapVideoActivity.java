package com.heyzap.sdk.ads;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

public class HeyzapVideoActivity extends AbstractActivity {
	
	protected InterstitialWebView webView;
	protected FullscreenVideoView videoView;
	protected FrameLayout backgroundView;
	
	// SCREEN CODES
	final private static int NO_SCREEN = 0;
	final private static int WEBVIEW_SCREEN = 1;
	final private static int VIDEO_SCREEN = 2;
	
	private int currentScreen = NO_SCREEN;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Utils.getSdkVersion() >= 9) {
			super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else {
			super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}
	
	@Override
	public Boolean onPrepared() {
		this.backgroundView = new FrameLayout(this);
		this.backgroundView.setBackgroundColor(Color.TRANSPARENT);
		
		this.webView = new InterstitialWebView(this, new WebViewActionListener());
		this.webView.render((VideoModel)this.currentAd);
		
		this.videoView = new FullscreenVideoView(this, new VideoActionListener());
		this.videoView.allowHide = ((VideoModel)this.currentAd).allowHide();
		this.videoView.allowSkip = ((VideoModel)this.currentAd).allowSkip();
		this.videoView.allowClick = ((VideoModel)this.currentAd).allowClick();
		this.videoView.waitMillisBeforeSkipShow = ((VideoModel)this.currentAd).getSkipLockoutTime();
		
		this.videoView.render((VideoModel)this.currentAd);
		
		this.backgroundView.addView(this.videoView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		
		this.currentScreen = VIDEO_SCREEN;
		
		// Tell listener audio is starting...
		if (Manager.displayListener != null) {
			Manager.displayListener.onAudioStarted();
		}
		
		return true;
	}

	@Override
	public View getContentView() {
		this.backgroundView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		return this.backgroundView;
	}
	
	protected void switchToView(int screen) {
		if (screen == currentScreen) return;
		
		Animation anim_fadein = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		Animation anim_fadeout = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		
		View currentView = null;
		View incomingView = null;
		
		switch(currentScreen) {
		case WEBVIEW_SCREEN:
			currentView = this.webView;
			break;
		case VIDEO_SCREEN:
			currentView = this.videoView;
			break;
		}
		
		switch(screen) {
		case WEBVIEW_SCREEN:
			incomingView = this.webView;
			incomingView.invalidate();
			// Tell listener audio is ending
			if (Manager.displayListener != null) {
				Manager.displayListener.onAudioFinished();
			}
			
			break;
		case VIDEO_SCREEN:
			incomingView = this.videoView;
			// Tell listener audio is starting
			if (Manager.displayListener != null) {
				Manager.displayListener.onAudioStarted();
			}
			
			break;
		}
		
		if (currentView != null && incomingView != null) {
			this.backgroundView.addView(incomingView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
			incomingView.startAnimation(anim_fadein);
			this.backgroundView.removeView(currentView);
			
			currentScreen = screen;
		}
	}
	
	@Override
	public void onHide() {
		Boolean incentivized = (this.currentAdContext == Manager.AD_UNIT_INCENTIVIZED);
		
		((VideoModel)this.currentAd).onComplete(incentivized, this.videoView.getPlaybackDuration(), this.videoView.getTotalVideoDuration(), this.currentAdComplete);
		
		// Send back an incentivized callback
		if (Manager.incentiveListener != null && incentivized) {
			if (this.currentAdComplete) {
				Manager.incentiveListener.onComplete();
			} else {
				Manager.incentiveListener.onIncomplete();
			}
			
		}
		
		super.onHide();
		
		this.webView.clear();
		this.videoView.clear();
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		
		// mediaplayer apparently won't re-attach to a new surface view on Android 2.2 and under. 
		if (Build.VERSION.SDK_INT < 11 && this.currentScreen == VIDEO_SCREEN) {
			if (((VideoModel)this.currentAd).showPostRollInterstitial()) {
				this.switchToView(WEBVIEW_SCREEN);
			} else {
				this.onHide();
			}
		}
	}
	
  
    @Override
    protected void onPause() {
       // TODO Auto-generated method stub
       super.onPause();
       
 		if (this.videoView != null) {
			this.videoView.stop();
		}
    }
	
	private class WebViewActionListener implements AbstractActivity.AdActionListener {

		@Override
		public void show() {}

		@Override
		public void hide() {
			HeyzapVideoActivity.this.onHide();
		}

		@Override
		public void click() {
			HeyzapVideoActivity.this.onClick();
		}

		@Override
		public void clickUrl(String url, String extraData) {
			HeyzapVideoActivity.this.onClick(url, extraData);
		}

		@Override
		public void installHeyzap() {
			Manager.getInstance().installHeyzap(HeyzapVideoActivity.this.currentAd);
		}

		@Override
		public void completed() {}

		@Override
		public void error() {}
		
		@Override
		public void restart() {
			HeyzapVideoActivity.this.switchToView(VIDEO_SCREEN);
			HeyzapVideoActivity.this.videoView.restart();
		}
		
	}
	
	private class VideoActionListener implements AbstractActivity.AdActionListener {

		@Override
		public void show() {
			
		}

		@Override
		public void hide() {
			if (!((VideoModel)HeyzapVideoActivity.this.currentAd).showPostRollInterstitial()) {
				HeyzapVideoActivity.this.onHide();
			} else {
				HeyzapVideoActivity.this.switchToView(WEBVIEW_SCREEN);
			}
		}

		@Override
		public void click() {
			HeyzapVideoActivity.this.onClick();
		}

		@Override
		public void clickUrl(String url, String extraData) {
			Logger.log(url);
		}

		@Override
		public void installHeyzap() {}

		@Override
		public void completed() {
			HeyzapVideoActivity.this.currentAdComplete = true;
			if (((VideoModel)HeyzapVideoActivity.this.currentAd).showPostRollInterstitial()) {
				HeyzapVideoActivity.this.switchToView(WEBVIEW_SCREEN);
			}
		}

		@Override
		public void error() {
		    
		    Logger.trace();
		    
			if (!Utils.isApplicationOnTop(Manager.applicationContext)) {
				this.hide();
				return;
			}
			
			if (((VideoModel)HeyzapVideoActivity.this.currentAd).showPostRollInterstitial()) {
				HeyzapVideoActivity.this.switchToView(WEBVIEW_SCREEN);
				// NEED SOME ANALYTICS HERE!!!! THIS ISN'T THE SAME CREATIVE ANYMORE...
			} else {
				HeyzapVideoActivity.this.onHide();
			}
		}
		
		@Override
		public void restart() {}
		
	}
}
