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