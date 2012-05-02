package com.dngames.mobilewebcam;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.app.AlarmManager;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.os.PowerManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.app.Notification;
import android.app.NotificationManager;

public class PhotoAlarmReceiver extends BroadcastReceiver
	implements ITextUpdater
{
	WeakReference<Context> mContext = null;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i("MobileWebCam", "Alarm went off");
		
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		
		SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);

		if(!prefs.getBoolean("mobilewebcam_enabled", true))
			return;
		
        String v = prefs.getString("camera_mode", "1");
        if(v.length() < 1 || v.length() > 9)
        	v = "1";
        int mode = Integer.parseInt(v);

		if(mode == 2 || mode == 3)
		{
	        v = prefs.getString("cam_refresh", "60");
	        if(v.length() < 1 || v.length() > 9)
	        	v = "60";
	        int refresh = Integer.parseInt(v);
	
			AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, PhotoAlarmReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
			alarmMgr.cancel(pendingIntent);
			time.add(Calendar.SECOND, refresh);
			alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
		
			Log.i("MobileWebCam", "Alarm is set to: " + time.getTimeInMillis() + "(" + refresh + ")");

			StartNotification(context);
			
	        MobileWebCamHttpService.start(context);			
		}
		
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
				
				if(mode == 2)
				{
					ConnectivityManager connmgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
					if(connmgr.getNetworkPreference() == ConnectivityManager.TYPE_WIFI)
					{
						WifiManager wmgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
						mWifiLock = wmgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MobileWebCam.PhotoAlarm");
						mWifiLock.acquire();
		
						Log.v("MobileWebCam", "WifiLock aquired!");
					}
					
					mContext = new WeakReference<Context>(context);
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
					else if(mode == 3)
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
		Log.i("MobileWebCam", "PhotoAlarmReceiver: " + msg);
	}
	
	@Override
	public void SetPreview(Bitmap image)
	{
		if(Preview.gPreview != null)
			Preview.gPreview.SetPreview(image);
	}
	
	private static PowerManager.WakeLock mWakeLock = null;
	private static WifiManager.WifiLock mWifiLock = null;
	
	@Override
	public void JobFinished()
	{
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

	public static final int ACTIVE_ID = 1;

	public static void StartNotification(Context c)
	{
	    SharedPreferences prefs = c.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
        String v = prefs.getString("camera_mode", "1");
        if(v.length() < 1 || v.length() > 9)
        	v = "1";
        int mode = Integer.parseInt(v);

	    String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager)c.getSystemService(ns);

		int icon = R.drawable.notification_icon;
		CharSequence tickerText = "MobileWebCam" + (mode == 2 ?  "" : " semi") + " background mode active";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		CharSequence contentTitle = "MobileWebCam Active";
		CharSequence contentText =  mode == 2 ?  "Hidden Mode" : "Semi Background Mode";
	    if(prefs.getBoolean("server_enabled", true))
	    {
			String myIP = SystemSettings.getLocalIpAddress(c);
			if(myIP != null)
				contentText = contentText + " http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs);
	    }
		Intent notificationIntent = new Intent(c, MobileWebCam.class);
		PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, 0);

		notification.setLatestEventInfo(c, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(ACTIVE_ID, notification);
	}

	public static void StopNotification(Context c)
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager)c.getSystemService(ns);
		mNotificationManager.cancel(ACTIVE_ID);
	}
}