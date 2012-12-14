package com.dngames.mobilewebcam;

import android.app.Activity;
import android.view.MenuItem;

public class HoneyCombFunctions
{
	private static boolean mHoneyCombAvailable; // front camera API level 11
	
	/* establish whether the "new" class is available to us */
	static
	{
		try
		{
			HoneyCombWrapper.checkAvailable();
			mHoneyCombAvailable = true;
		}
		catch (Throwable t)
		{
			mHoneyCombAvailable = false;
		}
	}

	public static void setShowAsAction(MenuItem item, int action)
	{
		if(mHoneyCombAvailable)
			HoneyCombWrapper.setShowAsAction(item, action);
	}
	
	public static void invalidateOptionsMenu(Activity a)
	{
		if(mHoneyCombAvailable)
			HoneyCombWrapper.invalidateOptionsMenu(a);
	}
}