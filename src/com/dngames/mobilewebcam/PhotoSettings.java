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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
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
        String val(); 
        String help() default "";
        String category() default "";
        String htmltype() default "text";
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
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BooleanPref
    {
        String key(); 
        boolean val();
        String help() default "";
        String category() default "";
    }

	@BooleanPref(key = "ftpserver_upload", val = false, help = "upload to ftp", category = "Upload")
	public boolean mFTPPictures = false;
	@BooleanPref(key = "cam_storepictures", val = false, help = "store on SDCard", category = "Upload")
	public boolean mStorePictures = false;
	
	final String mDefaultFTPurl = "FTP.YOURDOMAIN.COM";
    
	@StringPref(key = "cam_url", val = mDefaulturl)
	public String mURL = mDefaulturl;
    @StringPref(key = "cam_login", val = "")
	public String mLogin = "";
    @StringPref(key = "cam_password", val = "")
	public String mPassword = "";
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
    @BooleanPref(key = "picture_autofocus", val = false)
	public boolean mAutoFocus = false;
	@BooleanPref(key = "picture_rotate", val = false, help = "rotate picture to portrait", category = "Picture")
	public boolean mForcePortraitImages = false;
	@BooleanPref(key = "picture_flip", val = false)
	public boolean mFlipImages = false;
    @BooleanPref(key = "picture_autorotate", val = false)
	public boolean mAutoRotateImages = false;
	public enum Mode { MANUAL, NORMAL, HIDDEN, BACKGROUND, BROADCASTRECEIVER };
	public Mode mMode = Mode.NORMAL;
	@BooleanPref(key = "motion_detect", val = false, help = "motion detection enabled", category = "Activity")
	public boolean mMotionDetect = false;
	@IntPref(key = "motion_change", val = 15)
	public int mMotionColorChange = 15;
	@IntPref(key = "motion_value", val = 25)
	public int mMotionPixels = 25;
	@EditIntPref(key = "motion_keepalive_refresh", val = 3600, factor = 1000)
	public int mMotionDetectKeepAliveRefresh = 3600 * 1000;
	@StringPref(key = "broadcast_activation", val = "")
	public String mBroadcastReceiver = "";
	@BooleanPref(key = "night_detect", val = false, help = "no dark/night picture upload.", category = "Activity")
	public boolean mNightDetect = false;
	@BooleanPref(key = "night_autoflash", val = false, help = "auto flashlight for dark pictures", category = "Activity")
	public boolean mNightAutoFlash = false;
	public boolean mAutoStart = false;
	@EditIntPref(key = "reboot", val = 0)
	public int mReboot = 0; 
	@StringPref(key = "activity_starttime", val = "00:00", help = "Time Start", htmltype = "time", category = "Activity")
	public String mStartTime = "00:00";
	@StringPref(key = "activity_endtime", val = "24:00", help = "Time End", htmltype = "time", category = "Activity")
	public String mEndTime = "24:00";
	@BooleanPref(key = "lowbattery_pause", val = false, help = "low battery pause", category = "Activity")
	public boolean mLowBatteryPause = false;
	@StringPref(key = "imprint_datetimeformat", val = "yyyy/MM/dd   HH:mm:ss")
	public String mImprintDateTime = "yyyy/MM/dd   HH:mm:ss";
	@StringPref(key = "imprint_text", val = "mobilewebcam", help = "Title", category = "Picture")
	public String mImprintText = "mobilewebcam " + android.os.Build.MODEL;
	@StringPref(key = "imprint_statusinfo", val = "Battery %d%% %.1f°C")
	public String mImprintStatusInfo = "Battery %03d%% %3.1f�C";
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
	public int mStatusInfoBackgroundColor = Color.TRANSPARENT;
	public boolean mStatusInfoBackgroundLine = false;	
	@BooleanPref(key = "imprint_gps", val = false)
	public boolean mImprintGPS = false;
	@BooleanPref(key = "imprint_location", val = false)
	public boolean mImprintLocation = false;
	public int mGPSX = 85, mGPSY = 87;
	public Paint.Align mGPSAlign = Paint.Align.CENTER;
	public boolean mGPSBackgroundLine = false;
	@BooleanPref(key = "imprint_picture", val = false)
	public boolean mImprintPicture = false;
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
    @EditIntPref(key = "zoom", val = 0, help = "Zoom 0-100", category = "Picture")        
	public int mZoom = 0;
    @StringPref(key = "whitebalance", val = Camera.Parameters.WHITE_BALANCE_AUTO)
	public String mWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
    @BooleanPref(key = "cam_flash", val = false, help = "flashlight enabled", category = "Activity")
	public boolean mCameraFlash = false;
	
	@EditIntPref(key = "ftp_every", val = 1, min = 1)
	public int mFTPFreq = 1;
	@EditIntPref(key = "store_every", val = 1, min = 1)
	public int mStoreFreq = 1;
	
	@BooleanPref(key = "log_upload", val = false, help = "send log", category = "Activity")
	public boolean mLogUpload = false;
	    
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
    	if(key == "cam_refresh")
    	{
    		int new_refresh = getEditInt(mContext, prefs, "cam_refresh", 60);
    		if(!mNoToasts && new_refresh != mRefreshDuration)
    			Toast.makeText(mContext, "Camera refresh set to " + new_refresh + " seconds!", Toast.LENGTH_SHORT).show();
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
					} catch (Exception e) {
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
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
				{
					try {
						f.set(this, prefs.getString(sp.key(), sp.val()));
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

		mStatusInfoX = prefs.getInt("statusinfo_x", 2);
		mStatusInfoY = prefs.getInt("statusinfo_y", 98);
		mStatusInfoAlign = Paint.Align.valueOf(prefs.getString("statusinfo_imprintalign", "LEFT"));
		mStatusInfoBackgroundColor = GetPrefColor(prefs, "statusinfo_backcolor", "#00000000", Color.TRANSPARENT);
		mStatusInfoBackgroundLine = prefs.getBoolean("statusinfo_fillline", false); 

		mGPSX = prefs.getInt("gps_x", 98);
		mGPSY = prefs.getInt("gps_y", 2);
		mGPSAlign = Paint.Align.valueOf(prefs.getString("gps_imprintalign", "RIGHT"));
		mGPSBackgroundLine = prefs.getBoolean("gps_fillline", false); 
		
		mFilterPicture = false; //***prefs.getBoolean("filter_picture", false);
        mFilterType = getEditInt(mContext, prefs, "filter_sel", 0);
		
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
	
	public static String DumpSettings(SharedPreferences prefs)
	{
		StringBuilder s = new StringBuilder();

		Map<String, Object[]> all = new HashMap<String, Object[]>();
		
		for(Field f : PhotoSettings.class.getFields())
		{
			{
				BooleanPref bp = f.getAnnotation(BooleanPref.class);
				if(bp != null)
				{
					boolean val = prefs.getBoolean(bp.key(), bp.val());
					all.put(bp.key(), new Object[] { val, bp.help() });
				}
			}
			{
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
				{
					String val = prefs.getString(sp.key(), sp.val());
					all.put(sp.key(), new Object[] { val, sp.help() });
				}
			}
			{
				EditIntPref ip = f.getAnnotation(EditIntPref.class);
				if(ip != null)
				{
					String val = prefs.getString(ip.key(), "" + ip.val());
					all.put(ip.key(), new Object[] { val, ip.help() });
				}
			}
			{
				IntPref ip = f.getAnnotation(IntPref.class);
				if(ip != null)
				{
					int val = prefs.getInt(ip.key(), ip.val());
					all.put(ip.key(), new Object[] { val, ip.help() });
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
							PhotoSettings.GETSettings(result, prefs);
						}
					}
					else if(!noToasts)
						Toast.makeText(context, "GET config failed!", Toast.LENGTH_SHORT).show();
				}					
			}.execute();
		}
	}
	
	public static void GETSettings(String configtext, SharedPreferences prefs)
	{
		String[] settings = configtext.split("\n");
		
		Editor edit = prefs.edit();
		Map<String, Object> all = new HashMap<String, Object>();
		
		for(Field f : PhotoSettings.class.getFields())
		{
			{
				BooleanPref bp = f.getAnnotation(BooleanPref.class);
				if(bp != null)
					all.put(bp.key(), bp.val());
			}
			{
				StringPref sp = f.getAnnotation(StringPref.class);
				if(sp != null)
					all.put(sp.key(), sp.val());
			}
			{
				EditIntPref ip = f.getAnnotation(EditIntPref.class);
				if(ip != null)
					all.put(ip.key(), ip.val() + "");
			}
			{
				IntPref ip = f.getAnnotation(IntPref.class);
				if(ip != null)
					all.put(ip.key(), ip.val());
			}
		}
		
		for(Map.Entry<String, ?> p : all.entrySet())
		{
			for(String s : settings)
			{
				if(s.startsWith(p.getKey()))
				{
					try
					{
						String value = s.split(":", 2)[1];
						if(value.length() > 0)
						{
							Class c = p.getValue().getClass();
							Object val = parseObjectFromString(value, c);
	
							if(c == String.class)
								edit.putString(p.getKey(), (String)val);
							else if(c == Boolean.class)
								edit.putBoolean(p.getKey(), (Boolean)val);
							else if(c == String.class)
								edit.putString(p.getKey(), val.toString());
							else if(c == Integer.class)
								edit.putInt(p.getKey(), (Integer)val);
							else
								throw new UnsupportedOperationException(c.toString());
						}
						else
						{
							MobileWebCam.LogE("Warning: config.txt entry '" + p.getKey() + "' value is empty!");
						}
					}
					catch(Exception e)
					{
						if(e.getMessage() != null)
							MobileWebCam.LogE(e.getMessage());
						e.printStackTrace();
					}
					break;
				}
			}
		}
		edit.commit();
	}

	public void setCameraFlash(boolean on)
	{
		SharedPreferences.Editor edit = mPrefs.edit();
		edit.putBoolean("cam_flash", on);
		edit.commit();
	}	
}