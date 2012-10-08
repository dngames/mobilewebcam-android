package com.dngames.mobilewebcam;

import java.lang.ref.WeakReference;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.widget.Toast;

public class CustomReceiverService extends Service implements OnSharedPreferenceChangeListener
{
	static public void start(Context c)
	{
		Intent i = new Intent(c, CustomReceiverService.class);  
		c.startService(i);
	}
	
	static public void stop(Context c)
	{
		Intent i = new Intent(c, CustomReceiverService.class);  
		c.stopService(i);
	}
	
	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		
    	SharedPreferences prefs = CustomReceiverService.this.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		mBroadcastReceiver = prefs.getString("broadcast_activation", "");

    	IntentFilter filter = new IntentFilter();
        filter.addAction(mBroadcastReceiver);
        CustomReceiverService.this.registerReceiver(gCustomReceiver, filter);
        Toast.makeText(CustomReceiverService.this, "Registered broadcast receiver\n" + mBroadcastReceiver, Toast.LENGTH_LONG).show();
        MobileWebCam.LogI("Registered broadcast receiver\n" + mBroadcastReceiver);
        
        PhotoReceiver.StartNotification(CustomReceiverService.this);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		CustomReceiverService.this.unregisterReceiver(gCustomReceiver);

        PhotoReceiver.StopNotification(CustomReceiverService.this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String mBroadcastReceiver = "";
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		String old = mBroadcastReceiver;
		mBroadcastReceiver = sharedPreferences.getString("broadcast_activation", "");
		
		if(key.equals("broadcast_activation"))
		{
			if(!old.equals(mBroadcastReceiver))
			{
				stop(CustomReceiverService.this);
				start(CustomReceiverService.this);
			}
		}
	}
	
    static public final BroadcastReceiver gCustomReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
        	SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
    		if(intent.getAction().equals(prefs.getString("broadcast_activation", "")))
    		{
    			BackgroundPhoto mBGPhoto = new BackgroundPhoto();
    			mBGPhoto.mContext = new WeakReference<Context>(context);    			
    			mBGPhoto.TakePhoto(context, prefs, Mode.BROADCASTRECEIVER);
    		}
        }
    };		
}