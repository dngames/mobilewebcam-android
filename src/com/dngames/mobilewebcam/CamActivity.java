package com.dngames.mobilewebcam;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.TextView;

public class CamActivity extends Activity
{
    public PhotoSettings mSettings = null;
    
    protected Preview mPreview = null;
	
    private PowerManager.WakeLock mWakeLock = null;	

    public DrawOnTop mDrawOnTop;
	public TextView mTextView = null;
	public TextView mMotionTextView = null;	
	
	protected SharedPreferences mPrefs;
    
    protected Handler mHandler = new Handler();
    
    protected int mLayout = R.layout.layout; 
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the window title.
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);
    
        mPrefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		mSettings = new PhotoSettings(CamActivity.this);

		setContentView(mLayout);
        mPreview = (Preview)findViewById(R.id.preview);
        mPreview.SetSettings(mSettings);
        
//***        if(DEBUG_MOTIONDETECT)
        {
        	mDrawOnTop = (DrawOnTop)findViewById(R.id.drawontop);
        }
		
		mTextView = (TextView)findViewById(R.id.status);
		mMotionTextView = (TextView)findViewById(R.id.motion);
		
        mSettings.EnableMobileWebCam(mSettings.mCameraStartupEnabled);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	if(mWakeLock == null)
    	{
	        PowerManager pm = (PowerManager)CamActivity.this.getSystemService(Context.POWER_SERVICE);
	        if(mSettings.mMode == Mode.BACKGROUND)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "MobileWebCam");
	        else if(mSettings.mFullWakeLock)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MobileWebCam");
	        else
	        	mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWebCam");
	        mWakeLock.acquire();
    	}
        
        mPreview.onResume();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	
		if(mWakeLock.isHeld())
			mWakeLock.release();
		mWakeLock = null;
		
		mPreview.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	
    	if(mPreview != null)
    		mPreview.onDestroy();
    	mPreview = null;
    }    
}