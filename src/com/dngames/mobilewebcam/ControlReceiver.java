package com.dngames.mobilewebcam;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ControlReceiver extends BroadcastReceiver
{
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
    }

	public static void Start(Context context, SharedPreferences prefs)
	{
		SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean("mobilewebcam_enabled", true);
		edit.commit();

		String v = prefs.getString("camera_mode", "1");
		if(v.length() < 1 || v.length() > 9)
	        v = "1";
		switch(Integer.parseInt(v))
		{
		case 2:
		case 3:
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
		case 0:
		case 1:
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
		switch(Integer.parseInt(v))
		{
		case 2:
		case 3:
			{
				AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(context, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
				alarmMgr.cancel(pendingIntent);
				PhotoAlarmReceiver.StopNotification(context);
			}
			break;
		case 0:
		case 1:
		default:
			Intent i = new Intent(context, MobileWebCam.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("command", "stop");
			context.startActivity(i);
			break;
		}
	}
}