package com.dngames.mobilewebcam;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.widget.Toast;

public class ControlReceiver extends BroadcastReceiver
{
	public static boolean Triggered = false;
	
    @Override
    public void onReceive(Context context, Intent intent)
    {
		SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		if(intent.getAction().equals("com.dngames.mobilewebcam.START"))
		{
			Start(context, prefs);
		}
		else if(intent.getAction().equals("com.dngames.mobilewebcam.STOP"))
		{
			Stop(context, prefs);
		}
		else if(intent.getAction().equals("com.dngames.mobilewebcam.PHOTO"))
		{
			Triggered = true;
			
			String v = prefs.getString("camera_mode", "1");
			if(v.length() < 1 || v.length() > 9)
	        	v = "1";
			int mode = Integer.parseInt(v);
			if(mode == 2 || mode == 3)
			{
				AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(context, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
				// for this command we do NOT cancel any pending intents!
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(System.currentTimeMillis());
				time.add(Calendar.SECOND, 0);
				alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
			}
			else
			{
				Intent i = new Intent(context, MobileWebCam.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("command", "photo");
				context.startActivity(i);
			}
		}
    }

	public static void Start(Context context, SharedPreferences prefs)
	{
		SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean("mobilewebcam_enabled", true);
		edit.commit();

		String v = prefs.getString("camera_mode", "1");
		if(v.length() < 1 || v.length() > 9)
	        v = "1";
		switch(PhotoSettings.Mode.values()[Integer.parseInt(v)])
		{
		case HIDDEN:
		case BACKGROUND:
			{
				AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(context, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
				alarmMgr.cancel(pendingIntent);
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(System.currentTimeMillis());
				time.add(Calendar.SECOND, 1);
				alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
			}
			break;
		case BROADCASTRECEIVER:
			CustomReceiverService.start(context);
            break;
		case MANUAL:
		case NORMAL:
		default:
			Intent i = new Intent(context, MobileWebCam.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("command", "start");
			context.startActivity(i);
			break;
		}
	}

	public static void Stop(Context context, SharedPreferences prefs)
	{
		SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean("mobilewebcam_enabled", false);
		edit.commit();
		
		String v = prefs.getString("camera_mode", "1");
		if(v.length() < 1 || v.length() > 9)
	        v = "1";
		switch(PhotoSettings.Mode.values()[Integer.parseInt(v)])
		{
		case HIDDEN:
		case BACKGROUND:
			{
				AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(context, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
				alarmMgr.cancel(pendingIntent);
				PhotoAlarmReceiver.StopNotification(context);
			}
			break;
		case BROADCASTRECEIVER:
			CustomReceiverService.stop(context);
			break;
		case MANUAL:
		case NORMAL:
		default:
			Intent i = new Intent(context, MobileWebCam.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("command", "stop");
			context.startActivity(i);
			break;
		}
	}
}