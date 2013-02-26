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

import android.graphics.Typeface;
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
	
	public static Typeface createTypefaceFromFile(String path)
	{
		return mExifAvailable ? ExifWrapper.createTypefaceFromFile(path) : Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
	}	
}