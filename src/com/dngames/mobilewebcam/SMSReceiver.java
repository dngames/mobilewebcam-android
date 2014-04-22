package com.dngames.mobilewebcam;

import java.io.IOException;
import java.util.Calendar;

import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.telephony.gsm.SmsMessage;

public class SMSReceiver extends BroadcastReceiver
{
	static final String ACTION = "android.intent.action.DATA_SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		if(prefs.getBoolean("sms_commands", false) && (Integer.parseInt(android.os.Build.VERSION.SDK) >= 4))
		{
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			String str = "";            
			if (bundle != null)
			{
				//---retrieve the SMS message received---
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];            
				for (int i=0; i<msgs.length; i++)
				{
					msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
//					str += "SMS from " + msgs[i].getOriginatingAddress();                     
//					str += " :";
					str += msgs[i].getMessageBody().toString();
				}
				
				MobileWebCam.LogI("SMS: " + str);
				
	    		if(str.startsWith("start") || str.startsWith("stop"))
	    		{
					abortBroadcast();

					if(str.startsWith("start"))
						ControlReceiver.Start(context, prefs, "sms");
					else if(str.startsWith("stop"))
						ControlReceiver.Stop(context, prefs);
	    		}
	    		else if(str.startsWith("refresh"))
	    		{
					abortBroadcast();
					
					Toast.makeText(context, "refresh sms command", Toast.LENGTH_LONG).show();
					
			        SharedPreferences.Editor ed = prefs.edit();
			        String[] d = str.split("refresh\\s*(\\d+)");
			        if(d.length > 1)
			        	ed.putString("cam_refresh", d[1]);
			        ed.commit();
	    		}
	    		else if(str.startsWith("reboot"))
	    		{
					try {
						Runtime.getRuntime().exec("su");
						Runtime.getRuntime().exec(new String[]{"su","-c","reboot now"});
					}
					catch(IOException e)
					{
						try
						{
							Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot now"});
						}
						catch(IOException e1)
						{
							if(e1.getMessage() != null)
								Toast.makeText(context, "Root for device reboot!\n" + e1.getMessage(), Toast.LENGTH_LONG).show(); 		
							e1.printStackTrace();
						}
					}
					Toast.makeText(context, "Root for device reboot!", Toast.LENGTH_LONG).show(); 		
	    		}
	    		else if(str.startsWith("photo"))
	    		{
					String v = prefs.getString("camera_mode", "1");
					if(v.length() < 1 || v.length() > 9)
	        			v = "1";
					int mode = Integer.parseInt(v);
					if(mode == 2 || mode == 3)
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
					else
					{
						Intent i = new Intent(context, MobileWebCam.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.putExtra("command", "photo");
						context.startActivity(i);
					}
	    		}
			}
		}
	}
}