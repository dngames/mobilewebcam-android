package com.dngames.mobilewebcam;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.AlarmManager;
import java.util.Calendar;
import android.app.PendingIntent;
import android.content.Context;

public class StartMobileWebCamAction extends Activity
{
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        KeyguardManager mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyguardManager.newKeyguardLock("MobileWebCam");
        mLock.disableKeyguard();

		try
		{
			Thread.sleep(10000);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
        
		SharedPreferences prefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);

		String v = prefs.getString("camera_mode", "1");
		if(v.length() < 1 || v.length() > 9)
	        v = "1";
		switch(Integer.parseInt(v))
		{
		case 2:
		case 3:
			{
				AlarmManager alarmMgr = (AlarmManager)StartMobileWebCamAction.this.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(StartMobileWebCamAction.this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(StartMobileWebCamAction.this, 0, i, 0);
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
			Intent i = new Intent(StartMobileWebCamAction.this, MobileWebCam.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			break;
		}

	    finish();
    }
}