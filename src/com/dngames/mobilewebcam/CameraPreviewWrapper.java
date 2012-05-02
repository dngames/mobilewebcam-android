package com.dngames.mobilewebcam;

import java.io.IOException;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.util.Log;

public class CameraPreviewWrapper
{
	/* calling here forces class initialization */
	public static void checkAvailable() {}

	public static List<Camera.Size> getSupportedPreviewSizes(Camera.Parameters parameters)
	{
		return parameters.getSupportedPreviewSizes();
	}	
}
