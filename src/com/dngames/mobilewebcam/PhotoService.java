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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class PhotoService
{
	private Handler mHandler = new Handler();
	
	private Context mContext = null;
	
	private PhotoSettings mSettings = null;
	
	private ITextUpdater mTextUpdater = null;

	private static RelativeLayout mRelative = null;
	
	public PhotoService(Context c, ITextUpdater tu)
	{
		mContext = c;
		mTextUpdater = tu;
		
		mSettings = new PhotoSettings(mContext);
	}
	
	public static Camera mCamera = null;
	
	public static boolean CheckHiddenCamInit()
	{
		if(!Preview.mPhotoLock.getAndSet(true))
		{
			Preview.mPhotoLockTime = System.currentTimeMillis();
			Camera cam = Camera.open();
			if(cam != null)
			{
				cam.startPreview();
				cam.stopPreview();
				cam.release();
				System.gc();
				Preview.mPhotoLock.set(false);
				return true;
			}
		}
		
		return false;
	}
	
	public void TakePicture()
	{
		Log.i("MobileWebCam", "TakePicture");
		mCamera = null;
		if(Preview.mPhotoLock.getAndSet(true))
		{
			Log.w("MobileWebCam", "Photo locked!");
			return;
		}
		Preview.mPhotoLockTime = System.currentTimeMillis();
		
		try
		{
			if(mSettings.mFrontCamera || NewCameraFunctions.getNumberOfCameras() == 1 && NewCameraFunctions.isFrontCamera(0))
			{
				Log.v("MobileWebCam", "Trying to open CAMERA 1!");
				mCamera = NewCameraFunctions.openFrontCamera();
			}
			
			if(mCamera == null)
			{
				try
				{
					mCamera = Camera.open();
				}
				catch(RuntimeException e)
				{
					e.printStackTrace();
					if(e.getMessage() != null)
					{
						MobileWebCam.LogE(e.getMessage());						
						mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
					}
				}
			}
			if(mCamera != null)
			{
				mCamera.setErrorCallback(new Camera.ErrorCallback()
				{
					@Override
					public void onError(int error, Camera camera)
					{
						if(error != 0) // Samsung Galaxy S returns 0? https://groups.google.com/forum/?fromgroups=#!topic/android-developers/ZePJqveaExk
						{
							MobileWebCam.LogE("Camera TakePicture error: " + error);
							mCamera = null;
							camera.setPreviewCallback(null);
							camera.stopPreview();
							camera.release();
							System.gc();
							mTextUpdater.JobFinished();
						}
					}
				});
			}
		}
		catch(RuntimeException e)
		{
			e.printStackTrace();
			if(e.getMessage() != null)
			{
				mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
				MobileWebCam.LogE(e.getMessage());
			}
		}
		if(mCamera != null)
		{
			mCamera.startPreview();
			
			if(!mSettings.mShutterSound)
			{
				AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
				mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}			
			
			Camera.Parameters params = mCamera.getParameters();
			if(params != null)
			{
				if(mSettings.mImageSize == 4 || mSettings.mImageSize == 5)
				{
	        		List<Camera.Size> sizes = NewCameraFunctions.getSupportedPictureSizes(params);
	        		if(sizes != null)
	        		{
	        			params.setPictureSize(sizes.get(0).width, sizes.get(0).height);
	        			if(mSettings.mImageSize == 5)
	        			{
	        				// find best matching size (next larger)
	        				for(int i = sizes.size() - 1; i >= 0; i--)
	        				{
	        					Camera.Size s = sizes.get(i);
	        					if(s.width >= mSettings.mCustomImageW && s.height >= mSettings.mCustomImageH)
	        					{
		        					params.setPictureSize(s.width, s.height);
		        					break;
	        					}
	        				}
	        			}
	        		}
				}				
				mSettings.SetCameraParameters(params);
        		mCamera.setParameters(params);
			}
			
			MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();			
			
			Log.i("MobileWebCam", "takePicture");
/*			if(mSettings.mAutoFocus)
				mCamera.autoFocus(autofocusCallback);
			else*/
			try
			{
				mCamera.takePicture(shutterCallback, null, photoCallback);
				Log.i("MobileWebCam", "takePicture done");
			}
			catch(RuntimeException e)
			{
				MobileWebCam.LogE("takePicture failed!");
				e.printStackTrace();
				Preview.mPhotoLock.set(false);
			}
		}
		else
		{
			mTextUpdater.Toast("Error: unable to open camera", Toast.LENGTH_SHORT);
			mTextUpdater.JobFinished();
			Preview.mPhotoLock.set(false);
		}		
	}
	
	Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean success, Camera camera)
		{
			Log.i("MobileWebCam", "takePicture onAutoFocus");
			
			// take picture now
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Log.i("MobileWebCam", "takePicture onAutoFocus.run");
					
					if(mCamera != null)
						mCamera.takePicture(shutterCallback, null, photoCallback);

					Log.i("MobileWebCam", "takePicture onAutoFocus.run takePicture done");
				}
			});
		}
	};	

	Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
	{
		public void onShutter()
		{
			// no sound?
		}
	};

	Camera.PictureCallback photoCallback = new Camera.PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			Date date = new Date(); // first store current time!
			
			WorkImage work = null;

			Log.i("MobileWebCam", "onPictureTaken");
			Camera.Parameters parameters = camera.getParameters();
			Camera.Size s = parameters.getPictureSize();
			if(s != null)
			{
				work = new WorkImage(mContext, mTextUpdater, data, s, date);
				MobileWebCam.gPictureCounter++;
				
				mTextUpdater.UpdateText();
				
				mHandler.post(work);
				Log.i("MobileWebCam", "work to do");
			}

			if(mCamera != null)
			{
				mCamera.startPreview();
			
				if(ControlReceiver.takePicture())
				{
					// PHOTO intent requested several pictures!
					mCamera.takePicture(shutterCallback, null, photoCallback);
					Log.i("MobileWebCam", "another takePicture done");
					return; // do not yet shut camera down!
				}
			}
			
			Log.i("MobileWebCam", "onPictureTaken end");			
			
			AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);			
			
			final boolean finishedjob = work == null;
			mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if(mCamera != null)
						{
							Camera c = mCamera;
							mCamera = null;
							c.setPreviewCallback(null);
							c.stopPreview();
							c.release();
							System.gc();
						}
						
						Log.i("MobileWebCam", "takePicture finished");

						if(finishedjob)
						{
							Preview.mPhotoLock.set(false);
							Log.v("MobileWebCam", "PhotoLock released!");
							mTextUpdater.JobFinished();
						}
					}
				});
			
			// now start to work on the data
//			if(work != null)
//				new Thread(work).start();
		}
	};
	
	// base for all image storage
	private static abstract class UploadTask extends AsyncTask<byte[], String, String>
	{
		protected Context mContext = null;
		protected ITextUpdater mTextUpdater = null;
		protected PhotoSettings mSettings = null;
		
		protected Date mDate = null;
		
		protected UploadTask(Context c, ITextUpdater tu, PhotoSettings s, Date date)
		{
			mContext = c;
			mTextUpdater = tu;
			mSettings = s;
			mDate = date;
		}

		protected void doInBackgroundBegin()
		{
			MobileWebCam.gUploadingCount++;
			mTextUpdater.UpdateText();
		}
		
		protected void doInBackgroundEnd(boolean getsettings)
		{
			MobileWebCam.gUploadingCount--;
			
			mTextUpdater.UpdateText();
	
			if(getsettings)
				PhotoSettings.GETSettings(mContext);
			
			mTextUpdater.JobFinished();
		}
	}
	

	public static class UploadFTPPhotoTask extends UploadTask
	{
		static FTPClient client		= null;
				
		public UploadFTPPhotoTask(Context c, ITextUpdater tu, PhotoSettings s, Date date)
		{
			super(c, tu, s, date);
		}

		@Override
		protected String doInBackground(byte[]... jpeg)
		{
			doInBackgroundBegin();

			if(mSettings.mRefreshDuration >= 10 && (mSettings.mFTPBatch == 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) == 0)))
				publishProgress(mContext.getString(R.string.uploading, mSettings.mFTP + mSettings.mFTPDir));
			else if(mSettings.mFTPBatch > 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) != 0))
				publishProgress("batch ftp store");
			
ftpupload:	{
				try
				{				 
			        BufferedInputStream buffIn = null;
			        buffIn = new BufferedInputStream(new ByteArrayInputStream(jpeg[0]));
			 
		            SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
	
		            String filename = mSettings.mDefaultname;
			        if(mSettings.mFTPKeepPics == 0)
			        {
				        if(mSettings.mFTPNumbered)
				        {
					        if(mSettings.mFTPTimestamp)
					        	filename = MobileWebCam.gPictureCounter + sdf.format(mDate) + ".jpg";
					        else
					        	filename = MobileWebCam.gPictureCounter + ".jpg";
				        }
				        else if(mSettings.mFTPTimestamp)
				        {
				        	filename = sdf.format(mDate) + ".jpg";
				        }
			        }
			        
			        boolean result = false;
			        boolean deletetmpfile = false;
			        boolean upload_now = true;
					if(mSettings.mStoreGPS)
					{
	        			try
						{
							byte[] buffer = new byte[1024 * 8];
							// Creates a file in the internal, app private storage
							FileOutputStream fos;
							fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
							int r = 0;
							while((r = buffIn.read(buffer)) > -1)
								fos.write(buffer, 0, r);
							buffIn.close();
							fos.close();
							deletetmpfile = true;
						}
						catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
							MobileWebCam.LogE("No file to write EXIF gps tag!");
						}
	
						File filePath = mContext.getFilesDir();
						File file = new File(filePath, filename);
						ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude);
	        			
	        			buffIn = new BufferedInputStream(mContext.openFileInput(filename));
					}
					else if(mSettings.mFTPBatch > 1 && ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) != 0))
					{
						// store picture for later upload!
						byte[] buffer = new byte[1024 * 8];
						FileOutputStream fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
						int r = 0;
						while((r = buffIn.read(buffer)) > -1)
							fos.write(buffer, 0, r);
						buffIn.close();
						fos.close();
						upload_now = false;
					}
					
					if(upload_now)
					{
						client = new FTPClient();  
						client.connect(InetAddress.getByName(mSettings.mFTP), mSettings.mFTPPort);
						client.login(mSettings.mFTPLogin, mSettings.mFTPPassword);
						client.changeWorkingDirectory(mSettings.mFTPDir);
	
						if(!client.getReplyString().contains("250"))
					    {
					    	publishProgress("wrong ftp response: " + client.getReplyString() + "\nAre login and dir correct?");
					    	MobileWebCam.LogE("wrong ftp response: " + client.getReplyString() + "\nAre login and dir correct?");
							client = null;
					    	break ftpupload;
					    }

						client.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

				        if(mSettings.mFTPPassive)
				        	client.enterLocalPassiveMode();
				        else
				        	client.enterLocalActiveMode();
						
				        if(mSettings.mFTPKeepPics > 0)
				        {
				        	String replacename = filename.replace(".jpg", "");  
				        	// rename old pictures first
				        	for(int i = mSettings.mFTPKeepPics - 1; i > 0; i--)
				        	{
				        		try
				        		{
									client.rename(replacename + i + ".jpg", replacename + (i + 1) + ".jpg");
				        		}
				        		catch(IOException e)
				        		{
				        		}
				        	}
			        		try
			        		{
								client.rename(mSettings.mDefaultname, replacename + "1.jpg");
			        		}
			        		catch(Exception e)
			        		{
			        			if(e.getMessage() != null)
			        				MobileWebCam.LogE(e.getMessage());
			        			e.printStackTrace();
			        		}
				        }						
						
						do
						{
							// upload now
							result = client.storeFile(filename, buffIn);
					        buffIn.close();
					        buffIn = null;
					        
					        if(deletetmpfile)
					        	mContext.deleteFile(filename);
					        
					        if(mSettings.mFTPBatch > 1 || mSettings.mReliableUpload)
					        {
					        	// find older pictures not sent yet
					        	File file = mContext.getFilesDir();
					        	String[] pics = file.list(new FilenameFilter()
					        		{
					        			public boolean accept(File dir, String filename)
					        			{
					        				if(filename.endsWith(".jpg"))
					        					return true;
					        				return false;
					        			}
					        		});
					        	if(pics.length > 0)
					        	{
					        		filename = pics[0];
				        			buffIn = new BufferedInputStream(mContext.openFileInput(filename));
				        			deletetmpfile = true; // delete this file after upload!
				        			// rerun
				        			MobileWebCam.LogI("ftp batched upload: " + filename);
					        	}
					        }
						}
						while(buffIn != null);
						
				        InputStream logIS = null;
						if(mSettings.mLogUpload)
						{
							String log = MobileWebCam.GetLog(mContext, mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0), mSettings);
					        logIS = new ByteArrayInputStream(log.getBytes("UTF-8"));					
							if(logIS != null)
							{
								result &= client.storeFile("log.txt", logIS);
							}
						}
						
				        if(result)
				        {
					    	publishProgress("ok");
					    	MobileWebCam.LogI("ok");
				        }
				        else
				        {
					    	publishProgress("ftp error: " + client.getReplyString());
					    	MobileWebCam.LogE("ftp error: " + client.getReplyString());
				        }

						if(!mSettings.mFTPKeepConnected)
						{
							client.logout();
				        	client.disconnect();
				        	client = null;
						}
					}
				}
				catch (SocketException e)
				{
					e.printStackTrace();
					publishProgress("Ftp socket exception!");
					MobileWebCam.LogE("Ftp socket exception!");
					session = null;
					client = null;
				}
				catch (UnknownHostException e)
				{
					e.printStackTrace();
					publishProgress("Unknown ftp host!");
					MobileWebCam.LogE("Unknown ftp host!");
					session = null;
					client = null;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					if(e.getMessage() != null)
					{
						publishProgress("IOException: ftp\n" + e.getMessage());
						MobileWebCam.LogE("IOException: ftp\n" + e.getMessage());
					}
					else
					{
						publishProgress("ftp IOException");
						MobileWebCam.LogE("ftp IOException");
					}
					client = null;
				}
				catch (NullPointerException e)
				{
					MobileWebCam.LogE("NullPointerException:\n" + e.getMessage());
					session = null;
					client = null;
				}
			}
				
			doInBackgroundEnd(mSettings.mFTPBatch == 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) == 0));
			
			return(null);
		}

		@Override
		protected void onProgressUpdate(String... values)
		{
			mTextUpdater.Toast(values[0], Toast.LENGTH_SHORT);            
		}
	}
	
	public static AtomicBoolean SaveLocked = new AtomicBoolean(false);
	public static AtomicBoolean MailLocked = new AtomicBoolean(false);

	public static class SavePhotoTask extends AsyncTask<byte[], String, String>
	{
		private Context mContext = null;
		private PhotoSettings mSettings = null;
		private ITextUpdater mTextUpdater = null;

		private Date mDate = null;
		
		public SavePhotoTask(Context c, ITextUpdater tu, PhotoSettings s, Date date)
		{
			mContext = c;
			mTextUpdater = tu;
			mSettings = s;
			mDate = date;
		}

		@Override
		protected String doInBackground(byte[]... jpeg)
		{
			SaveLocked.set(true);
			
			if(mSettings.mRefreshDuration >= 10)
			{
				if(!mSettings.mNoToasts)
					publishProgress(mContext.getString(R.string.storing));
			}
			
			try
			{
		    	File path = new File(Environment.getExternalStorageDirectory() + mSettings.mSDCardDir);
		    	/*        if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 7)
		    	        	path = context.getExternalFilesDir(null); //***Environment.DIRECTORY_PICTURES);*/
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
    					out.write(jpeg[0]);
    					out.flush();
    					out.close();
//***    					MediaStore.Images.Media.insertImage(mContext.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
    					mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    					
    					if(mSettings.mStoreGPS)
    						ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude);
    					
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
			
			return(null);
		}

		@Override
		protected void onProgressUpdate(String... values)
		{
			mTextUpdater.Toast(values[0], Toast.LENGTH_SHORT);            
		}
	}

}