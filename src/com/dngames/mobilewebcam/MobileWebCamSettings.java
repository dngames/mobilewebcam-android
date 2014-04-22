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

import android.os.Bundle;
import android.preference.PreferenceActivity;

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