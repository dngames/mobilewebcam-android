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

import java.util.Date;

import android.content.Context;
import android.widget.Toast;

// base for all image storage
abstract class Upload extends Thread
{
	protected Context mContext = null;
	protected ITextUpdater mTextUpdater = null;
	protected PhotoSettings mSettings = null;
	
	protected byte[] mJpeg;
	protected Date mDate = null;
	protected String mPhotoEvent = null;
	
	protected Upload(Context c, ITextUpdater tu, PhotoSettings s, byte[] jpeg, Date date, String event)
	{
		mContext = c;
		mTextUpdater = tu;
		mSettings = s;
		mJpeg = jpeg;
		mDate = date;
		mPhotoEvent = event;
		
		mTextUpdater.JobStarted();			
	}

	protected void doInBackgroundBegin()
	{
		MobileWebCam.gUploadingCount++;
		mTextUpdater.UpdateText();
	}
	
	protected void doInBackgroundEnd(boolean getsettings)
	{
		MobileWebCam.gUploadingCount--;
		
		mTextUpdater.UpdateText();

		if(getsettings)
			PhotoSettings.GETSettings(mContext);
		
		mTextUpdater.JobFinished();
	}

	protected void publishProgress(String msg)
	{
		mTextUpdater.Toast(msg, Toast.LENGTH_SHORT);            
	}
}