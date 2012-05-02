package com.dngames.mobilewebcam;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.view.WindowManager;

public class UploadSetup extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.uploadsetup);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        getPreferenceManager().findPreference("sensr_setup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	         @Override
	         public boolean onPreferenceClick(Preference preference) {
	        	 Intent intent = new Intent(Intent.ACTION_VIEW);
	        	 intent.setData(Uri.parse("http://sensr.net"));
	             startActivity(intent);
	             return true;
	         }
        });

        getPreferenceManager().findPreference("ftp_server_setup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	         @Override
	         public boolean onPreferenceClick(Preference preference) {
	        	 Intent intent = new Intent().setClass(UploadSetup.this, FTPSettings.class);
	             startActivity(intent);
	             return true;
	         }
        });

        getPreferenceManager().findPreference("sdcard_setup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	         @Override
	         public boolean onPreferenceClick(Preference preference) {
	        	 Intent intent = new Intent().setClass(UploadSetup.this, SDCardSettings.class);
	             startActivity(intent);
	             return true;
	         }
        });
	 }

     public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
     {
     }
     
    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
};