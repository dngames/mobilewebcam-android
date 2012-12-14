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

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;


@TargetApi(9)
public class NewCameraWrapper
{
	/* calling here forces class initialization */
	public static void checkAvailable() {}

	public static int getNumberOfCameras()
	{
		try
		{
			return Camera.getNumberOfCameras();
		}
		catch(NoSuchMethodError e)
		{
		}

		return 1; 
	}
	
	public static Camera openFrontCamera()
	{
		try
		{
			int cameraCount = 0;
		    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		    cameraCount = Camera.getNumberOfCameras();
		    for(int camIdx = 0; camIdx < cameraCount; camIdx++)
		    {
		        Camera.getCameraInfo( camIdx, cameraInfo );
		        if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		        {
		            try
		            {
		                return Camera.open(camIdx);
		            }
		            catch (RuntimeException e)
		            {
		            	MobileWebCam.LogE("Front camera failed to open: " + e.getMessage());
		            }
		        }
		    }
		}
		catch(NoSuchMethodError e)
		{
			MobileWebCam.LogE("Front camera failed to open: " + e.getMessage());
		}
	    
	    return null;
    }
	
	public static boolean isZoomSupported(Camera.Parameters params)
	{
		try
		{
			return params.isZoomSupported();
		}
		catch(NoSuchMethodError e)
		{
		}
		
		return false;
	}
	
	public static void setZoom(Camera.Parameters params, int zoom)
	{
		try
		{
			params.setZoom(params.getMaxZoom() * zoom / 100);
		}
		catch(NoSuchMethodError e)
		{
		}
	}
	
	public static boolean isFlashSupported(Camera.Parameters params)
	{
		try
		{
			return params.getSupportedFlashModes() != null;
		}
		catch(NoSuchMethodError e)
		{
		}
		
		return false;
	}
	
	public static void setFlash(Camera.Parameters params, String flashmode)
	{
		try
		{
			params.setFlashMode(flashmode);
		}
		catch(NoSuchMethodError e)
		{
		}
	}
}
