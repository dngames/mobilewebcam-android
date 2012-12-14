package com.dngames.mobilewebcam;

import android.annotation.TargetApi;
import android.app.Activity;
import android.view.MenuItem;

@TargetApi(11)
public class HoneyCombWrapper
{
	/* calling here forces class initialization */
	public static void checkAvailable() {}

	public static void setShowAsAction(MenuItem item, int action)
	{
		try
		{
			item.setShowAsAction(action);
		}
		catch(NoSuchMethodError e)
		{
		}
	}
	
	public static void invalidateOptionsMenu(Activity a)
	{
		try
		{
			a.invalidateOptionsMenu();
		}
		catch(NoSuchMethodError e)
		{
		}
	}
}
