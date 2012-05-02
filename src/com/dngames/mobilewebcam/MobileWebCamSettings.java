package com.dngames.mobilewebcam;

import java.util.Calendar;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.preference.ListPreference;
import android.os.Build.VERSION;

public class MobileWebCamSettings extends PreferenceActivity
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.settings);
		
/*        getPreferenceManager().findPreference("info_imagemagick").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	         @Override
	         public boolean onPreferenceClick(Preference preference) {
	        	 Intent intent = new Intent(Intent.ACTION_VIEW);
	        	 intent.setData(Uri.parse("http://www.imagemagick.org"));
	             startActivity(intent);
	             return true;
	         }
        	});*/
	 }
     
	@Override
    protected void onDestroy() {
        super.onDestroy();
    }
};