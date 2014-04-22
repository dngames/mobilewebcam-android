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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class PhotoSettings implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Context mContext = null;
    private SharedPreferences mPrefs = null;
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface StringPref
    {
        String key(); 
        String val() default "";
        int valId() default 0; 
        String help() default "";
        String category() default "";
        String htmltype() default "text";
        boolean night() default false;
    }
    private static String getDefaultString(Context c, StringPref sp)
    {
    	if(sp.val().length() == 0 && sp.valId() != 0)
    		return c.getString(sp.valId());
    	else
    		return sp.val();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface IntPref
    {
        String key(); 
        int val(); 
        int factor() default 1; // factor to apply for calculations (settings var but not pref value)
        int min() default Integer.MIN_VALUE; // range min allowed value
        int max() default Integer.MAX_VALUE; // range max allowed value
        String help() default "";
        String category() default "";
        String select() default "";
        boolean night() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface EditFloatPref
    {
        String key(); 
        float val(); 
        float min() default Float.MIN_VALUE; // range min allowed value
        float max() default Float.MAX_VALUE; // range max allowed value
        String help() default "";
        String category() default "";
        boolean night() default false;
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface EditIntPref
    {
        String key(); 
        int val(); 
        int factor() default 1; // factor to apply for calculations (settings var but not pref value)
        int min() default Integer.MIN_VALUE; // range min allowed value
        int max() default Integer.MAX_VALUE; // range max allowed value
        String help() default "";
        String category() default "";
        String select() default "";
        boolean night() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BooleanPref
    {
        String key(); 
        boolean val();
        String help() default "";
        String category() default "";
        boolean night() default false;
    }

	@BooleanPref(key = "ftpserver_upload", val = false, help = "upload to ftp", category = "Upload")
	public boolean mFTPPictures = false;
	@BooleanPref(key = "cam_storepictures", val = false, help = "store on SDCard", category = "Upload")
	public boolean mStorePictures = false;
	@EditIntPref(key = "eventtrigger_pausetime", val = 60, factor = 1000, help = "Time between events", category = "Events")
	public int mEventTriggerPauseTime = 0 * 1000;
	
	final String mDefaultFTPurl = "FTP.YOURDOMAIN.COM";
    
    @StringPref(key = "cam_login", val = "")
	public String mLogin = "";
    @StringPref(key = "cam_password", val = "")
	public String mPassword = "";
    @BooleanPref(key = "cam_filename_datetime", val = false)
    public boolean mTimeStampedFilePostName;
    @StringPref(key = "ftpserver_defaultname", val = "current.jpg")
	public String mDefaultname = "current.jpg";
    @EditIntPref(key = "ftp_batchupload", val = 1, min = 1, help = "FTP Batch Upload", category = "Upload")
    public int mFTPBatch = 1;
    @BooleanPref(key = "ftp_batchalways", val = false, help = "FTP reliable upload", category = "Upload")
    public boolean mReliableUpload = false;
    @BooleanPref(key = "cam_filenames", val = true)
	public boolean mFTPNumbered = true;
    @BooleanPref(key = "cam_datetime", val = false)
	public boolean mFTPTimestamp = false;
    @EditIntPref(key = "ftp_keepoldpics", val = 0)
	public int mFTPKeepPics = 0;
	@StringPref(key = "ftpserver_url", val = mDefaultFTPurl)
	public String mFTP = mDefaultFTPurl;
	@EditIntPref(key = "ftp_port", val = 21)
	public int mFTPPort = 21;
	@StringPref(key = "ftp_dir", val = "")
	public String mFTPDir = "";
    @StringPref(key = "ftp_login", val = "")
	public String mFTPLogin = "";
    @StringPref(key = "ftp_password", val = "")
	public String mFTPPassword = "";
    @BooleanPref(key = "cam_passiveftp", val = true)
	public boolean mFTPPassive = true;
	@BooleanPref(key = "ftp_keep_open", val = false)
	public boolean mFTPKeepConnected = false;
    @StringPref(key = "sdcard_dir", val = "/MobileWebCam/")
    public String mSDCardDir = "/MobileWebCam/";
    @BooleanPref(key = "secondary_sd", val = false)
    public boolean mSecondarySD = false;
    @EditIntPref(key = "sdcard_keepoldpics", val = 0)
    public int mSDCardKeepPics = 0;
    
    @EditIntPref(key = "cam_refresh", val = 60, factor = 1000, help = "Refresh Duration", category = "Activity")
	public int mRefreshDuration = 60000;
    @EditIntPref(key = "picture_size_sel", val = 1, help = "Picture Size", select = "0 = Small, 1 = Normal, 2 = Medium, 3 = Large, 4 = Original, 5 = Custom", category = "Picture")
	public int mImageSize = 1;
    @EditIntPref(key = "picture_size_custom_w", val = 320, help = "Custom Picture Size Width", category = "Picture")
	public int mCustomImageW = 320;
    @EditIntPref(key = "picture_size_custom_h", val = 240, help = "Custom Picture Size Height", category = "Picture")
	public int mCustomImageH = 240;
	public enum ImageScaleMode { LETTERBOX, CROP, STRETCH, NOSCALE };
	public ImageScaleMode mCustomImageScale = ImageScaleMode.CROP;
    @IntPref(key = "picture_compression", val = 85)
	public int mImageCompression = 85;
    @BooleanPref(key = "picture_autofocus", val = false, help = "autofocus", category = "Picture")
	public boolean mAutoFocus = false;
	@BooleanPref(key = "picture_rotate", val = false, help = "rotate picture to portrait", category = "Picture")
	public boolean mForcePortraitImages = false;
	@BooleanPref(key = "picture_flip", val = false, help = "flip picture", category = "Picture")
	public boolean mFlipImages = false;
    @BooleanPref(key = "picture_autorotate", val = false)
	public boolean mAutoRotateImages = false;
	public enum Mode { MANUAL, NORMAL, HIDDEN, BACKGROUND };
	public Mode mMode = Mode.NORMAL;
	@BooleanPref(key = "motion_detect", val = false, help = "motion detection enabled", category = "Activity")
	public boolean mMotionDetect = false;
	@IntPref(key = "motion_change", val = 15)
	public int mMotionColorChange = 15;
	@IntPref(key = "motion_value", val = 25)
	public int mMotionPixels = 25;
	@EditIntPref(key = "motion_keepalive_refresh", val = 3600, factor = 1000)
	public int mMotionDetectKeepAliveRefresh = 3600 * 1000;
	@StringPref(key = "cam_broadcast_activation", val = "", help = "Triggered by Intent Broadcast", category = "Events")
	public String mBroadcastReceiver = "";
	@EditIntPref(key = "cam_intents_repeat", val = 1)
	public int mPhotoIntentRepeat = 1;
	@BooleanPref(key = "night_detect", val = false, help = "no dark/night picture upload.", category = "Night")
	public boolean mNightDetect = false;
	@BooleanPref(key = "night_auto_config", val = false, help = "automatic night detection", category = "Night")
	public boolean mNightAutoConfig = false;
	public boolean mNightAutoConfigEnabled = false;
	@BooleanPref(key = "night_ir_light", val = false, help = "recolor ir light pictures", category = "Night")
	public boolean mNightIRLight = false;
	@BooleanPref(key = "night_autobright", val = false, help = "auto brightness for night pictures", category = "Night")
	public boolean mNightAutoBrightness = false;

	@EditFloatPref(key = "night_autocontrastfactor", val = 3f, help = "Contrast", category = "Night")
	public float mNightAutoContrastFactor = 3;
	@EditFloatPref(key = "night_autobrightnessadd", val = 64f, help = "Brightness", category = "Night")
	public float mNightAutoBrightnessAdd = 64;
	@EditFloatPref(key = "night_autogreenfactor", val = 0.05f, help = "Green", category = "Night")
	public float mNightAutoGreenFactor = 0.05f;
	@IntPref(key = "night_autoexposure", val = 100, help = "Exposure", category = "Night")
	public int mNightAutoBrightnessExposure = 100;
	@StringPref(key = "night_autoswhitebalance", val = Camera.Parameters.WHITE_BALANCE_INCANDESCENT, help = "Whitebalance", category = "Night")
	public String mNightAutoBrightnessWhitebalance = Camera.Parameters.WHITE_BALANCE_INCANDESCENT;
	@StringPref(key = "night_autoscenemode", val = Camera.Parameters.SCENE_MODE_NIGHT, help = "Scenemode", category = "Night")
	public String mNightAutoBrightnessScenemode = Camera.Parameters.SCENE_MODE_NIGHT;
	@StringPref(key = "night_starttime", val = "00:00", help = "Night Start", htmltype = "time", category = "Night")
	public String mNightStartTime = "00:00";
	@StringPref(key = "night_endtime", val = "00:00", help = "Night End", htmltype = "time", category = "Night")
	public String mNightEndTime = "00:00";
	
	public boolean mAutoStart = false;
	
	@EditIntPref(key = "reboot", val = 0)
	public int mReboot = 0; 
	@EditIntPref(key = "cam_openeddelay", val = 500, help = "Cam Delay ms", category = "Activity") // ms to delay capture when camera is started (use to avoid overexposure in background mode)
	public int mDelayAfterCameraOpened = 500;
	@StringPref(key = "activity_starttime", val = "00:00", help = "Time Start", htmltype = "time", category = "Activity")
	public String mStartTime = "00:00";
	@StringPref(key = "activity_endtime", val = "00:00", help = "Time End", htmltype = "time", category = "Activity")
	public String mEndTime = "00:00";
	@BooleanPref(key = "lowbattery_pause", val = false, help = "low battery pause", category = "Activity")
	public boolean mLowBatteryPause = false;
	public static final int gLowBatteryPower = 5;
	@StringPref(key = "imprint_datetimeformat", val = "yyyy/MM/dd   HH:mm:ss")
	public String mImprintDateTime = "yyyy/MM/dd   HH:mm:ss";
	@StringPref(key = "imprint_text", val = "mobilewebcam", help = "Title", category = "Picture")
	public String mImprintText = "mobilewebcam " + android.os.Build.MODEL;
	@StringPref(key = "imprint_statusinfo", valId = R.string.battery_imprint_default)
	public String mImprintStatusInfo = "Battery %03d%% %3.1f\370C";
	public int mTextColor = Color.WHITE;
	public int mTextShadowColor = Color.BLACK;
	public int mTextBackgroundColor = Color.argb(0x80, 0xA0, 0x00, 0x00);
	public boolean mTextBackgroundLine = true;
	public int mTextX = 15, mTextY = 5;
	public Paint.Align mTextAlign = Paint.Align.LEFT;
	public float mTextFontScale = 6.0f;
	public String mTextFontname = "";
	public int mDateTimeColor = Color.WHITE;
	public int mDateTimeShadowColor = Color.BLACK;
	public int mDateTimeBackgroundColor = Color.argb(0x80, 0xA0, 0x00, 0x00);
	public boolean mDateTimeBackgroundLine = true;
	public int mDateTimeX = 85, mDateTimeY = 97;
	public Paint.Align mDateTimeAlign = Paint.Align.RIGHT;
	public float mDateTimeFontScale = 6.0f;
	public int mStatusInfoX = 85, mStatusInfoY = 92;
	public Paint.Align mStatusInfoAlign = Paint.Align.LEFT;
	public int mStatusInfoColor = Color.WHITE;
	public int mStatusInfoShadowColor = Color.BLACK;
	public int mStatusInfoBackgroundColor = Color.TRANSPARENT;
	public boolean mStatusInfoBackgroundLine = false;	
	public float mStatusInfoFontScale = 6.0f;
	@BooleanPref(key = "imprint_gps", val = false)
	public boolean mImprintGPS = false;
	@BooleanPref(key = "imprint_location", val = false)
	public boolean mImprintLocation = false;
	public int mGPSColor = Color.WHITE;
	public int mGPSShadowColor = Color.BLACK;
	public int mGPSBackgroundColor = Color.argb(0x80, 0xA0, 0x00, 0x00);
	public int mGPSX = 85, mGPSY = 87;
	public Paint.Align mGPSAlign = Paint.Align.RIGHT;
	public float mGPSFontScale = 6.0f;
	public boolean mGPSBackgroundLine = false;
	
	@BooleanPref(key = "imprint_picture", val = false, help = "stamp picture over photo", category = "Imprint")
	public boolean mImprintPicture = false;
	@StringPref(key = "imprint_picture_url", val = "", help = "download stamp picture", category = "Imprint")
	public String mImprintPictureURL = "";
	@BooleanPref(key = "imprint_picture_refresh", val = false, help = "reload stamp picture", category = "Imprint")
	public boolean mImprintPictureRefresh = false;
	public int mImprintPictureX = 0, mImprintPictureY = 0;
	@BooleanPref(key = "imprint_picture_stretch", val = true, help = "stretch stamp picture", category = "Imprint")
	public boolean mImprintPictureStretch = true;
	
	public boolean mFilterPicture = false;
	public int mFilterType = 0;
	@BooleanPref(key = "store_gps", val = false)
	public boolean mStoreGPS = false;
	public boolean mNoToasts = false;
	public boolean mFullWakeLock = true;
	public boolean mCameraStartupEnabled = true;
	@BooleanPref(key = "mobilewebcam_enabled", val = true)
	public boolean mMobileWebCamEnabled = true;
	boolean mShutterSound = true;
    @BooleanPref(key = "cam_front", val = false)
	public boolean mFrontCamera = false;
    public boolean mSetNightConfiguration = false;    
    @EditIntPref(key = "zoom", val = 0, help = "Zoom 0-100", category = "Picture", night = true)        
	public int mZoom = 0;
    @EditIntPref(key = "exposurecompensation", val = 50, help = "Exposure Compensation", category = "Picture", night = true)
	public int mExposureCompensation = 50;
    @StringPref(key = "whitebalance", val = Camera.Parameters.WHITE_BALANCE_AUTO, help = "White Balance", category = "Picture", night = true)
	public String mWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
    @StringPref(key = "scenemode", val = Camera.Parameters.SCENE_MODE_AUTO, help = "Scene Mode", category = "Picture", night = true)
	public String mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
    @StringPref(key = "coloreffect", val = Camera.Parameters.EFFECT_NONE, help = "Effect", category = "Picture", night = true)
	public String mColorEffect = Camera.Parameters.EFFECT_NONE;
    @BooleanPref(key = "cam_flash", val = false, help = "flashlight enabled", category = "Activity", night = true)
	public boolean mCameraFlash = false;
	
	@EditIntPref(key = "ftp_every", val = 1, min = 1)
	public int mFTPFreq = 1;
	@EditIntPref(key = "store_every", val = 1, min = 1)
	public int mStoreFreq = 1;
	
	@BooleanPref(key = "log_upload", val = false, help = "send log", category = "Activity")
	public boolean mLogUpload = false;
	    
	public static Object gImprintBitmapLock = new Object();
	public static Bitmap gImprintBitmap = null;
	
	private static int getEditInt(SharedPreferences prefs, String name, int d) throws NumberFormatException
	{
    	String v = prefs.getString(name, Integer.toString(d));
        if(v.length() < 1 || v.length() > 9)
        	return d;
    	return Integer.parseInt(v);
	}

	public static int getEditInt(Context c, SharedPreferences prefs, String name, int d) throws NumberFormatException
	{
    	int i = 0;
    	try
    	{
    		i = getEditInt(prefs, name, d);
    	}
    	catch(NumberFormatException e)
    	{
    		String msg = e.toString();
    		if(e.getMessage() != null)
    			msg = e.getMessage();
    		if(MobileWebCam.gIsRunning)
    		{
    			try
    			{
        			Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    			}
    			catch(RuntimeException er)
    			{
    				er.printStackTrace();
    			}
    		}
    		else
    			MobileWebCam.LogE(msg);
    	}
    	return i;
	}
	
	private static float getEditFloat(SharedPreferences prefs, String name, float d) throws NumberFormatException
	{
    	String v = prefs.getString(name, Float.toString(d));
        if(v.length() < 1 || v.length() > 9)
        	return d;
    	return Float.parseFloat(v);
	}	

	public static float getEditFloat(Context c, SharedPreferences prefs, String name, float d) throws NumberFormatException
	{
    	float f = 0.0f;
    	try
    	{
    		f = getEditFloat(prefs, name, d);
    	}
    	catch(NumberFormatException e)
    	{
    		String msg = e.toString();
    		if(e.getMessage() != null)
    			msg = e.getMessage();
    		if(MobileWebCam.gIsRunning)
    		{
    			try
    			{
        			Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    			}
    			catch(RuntimeException er)
    			{
    				er.printStackTrace();
    			}
    		}
    		else
    			MobileWebCam.LogE(msg);
    	}
    	return f;
	}
	
	public static Mode getCamMode(SharedPreferences prefs)
	{
		Mode m = Mode.NORMAL;
		try
		{
			int val = getEditInt(prefs, "camera_mode", 1);
			m = Mode.values()[val];
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return m;
	}
	
	private static String NIGHTPOSTFIX = "_night"; 
	
	public String getNightPreferencesPostfix()
	{
		return IsNight() ? NIGHTPOSTFIX : "";
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
    	if(key == "cam_refresh")
    	{
    		int new_refresh = getEditInt(mContext, prefs, "cam_refresh", 60);
    		String msg = "Camera refresh set to " + new_refresh + " seconds!";
    		if(MobileWebCam.gIsRunning)
    		{
	    		if(!mNoToasts && new_refresh != mRefreshDuration)
	    		{
	    			try
	    			{
	    				Toast.makeText(mContext.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	    			}
	    			catch(RuntimeException e)
	    			{
	    				e.printStackTrace();
	    			}
	    		}
    		}
    		else if(new_refresh != mRefreshDuration)
    		{
    			MobileWebCam.LogI(msg);
    		}
    	}
    	
		// get all preferences
		for(Field f : getClass().getFields())
		{
			{
				BooleanPref bp = f.getAnnotation(BooleanPref.class);
				if(bp != null)
				{
					try {
						f.setBoolean(this, prefs.getBoolean(bp.key(), bp.val()));
					} catch (Exception e)
					{
						Log.e("MobileWebCam", "Exception: " + bp.key() + " <- " + bp.val());
						e.printStackTrace();
					}
				}
			}
			{
				EditIntPref ip = f.getAnnotation(EditIntPref.class);
				if(ip != null)
				{
					try
					{
				        int eval = getEditInt(mContext, prefs, ip.key(), ip.val()) * ip.factor();
				        if(ip.max() != Integer.MAX_VALUE)
				        	eval = Math.min(eval, ip.max());
				        if(ip.min() != Integer.MIN_VALUE)
				        	eval = Math.max(eval, ip.min());
						f.setInt(this, eval);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			{
				IntPref ip = f.getAnnotation(IntPref.class);
				if(ip != null)
				{
					try
					{
				        int eval = prefs.getInt(ip.key(), ip.val()) * ip.factor();
				        if(ip.max() != Integer.MAX_VALUE)
				        	eval = Math.min(eval, ip.max());
				        if(ip.min() != Integer.MIN_VALUE)
				        	eval = Math.max(eval, ip.min());
						f.setInt(this, eval);
					}
					catch (Exception e)
					{
						// handle wrong set class
						e.printStackTrace();
						Editor edit = prefs.edit();
						edit.remove(ip.key());
						edit.putInt(ip.key(), ip.val());
						edit.commit();
						try {
							f.setInt(this, ip.val());
						} catch (IllegalArgumentException e1) {
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			{
				EditFloatPref fp = f.getAnnotation(EditFloatPref.class);
				if(fp != null)
				{
					try
					{
				        float eval = getEditFloat(mContext, prefs, fp.key(), fp.val());
				        if(fp.max() != Float.MAX_VALUE)
				        	eval = Math.min(eval, fp.max());
				        if(fp.min() != Float.MIN_VALUE)
				        	eval = Math.max(eval, fp.min());
						f.setFloat(this, eval);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			{
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
				{
					try {
						f.set(this, prefs.getString(sp.key(), getDefaultString(mContext, sp)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
        
	    mCustomImageScale = Enum.valueOf(ImageScaleMode.class, prefs.getString("custompicscale", "CROP"));
                
		mAutoStart = prefs.getBoolean("autostart", false);
		mCameraStartupEnabled = prefs.getBoolean("cam_autostart", true);
        mShutterSound = prefs.getBoolean("shutter", true);
		mDateTimeColor = GetPrefColor(prefs, "datetime_color", "#FFFFFFFF", Color.WHITE);
		mDateTimeShadowColor = GetPrefColor(prefs, "datetime_shadowcolor", "#FF000000", Color.BLACK);
		mDateTimeBackgroundColor = GetPrefColor(prefs, "datetime_backcolor", "#80FF0000", Color.argb(0x80, 0xFF, 0x00, 0x00));
        mDateTimeBackgroundLine = prefs.getBoolean("datetime_fillline", true); 
		mDateTimeX = prefs.getInt("datetime_x", 98);
		mDateTimeY = prefs.getInt("datetime_y", 98);
		mDateTimeAlign = Paint.Align.valueOf(prefs.getString("datetime_imprintalign", "RIGHT"));

		mDateTimeFontScale = (float)prefs.getInt("datetime_fontsize", 6);
	
		mTextColor = GetPrefColor(prefs, "text_color", "#FFFFFFFF", Color.WHITE);
		mTextShadowColor = GetPrefColor(prefs, "text_shadowcolor", "#FF000000", Color.BLACK);
		mTextBackgroundColor = GetPrefColor(prefs, "text_backcolor", "#80FF0000", Color.argb(0x80, 0xFF, 0x00, 0x00));
        mTextBackgroundLine = prefs.getBoolean("text_fillline", true); 
		mTextX = prefs.getInt("text_x", 2);
		mTextY = prefs.getInt("text_y", 2);
		mTextAlign = Paint.Align.valueOf(prefs.getString("text_imprintalign", "LEFT"));
		mTextFontScale = (float)prefs.getInt("infotext_fontsize", 6);
		mTextFontname = prefs.getString("infotext_fonttypeface", "");

		mStatusInfoColor = GetPrefColor(prefs, "statusinfo_color", "#FFFFFFFF", Color.WHITE);
		mStatusInfoShadowColor = GetPrefColor(prefs, "statusinfo_shadowcolor", "#FF000000", Color.BLACK);
		mStatusInfoX = prefs.getInt("statusinfo_x", 2);
		mStatusInfoY = prefs.getInt("statusinfo_y", 98);
		mStatusInfoAlign = Paint.Align.valueOf(prefs.getString("statusinfo_imprintalign", "LEFT"));
		mStatusInfoBackgroundColor = GetPrefColor(prefs, "statusinfo_backcolor", "#00000000", Color.TRANSPARENT);
		mStatusInfoFontScale = (float)prefs.getInt("statusinfo_fontsize", 6);
		mStatusInfoBackgroundLine = prefs.getBoolean("statusinfo_fillline", false); 

		mGPSColor = GetPrefColor(prefs, "gps_color", "#FFFFFFFF", Color.WHITE);
		mGPSShadowColor = GetPrefColor(prefs, "gps_shadowcolor", "#FF000000", Color.BLACK);
		mGPSX = prefs.getInt("gps_x", 98);
		mGPSY = prefs.getInt("gps_y", 2);
		mGPSAlign = Paint.Align.valueOf(prefs.getString("gps_imprintalign", "RIGHT"));
		mGPSBackgroundColor = GetPrefColor(prefs, "gps_backcolor", "#00000000", Color.TRANSPARENT);
		mGPSFontScale = (float)prefs.getInt("gps_fontsize", 6);
		mGPSBackgroundLine = prefs.getBoolean("gps_fillline", false); 

		mImprintPictureX = prefs.getInt("imprint_picture_x", 0);
		mImprintPictureY = prefs.getInt("imprint_picture_y", 0);
		
		mNightAutoConfigEnabled = prefs.getBoolean("night_auto_enabled", false);

    	mSetNightConfiguration = prefs.getBoolean("cam_nightconfiguration", false);
    	// override night camera parameters (read again with postfix)
    	getCurrentNightSettings();
		
		mFilterPicture = false; //***prefs.getBoolean("filter_picture", false);
        mFilterType = getEditInt(mContext, prefs, "filter_sel", 0);
		
		if(mImprintPicture)
		{
			if(mImprintPictureURL.length() == 0)
			{
				// sdcard image
				File path = new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/");
		    	if(path.exists())
		    	{
		    		synchronized(gImprintBitmapLock)
		    		{
						if(gImprintBitmap != null)
							gImprintBitmap.recycle();
						gImprintBitmap = null;
			
						File file = new File(path, "imprint.png");
						try
						{
							FileInputStream in = new FileInputStream(file);
							gImprintBitmap = BitmapFactory.decodeStream(in);
							in.close();
						}
						catch(IOException e)
						{
							Toast.makeText(mContext, "Error: unable to read imprint bitmap " + file.getName() + "!", Toast.LENGTH_SHORT).show();
							gImprintBitmap = null;
						}
						catch(OutOfMemoryError e)
						{
							Toast.makeText(mContext, "Error: imprint bitmap " + file.getName() + " too large!", Toast.LENGTH_LONG).show();
							gImprintBitmap = null;				
						}
		    		}
		    	}
	    	}
			else
			{
				DownloadImprintBitmap();
			}
			
			synchronized(gImprintBitmapLock)
			{
				if(gImprintBitmap == null)
				{
					// last resort: resource default
					try
					{
						gImprintBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.imprint);
					}
					catch(OutOfMemoryError e)
					{
						Toast.makeText(mContext, "Error: default imprint bitmap too large!", Toast.LENGTH_LONG).show();
						gImprintBitmap = null;
					}
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
		case 1:
		default:
			mMode = Mode.NORMAL;
			break;
		}
    }
	
	// depending on night state override some settings
	public void getCurrentNightSettings()
	{
		String nightsettings = getNightPreferencesPostfix();
		mZoom = getEditInt(mContext, mPrefs, "zoom" + nightsettings, mZoom);
    	mExposureCompensation = getEditInt(mContext, mPrefs, "exposurecompensation" + nightsettings, mExposureCompensation);
    	mWhiteBalance = mPrefs.getString("whitebalance" + nightsettings, mWhiteBalance);
    	mSceneMode = mPrefs.getString("scenemode" + nightsettings, mSceneMode);
    	mColorEffect = mPrefs.getString("coloreffect" + nightsettings, mColorEffect);
    	mCameraFlash = mPrefs.getBoolean("cam_flash", mCameraFlash);
	}
	
	public void DownloadImprintBitmap()
	{
		// get from URL
		Handler h = new Handler(mContext.getMainLooper());
	    h.post(new Runnable()
	    {
	        @Override
	        public void run()
	        {
	    		try
	    		{
					new AsyncTask<String, Void, Bitmap>()
					{
						@Override
						protected Bitmap doInBackground(String... params)
						{
							try
							{
								URL url = new URL(mImprintPictureURL);
								HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
								InputStream is = connection.getInputStream();
								return BitmapFactory.decodeStream(is);
							}
							catch (MalformedURLException e)
							{
								if(e.getMessage() != null)
									MobileWebCam.LogE("Imprint picture URL error: " + e.getMessage());
								else
									MobileWebCam.LogE("Imprint picture URL invalid!");								
								e.printStackTrace();
							}
							catch (IOException e)
							{
								if(e.getMessage() != null)
									MobileWebCam.LogE(e.getMessage());
								e.printStackTrace();
							}
							
							return null;
						}
						
						@Override
						protected void onPostExecute(Bitmap result)
						{
							if(result == null && !mNoToasts)
								Toast.makeText(mContext, "Imprint picture download failed!", Toast.LENGTH_SHORT).show();
							
							synchronized(PhotoSettings.gImprintBitmapLock)
							{
								PhotoSettings.gImprintBitmap = result;
							}
						}
					}.execute().get();
	    		}
	    		catch (Exception e)
	    		{
	    			if(e.getMessage() != null)
	    				MobileWebCam.LogE(e.getMessage());
	    			e.printStackTrace();
	    		}				
	        }
	    });			
	}
	
	public boolean IsNight()
	{
		boolean night = Preview.CheckInTime(new Date(), mNightStartTime, mNightEndTime, true);
		return night || mSetNightConfiguration || (mNightAutoConfig && mNightAutoConfigEnabled);
	}
	
	private boolean mLastNightSettings = false;
	
	public void SetCameraParameters(Camera.Parameters params, boolean configure)
	{
		if(!configure) // no auto detection or reset of stuff when just manually setting from options menu
		{
			// auto night detection switched off now?
			if(!mNightAutoConfig && mNightAutoConfigEnabled)
			{
				mNightAutoConfigEnabled = false;
				getCurrentNightSettings();
			}			

			if(IsNight() != mLastNightSettings)
			{
				// possibly override camera settings for night mode!
		        setCameraNightSettings(params, configure);
			}
		}

		if(NewCameraFunctions.isZoomSupported(params))
			NewCameraFunctions.setZoom(params, mZoom);
		if(NewCameraFunctions.getSupportedWhiteBalance(params) != null)
			NewCameraFunctions.setWhiteBalance(params, mWhiteBalance);
		if(NewCameraFunctions.getSupportedSceneModes(params) != null)
			NewCameraFunctions.setSceneMode(params, mSceneMode);
		if(NewCameraFunctions.getSupportedColorEffects(params) != null)
			NewCameraFunctions.setColorEffect(params, mColorEffect);
		if(NewCameraFunctions.isFlashSupported(params))
			NewCameraFunctions.setFlash(params, mCameraFlash ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);											
		int minexp = NewCameraFunctions.getMinExposureCompensation(params);
		int maxexp = NewCameraFunctions.getMaxExposureCompensation(params);
		if(minexp != 0 || maxexp != 0)
			NewCameraFunctions.setExposureCompensation(params, (maxexp - minexp) * mExposureCompensation / 100 + minexp);
		
		mLastNightSettings = IsNight();
	}

	// set all the configured camera parameters
	public void SetCameraParameters(Camera cam, boolean configure)
	{
		Camera.Parameters params = cam.getParameters();
		if(params != null)
		{
			SetCameraParameters(params, configure);
			try
			{
				cam.setParameters(params);
			}
			catch(RuntimeException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	// start taking pictures or stop it
	public void EnableMobileWebCam(boolean enabled)
	{
		SharedPreferences.Editor edit = mPrefs.edit();
		edit.putBoolean("mobilewebcam_enabled", enabled);
		edit.commit();
	}
	
	// write all current settings to a string
	public static String DumpSettings(Context context, SharedPreferences prefs)
	{
		StringBuilder s = new StringBuilder();

		Map<String, Object[]> all = new HashMap<String, Object[]>();
		
		for(Field f : PhotoSettings.class.getFields())
		{
			{
				BooleanPref bp = f.getAnnotation(BooleanPref.class);
				if(bp != null)
				{
					String key = bp.key();
					boolean val = prefs.getBoolean(key, bp.val());
					all.put(key, new Object[] { val, bp.help() });
					if(bp.night())
					{
						key = key + PhotoSettings.NIGHTPOSTFIX;
						val = prefs.getBoolean(key, bp.val());
						all.put(key, new Object[] { val, bp.help() });
					}
				}
			}
			{
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
				{
					String key = sp.key();
					String val = prefs.getString(key, getDefaultString(context, sp));
					all.put(key, new Object[] { val, sp.help() });
					if(sp.night())
					{
						key = key + PhotoSettings.NIGHTPOSTFIX;
						val = prefs.getString(key, getDefaultString(context, sp));
						all.put(key, new Object[] { val, sp.help() });
					}
				}
			}
			{
				EditIntPref ip = f.getAnnotation(EditIntPref.class);
				if(ip != null)
				{
					String key = ip.key();
					String val = prefs.getString(key, "" + ip.val());
					all.put(ip.key(), new Object[] { val, ip.help() });
					if(ip.night())
					{
						key = key + PhotoSettings.NIGHTPOSTFIX;
						val = prefs.getString(key, "" + ip.val());
						all.put(key, new Object[] { val, ip.help() });
					}
				}
			}
			{
				IntPref ip = f.getAnnotation(IntPref.class);
				if(ip != null)
				{
					String key = ip.key();
					int val = prefs.getInt(key, ip.val());
					all.put(ip.key(), new Object[] { val, ip.help() });
					if(ip.night())
					{
						key = key + PhotoSettings.NIGHTPOSTFIX;
						val = prefs.getInt(key, ip.val());
						all.put(key, new Object[] { val, ip.help() });
					}
				}
			}
			{
				EditFloatPref fp = f.getAnnotation(EditFloatPref.class);
				if(fp != null)
				{
					String key = fp.key();
					String val = prefs.getString(key, "" + fp.val());
					all.put(fp.key(), new Object[] { val, fp.help() });
					if(fp.night())
					{
						key = key + PhotoSettings.NIGHTPOSTFIX;
						val = prefs.getString(key, "" + fp.val());
						all.put(key, new Object[] { val, fp.help() });
					}
				}
			}
		}
		
		for(Map.Entry<String, ?> p : all.entrySet())
		{
			Object[] vals = (Object[])p.getValue();
			if(((String)vals[1]).length() > 0)
				s.append("// " + vals[1] + "\n");
			s.append(p.getKey() + ":" + vals[0] + "\n");
		}
		
		return s.toString();
	}
	
	private static <T> T parseObjectFromString(String s, Class<T> c) throws Exception
	{
	    return c.getConstructor(new Class[] {String.class }).newInstance(s);
	}
	
	private static int gLastGETSettingsPictureCnt = -1;
	
	public static void GETSettings(final Context context)
	{
		// check for new settings when done
		final SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		final String settingsurl = prefs.getString("remote_config_url", "");
		final int settingsfreq = Math.max(1, PhotoSettings.getEditInt(context, prefs, "remote_config_every", 1));
		final String login = prefs.getString("remote_config_login", "");
		final String password = prefs.getString("remote_config_password", "");
		final boolean noToasts = prefs.getBoolean("no_messages", false);
		if(settingsurl.length() > 0 && gLastGETSettingsPictureCnt < MobileWebCam.gPictureCounter && (MobileWebCam.gPictureCounter % settingsfreq) == 0)
		{
			gLastGETSettingsPictureCnt = MobileWebCam.gPictureCounter;

			Handler h = new Handler(context.getMainLooper());
		    h.post(new Runnable()
		    {
		        @Override
		        public void run()
		        {
					new AsyncTask<String, Void, String>()
					{
						@Override
						protected String doInBackground(String... params)
						{
							try
							{
								DefaultHttpClient httpclient = new DefaultHttpClient();
								if(login.length() > 0)
								{
									try
									{
				    					((AbstractHttpClient) httpclient).getCredentialsProvider().setCredentials(
					    						new AuthScope(null, -1),
					    						new UsernamePasswordCredentials(login, password));
									}
									catch(Exception e)
									{
										e.printStackTrace();
										if(e.getMessage() != null)
											MobileWebCam.LogE("http login " + e.getMessage());
										else
											MobileWebCam.LogE("http: unable to log in");
										
										return null;
									}
								}
								HttpGet get = new HttpGet(settingsurl);
								HttpResponse response = httpclient.execute(get);
								HttpEntity ht = response.getEntity();
						        BufferedHttpEntity buf = new BufferedHttpEntity(ht);
						        InputStream is = buf.getContent();
						        BufferedReader r = new BufferedReader(new InputStreamReader(is));
						        StringBuilder total = new StringBuilder();
						        String line;
						        while((line = r.readLine()) != null)
						            total.append(line + "\n");
								
								if(ht.getContentType().getValue().startsWith("text/plain"))
									return total.toString();
								else
									return "GET Config Error!\n" + total.toString(); 							
							}
							catch(Exception e)
							{
								e.printStackTrace();
								if(e.getMessage() != null)
								{
									MobileWebCam.LogE(e.getMessage());
									return "GET Config Error!\n" + e.getMessage(); 
								}						
							}

							return null;
						}
						
						@Override
						protected void onPostExecute(String result)
						{
							if(result != null)
							{
								if(result.startsWith("GET Config Error!\n"))
								{
									if(!noToasts)
										Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
								}
								else
								{
									PhotoSettings.GETSettings(context, result, prefs);
								}
							}
							else if(!noToasts)
								Toast.makeText(context, "GET config failed!", Toast.LENGTH_SHORT).show();
						}					
					}.execute();
		        }
		    });			
		}
	}
	
	public static void GETSettings(Context context, String configtext, SharedPreferences prefs)
	{
		String[] settings = configtext.split("\n");
		
		Editor edit = prefs.edit();
		Map<String, Object> all = new HashMap<String, Object>();
		
		for(Field f : PhotoSettings.class.getFields())
		{
			{
				BooleanPref bp = f.getAnnotation(BooleanPref.class);
				if(bp != null)
				{
					all.put(bp.key(), bp.val());
					if(bp.night())
						all.put(bp.key() + PhotoSettings.NIGHTPOSTFIX, bp.val());
				}
			}
			{
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
				{
					all.put(sp.key(), getDefaultString(context, sp));
					if(sp.night())
						all.put(sp.key() + PhotoSettings.NIGHTPOSTFIX, getDefaultString(context, sp));
				}
			}
			{
				EditIntPref ip = f.getAnnotation(EditIntPref.class);
				if(ip != null)
				{
					all.put(ip.key(), ip.val() + "");
					if(ip.night())
						all.put(ip.key() + PhotoSettings.NIGHTPOSTFIX, ip.val() + "");
				}
			}
			{
				IntPref ip = f.getAnnotation(IntPref.class);
				if(ip != null)
				{
					all.put(ip.key(), ip.val());
					if(ip.night())
						all.put(ip.key() + PhotoSettings.NIGHTPOSTFIX, ip.val());
				}
			}
			{
				EditFloatPref fp = f.getAnnotation(EditFloatPref.class);
				if(fp != null)
				{
					all.put(fp.key(), fp.val() + "");
					if(fp.night())
						all.put(fp.key() + PhotoSettings.NIGHTPOSTFIX, fp.val() + "");
				}
			}
		}
		
		for(String s : settings)
		{
			try
			{
				String[] setting = s.split(":", 2);
				String param = setting[0];
				if(all.containsKey(param))
				{
					String value = setting[1];
					if(value.length() > 0)
					{
						Class<? extends Object> c = all.get(param).getClass();
						Object val = parseObjectFromString(value, c);

						if(c == String.class)
							edit.putString(param, (String)val);
						else if(c == Boolean.class)
							edit.putBoolean(param, (Boolean)val);
						else if(c == Integer.class)
							edit.putInt(param, (Integer)val);
						else
							throw new UnsupportedOperationException(c.toString());
					}
					else
					{
						MobileWebCam.LogE("Warning: config.txt entry '" + param + "' value is empty!");
					}
				}
			}
			catch(Exception e)
			{
				if(e.getMessage() != null)
					MobileWebCam.LogE(e.getMessage());
				e.printStackTrace();
			}
		}
		edit.commit();
	}
	
	public void setCameraNightSettings(Camera.Parameters params, boolean configure)
	{
		boolean isnightvision = mNightAutoBrightness && IsNight();
		if(isnightvision)
		{
			// set night settings
        	mExposureCompensation = mNightAutoBrightnessExposure;
        	List<String> modes = NewCameraFunctions.getSupportedWhiteBalance(params);
        	if(modes != null)
        	{
        		for(String sm : modes)
        		{
        			if(sm.equals(mNightAutoBrightnessWhitebalance))
        				mWhiteBalance = mNightAutoBrightnessWhitebalance;
        		}
        	}
        	if(!mWhiteBalance.equals(mNightAutoBrightnessWhitebalance))
        		Log.e("MobileWebCam", "Auto night brightness white balance mode " + mNightAutoBrightnessWhitebalance + " not found!");
        	modes = NewCameraFunctions.getSupportedSceneModes(params);
        	if(modes != null)
        	{
        		for(String sm : modes)
        		{
        			if(sm.equals(mNightAutoBrightnessScenemode))
        				mSceneMode = mNightAutoBrightnessScenemode;
        		}
        	}
        	if(!mSceneMode.equals(mNightAutoBrightnessScenemode))
        		Log.e("MobileWebCam", "Auto night brightness scene mode " + mNightAutoBrightnessScenemode + " not found!");
		}
		else
		{
			getCurrentNightSettings();
		}
	}	
}