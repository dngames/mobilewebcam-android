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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class FTPSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.ftpserversettings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	 }

     public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
     {
 		// need to listen for ftp settings changes and reconnect later
    	if(key == "ftp_keep_open")
    	{
	 		if(UploadFTPPhoto.FTPConnection.session != null)
	 		{
	 			try
	 			{
		 			if(UploadFTPPhoto.FTPConnection.channelSftp != null)
		 				UploadFTPPhoto.FTPConnection.channelSftp.disconnect();
	 			}
	 			catch (Exception e)
	 			{
	 				e.printStackTrace();
	 			}
	 			UploadFTPPhoto.FTPConnection.session = null;
	 		}
	 		if(UploadFTPPhoto.FTPConnection.client != null)
	 		{
	 			try
	 			{
	 				UploadFTPPhoto.FTPConnection.client.abort();
	 				UploadFTPPhoto.FTPConnection.client.disconnect();
	 			}
	 			catch (IOException e)
	 			{
	 				e.printStackTrace();
	 			}
	 			UploadFTPPhoto.FTPConnection.client = null;
	 		}
    	}
     }
     
    @Override
    protected void onDestroy()
    {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
    
	@Override
	protected void onResume()
	{
		super.onResume();

		MobileWebCam.gInSettings = true;
	}    
    
    public void onPause()
    {
    	super.onPause();
    	
		MobileWebCam.gInSettings = false;
    }    
};