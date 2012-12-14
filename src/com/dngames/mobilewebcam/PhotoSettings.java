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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class PhotoSettings implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Context mContext = null;
    private SharedPreferences mPrefs = null;
    
	public boolean mUploadPictures = true;
	public boolean mFTPPictures = false;
	public boolean mStorePictures = false;
	public boolean mMailPictures = false;
	public boolean mDropboxPictures = false;
	
	final String mDefaulturl = "http://www.YOURDOMAIN.COM/mobilewebcam.php";
	final String mDefaultFTPurl = "FTP.YOURDOMAIN.COM";
    
	public String mURL = mDefaulturl;
	public String mLogin = "";
	public String mPassword = "";
	public String mDefaultname = "current.jpg";
	public boolean mFTPNumbered = true;
	public boolean mFTPTimestamp = false;
	public int mFTPKeepPics = 0;
	public String mFTP = mDefaultFTPurl;
	public int mFTPPort = 21;
	public String mFTPDir = "";
	public String mFTPLogin = "";
	public String mFTPPassword = "";
	public boolean mFTPPassive = true;
	public String mDropboxDir = "";
	public String mDropboxDefaultname = "current.jpg";
	public boolean mDropboxNumbered = true;
	public boolean mDropboxTimestamp = false;
	public int mDropboxKeepPics = 0;
	public int mRefreshDuration = 60000;
	public int mImageSize = 1;
	public int mCustomImageW = 320;
	public int mCustomImageH = 240;
	public enum ImageScaleMode { LETTERBOX, CROP, STRETCH, NOSCALE };
	public ImageScaleMode mCustomImageScale = ImageScaleMode.CROP;
	public int mImageCompression = 85;
	public boolean mAutoFocus = false;
	public boolean mForcePortraitImages = false;
	public boolean mFlipImages = false;
	public boolean mAutoRotateImages = false;
	public String mEmailReceiverAddress = "";
	public String mEmailSubject = "";
	public enum Mode { MANUAL, NORMAL, HIDDEN, BACKGROUND, BROADCASTRECEIVER };
	public Mode mMode = Mode.NORMAL;
	public boolean mMotionDetect = false;
	public int mMotionColorChange = 15;
	public int mMotionPixels = 25;
	public int mMotionDetectKeepAliveRefresh = 3600;
	public String mBroadcastReceiver = "";
	public boolean mNightDetect = false;
	public boolean mAutoStart = false;
	public int mReboot = 0; 
	public String mStartTime = "00:00";
	public String mEndTime = "24:00";
	public String mImprintDateTime = "yyyy/MM/dd   HH:mm:ss";
	public String mImprintText = "mobilewebcam " + android.os.Build.MODEL;
	public String mImprintStatusInfo = "Battery %03d%% %3.1f°C";
	public int mTextColor = Color.WHITE;
	public int mTextShadowColor = Color.BLACK;
	public int mTextBackgroundColor = Color.TRANSPARENT;
	public boolean mTextBackgroundLine = true;
	public int mTextX = 15, mTextY = 3;
	public int mDateTimeColor = Color.WHITE;
	public int mDateTimeShadowColor = Color.BLACK;
	public int mDateTimeBackgroundColor = Color.TRANSPARENT;
	public boolean mDateTimeBackgroundLine = true;
	public int mDateTimeX = 85, mDateTimeY = 97;
	public int mStatusInfoX = 85, mStatusInfoY = 92;
	public boolean mImprintGPS = false;
	public boolean mImprintLocation = false;
	public int mGPSX = 85, mGPSY = 87;
	public boolean mImprintPicture = false;
	public boolean mFilterPicture = false;
	public int mFilterType = 0;
	public boolean mStoreGPS = false;
	public boolean mNoToasts = false;
	public boolean mFullWakeLock = true;
	public boolean mCameraStartupEnabled = true;
	public boolean mMobileWebCamEnabled = true;
	boolean mShutterSound = true;
	public boolean mFrontCamera = false;
	public int mZoom = 0;
	public String mWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
	public boolean mCameraFlash = false;
	
	public String mMailAddress = "";
	public String mMailPassword = "";
	public String mMailHost = "smtp.gmail.com";
	public String mMailPort = "465";
	public boolean mMailSSL = true;

	public int mServerFreq = 1;
	public int mFTPFreq = 1;
	public int mMailFreq = 1;
	public int mDropboxFreq = 1;
	public int mStoreFreq = 1;
	    
	public Bitmap mImprintBitmap = null;
	
	public static int getEditInt(Context c, SharedPreferences prefs, String name, int d)
	{
    	String v = prefs.getString(name, Integer.toString(d));
        if(v.length() < 1 || v.length() > 9)
        	return d;
    	int i = 0;
    	try
    	{
    		i = Integer.parseInt(v);
    	}
    	catch(NumberFormatException e)
    	{
    		if(e.getMessage() != null)
    			Toast.makeText(c, e.getMessage(), Toast.LENGTH_LONG);
    		else
    			Toast.makeText(c, e.toString(), Toast.LENGTH_LONG);
    	}
    	return i;
	}
	
    public PhotoSettings(Context c)
	{
    	mContext = c;
        mPrefs = c.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);

		mPrefs.registerOnSharedPreferenceChangeListener(this);  

/*		mImprintText = mPrefs.getString("imprint_text", "mobilewebcam " + android.os.Build.MODEL);
		SharedPreferences.Editor edit = mPrefs.edit();
		edit.putString("imprint_text", mImprintText);
		edit.commit(); */

        onSharedPreferenceChanged(mPrefs, null);
	}
    
    private static int GetPrefColor(SharedPreferences prefs, String name, String def, int c)
    {
    	String v = prefs.getString(name, def);
	    if(v.length() == 9)
		{
			try
			{
	    		return Color.parseColor(v);
			}
			catch(IllegalArgumentException e)
			{
				MobileWebCam.LogE("Wrong color string: '" + v + "'");
				e.printStackTrace();
			}
		}

	    return c;
	}
    
	@Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
	{
		mMobileWebCamEnabled = prefs.getBoolean("mobilewebcam_enabled", true);
		
    	mUploadPictures = prefs.getBoolean("server_upload", true);
    	mFTPPictures = prefs.getBoolean("ftpserver_upload", false);
    	mStorePictures = prefs.getBoolean("cam_storepictures", false);
		mMailPictures = prefs.getBoolean("cam_mailphoto", false);
    	mDropboxPictures = prefs.getBoolean("dropbox_upload", false);

		mFTPFreq = Math.max(getEditInt(mContext, prefs, "ftp_every", 1), 1);

		mURL = prefs.getString("cam_url", mDefaulturl);
        mLogin = prefs.getString("cam_login", "");
        mPassword = prefs.getString("cam_password", "");
        
        mDefaultname = prefs.getString("ftpserver_defaultname", "current.jpg");
        mFTPNumbered = prefs.getBoolean("cam_filenames", true);
        mFTPTimestamp = prefs.getBoolean("cam_datetime", false);
        mFTPKeepPics = getEditInt(mContext, prefs, "ftp_keepoldpics", 0);
		mFTP = prefs.getString("ftpserver_url", mDefaultFTPurl);
		mFTPPort = getEditInt(mContext, prefs, "ftp_port", 21);
		mFTPDir = prefs.getString("ftp_dir", "");
        mFTPLogin = prefs.getString("ftp_login", "");
        mFTPPassword = prefs.getString("ftp_password", "");
        mFTPPassive = prefs.getBoolean("cam_passiveftp", true);

		mDropboxDir = prefs.getString("dropbox_dir", "");
        mDropboxDefaultname = prefs.getString("dropbox_defaultname", "current.jpg");
        mDropboxNumbered = prefs.getBoolean("dropbox_filenames", false);
        mDropboxTimestamp = prefs.getBoolean("dropbox_datetime", false);
        mDropboxKeepPics = getEditInt(mContext, prefs, "dropbox_keepoldpics", 0);

        mRefreshDuration = getEditInt(mContext, prefs, "cam_refresh", 60) * 1000;
        
        mImageSize = getEditInt(mContext, prefs, "picture_size_sel", 1);
        mCustomImageW = getEditInt(mContext, prefs, "picture_size_custom_w", 320);
        mCustomImageH = getEditInt(mContext, prefs, "picture_size_custom_h", 240);
        mCustomImageScale = Enum.valueOf(ImageScaleMode.class, prefs.getString("custompicscale", "CROP"));
        mImageCompression = prefs.getInt("picture_compression", 85);
        mAutoFocus = prefs.getBoolean("picture_autofocus", false);
        
        mFrontCamera = prefs.getBoolean("cam_front", false);
        mCameraFlash = prefs.getBoolean("cam_flash", false);
        mZoom = getEditInt(mContext, prefs, "zoom", 0);        
        mWhiteBalance = prefs.getString("whitebalance", Camera.Parameters.WHITE_BALANCE_AUTO);
        
        mAutoRotateImages = prefs.getBoolean("picture_autorotate", false);
		mForcePortraitImages = prefs.getBoolean("picture_rotate", false);
		mFlipImages = prefs.getBoolean("picture_flip", false); 
        mEmailReceiverAddress = prefs.getString("cam_email", "");
		mEmailSubject = prefs.getString("cam_emailsubject", "");

		mMotionDetect = prefs.getBoolean("motion_detect", false);
		mMotionColorChange = prefs.getInt("motion_change", 15);
		mMotionPixels = prefs.getInt("motion_value", 25);
		mMotionDetectKeepAliveRefresh = getEditInt(mContext, prefs, "motion_keepalive_refresh", 3600);
		mBroadcastReceiver = prefs.getString("broadcast_activation", "");
		mNightDetect = prefs.getBoolean("night_detect", false);
		mAutoStart = prefs.getBoolean("autostart", false);
		mCameraStartupEnabled = prefs.getBoolean("cam_autostart", true);
        mShutterSound = prefs.getBoolean("shutter", true); 
        mReboot = getEditInt(mContext, prefs, "reboot", 0);
		mStartTime = prefs.getString("activity_starttime", "00:00");
		mEndTime = prefs.getString("activity_endtime", "24:00");
		mImprintDateTime = prefs.getString("imprint_datetimeformat", "yyyy/MM/dd   HH:mm:ss");
		mDateTimeColor = GetPrefColor(prefs, "datetime_color", "#FFFFFFFF", Color.WHITE);
		mDateTimeShadowColor = GetPrefColor(prefs, "datetime_shadowcolor", "#FFFFFFFF", Color.BLACK);
		mDateTimeBackgroundColor = GetPrefColor(prefs, "datetime_backcolor", "#00000000", Color.TRANSPARENT);
        mDateTimeBackgroundLine = prefs.getBoolean("datetime_fillline", true); 
		mDateTimeX = prefs.getInt("datetime_x", 85);
		mDateTimeY = prefs.getInt("datetime_y", 97);
	
		mImprintText = prefs.getString("imprint_text", "mobilewebcam " + android.os.Build.MODEL);
		mTextColor = GetPrefColor(prefs, "text_color", "#FFFFFFFF", Color.WHITE);
		mTextShadowColor = GetPrefColor(prefs, "text_shadowcolor", "#FFFFFFFF", Color.BLACK);
		mTextBackgroundColor = GetPrefColor(prefs, "text_backcolor", "#00000000", Color.TRANSPARENT);
        mTextBackgroundLine = prefs.getBoolean("text_fillline", true); 
		mTextX = prefs.getInt("text_x", 15);
		mTextY = prefs.getInt("text_y", 3);

		mImprintStatusInfo = prefs.getString("imprint_statusinfo", "Battery %03d%% %3.1f°C");
		mStatusInfoX = prefs.getInt("statusinfo_x", 85);
		mStatusInfoY = prefs.getInt("statusinfo_y", 92);

		mImprintGPS = prefs.getBoolean("imprint_gps", false);
		mImprintLocation = prefs.getBoolean("imprint_location", false);
		mGPSX = prefs.getInt("gps_x", 85);
		mGPSY = prefs.getInt("gps_y", 87);
		
		mImprintPicture = prefs.getBoolean("imprint_picture", false);
		mFilterPicture = false; //***prefs.getBoolean("filter_picture", false);
        mFilterType = getEditInt(mContext, prefs, "filter_sel", 0);
		mStoreGPS = prefs.getBoolean("store_gps", false);
		
		mMailAddress = prefs.getString("email_sender", "");
		mMailPassword = prefs.getString("email_password", "");
		mMailHost = prefs.getString("email_host", "smtp.gmail.com");
		mMailPort = prefs.getString("email_port", "465");
		mMailSSL = prefs.getBoolean("email_ssl", true);

		if(mImprintPicture)
		{
			File path = new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/");
	    	if(path.exists())
	    	{
				if(mImprintBitmap != null)
					mImprintBitmap.recycle();
				mImprintBitmap = null;
	
				File file = new File(path, "imprint.png");
				try
				{
					FileInputStream in = new FileInputStream(file);
					mImprintBitmap = BitmapFactory.decodeStream(in);
					in.close();
				}
				catch(IOException e)
				{
					Toast.makeText(mContext, "Error: unable to read imprint bitmap " + file.getName() + "!", Toast.LENGTH_SHORT).show();
					mImprintBitmap = null;
				}
				catch(OutOfMemoryError e)
				{
					Toast.makeText(mContext, "Error: imprint bitmap " + file.getName() + " too large!", Toast.LENGTH_LONG).show();
					mImprintBitmap = null;				
				}
	    	}
			if(mImprintBitmap == null)
			{
				try
				{
					mImprintBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.imprint);
				}
				catch(OutOfMemoryError e)
				{
					Toast.makeText(mContext, "Error: default imprint bitmap too large!", Toast.LENGTH_LONG).show();
					mImprintBitmap = null;
				}
			}
		}
		
		mNoToasts = prefs.getBoolean("no_messages", false);
		mFullWakeLock = prefs.getBoolean("full_wakelock", true);
		
        switch(getEditInt(mContext, prefs, "camera_mode", 1))
		{
		case 0:
			mMode = Mode.MANUAL;
			break;
		case 2:
			mMode = Mode.HIDDEN;
			break;
		case 3:
			mMode = Mode.BACKGROUND;
			break;
		case 4:
			mMode = Mode.BROADCASTRECEIVER;
			break;
		case 1:
		default:
			mMode = Mode.NORMAL;
			break;
		}		
    }
	
	public void EnableMobileWebCam(boolean enabled)
	{
		SharedPreferences.Editor edit = mPrefs.edit();
		edit.putBoolean("mobilewebcam_enabled", enabled);
		edit.commit();
	}
}