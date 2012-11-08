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

import java.io.File;

import android.app.ProgressDialog;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.content.Context;

import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import org.apache.http.conn.util.InetAddressUtils;
import java.util.Enumeration;

public class SystemSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.systemsettings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        getPreferenceManager().findPreference("info_open").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	         @Override
	         public boolean onPreferenceClick(Preference preference) {
	        	 Intent intent = new Intent(Intent.ACTION_VIEW);
	        	 intent.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?t=950933"));
	             startActivity(intent);
	             return true;
	         }
         });
        
        /*        try
        {
			if(getPackageManager().getApplicationInfo("com.android.vending", PackageManager.GET_META_DATA) != null)
			{*/
		        getPreferenceManager().findPreference("market_open").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			         @Override
			         public boolean onPreferenceClick(Preference preference) {
			        	 Intent intent = new Intent(Intent.ACTION_VIEW);
			        	 intent.setData(Uri.parse("https://market.android.com/details?id=com.dngames.mobilewebcam"));
			             startActivity(intent);
			             return true;
			         }
		        });
/*			}
		}
        catch (NameNotFoundException e2)
		{
        	getPreferenceManager().findPreference("market_open").setEnabled(false);
		}*/

		setIP();
		
        getPreferenceManager().findPreference("nano").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	       	 Intent intent = new Intent(Intent.ACTION_VIEW);
	       	 intent.setData(Uri.parse("http://elonen.iki.fi/code/nanohttpd/"));
	            startActivity(intent);
	            return true;
	        }
	    });
	    	    		}
	 
     public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
     {
       	 if(key.equals("server_enabled"))
       	 {
       		 if(sharedPreferences.getBoolean(key, true))
   				 MobileWebCamHttpService.start(SystemSettings.this);
       		 else
   				 MobileWebCamHttpService.stop(SystemSettings.this);
       	 }
     }
     
    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public static String getLocalIpAddress(Context c)
    {
		ConnectivityManager connManager = (ConnectivityManager)c.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if(mWifi.isConnected())
		{
			try
			{
				for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
				{
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
					{
						InetAddress inetAddress = enumIpAddr.nextElement();
						String ipv4 = null;
						if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4=inetAddress.getHostAddress()))
							return ipv4;
					}
				}
			}
			catch (SocketException ex)
			{
				Log.e("RemoteLogCat", ex.toString());
			}
		}
        return null;
    }

    private void setIP()
    {
		final String myIP = getLocalIpAddress(SystemSettings.this);
		if(myIP != null)
		{
			final SharedPreferences prefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
			getPreferenceManager().findPreference("info_ip").setTitle("http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs));
	
	        getPreferenceManager().findPreference("info_ip").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		         @Override
		         public boolean onPreferenceClick(Preference preference) {
		             final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		             emailIntent.setType("text/html");
		             emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Your MobileWebCam URL");
		             emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs));
		             startActivity(Intent.createChooser(emailIntent, "Send URL via email:"));
		             return true;
		         }
	       });
		}
		else
		{
			getPreferenceManager().findPreference("info_ip").setTitle("no WIFI connection");
			getPreferenceManager().findPreference("info_ip").setEnabled(false);
		}
	}
};