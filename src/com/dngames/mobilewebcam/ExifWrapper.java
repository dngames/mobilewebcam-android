package com.dngames.mobilewebcam;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.util.Log;

public class ExifWrapper
{
	/* calling here forces class initialization */
	public static void checkAvailable() {}
	
	private static String DegreesToDaysMinutesSeconds(double coord)
	{
		// Compute degrees, minutes and seconds:
		int deg = (int)coord;
		float minfloat = 60 * ((float)coord - (float)deg); 
		int min = (int)minfloat;
		float secfloat = 60 * (minfloat - (float)min);
		// Return output:
		return deg + "/1," + min + "/1," + String.format(Locale.US, "%.4f", secfloat * 1000) + "/1000";
	}
	
	private static String GetLatRef(double lat)
	{
		if(lat > 0.0f)
			return "N";
		else
			return "S";
	}

	private static String GetLonRef(double lon)
	{
		if(lon > 0.0f)
			return "E";
		else
			return "W";
	}

	public static void addCoordinates(String filename, double lat, double lon)
	{
		
		try {
			ExifInterface exif = new ExifInterface(filename);
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, DegreesToDaysMinutesSeconds(Math.abs(lat)));
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GetLatRef(lat));
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, DegreesToDaysMinutesSeconds(Math.abs(lon)));
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GetLonRef(lon));
			exif.saveAttributes();
		} catch (IOException e) {
			e.printStackTrace();
			MobileWebCam.LogE("No EXIF gps tag written!");
		}
	}	
}
