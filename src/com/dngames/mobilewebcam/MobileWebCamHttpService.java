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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;

public class MobileWebCamHttpService extends Service implements OnSharedPreferenceChangeListener
{
	private MobileWebCamHttpServer mServer = null;
	
	public static AtomicBoolean gImageDataLock = new AtomicBoolean(false);
	public static byte[] gImageData = null;
	public static int gImageWidth = -1;
	public static int gImageHeight = -1;
	public static int gOriginalImageWidth = -1;
	public static int gOriginalImageHeight = -1;
	public static int gImageIndex = 0;
	
	public static void start(Context context)
	{
	    SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
	    if(prefs.getBoolean("server_enabled", true))
	    {
			Intent i = new Intent(context, MobileWebCamHttpService.class);  
			i.putExtra("port", getPort(prefs));
			context.startService(i);
	    }
	}

	public static boolean stop(Context context)
	{
		Intent i = new Intent(context, MobileWebCamHttpService.class);  
		return context.stopService(i);
	}
	
	static int getPort(SharedPreferences prefs)
	{
		String v = prefs.getString("port", "8080");
		if(v.length() == 0)
			v = "8080";
		return Integer.parseInt(v);
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		SharedPreferences prefs = MobileWebCamHttpService.this.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
	    if(prefs.getBoolean("server_enabled", true))
	    {
			if(mServer == null)
			{
				try
				{
					if(intent != null && intent.getExtras() != null)
					{
						mServer = new MobileWebCamHttpServer(intent.getExtras().getInt("port", 8080), MobileWebCamHttpService.this);
					}
					else
					{
						mServer = new MobileWebCamHttpServer(getPort(prefs), MobileWebCamHttpService.this);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
	    }
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if(mServer != null)
			mServer.stop();
		mServer = null;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.equals("port"))
		{
			if(mServer != null)
				mServer.stop();
			try
			{
				mServer = new MobileWebCamHttpServer(getPort(sharedPreferences), MobileWebCamHttpService.this);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else if(key.equals("server_enabled"))
		{
			if(sharedPreferences.getBoolean(key, true))
			{
				Intent i = new Intent();
	        	i.setClassName("com.dngames.mobilewebcam", "com.dngames.mobilewebcam.MobileWebCamHttpService");
				startService(i);
			}
			else
			{
				if(mServer != null)
					mServer.stop();
				mServer = null;
				stopSelf();
			}
		}
	}
}
