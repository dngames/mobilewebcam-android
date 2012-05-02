package com.dngames.mobilewebcam;

import android.util.Log;

public class ExifFunctions
{
	private static boolean mExifAvailable;
	
	/* establish whether the "new" class is available to us */
	static
	{
		try
		{
			ExifWrapper.checkAvailable();
			mExifAvailable = true;
		}
		catch (Throwable t)
		{
			mExifAvailable = false;
		}
	}

	public static void addCoordinates(String filename, double lat, double lon)
	{
		if(mExifAvailable)
			ExifWrapper.addCoordinates(filename, lat, lon);
	}
}