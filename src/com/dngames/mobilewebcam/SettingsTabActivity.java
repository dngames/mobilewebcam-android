/* Licensed under the Apache License, Version 2.0 (the "License");
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

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;

public class SettingsTabActivity extends TabActivity
{
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//***		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.tab_settings);

        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        intent = new Intent().setClass(this, UploadSetup.class);
        spec = tabHost.newTabSpec("Upload Setup").setContent(intent);
        spec.setIndicator("Upload", getResources().getDrawable(android.R.drawable.ic_menu_share));
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, ActivitySettings.class);
        spec = tabHost.newTabSpec("Activity").setContent(intent);
        spec.setIndicator("Activity", getResources().getDrawable(android.R.drawable.ic_menu_my_calendar));
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, MobileWebCamSettings.class);
        spec = tabHost.newTabSpec("Picture").setContent(intent);
        spec.setIndicator("Picture", getResources().getDrawable(android.R.drawable.ic_menu_camera));
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, RemoteControlSettings.class);
        spec = tabHost.newTabSpec("Remote Control").setContent(intent);
        spec.setIndicator("Remote Control", getResources().getDrawable(android.R.drawable.ic_menu_call));
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, SystemSettings.class);
        spec = tabHost.newTabSpec("System").setContent(intent);
        spec.setIndicator("System", getResources().getDrawable(android.R.drawable.ic_menu_preferences));
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);
    }
    
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		MobileWebCam.gInSettings = true;
	}    
    
    public void onPause()
    {
    	super.onPause();
    	
		MobileWebCam.gInSettings = false;

		Log.i("MobileWebCam", "Settings onPause");
    }
}