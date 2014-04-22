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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

public class SavePhoto extends Thread
{
	private Context mContext = null;
	private PhotoSettings mSettings = null;
	private ITextUpdater mTextUpdater = null;

	private byte[] mJpeg = null;
	private Date mDate = null;
	
	public static AtomicBoolean SaveLocked = new AtomicBoolean(false);

	public SavePhoto(Context c, ITextUpdater tu, PhotoSettings s, byte[] jpeg, Date date)
	{
		mContext = c;
		mTextUpdater = tu;
		mSettings = s;
		mJpeg = jpeg;
		mDate = date;

		mTextUpdater.JobStarted();			
	}

	@Override
	public void run()
	{
		SaveLocked.set(true);
		
		if(mSettings.mRefreshDuration >= 10)
		{
			if(!mSettings.mNoToasts)
				publishProgress(mContext.getString(R.string.storing));
		}
		
		try
		{
			File sdpath = Environment.getExternalStorageDirectory();
			if(mSettings.mSecondarySD)
				sdpath = sdpath.getParentFile();
	    	File path = new File(sdpath + mSettings.mSDCardDir);
	    	boolean exists = path.exists();
	    	if(!exists)
	    	    exists = path.mkdirs();
	    	if(exists)
	    	{
	    		final String dateformat = "yyyyMMddHHmmss"; 
	    		if(mSettings.mSDCardKeepPics > 0)
	    		{
	    			// delete everything except the n most recent entries
		        	File[] files = path.listFiles(new FilenameFilter()
		        		{
		        			public boolean accept(File dir, String filename)
		        			{
		        				if(filename.endsWith(".jpg") && filename.length() == dateformat.length() + 4)
		        				{
		        					try
		        					{
		        						Long.parseLong(filename.substring(0, filename.length() - 4));
			        					return true;
		        					}
		        					catch(NumberFormatException e)
		        					{
		        					}
		        				}
		        				return false;
		        			}
		        		});
		        	
		        	// sort by last file time
		        	Arrays.sort(files, new Comparator<File>()
		        		{
		        	    	public int compare(File f1, File f2)
		        	    	{
		        	    		return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
		        	    	}
		        	    });
		        	
		        	// delete leftovers
		        	for(int i = mSettings.mSDCardKeepPics; i < files.length; i++)
		        		files[i].delete();
	    		}
	    		
	            SimpleDateFormat sdf = new SimpleDateFormat(dateformat);
	            String filename = sdf.format(mDate) + ".jpg";
	            File file = new File(path, filename);
				try
		        {
					FileOutputStream out = new FileOutputStream(file);
					out.write(mJpeg);
					out.flush();
					out.close();
//***    					MediaStore.Images.Media.insertImage(mContext.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
					mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
					
					if(mSettings.mStoreGPS)
						ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude, WorkImage.gAltitude);
					
					if(mSettings.mLogUpload)
					{
    					try
    					{
    						PrintStream ps = new PrintStream(new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/log.txt"));
    						ps.print(MobileWebCam.GetLog(mContext, mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0), mSettings));
    						ps.close();
    					}
    					catch (FileNotFoundException e)
    					{
    						publishProgress("Error: " + e.getMessage());
    						e.printStackTrace();
    					}
					}
		        }
		        catch(IOException e)
		        {
		        	e.printStackTrace();
		        }
				catch(OutOfMemoryError err)
				{
					err.printStackTrace();
				}
	    	}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			publishProgress("Unable to write to sdcard!");
		}
		
		SaveLocked.set(false);

		PhotoSettings.GETSettings(mContext);

		mTextUpdater.JobFinished();											
	}

	protected void publishProgress(String msg)
	{
		mTextUpdater.Toast(msg, Toast.LENGTH_SHORT);            
	}
}