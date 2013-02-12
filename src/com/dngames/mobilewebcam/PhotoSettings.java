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
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface IntegerPref
    {
        String key(); 
        int val(); 
        int factor() default 1; // factor to apply for calculations (settings var but not pref value)
        int min() default Integer.MIN_VALUE; // range min allowed value
        int max() default Integer.MAX_VALUE; // range max allowed value
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BooleanPref
    {
        String key(); 
        boolean val(); 
    }

    @BooleanPref(key = "server_upload", val = true)
	public boolean mUploadPictures = true;
	@BooleanPref(key = "ftpserver_upload", val = false)
	public boolean mFTPPictures = false;
	@BooleanPref(key = "cam_storepictures", val = false)
	public boolean mStorePictures = false;
	@BooleanPref(key = "cam_mailphoto", val = false)
	public boolean mMailPictures = false;
	@BooleanPref(key = "dropbox_upload", val = false)
	public boolean mDropboxPictures = false;
	
	final String mDefaulturl = "http://www.YOURDOMAIN.COM/mobilewebcam.php";
	final String mDefaultFTPurl = "FTP.YOURDOMAIN.COM";
    
	@StringPref(key = "cam_url", val = mDefaulturl)
	public String mURL = mDefaulturl;
    @StringPref(key = "cam_login", val = "")
	public String mLogin = "";
    @StringPref(key = "cam_password", val = "")
	public String mPassword = "";
    @StringPref(key = "ftpserver_defaultname", val = "current.jpg")
	public String mDefaultname = "current.jpg";
    @BooleanPref(key = "use_sftp", val = false)
	public boolean mSFTP = false;
    @BooleanPref(key = "cam_filenames", val = true)
	public boolean mFTPNumbered = true;
    @BooleanPref(key = "cam_datetime", val = false)
	public boolean mFTPTimestamp = false;
    @IntegerPref(key = "ftp_keepoldpics", val = 0)
	public int mFTPKeepPics = 0;
	@StringPref(key = "ftpserver_url", val = mDefaultFTPurl)
	public String mFTP = mDefaultFTPurl;
	@IntegerPref(key = "ftp_port", val = 21)
	public int mFTPPort = 21;
	@StringPref(key = "ftp_dir", val = "")
	public String mFTPDir = "";
    @StringPref(key = "ftp_login", val = "")
	public String mFTPLogin = "";
    @StringPref(key = "ftp_password", val = "")
	public String mFTPPassword = "";
    @BooleanPref(key = "cam_passiveftp", val = true)
	public boolean mFTPPassive = true;
	@StringPref(key = "dropbox_dir", val = "")
	public String mDropboxDir = "";
    @StringPref(key = "dropbox_defaultname", val = "current.jpg")
	public String mDropboxDefaultname = "current.jpg";
    @BooleanPref(key = "dropbox_filenames", val = false)
	public boolean mDropboxNumbered = true;
    @BooleanPref(key = "dropbox_datetime", val = false)
	public boolean mDropboxTimestamp = false;
    @IntegerPref(key = "dropbox_keepoldpics", val = 0)
	public int mDropboxKeepPics = 0;
    
    @IntegerPref(key = "cam_refresh", val = 60, factor = 1000)
	public int mRefreshDuration = 60000;
    @IntegerPref(key = "picture_size_sel", val = 1)
	public int mImageSize = 1;
    @IntegerPref(key = "picture_size_custom_w", val = 320)
	public int mCustomImageW = 320;
    @IntegerPref(key = "picture_size_custom_h", val = 240)
	public int mCustomImageH = 240;
	public enum ImageScaleMode { LETTERBOX, CROP, STRETCH, NOSCALE };
	public ImageScaleMode mCustomImageScale = ImageScaleMode.CROP;
    @IntegerPref(key = "picture_compression", val = 85)
	public int mImageCompression = 85;
    @BooleanPref(key = "picture_autofocus", val = false)
	public boolean mAutoFocus = false;
	@BooleanPref(key = "picture_rotate", val = false)
	public boolean mForcePortraitImages = false;
	@BooleanPref(key = "picture_flip", val = false)
	public boolean mFlipImages = false;
    @BooleanPref(key = "picture_autorotate", val = false)
	public boolean mAutoRotateImages = false;
    @StringPref(key = "cam_email", val = "")
	public String mEmailReceiverAddress = "";
	@StringPref(key = "cam_emailsubject", val = "")
	public String mEmailSubject = "";
	public enum Mode { MANUAL, NORMAL, HIDDEN, BACKGROUND, BROADCASTRECEIVER };
	public Mode mMode = Mode.NORMAL;
	@BooleanPref(key = "motion_detect", val = false)
	public boolean mMotionDetect = false;
	@IntegerPref(key = "motion_change", val = 15)
	public int mMotionColorChange = 15;
	@IntegerPref(key = "motion_value", val = 25)
	public int mMotionPixels = 25;
	@IntegerPref(key = "motion_keepalive_refresh", val = 3600, factor = 1000)
	public int mMotionDetectKeepAliveRefresh = 3600 * 1000;
	@StringPref(key = "broadcast_activation", val = "")
	public String mBroadcastReceiver = "";
	@BooleanPref(key = "night_detect", val = false)
	public boolean mNightDetect = false;
	@BooleanPref(key = "night_autoflash", val = false)
	public boolean mNightAutoFlash = false;
	public boolean mAutoStart = false;
    @IntegerPref(key = "reboot", val = 0)
	public int mReboot = 0; 
	@StringPref(key = "activity_starttime", val = "00:00")
	public String mStartTime = "00:00";
	@StringPref(key = "activity_endtime", val = "24:00")
	public String mEndTime = "24:00";
	@StringPref(key = "imprint_datetimeformat", val = "yyyy/MM/dd   HH:mm:ss")
	public String mImprintDateTime = "yyyy/MM/dd   HH:mm:ss";
	public String mImprintText = "mobilewebcam " + android.os.Build.MODEL;
	@StringPref(key = "imprint_statusinfo", val = "Battery %d%% %.1f°C")
	public String mImprintStatusInfo = "Battery %03d%% %3.1f�C";
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
	@BooleanPref(key = "imprint_gps", val = false)
	public boolean mImprintGPS = false;
	@BooleanPref(key = "imprint_location", val = false)
	public boolean mImprintLocation = false;
	public int mGPSX = 85, mGPSY = 87;
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
    @IntegerPref(key = "zoom", val = 0)        
	public int mZoom = 0;
    @StringPref(key = "whitebalance", val = Camera.Parameters.WHITE_BALANCE_AUTO)
	public String mWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
    @BooleanPref(key = "cam_flash", val = false)
	public boolean mCameraFlash = false;
	
	@StringPref(key = "email_sender", val = "")
	public String mMailAddress = "";
	@StringPref(key = "email_password", val = "")
	public String mMailPassword = "";
	@StringPref(key = "email_host", val = "smtp.gmail.com")
	public String mMailHost = "smtp.gmail.com";
	@StringPref(key = "email_port", val = "465")
	public String mMailPort = "465";
	@BooleanPref(key = "email_ssl", val = true)
	public boolean mMailSSL = true;

	@IntegerPref(key = "server_every", val = 1, min = 1)
	public int mServerFreq = 1;
	@IntegerPref(key = "ftp_every", val = 1, min = 1)
	public int mFTPFreq = 1;
	@IntegerPref(key = "mail_every", val = 1, min = 1)
	public int mMailFreq = 1;
	@IntegerPref(key = "dropbox_every", val = 1, min = 1)
	public int mDropboxFreq = 1;
	@IntegerPref(key = "store_every", val = 1, min = 1)
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
		// get all preferences
		for(Field f : getClass().getFields())
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
			IntegerPref ip = f.getAnnotation(IntegerPref.class);
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
        
	    mCustomImageScale = Enum.valueOf(ImageScaleMode.class, prefs.getString("custompicscale", "CROP"));
                
		mAutoStart = prefs.getBoolean("autostart", false);
		mCameraStartupEnabled = prefs.getBoolean("cam_autostart", true);
        mShutterSound = prefs.getBoolean("shutter", true); 
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

		mStatusInfoX = prefs.getInt("statusinfo_x", 85);
		mStatusInfoY = prefs.getInt("statusinfo_y", 92);

		mGPSX = prefs.getInt("gps_x", 85);
		mGPSY = prefs.getInt("gps_y", 87);
		
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

		Map<String, Object> all = new HashMap<String, Object>();
		
		for(Field f : PhotoSettings.class.getFields())
		{
			BooleanPref bp = f.getAnnotation(BooleanPref.class);
			if(bp != null)
			{
				boolean val = prefs.getBoolean(bp.key(), bp.val());
				all.put(bp.key(), val);
			}
			StringPref sp = f.getAnnotation(StringPref.class);
			if(sp != null)
			{
				String val = prefs.getString(sp.key(), sp.val());
				all.put(sp.key(), val);
			}
			IntegerPref ip = f.getAnnotation(IntegerPref.class);
			if(ip != null)
			{
				try
				{
					String val = prefs.getString(ip.key(), "" + ip.val());
					all.put(ip.key(), val);
				}
				catch(ClassCastException e)
				{
					try
					{
						int val = prefs.getInt(ip.key(), ip.val());
						all.put(ip.key(), val);
					}
					catch(ClassCastException ei)
					{
						ei.printStackTrace();
					}
				}
			}
		}
		
		for(Map.Entry<String, ?> p : all.entrySet())
			s.append(p.getKey() + ":" + p.getValue() + "\n");
		
		return s.toString();
	}
	
	private static <T> T parseObjectFromString(String s, Class<T> c) throws Exception
	{
	    return c.getConstructor(new Class[] {String.class }).newInstance(s);
	}	
	
	public static void GETSettings(final Context context)
	{
		// check for new settings when done
		final SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		final String settingsurl = prefs.getString("remote_config_url", "");
		final String login = prefs.getString("remote_config_login", "");
		final String password = prefs.getString("remote_config_password", "");
		if(settingsurl.length() > 0)
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
							Toast.makeText(context, result, Toast.LENGTH_LONG).show();
						else
							PhotoSettings.GETSettings(result, prefs);
					}
					else
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
			BooleanPref bp = f.getAnnotation(BooleanPref.class);
			if(bp != null)
				all.put(bp.key(), bp.val());
			StringPref sp = f.getAnnotation(StringPref.class);
			if(sp != null)
				all.put(sp.key(), sp.val());
			IntegerPref ip = f.getAnnotation(IntegerPref.class);
			if(ip != null)
				all.put(ip.key(), ip.val());
		}
		
		for(Map.Entry<String, ?> p : all.entrySet())
		{
			for(String s : settings)
			{
				if(s.startsWith(p.getKey()))
				{
					try
					{
						String value = s.split(":")[1];
						Class c = p.getValue().getClass();
						Object val = parseObjectFromString(value, c);

						if(c == String.class)
							edit.putString(p.getKey(), (String)val);
						else if(c == Boolean.class)
							edit.putBoolean(p.getKey(), (Boolean)val);
						else if(c == Integer.class)
							edit.putString(p.getKey(), val.toString());
						else
							throw new UnsupportedOperationException(c.toString());
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