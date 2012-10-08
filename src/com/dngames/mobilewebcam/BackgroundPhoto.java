package com.dngames.mobilewebcam;

import java.lang.ref.WeakReference;
import java.util.Date;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class BackgroundPhoto implements ITextUpdater
{
	WeakReference<Context> mContext = null;
	
	public void TakePhoto(Context context, SharedPreferences prefs, PhotoSettings.Mode mode)
	{
		String startTime = prefs.getString("activity_starttime", "00:00");
		String endTime = prefs.getString("activity_endtime", "24:00");

		Date date = new Date();
		int h = Integer.parseInt(startTime.split(":")[0]);
		int m = Integer.parseInt(startTime.split(":")[1]);
		int cur_dayminutes = date.getHours() * 60 + date.getMinutes();
		int check_dayminutes = h * 60 + m; 
		if(cur_dayminutes >= check_dayminutes || startTime.equals(endTime))
		{
			h = Integer.parseInt(endTime.split(":")[0]);
			m = Integer.parseInt(endTime.split(":")[1]);
			check_dayminutes = h * 60 + m; 
			if(cur_dayminutes < check_dayminutes || startTime.equals(endTime))
			{
				if(mWakeLock == null || !mWakeLock.isHeld())
				{
					PowerManager pwrmgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
					mWakeLock = pwrmgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWebCam.PhotoAlarm");
				    mWakeLock.acquire();
				    
				    Log.v("MobileWebCam", "PhotoAlarmWakeLock aquired!");
				}
				
				if(mode == Mode.HIDDEN)
				{
					ConnectivityManager connmgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
					if(connmgr.getNetworkPreference() == ConnectivityManager.TYPE_WIFI)
					{
						WifiManager wmgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
						if(mWifiLock == null || !mWifiLock.isHeld())
						{
							mWifiLock = wmgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MobileWebCam.PhotoAlarm");
							mWifiLock.acquire();
							Log.v("MobileWebCam", "WifiLock aquired!");
						}		
					}
					
					new PhotoService(context, this).TakePicture();
				}
				else if(!MobileWebCam.gInSettings)
				{
					if(MobileWebCam.gIsRunning)
					{
						Intent i = new Intent(context, MobileWebCam.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.putExtra("alarm", "photo");
						context.startActivity(i);
					}
					else if(mode == Mode.BACKGROUND || mode == Mode.BROADCASTRECEIVER)
					{
						Intent i = new Intent(context, TakeHiddenPicture.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);
						i.putExtra("alarm", "photo");
						context.startActivity(i);
					}
					else
					{
						JobFinished();
					}
				}
				else
				{
					JobFinished();
				}
			}
		}
	}	

	@Override
	public void UpdateText()
	{
		if(Preview.gPreview != null)
			Preview.gPreview.UpdateText();
	}

	@Override
	public void Toast(String msg, int length)
	{
		Context c = mContext.get();
		if(c != null)
		{
			SharedPreferences prefs = mContext.get().getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
			if(!prefs.getBoolean("no_messages", false))
			{
				if(mContext.get() != null)
					Toast.makeText(mContext.get(), msg, length).show();
			}
		}
		Log.i("MobileWebCam", "BackgroundPhoto: " + msg);
	}
	
	@Override
	public void SetPreview(Bitmap image)
	{
		if(Preview.gPreview != null && MobileWebCam.gIsRunning)
			Preview.gPreview.SetPreview(image);
	}
	
	private static PowerManager.WakeLock mWakeLock = null;
	private static WifiManager.WifiLock mWifiLock = null;
	
	@Override
	public void JobFinished()
	{
		if(Preview.gPreview != null)
			Preview.gPreview.JobFinished();

		if(mWifiLock != null)
		{
			if(mWifiLock.isHeld())
				mWifiLock.release();
			Log.v("MobileWebCam", "WifiLock released!");
			mWifiLock = null;
		}
		
		if(mWakeLock != null)
		{
			PowerManager.WakeLock tmp = mWakeLock;
			Log.v("MobileWebCam", "PhotoAlarmWakeLock released!");
			mWakeLock = null;

			if(tmp.isHeld())
				tmp.release();
		}
	}
}