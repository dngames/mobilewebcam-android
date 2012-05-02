package com.dngames.mobilewebcam;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import android.os.Bundle;
import android.os.PowerManager;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import 	android.content.Intent;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

// ----------------------------------------------------------------------

public class TakeHiddenPicture extends CamActivity
{
	KeyguardLock mLock = null;
	
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
		 WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);    	
    	
//        PowerManager pm = (PowerManager)TakeHiddenPicture.this.getSystemService(Context.POWER_SERVICE);
//        if(!pm.isScreenOn())
        {
/*	        WindowManager.LayoutParams lp = getWindow().getAttributes();
	        lp.screenBrightness = 0.0f;
	        getWindow().setAttributes(lp);
*/
//	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	        KeyguardManager mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
	        mLock = mKeyguardManager.newKeyguardLock("MobileWebCam");
	        mLock.disableKeyguard();	        
        }
        
        mLayout = R.layout.hiddenlayout;
    	super.onCreate(savedInstanceState);

/*    	mDrawOnTop.setVisibility(View.INVISIBLE);
    	mPreview.setVisibility(View.INVISIBLE);
    	mTextView.setVisibility(View.INVISIBLE);
    	mMotionTextView.setVisibility(View.INVISIBLE);*/
    }

	@Override
	public boolean dispatchKeyEvent (KeyEvent event)
	{
		return super.dispatchKeyEvent(event);
	}
	
    @Override
    public void onResume()
    {
/*        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.0f;
        getWindow().setAttributes(lp);
*/
    	if(!MobileWebCam.gIsRunning)
    	{
	    	if(mLock == null)
	    	{
		        KeyguardManager mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
		        mLock = mKeyguardManager.newKeyguardLock("MobileWebCam");
		        mLock.disableKeyguard();
	    	}
	        
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
    	}

    	super.onResume();

    	if(!MobileWebCam.gIsRunning)
    	{
			mPreview.setVisibility(View.VISIBLE);
			mDrawOnTop.setVisibility(View.INVISIBLE);
    		
/*			Bundle extras=getIntent().getExtras();
	    	if(extras != null)
			{
	    		if(extras.getString("alarm") != null && extras.getString("alarm").startsWith("photo"))
				{
					if(mPreview != null)
					{
						mPreview.TakePhoto();
					}
				}
	    	}*/
			
			// timeout in case anything went wrong!
			mHandler.removeCallbacks(mTimeOut);
			mHandler.postDelayed(mTimeOut, 120 * 1000);
    	}
    	else
    	{
    		finish();
    	}
    	Log.v("MobileWebCam", "TakeHiddenPicture.onResume");
    }
    
    private Runnable mTimeOut = new Runnable()
	{
		@Override
		public void run()
		{
			MobileWebCam.LogE("TakeHiddenPicture timeout - finish!");
			if(mLock != null)
			{
		    	mLock.reenableKeyguard();
		    	mLock = null;
			}
			finish();
		}
	};

    @Override
    public void onPause()
    {
    	super.onPause();

    	Log.v("MobileWebCam", "TakeHiddenPicture.onPause");
    	
		mHandler.removeCallbacks(mTimeOut);
    	if(mLock != null)
    	{
	    	mLock.reenableKeyguard();
	    	mLock = null;
    	}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();

    	Log.v("MobileWebCam", "TakeHiddenPicture.onDestroy");
    	
		mHandler.removeCallbacks(mTimeOut);
    	if(mLock != null)
    	{
	    	mLock.reenableKeyguard();
	    	mLock = null;
    	}
    }
    
	@Override
	protected void onNewIntent(Intent intent)
	 {
    	if(!MobileWebCam.gIsRunning)
    	{
			setIntent(intent); //must store the new intent unless getIntent() will return the old one
	
			Bundle extras=intent.getExtras();
	    	if(extras != null)
			{
	    		if(extras.getString("alarm") != null && extras.getString("alarm").startsWith("photo"))
				{
					if(mPreview != null)
					{
						mPreview.TakePhoto();
					}
				}
	    	}
	    }
    	else
    	{
    		finish();
    	}
	}    
}