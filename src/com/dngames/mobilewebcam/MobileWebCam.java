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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Debug;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.app.AlarmManager;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.PendingIntent;
import 	android.content.Intent;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.util.Log;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

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
    private static final int MENU_SET_SCENEMODE = 6;
    private static final int MENU_SET_COLOREFFECT = 7;
    private static final int MENU_SET_EXPOSURE = 8;
    private static final int MENU_SET_FLASH = 9;
    private static final int MENU_SETTINGS = 10;
    private static final int MENU_CONFIGURE_NIGHT = 11;
    private static final int MENU_SHUTDOWN = 12;

	public static int gUploadingCount = 0;
	public static int gPictureCounter = 0;
	
	public static boolean gIsRunning = false;
	public static boolean gInSettings = false;
	
	public static long gLastMotionTime = 0; 
	public static long gLastMotionKeepAliveTime = 0;
	
	public static int gCurLogMessage = 0;
	public static String[] gLogMessages = new String[16];

	public static int gCurLogInfos = 0;
	public static String[] gLogInfos = new String[16];
	
	public static boolean gCustomReceiverActive = false;

    private List<String> menuWhiteBalanceModes = null;
    private List<String> menuSceneModes = null;
    private List<String> menuColorEffects = null;
    
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
    
	public static String GetLog(Context c, SharedPreferences prefs, PhotoSettings settings)
	{
		String info_device = android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE + " " + android.os.Build.DISPLAY;		

		String log = "MobileWebCam " + MobileWebCamHttpServer.getVersionNumber(c) + " (" + settings.mImprintText + ") " + info_device + "\r\n\r\n";

		log += "Date and time: " + new Date().toString() + "\r\n";

		log += WorkImage.getBatteryInfo(c, "Battery %d%% %.1f\248C") + "\r\n";

		if(settings.mStoreGPS || settings.mImprintGPS)
		{
			String lat = String.format(Locale.US, "%f", WorkImage.gLatitude);
			String lon = String.format(Locale.US, "%f", WorkImage.gLongitude);
			String alt = String.format(Locale.US, "%f", WorkImage.gAltitude);
			log += "Latitude: " + lat + "\r\n";
			log += "Longitude: " + lon + "\r\n";
			log += "Altitude: " + alt + "\r\n";
			log += "http://maps.google.com/maps?q=" + lat + "," + lon + "+(MobileWebCam+Location)&z=18&ll=" + lat + "," + lon + "\r\n";
		}
		
	    if(prefs.getBoolean("server_enabled", false))
	    {
			String myIP = RemoteControlSettings.getIpAddress(c, false);
			if(myIP != null)
				log += "Browser access URL: http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs) + "\r\n";
	    }
	    
	    log += "\r\n";
	    
		if(settings.mMode == Mode.MANUAL)
			log += "Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "   Manual Mode active" + "\r\n";
		else
			log += "Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "\r\n";
		log += "Orientation: " + Preview.gOrientation + "\r\n";;
		if(settings.mMode == Mode.MANUAL)
			log += "Mode: " + c.getResources().getStringArray(R.array.entries_list_camera_mode)[0];
		else if(settings.mMode == Mode.NORMAL)
			log += "Mode: " + c.getResources().getStringArray(R.array.entries_list_camera_mode)[1];
		else if(settings.mMode == Mode.BACKGROUND)
			log += "Mode: " + c.getResources().getStringArray(R.array.entries_list_camera_mode)[2];
		else if(settings.mMode == Mode.HIDDEN)
			log += "Mode: " + c.getResources().getStringArray(R.array.entries_list_camera_mode)[3];
		if(settings.mMotionDetect)
			log += " detect motion";
		if(settings.mNightAutoConfig && settings.mNightAutoConfigEnabled)
			log += " night detected";
		if(settings.mNightAutoBrightness && settings.IsNight())
			log += " autobright";
		if(settings.mNightIRLight)
			log += " IR";
		if(settings.mBroadcastReceiver.length() > 0)
			log += "\r\nCustom Reiver: " + settings.mBroadcastReceiver + " " + MobileWebCam.gCustomReceiverActive; 
		if(!settings.mNightAutoBrightness || !settings.IsNight())
			log += String.format("\r\nWhite Balance: %s, Color Effect: %s, Scene Mode: %s, Exposure Compensation: %d", settings.mWhiteBalance, settings.mColorEffect, settings.mSceneMode, settings.mExposureCompensation);
		else
			log += String.format("\r\nWhite Balance: %s, Color Effect: %s, Scene Mode: %s, Exposure Compensation: %d", settings.mNightAutoBrightnessWhitebalance, settings.mColorEffect, settings.mNightAutoBrightnessScenemode, settings.mNightAutoBrightnessExposure);
		float usedMegs = (float)Debug.getNativeHeapAllocatedSize() / (float)1048576L;
		log += String.format("\r\nMemory used: %.2f MB\r\n", usedMegs);
		log += String.format("Used upload image size: %d x %d (from %d x %d)\r\n", MobileWebCamHttpService.gImageWidth, MobileWebCamHttpService.gImageHeight, MobileWebCamHttpService.gOriginalImageWidth, MobileWebCamHttpService.gOriginalImageHeight);	    

		log += "\r\nActivity:\r\n";

		int cnt = 0;
		int i = MobileWebCam.gCurLogInfos;
		while(cnt < MobileWebCam.gLogInfos.length)
		{
			if(MobileWebCam.gLogInfos[i] != null)
				log += MobileWebCam.gLogInfos[i] + "\r\n";
			i++;
			if(i >= MobileWebCam.gLogInfos.length)
				i = 0;
			cnt++;
		}

		log += "\r\nErrors:\r\n";
		
		cnt = 0;
		i = MobileWebCam.gCurLogMessage;
		while(cnt < MobileWebCam.gLogMessages.length)
		{
			if(MobileWebCam.gLogMessages[i] != null)
				log += MobileWebCam.gLogMessages[i] + "\r\n";
			i++;
			if(i >= MobileWebCam.gLogMessages.length)
				i = 0;
			cnt++;
		}
		
		return log;
	}    
    
    private void addMenuItem(Menu menu, int id, String text, int icon, int show)
    {
    	MenuItem item = menu.add(0, id, 0, text);
//    	item.setAlphabeticShortcut('a');
    	item.setIcon(icon);
    	HoneyCombFunctions.setShowAsAction(item, show);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	boolean r = super.onPrepareOptionsMenu(menu);

		menu.clear();
    	
		addMenuItem(menu, MENU_SETTINGS, "Configure", android.R.drawable.ic_menu_preferences, MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		if(mSettings.mMobileWebCamEnabled)
			addMenuItem(menu, MENU_SET_ON_OFFLINE, "Set OFFLINE", android.R.drawable.presence_online, MenuItem.SHOW_AS_ACTION_ALWAYS);
		else
			addMenuItem(menu, MENU_SET_ON_OFFLINE, "Enable Camera", android.R.drawable.presence_offline, MenuItem.SHOW_AS_ACTION_ALWAYS);

    	if(!mSettings.mURL.equals(mSettings.mDefaulturl) && mSettings.mUploadPictures)
		{
			if(mSettings.mEmailReceiverAddress.length() > 0 && mSettings.mMailPictures)
				addMenuItem(menu, MENU_SHARE_URL, "Share URL", android.R.drawable.ic_menu_send, MenuItem.SHOW_AS_ACTION_ALWAYS);
			else
				addMenuItem(menu, MENU_SHARE_URL, "Share URL", android.R.drawable.ic_menu_share, MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		if(mSettings.mMode == Mode.MANUAL || mSettings.mMode == Mode.NORMAL)
			addMenuItem(menu, MENU_SHARE_IMAGE, "Share Image", android.R.drawable.ic_menu_gallery, MenuItem.SHOW_AS_ACTION_ALWAYS);
    	
		if(NewCameraFunctions.getNumberOfCameras() > 1)
    		addMenuItem(menu, MENU_SET_FRONT_BACK, "Toggle Front/Back Camera", android.R.drawable.ic_menu_camera, MenuItem.SHOW_AS_ACTION_ALWAYS);
    	if(mPreview != null && mPreview.mCamera != null)
    	{
    		Camera.Parameters params = mPreview.mCamera.getParameters();
    		if(params != null)
    		{
    			if(NewCameraFunctions.isZoomSupported(params))
    				addMenuItem(menu, MENU_SET_ZOOM, "Zoom", android.R.drawable.ic_menu_crop, MenuItem.SHOW_AS_ACTION_IF_ROOM);
    			menuWhiteBalanceModes = NewCameraFunctions.getSupportedWhiteBalance(params);
    			if(menuWhiteBalanceModes != null)
    				addMenuItem(menu, MENU_SET_WHITEBALANCE, "White Balance", android.R.drawable.ic_menu_view, MenuItem.SHOW_AS_ACTION_IF_ROOM);
    			menuSceneModes = NewCameraFunctions.getSupportedSceneModes(params);
    			if(menuSceneModes != null)
    				addMenuItem(menu, MENU_SET_SCENEMODE, "Scene Mode", android.R.drawable.ic_menu_view, MenuItem.SHOW_AS_ACTION_IF_ROOM);
    			menuColorEffects = NewCameraFunctions.getSupportedColorEffects(params);
    			if(menuSceneModes != null)
    				addMenuItem(menu, MENU_SET_COLOREFFECT, "Color Effect", android.R.drawable.ic_menu_view, MenuItem.SHOW_AS_ACTION_IF_ROOM);
    			if(NewCameraFunctions.getMinExposureCompensation(params) != 0 || NewCameraFunctions.getMaxExposureCompensation(params) != 0)
    				addMenuItem(menu, MENU_SET_EXPOSURE, "Exposure Compensation", android.R.drawable.ic_menu_view, MenuItem.SHOW_AS_ACTION_IF_ROOM);

    	    	if(NewCameraFunctions.isFlashSupported(params))
    	    		addMenuItem(menu, MENU_SET_FLASH, "Toggle Camera Flashlight", android.R.drawable.ic_dialog_alert, MenuItem.SHOW_AS_ACTION_IF_ROOM);

    			addMenuItem(menu, MENU_CONFIGURE_NIGHT, "Toggle Day/Night Settings", android.R.drawable.ic_menu_day, MenuItem.SHOW_AS_ACTION_IF_ROOM);
    		}
    	}

		addMenuItem(menu, MENU_SHUTDOWN, "Shutdown and Close App", android.R.drawable.ic_lock_power_off, MenuItem.SHOW_AS_ACTION_ALWAYS /*MenuItem.SHOW_AS_ACTION_IF_ROOM*/);    	

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
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	final String nightsettings = mSettings.getNightPreferencesPostfix();
    	
        switch (item.getItemId())
        {
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
		        
	        	HoneyCombFunctions.invalidateOptionsMenu(MobileWebCam.this);
		        
	        	return true;	        	
	        	
	        case MENU_SET_ZOOM:
		        {
		            AlertDialog.Builder builder = new AlertDialog.Builder(this);
		            builder.setTitle("Set Zoom");
		            
		            LinearLayout layout = new LinearLayout(this); 
		            layout.setOrientation(1); 
		            layout.setPadding(6,6,6,6);
		            final SeekBar seek = new SeekBar(this); 
		            seek.setProgress(mSettings.mZoom);
		            seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		            {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
						{
							if(fromUser)
							{
			                    mSettings.mZoom = progress;
					        	
								if(mPreview != null && mPreview.mCamera != null)
									mSettings.SetCameraParameters(mPreview.mCamera, true);
							}
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar)
						{
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar)
						{
				        	SharedPreferences.Editor edit = mPrefs.edit();
				        	edit.putString("zoom" + nightsettings, "" + mSettings.mZoom);
				        	edit.commit();
						}});
		            layout.addView(seek); 
/*		            TextView text = new TextView(this); 
		            text.setText("0..100"); 
		            text.setPadding(10, 10, 10, 10); 
		            linear.addView(text); */
		            builder.setView(layout); 
		            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		            {
		                public void onClick(DialogInterface dialog, int item)
		                {
							Toast.makeText(MobileWebCam.this, "Zoom " + mSettings.mZoom + "%", Toast.LENGTH_SHORT).show();
		                }
		            }).show();
		        }	
	        	return true;

	        case MENU_SET_EXPOSURE:
		        {
		            AlertDialog.Builder builder = new AlertDialog.Builder(this);
		            builder.setTitle("Set Exposure Compensation");
		            
		            LinearLayout layout = new LinearLayout(this); 
		            layout.setOrientation(1); 
		            layout.setPadding(6,6,6,6);
		            final SeekBar seek = new SeekBar(this); 
		            seek.setProgress(mSettings.mExposureCompensation);
		            seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		            {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
						{
							if(fromUser)
							{
			                    mSettings.mExposureCompensation = progress;
					        	
								if(mPreview != null && mPreview.mCamera != null)
									mSettings.SetCameraParameters(mPreview.mCamera, true);
							}
						}
	
						@Override
						public void onStartTrackingTouch(SeekBar seekBar)
						{
						}
	
						@Override
						public void onStopTrackingTouch(SeekBar seekBar)
						{
				        	SharedPreferences.Editor edit = mPrefs.edit();
				        	edit.putString("exposurecompensation" + nightsettings, "" + mSettings.mExposureCompensation);
				        	edit.commit();
						}});
		            layout.addView(seek); 
		            builder.setView(layout); 
		            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		            {
		                public void onClick(DialogInterface dialog, int item)
		                {
							Toast.makeText(MobileWebCam.this, "Exposure Compensation " + mSettings.mExposureCompensation, Toast.LENGTH_SHORT).show();
		                }
		            }).show();
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
						Toast.makeText(MobileWebCam.this, mSettings.mWhiteBalance, Toast.LENGTH_SHORT).show();

			        	SharedPreferences.Editor edit = mPrefs.edit();
			        	edit.putString("whitebalance" + nightsettings, "" + mSettings.mWhiteBalance);
			        	edit.commit();

						if(mPreview != null && mPreview.mCamera != null)
							mSettings.SetCameraParameters(mPreview.mCamera, true);
	                }
	            }).show();

				return true;
	        }

	        case MENU_SET_SCENEMODE:
	        {
	        	final CharSequence[] items = new CharSequence[menuSceneModes.size()];
	        	for(int i = 0; i < menuSceneModes.size(); i++)
	        		items[i] = menuSceneModes.get(i);

	            AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            builder.setTitle("Set Scene Mode");
	            builder.setItems(items, new DialogInterface.OnClickListener()
	            {
	                public void onClick(DialogInterface dialog, int item)
	                {
	                    mSettings.mSceneMode = menuSceneModes.get(item);
						Toast.makeText(MobileWebCam.this, mSettings.mSceneMode, Toast.LENGTH_SHORT).show();

						SharedPreferences.Editor edit = mPrefs.edit();
			        	edit.putString("scenemode" + nightsettings, "" + mSettings.mSceneMode);
			        	edit.commit();

						if(mPreview != null && mPreview.mCamera != null)
							mSettings.SetCameraParameters(mPreview.mCamera, true);
	                }
	            }).show();

	        	return true;
	        }

	        case MENU_SET_COLOREFFECT:
	        {
	        	if(menuColorEffects != null)
	        	{
		        	final CharSequence[] items = new CharSequence[menuColorEffects.size()];
		        	for(int i = 0; i < menuColorEffects.size(); i++)
		        		items[i] = menuColorEffects.get(i);
	
		            AlertDialog.Builder builder = new AlertDialog.Builder(this);
		            builder.setTitle("Set Color Effect");
		            builder.setItems(items, new DialogInterface.OnClickListener()
		            {
		                public void onClick(DialogInterface dialog, int item)
		                {
		                    mSettings.mColorEffect = menuColorEffects.get(item);
							Toast.makeText(MobileWebCam.this, mSettings.mColorEffect, Toast.LENGTH_SHORT).show();
	
							SharedPreferences.Editor edit = mPrefs.edit();
		    	        	edit.putString("coloreffect" + nightsettings, "" + mSettings.mColorEffect);
		    	        	edit.commit();
	
							if(mPreview != null && mPreview.mCamera != null)
								mSettings.SetCameraParameters(mPreview.mCamera, true);
		                }
		            }).show();
	        	}

				return true;
	        }
			
	        case MENU_SET_FLASH:
		        {
		        	mSettings.mCameraFlash = !mSettings.mCameraFlash;
		        	
		        	SharedPreferences.Editor edit = mPrefs.edit();
		        	edit.putBoolean("cam_flash" + nightsettings, mSettings.mCameraFlash);
		        	edit.commit();
		        	
					if(mPreview != null && mPreview.mCamera != null)
						mSettings.SetCameraParameters(mPreview.mCamera, true);
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

	        	HoneyCombFunctions.invalidateOptionsMenu(MobileWebCam.this);
	        	
	        	return true;
	        	
	        case MENU_CONFIGURE_NIGHT:
		        {
		        	mSettings.mSetNightConfiguration = !mSettings.mSetNightConfiguration;
		        	
		        	SharedPreferences.Editor edit = mPrefs.edit();
		        	edit.putBoolean("cam_nightconfiguration", mSettings.mSetNightConfiguration);
		        	edit.commit();
		        }
		        
				if(mPreview != null && mPreview.mCamera != null)
					mSettings.SetCameraParameters(mPreview.mCamera, true);
				
	        	HoneyCombFunctions.invalidateOptionsMenu(MobileWebCam.this);
	        	
				Toast.makeText(MobileWebCam.this,
						mSettings.mSetNightConfiguration ? "Choose Night Settings now!" :
							(mSettings.IsNight() ? "Disable night start/endtime first to configure day settings during night!" : "Choose Day Settings now!"),
							Toast.LENGTH_LONG).show();

	        	return true;	        	
	        	
            case MENU_SETTINGS:
            	Intent settingsIntent = new Intent(getApplicationContext(), SettingsTabActivity.class);
            	startActivity(settingsIntent);
                return true;
                
            case MENU_SHUTDOWN:
            	MobileWebCamHttpService.stop(MobileWebCam.this);
            	ControlReceiver.Stop(MobileWebCam.this, mPrefs);
            	finish();
            	return true;
        }

        return false;
    }
    
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        mPrefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
        if(mPrefs.getBoolean("fullscreen", false))
        {
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        mLayout = R.layout.layout_fullscreen;
        }
        
        super.onCreate(savedInstanceState);
        
//***        if(DEBUG_MOTIONDETECT)
        {
        	mDrawOnTop = (DrawOnTop)findViewById(R.id.drawontop);
        }
		
		mTextView = (TextView)findViewById(R.id.status);
		mCamNameView = (TextView)findViewById(R.id.camname);
		mMotionTextView = (TextView)findViewById(R.id.motion);
		mMotionTextView.setVisibility(View.INVISIBLE);
		mNightTextView = (TextView)findViewById(R.id.nightsettings);
		mNightTextView.setVisibility(View.INVISIBLE);
		mTextViewFrame = (LinearLayout)findViewById(R.id.statusframe);
		mCamNameViewFrame = (RelativeLayout)findViewById(R.id.ipaddrframe);
		mCamNameViewFrame.setBackgroundColor(mSettings.mTextBackgroundColor);
		mTextViewFrame.setBackgroundColor(mSettings.mDateTimeBackgroundColor);                
        
/*        try
        {
        	// show overflow menu ... always (even on devices with menu button)
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null)
            {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception ex)
        {
            // Ignore
        }*/        
        
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
        
		if(mCamNameView != null)
		{
			String info = mPrefs.getString("imprint_text", "mobilewebcam");
			if(info == "mobilewebcam")
				mCamNameView.setText("");
			else
				mCamNameView.setText(info);
		}

		TextView iptext = (TextView)findViewById(R.id.ipaddr);
		if(iptext != null)
		{
		    if(mPrefs.getBoolean("server_enabled", true))
		    {
		    	
				String myIP = RemoteControlSettings.getIpAddress(MobileWebCam.this, true);
				if(myIP != null)
					iptext.setText("http://" + myIP + ":" + MobileWebCamHttpService.getPort(mPrefs));
				else
					iptext.setText("");
		    }        
			else
				iptext.setText("");
		}
        
		mPrefs.registerOnSharedPreferenceChangeListener(this);            
        onSharedPreferenceChanged(mPrefs, null);

        MobileWebCamHttpService.start(MobileWebCam.this);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	gIsRunning = true;
    	
    	MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
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
			        
// why?					mHandler.postDelayed(new Runnable() { public void run() { finish(); } }, 2000);
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
    	if(key == null || key.equals("camera_mode"))
    	{
        	mSettings.mMode = PhotoSettings.getCamMode(prefs);
	        switch(mSettings.mMode)
			{
			case MANUAL:
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
			case HIDDEN:
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
	
				if(key != null)// && key.equals("camera_mode"))
					finish();
				break;
			case BACKGROUND:
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
				
				if(key != null)// && key.equals("camera_mode"))
					finish();
				break;
			case NORMAL:
			default:
	
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
	        
			if(mMotionTextView != null)
				mMotionTextView.setVisibility(mSettings.mMotionDetect/* && DEBUG_MOTIONDETECT */ ? View.VISIBLE : View.INVISIBLE);            		
    	}
    	if(key == null || key.equals("cam_broadcast_activation"))
    	{
    		if(prefs.getString("cam_broadcast_activation", "").length() > 0)
    		{
    			if(!MobileWebCam.gCustomReceiverActive)
    				CustomReceiverService.start(MobileWebCam.this);
    		}
    		else if(MobileWebCam.gCustomReceiverActive)
    		{
    			CustomReceiverService.stop(MobileWebCam.this);
    		}
    	}
    	if(key == null || key.equals("cam_nightconfiguration"))
    	{
			if(mNightTextView != null)
				mNightTextView.setVisibility(mSettings.mSetNightConfiguration ? View.VISIBLE : View.INVISIBLE);
    	}
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