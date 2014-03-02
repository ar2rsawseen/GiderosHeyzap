package com.heyzap.sdk.ads;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.heyzap.internal.Logger;
import com.heyzap.internal.Utils;
import com.heyzap.sdk.ads.VideoControlView.OnActionListener;

class FullscreenVideoView extends FrameLayout {
	private Context context;
	private AbstractActivity.AdActionListener listener;
	private MediaPlayer mediaPlayer;
	private MediaPlayerListener mpListener;
	public Boolean allowSkip = false;
	public Boolean allowHide = true;
	public Boolean allowClick = true;
	public Integer waitMillisBeforeSkipShow = 1;
	public Timer playbackTimer;
	public ProgressDialog loadingSpinner;
	public int bufferProgress = 0;
	private int totalDuration = 0;
	private int playbackDuration = 0;
	
	public SurfaceView videoSurface;
	private VideoControlView controlView;
	
	public FullscreenVideoView(Context context, AbstractActivity.AdActionListener listener) {
		super(context);
		this.context = context;
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        
		this.listener = listener;
    	this.playbackTimer = new Timer();
		
		// Setup the media player without views/data
		this.mediaPlayer = new MediaPlayer();
		this.mpListener = new MediaPlayerListener();
		this.mediaPlayer.setOnBufferingUpdateListener(this.mpListener);
		this.mediaPlayer.setOnCompletionListener(this.mpListener);
		this.mediaPlayer.setOnErrorListener(this.mpListener);
		this.mediaPlayer.setOnPreparedListener(this.mpListener);
		this.mediaPlayer.setOnVideoSizeChangedListener(this.mpListener);
		this.mediaPlayer.setScreenOnWhilePlaying(true);
		
		this.setBackgroundColor(Color.TRANSPARENT);
		
		// We are going to create the surface.
		this.videoSurface = new SurfaceView(context);
		// This is necessary for old versions of Android.
		if (Build.VERSION.SDK_INT < 11) this.videoSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		this.videoSurface.getHolder().addCallback(new VideoSurfaceViewCallback());
		
		this.videoSurface.setVisibility(View.GONE);
		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
		params.gravity = Gravity.CENTER;
		this.addView(this.videoSurface, params);
		
		// Setup Control View
		this.controlView = new VideoControlView(getContext(), null);
		this.controlView.setOnActionListener(new OnVideoActionListener());
        this.addView(this.controlView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        
        this.showControls();
	}
    
    public void showControls() {
    	final Activity activity = (Activity)getContext();
    	
		Animation anim_fadein = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
		anim_fadein.setDuration(150);
    	FullscreenVideoView.this.controlView.setVisibility(View.VISIBLE);
    	this.controlView.startAnimation(anim_fadein);
    }
    
    @SuppressLint("NewApi")
	public void hideControls() {    	
    	FullscreenVideoView.this.controlView.setVisibility(View.GONE);
    }
    
    public void onVideoTick() {
		if (this.mediaPlayer != null && !this.mediaPlayer.isPlaying()) {
			return;
		}
		
		int remainingTime = this.mediaPlayer.getDuration() - this.mediaPlayer.getCurrentPosition();
		float percentComplete = (float) this.mediaPlayer.getCurrentPosition()/this.mediaPlayer.getDuration();
		
		this.controlView.updateScrubber(remainingTime, percentComplete);
    }
    
    @SuppressLint("NewApi")
	public void onVideoStart() {
		if (FullscreenVideoView.this.listener != null) {
			FullscreenVideoView.this.listener.show();
		}
		
		this.setBackgroundColor(Color.BLACK);
		
		this.totalDuration = this.mediaPlayer.getDuration();
		
		this.videoSurface.setVisibility(View.VISIBLE);
		
    	if (this.playbackTimer != null) {
			this.playbackTimer.purge();
			this.playbackTimer = null;
    	}
	
		this.playbackTimer = new Timer();
		this.playbackTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				FullscreenVideoView.this.onVideoTick();
			}
		}, 0, 100);
		
    	// Skip button and hide button cannot be shown simultaneously
    	if (this.allowSkip) {
    		if (this.waitMillisBeforeSkipShow > 0) {
    			this.controlView.addSkipButton(true, this.waitMillisBeforeSkipShow);
    		} else {
    			// Add it immediately
    			this.controlView.addSkipButton(false, 0);
    		}
    	} else if (this.allowHide) {
    		this.controlView.addHideButton();
    	}
    }
    
    public void onVideoSizeChanged(int width, int height) {
		int screenWidth = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getWidth();
		int screenHeight = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getHeight();
		
		int videoHeight = this.mediaPlayer.getVideoHeight();
		int videoWidth = this.mediaPlayer.getVideoWidth();
		
		Float overallRatio = (float)this.mediaPlayer.getVideoWidth() / this.mediaPlayer.getVideoHeight();
		
		if (videoWidth > videoHeight && overallRatio > 1.6) { //Widescreen
			videoHeight = (int) (((float)height / (float)width) * (float)screenWidth);
			videoWidth  = LayoutParams.FILL_PARENT;
		} else {
			videoHeight = LayoutParams.FILL_PARENT;
			videoWidth = (int) (((float)width / (float)height) * (float)screenHeight);
		}
    	
    	FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.videoSurface.getLayoutParams();
    	params.width = videoWidth;
    	params.height = videoHeight;
    	this.videoSurface.setLayoutParams(params);
    	
    	this.controlView.setLayoutParams(params);
    }
	
	public Boolean render(Uri uri) {
		try {
			
			this.loadingSpinner = ProgressDialog.show(getContext(), "", "Loading...", true);
			
			this.mediaPlayer.setDataSource(this.context, uri);
			this.mediaPlayer.prepareAsync();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();			
			return false;
		}
	}
	
	public Boolean render(String path) {		
		FileInputStream input = null;
		try {
			File file = new File(Manager.applicationContext.getCacheDir() + "/" + path);
			if (!file.exists()) {
				throw new Exception("File does not exist.");
			}
			
			Logger.log(path);
			
			input = new FileInputStream(file);
			
			this.mediaPlayer.setDataSource(input.getFD());
			
			this.mediaPlayer.prepareAsync();
			
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			if (this.listener != null) {
				this.listener.error();
			}
			
			return false;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Boolean render(VideoModel video) {
		try {
			String cachedVideoPath = video.getCachedPath();
			
			try {
				if (cachedVideoPath == null) {
					throw new Exception("local");
				}
				
				if (!render(cachedVideoPath)) {
					throw new Exception("load");
				}
			} catch (Exception e) {
				if (!e.getMessage().equals("local")) {
					e.printStackTrace();
				}
				
				Logger.log("Local file not found. Falling back to stream and cancelling download.");
				video.cancelDownload();
				if (!render(video.getStreamingUri())) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			if (this.listener != null) {
				this.listener.error();
			}
		}
		
		return true;
	}
	
	public void clear() {
		if (this.playbackTimer != null) {
			this.playbackTimer.cancel();
			this.playbackTimer.purge();
		}
		
		if (this.mediaPlayer != null) {
			this.mediaPlayer.reset();
			this.mediaPlayer.release();
			this.mediaPlayer = null;
		}
	}
	
	public void stop() {
		if (this.mediaPlayer != null) {
			this.mediaPlayer.pause();
		}
	}
	
	public void show(VideoModel ad) {
		try {
			if (!this.render(ad)) {
				throw new Exception("No content to render or error.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			if (this.listener != null) {
				this.listener.error();
			}
		}
	}
	
	public void hide(Boolean fireCallbacks) {
		if (this.playbackTimer != null) {
			this.playbackTimer.cancel();
			this.playbackTimer.purge();
		}
		
		if (this.mediaPlayer != null && this.mediaPlayer.isPlaying()) {
			this.mediaPlayer.stop();
			this.mediaPlayer.reset();
		}
		
    	this.clear();
		
	    if (fireCallbacks) {
	    	if (this.listener != null) {
	    		this.listener.hide();
	    	}
		}
	}
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	// If the video is not playing, let them use the back button
        	if (this.mediaPlayer == null || !this.mediaPlayer.isPlaying()) {
        		FullscreenVideoView.this.hide(true);
        	}
        	
        	// If skips are allowed, and the video is playing...
        	if (this.allowSkip && this.mediaPlayer != null && this.mediaPlayer.isPlaying()) {
        			// If there is no skip timeout or the timeout has expired, hide.
        			if (FullscreenVideoView.this.waitMillisBeforeSkipShow == 0 
        					|| (FullscreenVideoView.this.waitMillisBeforeSkipShow > 0 && (this.mediaPlayer.getCurrentPosition() > FullscreenVideoView.this.waitMillisBeforeSkipShow))) {
        					FullscreenVideoView.this.hide(true);
        			}
        	// If hides are allowed, hide.
        	} else if (this.allowHide) {
        		FullscreenVideoView.this.hide(true);
        	}
        	
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    
    public void restart() {
    	this.mediaPlayer.seekTo(0);
    }
    
    public int getPlaybackDuration() {
    	return this.playbackDuration;
    }
    
    public int getTotalVideoDuration() {
    	return this.totalDuration;
    }
     
    /*
     * SurfaceView Listener
     */
    
    private class VideoSurfaceViewCallback implements SurfaceHolder.Callback {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			try {
				FullscreenVideoView.this.mediaPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				FullscreenVideoView.this.mediaPlayer.setDisplay(holder);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			try {
				if (FullscreenVideoView.this.mediaPlayer != null && FullscreenVideoView.this.mediaPlayer.isPlaying()) {
					FullscreenVideoView.this.mediaPlayer.pause();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	
    }
    
    /*
     * VideoControlView Listener
     */
    
    private class OnVideoActionListener implements VideoControlView.OnActionListener {

		@Override
		public void onSkip() {
			this.onHide();
		}

		@Override
		public void onHide() {
			if (FullscreenVideoView.this.listener != null) {
				if (FullscreenVideoView.this.mediaPlayer.isPlaying()) {
					
					int totalTimeWatched = FullscreenVideoView.this.mediaPlayer.getCurrentPosition();
					if (totalTimeWatched > FullscreenVideoView.this.playbackDuration) {
						FullscreenVideoView.this.playbackDuration = totalTimeWatched;
					}
					
					FullscreenVideoView.this.mediaPlayer.pause();
					FullscreenVideoView.this.listener.hide();
					return;
				} else {
					FullscreenVideoView.this.listener.hide();
				}
			}
		}

		@Override
		public void onClick() {
			if (FullscreenVideoView.this.allowClick) {
				if (FullscreenVideoView.this.mediaPlayer != null) {
					FullscreenVideoView.this.mediaPlayer.pause();
				}
				
				if (FullscreenVideoView.this.listener != null) {
					FullscreenVideoView.this.listener.click();
				}
			}
		}
    }
    
    /*
     * MediaPlayer callback classes
     */
    
    private class MediaPlayerListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {
    	
		@Override
		public void onPrepared(MediaPlayer mp) {
			
			if (FullscreenVideoView.this.loadingSpinner != null && FullscreenVideoView.this.loadingSpinner.isShowing()) {
				FullscreenVideoView.this.loadingSpinner.dismiss();
			}
			
			/* Explanation of this code path:
			 * To prevent a race condition between originally starting the video, and the video restarting when
			 * the app returns, the surface view is originally hidden (which prevents it from being created). When
			 * the video starts, the surface view is unhidden, and thus created, and start is fired from the surface
			 * view callback. This means that when the app returns from sleep, it will also go via the same code path
			 * because the surface is recreated.
			 */

			FullscreenVideoView.this.onVideoStart();
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			if (FullscreenVideoView.this.loadingSpinner != null) {
				FullscreenVideoView.this.loadingSpinner.dismiss();
				FullscreenVideoView.this.loadingSpinner = null;
			}
			
			FullscreenVideoView.this.playbackTimer.cancel();
			FullscreenVideoView.this.playbackTimer.purge();
			
			String failureExplain;
			String failureType;
			
			switch(what) {
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
				failureType = "Server Died.";
				break;
			case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			default:
				failureType = "Unknown";
				break;
			}
			
			switch(extra) {
			case 0xfffffc0e: //MEDIA_ERROR_UNSUPPORTED
				failureExplain = "Unsupported.";
				break;
			case 0xffffff92: //MEDIA_ERROR_TIMED_OUT
				failureExplain = "Timed Out.";
				break;
			case 0x000000c8: //MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
				failureExplain = "Not Valid for Progressive Playback.";
				break;
			case 0xfffffc11: //MEDIA_ERROR_MALFORMED
				failureExplain = "Malformed.";
				break;
			case 0xfffffc14: //MEDIA_ERROR_IO
				failureExplain = "Error IO.";
				break;
			default:
				failureExplain = "Unknown.";
				break;
			}
			
			Logger.log("MediaPlayer Error! What: " + failureType + " Extra: " + failureExplain);
			
			if (FullscreenVideoView.this.listener != null) {
				FullscreenVideoView.this.listener.error();
			}
			
			return true; //setting to false just means it fires the completion handler.
		}

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			FullscreenVideoView.this.bufferProgress = percent;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			FullscreenVideoView.this.playbackTimer.cancel();
			FullscreenVideoView.this.playbackTimer.purge();
			
			if (FullscreenVideoView.this.listener != null) {
				FullscreenVideoView.this.listener.completed();
			}
		}
		
		@Override
	    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
			FullscreenVideoView.this.onVideoSizeChanged(width, height);
	    }
    }
}
