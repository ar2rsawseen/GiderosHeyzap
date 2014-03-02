package com.heyzap.sdk.ads;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;

class VideoControlView extends FrameLayout {
	
	public String skipButtonText = "Skip";
	private TextView skipButtonTextView;
	public View scrubBar;
	private SimpleDateFormat secondFormatter;
	public TextView timeTextView;
	private RelativeLayout skipButton;
	private RelativeLayout hideButton;
	
	public OnActionListener listener;
	
	private VideoControlView(Context context){ super(context);};
	
	public VideoControlView(Context context, VideoModel video) {
		super(context);
		
		this.setBackgroundColor(0x00000000);
		
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (VideoControlView.this.listener != null) {
					VideoControlView.this.listener.onClick();
				}
			}
		});
		
		this.addScrubBar();
	}
	
	public void updateScrubber(final int remainingTime, final float percentComplete) {
		SpannableString spanString = null;
		final DisplayMetrics display = getContext().getResources().getDisplayMetrics();
		
		if (this.secondFormatter == null) {
			this.secondFormatter = new SimpleDateFormat("s", Locale.US);
		}
		
		// Do not show anything at below 1 second
		String txt = remainingTime >= 1000 ? this.secondFormatter.format(remainingTime) : "";
		
		spanString = new SpannableString(txt);
		spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
		
		final SpannableString remainingTextSpan = spanString;

		Activity activity = (Activity)getContext();
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (remainingTextSpan != null) {
					VideoControlView.this.timeTextView.setText(remainingTextSpan);
				}
				
				android.view.ViewGroup.LayoutParams lp = scrubBar.getLayoutParams();
				lp.width = (int) (percentComplete * display.widthPixels);
				scrubBar.setLayoutParams(lp);
			}
		});
	}
	
	public void addHideButton() {
    	// Define the Click Listener for the visible and invisible touch areas
		OnClickListener ocl = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (VideoControlView.this.listener != null) {
					VideoControlView.this.listener.onHide();
				}
			}
    	};
		
    	// The invisible touch area
    	this.hideButton = new RelativeLayout(getContext());
    	this.hideButton.setBackgroundColor(Color.TRANSPARENT);
    	this.hideButton.setOnClickListener(ocl);
		
    	// The the visible button
    	ImageView imageView = new ImageView(getContext());
    	
    	imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    	imageView.setPadding(0, Utils.dpToPx(getContext(), 10), Utils.dpToPx(getContext(), 10), 0);
    	
    	RelativeLayout.LayoutParams imageLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	imageLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT | RelativeLayout.ALIGN_PARENT_TOP);
    	this.hideButton.addView(imageView, imageLayoutParams);

    	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Utils.dpToPx(getContext(), 100), Utils.dpToPx(getContext(), 100));
    	lp.gravity = Gravity.RIGHT | Gravity.TOP;
    	this.addView(this.hideButton, lp);
	}
	
	public void addSkipButton(Boolean withDelay, long delayMillisTillActive) {

    	int gravity = Gravity.RIGHT;
    	
    	// Define the Click Listener for the visible and unvisible touch areas
		OnClickListener ocl = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (VideoControlView.this.listener != null) {
					VideoControlView.this.listener.onSkip();
				}
			}
    	};
		
    	// The invisible touch area (background)
    	this.skipButton = new RelativeLayout(getContext());
    	this.skipButton.setBackgroundColor(Color.TRANSPARENT);
    	this.skipButton.setOnClickListener(ocl);
		
		// The container for the visible area
    	LinearLayout visibleArea = new LinearLayout(getContext());
    	visibleArea.setOrientation(LinearLayout.HORIZONTAL);
    	visibleArea.setBackgroundColor(0x00000000);
    	visibleArea.setGravity(Gravity.CENTER_VERTICAL);
    	visibleArea.setPadding(0, Utils.dpToPx(getContext(), 9), Utils.dpToPx(getContext(), 9), 0);
    	
    	// The text view
		this.skipButtonTextView = new TextView(getContext());
		this.skipButtonTextView.setTextSize((float)20.0);
		this.skipButtonTextView.setTextColor(Color.WHITE);
		this.skipButtonTextView.setGravity(Gravity.CENTER);
		this.skipButtonTextView.setShadowLayer((float) 0.01, -2, 2, Color.GRAY);
		
		int rightPadding = 0;
		if (Utils.getSdkVersion() < 11) {
			rightPadding = Utils.dpToPx(getContext(), 7);
		}
		
		this.skipButtonTextView.setPadding(Utils.dpToPx(getContext(), 7), Utils.dpToPx(getContext(), -2), rightPadding, 0);
		visibleArea.addView(this.skipButtonTextView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		// A little icon for the skip button
    	ImageView imageView = new ImageView(getContext());
    	imageView.setImageResource(android.R.drawable.ic_media_next);
    	
    	if (Utils.getSdkVersion() < 11) {
    		imageView.setPadding(0, 0, rightPadding, 0);
    	}
    	
    	visibleArea.addView(imageView, new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
    	
    	RelativeLayout.LayoutParams visibleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	visibleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    	this.skipButton.addView(visibleArea, visibleLayoutParams);
    	
    	// Add whole thing to layout
    	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Utils.dpToPx(getContext(), 200), Utils.dpToPx(getContext(), 150));
    	lp.gravity = gravity;
		this.addView(this.skipButton, lp);
		
		if (!withDelay) {
			this.skipButton.setVisibility(View.VISIBLE);
			SpannableString spanString = new SpannableString(this.skipButtonText);
			spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
			this.skipButtonTextView.setText(spanString);
		} else {
			this.skipButton.setEnabled(false);
			
			new CountDownTimer(delayMillisTillActive, 100) {

			     public void onTick(long millisUntilFinished) {
			    	 
			    	 int secondsLeft = (int) Math.ceil((double)(millisUntilFinished / 1000.0));
			    	 String text = "Skip in " + String.format("%d",secondsLeft);
			    	 SpannableString spanString = new SpannableString(text);
			    	 spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
			    	 VideoControlView.this.skipButtonTextView.setText(spanString);
			     }

			     public void onFinish() {
			    	 VideoControlView.this.skipButton.setEnabled(true);
			    	 SpannableString spanString = new SpannableString(VideoControlView.this.skipButtonText);
			    	 spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
			    	 VideoControlView.this.skipButtonTextView.setText(spanString);
			    	 VideoControlView.this.skipButtonTextView.setTextColor(Color.WHITE);
			     }
			  }.start();
		}
	}
	
	public void addScrubBar() {
    	int scrubberHeightDp = 4;
    	
    	this.scrubBar = new RelativeLayout(getContext());
    	this.scrubBar.setBackgroundColor(0x00FFFFFF);
    	 
    	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, Utils.dpToPx(getContext(), scrubberHeightDp));
    	lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
    	this.addView(this.scrubBar, lp);
    	
    	this.timeTextView = new TextView(getContext());
    	this.timeTextView.setTextColor(Color.WHITE);
    	this.timeTextView.setGravity(Gravity.CENTER);
    	
    	if (!Utils.isTablet(getContext())) {
    		this.timeTextView.setPadding(10, 10, 10, 10);
    	}

    	this.timeTextView.setGravity(Gravity.LEFT);
    	this.timeTextView.setTextSize(40.0f);
		this.timeTextView.setShadowLayer((float) 0.01, -2, 2, Color.GRAY);
    	
    	FrameLayout.LayoutParams ttlp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    	ttlp.gravity = Gravity.BOTTOM | Gravity.LEFT;
    	ttlp.leftMargin = Utils.dpToPx(getContext(), 12);
    	this.addView(this.timeTextView, ttlp);
	}
	
	public void setOnActionListener(OnActionListener listener) {
		this.listener = listener;
	}
	
	public interface OnActionListener {
		public void onSkip();
		public void onHide();
		public void onClick();
	}
	
}
