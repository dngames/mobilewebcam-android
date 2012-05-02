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

import java.util.List;

import android.hardware.Camera;
import android.util.Log;

public class CameraPreviewFunctions
{
	private static boolean mCameraPreviewAvailable;
	
	/* establish whether the "new" class is available to us */
	static
	{
		try
		{
			ExifWrapper.checkAvailable();
			mCameraPreviewAvailable = true;
		}
		catch (Throwable t)
		{
			mCameraPreviewAvailable = false;
		}
	}

	public static List<Camera.Size> getSupportedPreviewSizes(Camera.Parameters parameters)
	{
		if(mCameraPreviewAvailable)
			return CameraPreviewWrapper.getSupportedPreviewSizes(parameters);
		
		return null;
	}
}