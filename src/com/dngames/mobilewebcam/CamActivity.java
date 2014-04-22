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
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CamActivity extends Activity
{
    public PhotoSettings mSettings = null;
    
    protected Preview mPreview = null;
	
	protected SharedPreferences mPrefs;
    
    protected Handler mHandler = new Handler();
    
    protected int mLayout = R.layout.layout;

    public DrawOnTop mDrawOnTop;
	public TextView mTextView = null;
	public TextView mCamNameView = null;
	public TextView mMotionTextView = null;	
	public TextView mNightTextView = null;	
	public LinearLayout mTextViewFrame = null;
	public RelativeLayout mCamNameViewFrame = null;
    
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Hide the window title.
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mPrefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		mSettings = new PhotoSettings(CamActivity.this);

		if(mSettings.mFullWakeLock)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(mLayout);
        mPreview = (Preview)findViewById(R.id.preview);
        mPreview.SetSettings(mSettings);
        
        mSettings.EnableMobileWebCam(mSettings.mCameraStartupEnabled);
    }
    
    private PowerManager.WakeLock mWakeLock = null;
    private WifiManager.WifiLock mWifiLock = null;
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
        if(mWakeLock == null || !mWakeLock.isHeld())
        {
        	// get lock for preview
	        PowerManager.WakeLock old = mWakeLock;

	        PowerManager pm = (PowerManager)CamActivity.this.getSystemService(Context.POWER_SERVICE);
	        if(mSettings.mMode == Mode.BACKGROUND)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "MobileWebCam");
	        else if(mSettings.mFullWakeLock)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MobileWebCam");
	        else
	        	mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWebCam");

	        mWakeLock.acquire();
//		    Log.v("MobileWebCam", "CamActivity WakeLock aquired!");
		    
		    if(old != null)
		    	old.release();
		    
		    WifiManager.WifiLock oldwifi = mWifiLock;
		    
			ConnectivityManager connmgr = (ConnectivityManager)CamActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(connmgr.getNetworkPreference() == ConnectivityManager.TYPE_WIFI)
			{
				WifiManager wmgr = (WifiManager)CamActivity.this.getSystemService(Context.WIFI_SERVICE);
				if(mWifiLock == null || !mWifiLock.isHeld())
				{
					mWifiLock = wmgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MobileWebCam.CamActivity");
					mWifiLock.acquire();
//				    Log.v("MobileWebCam", "CamActivity mWifiLock aquired!");
				}		
			}		    
		    
		    if(oldwifi != null)
		    	oldwifi.release();
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
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();

    	if(mPreview != null)
    		mPreview.onDestroy();
    	mPreview = null;
    }
    
    public void releaseLocks()
    {
		if(mWakeLock != null)
		{
			// release lock for preview
			PowerManager.WakeLock tmp = mWakeLock;
//			Log.v("MobileWebCam", "CamActivity WakeLock released!");
			mWakeLock = null;

			if(tmp.isHeld())
				tmp.release();

			WifiManager.WifiLock tmpwifi = mWifiLock;
//			Log.v("MobileWebCam", "CamActivity WifiLock released!");
			mWifiLock = null;

			if(tmpwifi != null && tmpwifi.isHeld())
				tmpwifi.release();
		}
    }
}