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

public class PhotoReceiver extends BroadcastReceiver
{
	private BackgroundPhoto mBGPhoto = new BackgroundPhoto();
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		if(!prefs.getBoolean("mobilewebcam_enabled", true))
			return;
		
		if(prefs.getBoolean("lowbattery_pause", false) && WorkImage.getBatteryLevel(context) < 15)
		{
			MobileWebCam.LogI("Battery low ... pause");
			return;
		}
		
		mBGPhoto.mContext = new WeakReference<Context>(context);
		
        String v = prefs.getString("camera_mode", "1");
        if(v.length() < 1 || v.length() > 9)
        	v = "1";
        PhotoSettings.Mode mode = PhotoSettings.Mode.values()[Integer.parseInt(v)];

		StartNotification(context);
		
        MobileWebCamHttpService.start(context);			
		
        mBGPhoto.TakePhoto(context, prefs, mode);		
	}
	
	public static final int ACTIVE_ID = 1;

	public static void StartNotification(Context c)
	{
	    SharedPreferences prefs = c.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
        String v = prefs.getString("camera_mode", "1");
        if(v.length() < 1 || v.length() > 9)
        	v = "1";
        PhotoSettings.Mode mode = Mode.values()[Integer.parseInt(v)];

	    String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager)c.getSystemService(ns);

		int icon = R.drawable.notification_icon;
		CharSequence tickerText[] = { "MobileWebCam background mode active", "MobileWebCam semi background mode active", "MobileWebCam broadcastreceiver '" + prefs.getString("broadcast_activation", "") + "' active" };
		long when = System.currentTimeMillis();

		CharSequence txt = tickerText[0];
		if(mode == Mode.BACKGROUND)
			txt = tickerText[1];
		else if(mode == Mode.BROADCASTRECEIVER)
			txt = tickerText[2];
		Notification notification = new Notification(icon, txt, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		CharSequence contentTitle = "MobileWebCam Active";
		CharSequence contentText[] =  { "Hidden Mode", "Semi Background Mode", "Broadcast Receiver Mode" };
		txt =  contentText[0];
		if(mode == Mode.BACKGROUND)
			txt = contentText[1];
		else if(mode == Mode.BROADCASTRECEIVER)
			txt = contentText[2];
	    if(prefs.getBoolean("server_enabled", true))
	    {
			String myIP = RemoteControlSettings.getIpAddress(c, true);
			if(myIP != null)
				txt = txt.subSequence(0, txt.length() - 5) + " http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs);
	    }
		Intent notificationIntent = new Intent(c, MobileWebCam.class);
		PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, 0);

		notification.setLatestEventInfo(c, contentTitle, txt, contentIntent);

		mNotificationManager.notify(ACTIVE_ID, notification);
	}

	public static void StopNotification(Context c)
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager)c.getSystemService(ns);
		mNotificationManager.cancel(ACTIVE_ID);
	}
}