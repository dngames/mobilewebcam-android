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
import java.util.Locale;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ECLAIR)
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

	public static void addCoordinates(String filename, double lat, double lon, double alt)
	{
		
		try {
			ExifInterface exif = new ExifInterface(filename);
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, DegreesToDaysMinutesSeconds(Math.abs(lat)));
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GetLatRef(lat));
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, DegreesToDaysMinutesSeconds(Math.abs(lon)));
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GetLonRef(lon));
/*			exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, DegreesToDaysMinutesSeconds(Math.abs(alt)));
			exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, GetAltRef(alt)); */
			exif.saveAttributes();
		} catch (IOException e) {
			e.printStackTrace();
			MobileWebCam.LogE("No EXIF gps tag written!");
		}
	}	

	public static Typeface createTypefaceFromFile(String path)
	{
		return Typeface.createFromFile(path);
	}
}
