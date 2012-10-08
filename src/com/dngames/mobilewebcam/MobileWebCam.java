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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlarmManager;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import 	android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.util.Log;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

import android.content.SharedPreferences;

// ----------------------------------------------------------------------

public class MobileWebCam extends CamActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String SHARED_PREFS_NAME="webcamsettings";
    
    public static final boolean DEBUG_MOTIONDETECT = false;
 
    private static final int MENU_SHARE_URL = 0;
    private static final int MENU_SHARE_IMAGE = 1;
    private static final int MENU_SET_ON_OFFLINE = 2;
    private static final int MENU_SET_FRONT_BACK = 3;
    private static final int MENU_SET_ZOOM = 4;
    private static final int MENU_SET_WHITEBALANCE = 5;
    private static final int MENU_SETTINGS = 6;

	public static int gUploadingCount = 0;
	public static int gPictureCounter = 0;
	
	public static boolean gIsRunning = false;
	public static boolean gInSettings = false;
	
	public static long gLastMotionTime = 0; 
	
	public static int gCurLogMessage = 0;
	public static String[] gLogMessages = new String[16];

	public static int gCurLogInfos = 0;
	public static String[] gLogInfos = new String[16];

    private List<String> menuWhiteBalanceModes = null;
    
    public static void LogI(String message)
    {
    	Log.i("MobileWebCam", message);

    	gLogInfos[gCurLogInfos++] = new String(new Date() + ": " + message);
    	if(gCurLogInfos >= gLogInfos.length)
    		gCurLogInfos = 0;
    }

    public static void LogE(String message)
    {
    	Log.e("MobileWebCam", message);

    	gLogMessages[gCurLogMessage++] = new String(new Date() + ": " + message);
    	if(gCurLogMessage >= gLogMessages.length)
    		gCurLogMessage = 0;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	boolean r = super.onPrepareOptionsMenu(menu);

		menu.clear();
		if(!mSettings.mURL.equals(mSettings.mDefaulturl) && mSettings.mUploadPictures)
		{
			if(mSettings.mEmailReceiverAddress.length() > 0 && mSettings.mMailPictures)
				menu.add(0, MENU_SHARE_URL, 0, "Share URL").setIcon(android.R.drawable.ic_menu_send);
			else
				menu.add(0, MENU_SHARE_URL, 0, "Share URL").setIcon(android.R.drawable.ic_menu_share);
		}
		if(mSettings.mMode == Mode.MANUAL || mSettings.mMode == Mode.NORMAL)
			menu.add(0, MENU_SHARE_IMAGE, 0, "Share Image").setIcon(android.R.drawable.ic_menu_gallery);
		if(mSettings.mMobileWebCamEnabled)
			menu.add(0, MENU_SET_ON_OFFLINE, 0, "Set OFFLINE").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		else
			menu.add(0, MENU_SET_ON_OFFLINE, 0, "Enable Camera").setIcon(android.R.drawable.ic_menu_slideshow);
    	if(NewCameraFunctions.getNumberOfCameras() > 1)
        	menu.add(0, MENU_SET_FRONT_BACK, 0, "Toggle Front/Back Camera").setIcon(android.R.drawable.ic_menu_camera);
    	if(mPreview != null && mPreview.mCamera != null)
    	{
    		Camera.Parameters params = mPreview.mCamera.getParameters();
    		if(params != null)
    		{
    			if(NewCameraFunctions.isZoomSupported(params))
    				menu.add(0, MENU_SET_ZOOM, 0, "Zoom In").setIcon(android.R.drawable.ic_menu_crop);
    			menuWhiteBalanceModes = NewCameraFunctions.getSupportedWhiteBalance(params);
    			if(menuWhiteBalanceModes != null)
    				menu.add(0, MENU_SET_WHITEBALANCE, 0, "White Balance").setIcon(android.R.drawable.ic_menu_view);
    		}
    	}
		menu.add(0, MENU_SETTINGS, 0, "Change Settings").setIcon(android.R.drawable.ic_menu_preferences);

		return r;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case MENU_SHARE_URL:
	        	{
					final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setType("plain/text");
					if(mSettings.mEmailReceiverAddress.length() > 0)
						shareIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { mSettings.mEmailReceiverAddress });
					if(mSettings.mEmailSubject.length() > 0)
						shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mSettings.mEmailSubject);
					shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mSettings.mURL.substring(0, mSettings.mURL.lastIndexOf("/") + 1));
					MobileWebCam.this.startActivity(Intent.createChooser(shareIntent, "Share URL ..."));
 
					return true;
	        	}
	        case MENU_SHARE_IMAGE:
	        	if(mPreview != null)
	        		mPreview.shareNextPicture();
	
				return true;
	        case MENU_SET_FRONT_BACK:
		        {
		        	mSettings.mFrontCamera = !mSettings.mFrontCamera;
		        	
		        	SharedPreferences.Editor edit = mPrefs.edit();
		        	edit.putBoolean("cam_front", mSettings.mFrontCamera);
		        	edit.commit();
		        	
		        	if(mPreview != null)
		        	{
		        		mPreview.RestartCamera();
		        	}
		        }
	        	return true;
	        	
	        case MENU_SET_ZOOM:
		        {
		        	mSettings.mZoom += 10;
		        	if(mSettings.mZoom > 100)
		        		mSettings.mZoom = 0;
	
		        	SharedPreferences.Editor edit = mPrefs.edit();
		        	edit.putString("zoom", "" + mSettings.mZoom);
		        	edit.commit();
		        	
					Toast.makeText(MobileWebCam.this, "Zoom " + mSettings.mZoom + "%", Toast.LENGTH_SHORT).show();
		        }	
	        	return true;

	        case MENU_SET_WHITEBALANCE:
	        {
	        	final CharSequence[] items = new CharSequence[menuWhiteBalanceModes.size()];
	        	for(int i = 0; i < menuWhiteBalanceModes.size(); i++)
	        		items[i] = menuWhiteBalanceModes.get(i);

	            AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            builder.setTitle("Set White Balance");
	            builder.setItems(items, new DialogInterface.OnClickListener()
	            {
	                public void onClick(DialogInterface dialog, int item)
	                {
	                    mSettings.mWhiteBalance = menuWhiteBalanceModes.get(item);
	                }
	            }).show();

	        	SharedPreferences.Editor edit = mPrefs.edit();
	        	edit.putString("whitebalance", "" + mSettings.mWhiteBalance);
	        	edit.commit();
	        }
        	return true;
	        	
	        case MENU_SET_ON_OFFLINE:
	        	if(mSettings.mMobileWebCamEnabled)
	        	{
	        		mSettings.EnableMobileWebCam(false);
		        	
		        	if(mPreview != null)
		        		mPreview.offline(true);

					PhotoAlarmReceiver.StopNotification(MobileWebCam.this);
	        	}
	        	else
	        	{
	        		mSettings.EnableMobileWebCam(true);

	        		if(mPreview != null)
		        		mPreview.online();
	        	}
	        	
	        	return true;
	        	
            case MENU_SETTINGS:
            	Intent settingsIntent = new Intent(getApplicationContext(), SettingsTabActivity.class);
            	startActivity(settingsIntent);
                return true;
        }

        return false;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		mPrefs.registerOnSharedPreferenceChangeListener(this);            
        onSharedPreferenceChanged(mPrefs, null);
        
        Button b = (Button)findViewById(R.id.configure);
        b.setVisibility(View.VISIBLE);
        b.setOnClickListener(new OnClickListener()
        	{
 			@Override
 			public void onClick(View v)
 			{
 				MobileWebCam.this.openOptionsMenu();
 			}
 		});        
        
	    if(mPrefs.getBoolean("server_enabled", true))
	    {
			String myIP = SystemSettings.getLocalIpAddress(MobileWebCam.this);
			if(myIP != null)
				setTitle(getTitle() + " http://" + myIP + ":" + MobileWebCamHttpService.getPort(mPrefs));
	    }        
        
        MobileWebCamHttpService.start(MobileWebCam.this);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	gIsRunning = true;
    }

    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	gIsRunning = false;
    }

	@Override
	protected void onNewIntent(Intent intent)
	 {
		super.onNewIntent(intent);

		setIntent(intent); //must store the new intent unless getIntent() will return the old one

		Bundle extras=intent.getExtras();
    	if(extras != null)
		{
    		String command = extras.getString("command");
    		if(command != null)
    		{
	    		if(command.startsWith("start"))
				{
					Toast.makeText(MobileWebCam.this, "start command received", Toast.LENGTH_SHORT).show();
	    			
					mSettings.EnableMobileWebCam(true);
	
		        	if(mPreview != null)
		        	{
			        	mPreview.online();
		        	}
				}
				else if(command.startsWith("stop"))
				{
					Toast.makeText(MobileWebCam.this, "stop command received", Toast.LENGTH_SHORT).show();
	
					mSettings.EnableMobileWebCam(false);
			        	
			        if(mPreview != null)
			        {
			        	mPreview.offline(false);
			        }
			        
			        mHandler.postDelayed(new Runnable() { public void run() { finish(); } }, 2000);
				}
				else if(command.startsWith("refresh"))
				{
					Toast.makeText(MobileWebCam.this, "refresh command received", Toast.LENGTH_SHORT).show();

					SharedPreferences.Editor edit = mPrefs.edit();
					edit.putString("cam_refresh", extras.getString("refreshduration"));
					edit.commit();
			        
			        mHandler.postDelayed(new Runnable() { public void run() { finish(); } }, 2000);
				}
				else if(command.startsWith("photo"))
				{
					Toast.makeText(MobileWebCam.this, "photo command received", Toast.LENGTH_SHORT).show();
	
					if(mPreview != null)
					{
						mPreview.TakePhoto();
					}
			        
			        mHandler.postDelayed(new Runnable() { public void run() { finish(); } }, 2000);
				}
    		}
    		else if(extras.getString("alarm") != null && extras.getString("alarm").startsWith("photo"))
			{
				if(mPreview != null)
				{
					mPreview.TakePhoto();
				}
			}
    	}
	}
	
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
	{
        String v = prefs.getString("camera_mode", "1");
        if(v.length() < 1 || v.length() > 9)
        	v = "1";
        switch(Integer.parseInt(v))
		{
		case 0:
			mSettings.mMode = Mode.MANUAL;

			{
				AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
				alarmMgr.cancel(pendingIntent);
				PhotoAlarmReceiver.StopNotification(MobileWebCam.this);
			}
			
			if(mPreview != null)
				mPreview.setVisibility(View.VISIBLE);
			if(mDrawOnTop != null)
				mDrawOnTop.setVisibility(View.INVISIBLE);
			break;
		case 2:
			mSettings.mMode = Mode.HIDDEN;

			{
				AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
				alarmMgr.cancel(pendingIntent);
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(System.currentTimeMillis());
				alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
			}

			if(mPreview != null)
				mPreview.setVisibility(View.INVISIBLE);
			if(mDrawOnTop != null)
				mDrawOnTop.setVisibility(View.VISIBLE);

			if(key != null && key.equals("camera_mode"))
				finish();
			break;
		case 3:
			mSettings.mMode = Mode.BACKGROUND;
			
			{
				AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
				alarmMgr.cancel(pendingIntent);
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(System.currentTimeMillis());
				alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
			}

			if(mPreview != null)
				mPreview.setVisibility(View.VISIBLE);
			if(mDrawOnTop != null)
				mDrawOnTop.setVisibility(View.INVISIBLE);
			
			if(key != null && key.equals("camera_mode"))
				finish();
			break;
		case 4:
			mSettings.mMode = Mode.BROADCASTRECEIVER;

			CustomReceiverService.start(MobileWebCam.this);
			
			if(mPreview != null)
				mPreview.setVisibility(View.VISIBLE);
			if(mDrawOnTop != null)
				mDrawOnTop.setVisibility(View.INVISIBLE);
			
			if(key != null && key.equals("camera_mode"))
				finish();
			break;
		case 1:
		default:
			mSettings.mMode = Mode.NORMAL;

			{
				AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
				alarmMgr.cancel(pendingIntent);
				PhotoAlarmReceiver.StopNotification(MobileWebCam.this);
			}

			if(mPreview != null)
				mPreview.setVisibility(View.VISIBLE);
			if(mDrawOnTop != null)
				mDrawOnTop.setVisibility(View.INVISIBLE);
			break;
		}

		gLastMotionTime = System.currentTimeMillis(); // no immediate detect
        
		if(mSettings.mMotionDetect && DEBUG_MOTIONDETECT)
			mMotionTextView.setVisibility(View.VISIBLE);            		
		else
			mMotionTextView.setVisibility(View.INVISIBLE);
    }

/*	 public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera)
	 {
		 android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		 android.hardware.Camera.getCameraInfo(cameraId, info);
		 int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		 int degrees = 0;
		 switch (rotation)
		 {
			 case Surface.ROTATION_0: degrees = 0; break;
			 case Surface.ROTATION_90: degrees = 90; break;
			 case Surface.ROTATION_180: degrees = 180; break;
			 case Surface.ROTATION_270: degrees = 270; break;
		 }

		 int result;
		 if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		 {
			 result = (info.orientation + degrees) % 360;
			 result = (360 - result) % 360;  // compensate the mirror
		 }
		 else
		 {
			 // back-facing
			 result = (info.orientation - degrees + 360) % 360;
		 }
		 camera.setDisplayOrientation(result);
	 }*/
    
    static public void decodeYUV420SPGrayscale(int[] rgb, byte[] yuv420sp, int width, int height)
    {
    	final int frameSize = width * height;
    	
    	for (int pix = 0; pix < frameSize; pix++)
    	{
    		int pixVal = (0xff & ((int) yuv420sp[pix])) - 16;
    		if (pixVal < 0) pixVal = 0;
    		if (pixVal > 255) pixVal = 255;
    		rgb[pix] = 0xff000000 | (pixVal << 16) | (pixVal << 8) | pixVal;
    	} // pix
    }
}