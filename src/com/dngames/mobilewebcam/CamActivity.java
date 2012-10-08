/* Copyright 2012 Michael Haar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
	        if(mSettings.mMode == Mode.BACKGROUND || mSettings.mMode == Mode.BROADCASTRECEIVER)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "MobileWebCam");
	        else if(mSettings.mFullWakeLock)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MobileWebCam");
	        else
	        	mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWebCam");
	        mWakeLock.acquire();
    	}
        
    	if(mPreview != null)
    		mPreview.onResume();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	if(mPreview != null)
    		mPreview.onPause();

/*    	if(mWakeLock != null)
    	{
			if(mWakeLock.isHeld())
				mWakeLock.release();
			mWakeLock = null;
    	}*/
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