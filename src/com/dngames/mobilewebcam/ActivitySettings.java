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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.widget.Toast;
import android.os.Build.VERSION;

public class ActivitySettings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.activitysettings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		ListPreference modelist = (ListPreference)getPreferenceManager().findPreference("camera_mode");
		if(VERSION.SDK.equals("3"))
		{
			modelist.setEntries(getResources().getStringArray(R.array.entries_list_camera_mode_no_hiddenmode));
			modelist.setEntryValues(getResources().getStringArray(R.array.entryvalues_list_camera_mode_no_hiddenmode));
		}
	 }

     public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
     {
    	 if(key != null && key.equals("camera_mode"))
    	 {
	        String v = sharedPreferences.getString("camera_mode", "1");
	        if(v.length() < 1 || v.length() > 9)
	        	v = "1";
	        switch(Integer.parseInt(v))
	        {
	        case 2:
				boolean ok = false;
				try
				{
					ok = PhotoService.CheckHiddenCamInit();
				}
				catch(RuntimeException e)
				{
					e.printStackTrace();
					SharedPreferences.Editor edit = sharedPreferences.edit();
					edit.putString("camera_mode", "3");
					edit.commit();
					Toast.makeText(ActivitySettings.this, "Hidden camera mode not working on your device!", Toast.LENGTH_LONG).show();
				}

				if(ok)
					finish();
	        	break;
	        case 3:
				finish();
				break;
	        }
    	 }
     }
     
	@Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
};