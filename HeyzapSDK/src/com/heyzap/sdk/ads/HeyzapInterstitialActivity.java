package com.heyzap.sdk.ads;

import android.view.View;

import com.heyzap.internal.Logger;

public class HeyzapInterstitialActivity extends AbstractActivity {
	
	private InterstitialWebView webview;

	@Override
	public Boolean onPrepared() {
		this.webview = new InterstitialWebView(this, new WebViewActionListener());
		this.webview.render((InterstitialModel)this.currentAd);
		return true;
	}

	@Override
	public View getContentView() {
		return this.webview;
	}
	
	private class WebViewActionListener implements AbstractActivity.AdActionListener {

		@Override
		public void show() {
			HeyzapInterstitialActivity.this.onShow();
		}

		@Override
		public void hide() {
			HeyzapInterstitialActivity.this.webview.clear();
			HeyzapInterstitialActivity.this.onHide();
		}

		@Override
		public void click() {
			HeyzapInterstitialActivity.this.onClick();
		}

		@Override
		public void clickUrl(String url, String extraData) {
			HeyzapInterstitialActivity.this.onClick(url, extraData);		
		}

		@Override
		public void installHeyzap() {
			Manager.getInstance().installHeyzap(HeyzapInterstitialActivity.this.currentAd);
		}
		
		@Override
		public void completed() {}
		
		@Override
		public void error() {}
		
		@Override
		public void restart() {}
	}
}
