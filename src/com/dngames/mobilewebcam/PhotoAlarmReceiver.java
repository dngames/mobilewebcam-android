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

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.app.AlarmManager;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

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

public class PhotoAlarmReceiver extends PhotoReceiver
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
        PhotoSettings.Mode mode = PhotoSettings.Mode.values()[Integer.parseInt(v)];

		if(mode == Mode.HIDDEN || mode == Mode.BACKGROUND)
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
		}
		
		super.onReceive(context, intent);
	}	
}