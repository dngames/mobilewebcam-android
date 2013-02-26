package com.dngames.mobilewebcam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import android.app.ProgressDialog;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;
import android.content.Context;

import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import org.apache.http.conn.util.InetAddressUtils;
import java.util.Enumeration;

public class SystemSettings extends PreferenceActivity
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
		        
		final SharedPreferences prefs = SystemSettings.this.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);		        
		        
        getPreferenceManager().findPreference("backup_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
	         @Override
	         public boolean onPreferenceClick(Preference preference)
	         {
		    	File path = new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/");
    	    	boolean exists = path.exists();
    	    	if(!exists)
    	    	    exists = path.mkdirs();
    	    	if(exists)
    	    	{
					try
					{
						PrintStream ps = new PrintStream(new File(path, "config.txt"));
						ps.print(PhotoSettings.DumpSettings(prefs));
						ps.close();
						Toast.makeText(SystemSettings.this, "ok", Toast.LENGTH_SHORT).show();						
					}
					catch (FileNotFoundException e)
					{
						Toast.makeText(SystemSettings.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
    	    	}
				return true;
	         }
         });

        getPreferenceManager().findPreference("read_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
	         @Override
	         public boolean onPreferenceClick(Preference preference)
	         {
	        	File path = new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/config.txt");
    	    	if(path.exists())
    	    	{
					StringBuilder cfg = new StringBuilder();
					try
					{
						BufferedReader br = new BufferedReader(new FileReader(path));
						String line;
						while ((line = br.readLine()) != null)
						{
							cfg.append(line);
							cfg.append('\n');
						}
						PhotoSettings.GETSettings(cfg.toString(), prefs);
						Toast.makeText(SystemSettings.this, "ok", Toast.LENGTH_SHORT).show();						
					}
					catch(IOException e)
					{
						Toast.makeText(SystemSettings.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
					}
    	    	}
    	    	else
    	    	{
					Toast.makeText(SystemSettings.this, path + "config.txt not found!", Toast.LENGTH_LONG).show();
    	    	}
				return true;
	         }
         });
	 }
};