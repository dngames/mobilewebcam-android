package com.dngames.mobilewebcam;

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class NewCameraFunctions
{
	private static boolean mNewCameraAvailable;
	
	/* establish whether the "new" class is available to us */
	static
	{
		try
		{
			NewCameraWrapper.checkAvailable();
			mNewCameraAvailable = true;
		}
		catch (Throwable t)
		{
			mNewCameraAvailable = false;
		}
	}

	public static List<Size> getSupportedPreviewSizes(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getSupportedPreviewSizes(params);

		return null;
	}
	
	public static int getNumberOfCameras()
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getNumberOfCameras();
		
		return 1;
	}
	
	public static Camera openFrontCamera()
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.openFrontCamera();

		return null;
	}
	
	public static boolean isZoomSupported(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.isZoomSupported(params);
		
		return false;
	}
	
	public static void setZoom(Camera.Parameters params, int zoom)
	{
		if(mNewCameraAvailable)
			NewCameraWrapper.setZoom(params, zoom);
	}

	public static List<String> getSupportedWhiteBalance(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getSupportedWhiteBalance(params);
		
		return null;
	}
	
	public static void setWhiteBalance(Camera.Parameters params, String balance)
	{
		if(mNewCameraAvailable)
			NewCameraWrapper.setWhiteBalance(params, balance);
	}
}