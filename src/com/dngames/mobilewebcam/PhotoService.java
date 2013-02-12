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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class PhotoService// implements SurfaceHolder.Callback
{
	private Handler mHandler = new Handler();
	
	private Context mContext = null;
	
	private PhotoSettings mSettings = null;
	
	private ITextUpdater mTextUpdater = null;

	private static RelativeLayout mRelative = null;
	
//***	private CenteredPreview mPreview = null;
	
	public PhotoService(Context c, ITextUpdater tu)
	{
		mContext = c;
		mTextUpdater = tu;
		
		mSettings = new PhotoSettings(mContext);
		
//***		mPreview = new CenteredPreview(mContext);
	}
	
	public static Camera mCamera = null;
//	public static SurfaceView mSurface = null;
	
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
		Log.i("MobileWebCam", "open");
		mCamera = null;
		if(Preview.mPhotoLock.getAndSet(true))
			return;
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
				mCamera.setErrorCallback(new Camera.ErrorCallback() {

					@Override
					public void onError(int error, Camera camera) {
						MobileWebCam.LogE("Camera TakePicture error: " + error);
						mCamera = null;
						camera.setPreviewCallback(null);
						camera.stopPreview();
						camera.release();
						System.gc();
						mTextUpdater.JobFinished();
					} });
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
		        		mCamera.setParameters(params);
	        		}
				}
				
				if(NewCameraFunctions.isZoomSupported(params))
					NewCameraFunctions.setZoom(params, mSettings.mZoom);
				if(NewCameraFunctions.getSupportedWhiteBalance(params) != null)
					NewCameraFunctions.setWhiteBalance(params, mSettings.mWhiteBalance);
				if(NewCameraFunctions.isFlashSupported(params))
					NewCameraFunctions.setFlash(params, mSettings.mCameraFlash ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);
				try
				{
					mCamera.setParameters(params);
				}
				catch(RuntimeException e)
				{
					e.printStackTrace();
				}
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
			}
		}
		else
		{
			mTextUpdater.Toast("Error: unable to open camera", Toast.LENGTH_SHORT);
			mTextUpdater.JobFinished();
		}
		
/* not working		Log.i("MobileWebCam", "TakePicture");
		
		AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);

//		if(mSurface == null)
		{
//			mSurface = new SurfaceView(mContext);
//			SurfaceHolder holder = mSurface.getHolder();
//			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			
			Log.v("MobileWebCam", "mContext = " + mContext.toString());
			mRelative = new RelativeLayout(mContext);
			Log.v("MobileWebCam", "mRelative = " + mRelative.toString());
			mRelative.addView(mPreview);
			Log.v("MobileWebCam", "addView = " + mPreview.toString());
			mRelative.measure(200, 200);
			mRelative.layout(0, 0, 200, 200);
			try
			{
				Bitmap tmp = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565);
				Canvas canvas = new Canvas(tmp);
				mRelative.draw(canvas);
			}
			catch(OutOfMemoryError e)
			{
				e.printStackTrace();
			}
		}

		if(mCamera != null)
		{
			Log.i("MobileWebCam", "takePicture");
//			if(mSettings.mAutoFocus)
//				mCamera.autoFocus(autofocusCallback);
//			else
			
			mCamera.setPreviewCallback(null);
			mCamera.takePicture(shutterCallback, null, photoCallback);
			Log.i("MobileWebCam", "takePicture done");
		} */		
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
			boolean nowork = false;
			Log.i("MobileWebCam", "onPictureTaken");
			Camera.Parameters parameters = camera.getParameters();
			Camera.Size s = parameters.getPictureSize();
			if(s != null)
			{
				final WorkImage work = new WorkImage(mContext, mTextUpdater, data, s);
				MobileWebCam.gPictureCounter++;
				
				mTextUpdater.UpdateText();
				
				mHandler.post(work);
				Log.i("MobileWebCam", "work posted");
			}
			else
			{
				nowork = true;
			}
			
			AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			
			if(mCamera != null)
				mCamera.startPreview();
			
			Log.i("MobileWebCam", "onPictureTaken end");

			final boolean finishedjob = nowork;
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
							mTextUpdater.JobFinished();						
					}
				});
		}
	};
	
	public static class UploadFTPPhotoTask extends AsyncTask<byte[], String, String>
	{
		private Context mContext = null;
		private ITextUpdater mTextUpdater = null;
		private PhotoSettings mSettings = null;
		
		public UploadFTPPhotoTask(Context c, ITextUpdater tu, PhotoSettings s)
		{
			mContext = c;
			mTextUpdater = tu;
			mSettings = s;
		}

		@Override
		protected String doInBackground(byte[]... jpeg)
		{
			MobileWebCam.gUploadingCount++;
			mTextUpdater.UpdateText();

			if(mSettings.mRefreshDuration >= 10)
				publishProgress(mContext.getString(R.string.uploading, mSettings.mFTP));
			
			FTPClient client = new FTPClient();  
			
				try
				{
						client.connect(InetAddress.getByName(mSettings.mFTP), mSettings.mFTPPort);
						client.login(mSettings.mFTPLogin, mSettings.mFTPPassword);
						client.changeWorkingDirectory(mSettings.mFTPDir);
	
			    if(client.getReplyString().contains("250"))
					    {
						client.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
			        BufferedInputStream buffIn = null;
			        buffIn = new BufferedInputStream(new ByteArrayInputStream(jpeg[0]));
				        if(mSettings.mFTPPassive)
				        	client.enterLocalPassiveMode();
				        else
				        	client.enterLocalActiveMode();
				 
					Date date = new Date();
		            SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
	
		            String filename = mSettings.mDefaultname;
			        if(mSettings.mFTPKeepPics > 0)
			        {
			        	filename = filename.replace(".jpg", "");
			        	// rename old pictures first
			        	for(int i = mSettings.mFTPKeepPics - 1; i > 0; i--)
			        	{
			        		try
			        		{
									client.rename(filename + i + ".jpg", filename + (i + 1) + ".jpg");
			        		}
			        		catch(IOException e)
			        		{
			        		}
			        	}
		        		try
		        		{
								client.rename(mSettings.mDefaultname, filename + "1.jpg");
		        		}
		        		catch(IOException e)
		        		{
		        		}
		        		filename = mSettings.mDefaultname;
			        }
			        else
			        {
				        if(mSettings.mFTPNumbered)
				        {
					        if(mSettings.mFTPTimestamp)
					        	filename = MobileWebCam.gPictureCounter + sdf.format(date) + ".jpg";
					        else
					        	filename = MobileWebCam.gPictureCounter + ".jpg";
				        }
				        else if(mSettings.mFTPTimestamp)
				        {
				        	filename = sdf.format(date) + ".jpg";
				        }
			        }
			        
			        boolean result = false;
			        boolean deletetmpfile = false;
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
	
						result = client.storeFile(filename, buffIn);
			        buffIn.close();
				        client.logout();
				        client.disconnect();
			        
			        if(deletetmpfile)
			        	mContext.deleteFile(filename);
	
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
			    }
			    else
			    {
			    	publishProgress("wrong ftp response: " + client.getReplyString());
			    	MobileWebCam.LogE("wrong ftp response: " + client.getReplyString());
			    }
				 
				} catch (SocketException e) {
					e.printStackTrace();
					publishProgress("Ftp socket exception!");
					MobileWebCam.LogE("Ftp socket exception!");
				} catch (UnknownHostException e) {
					e.printStackTrace();
					publishProgress("Unknown ftp host!");
					MobileWebCam.LogE("Unknown ftp host!");
				} catch (IOException e) {
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
					}
				
			MobileWebCam.gUploadingCount--;
			
			mTextUpdater.UpdateText();

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
	
	public static AtomicBoolean SaveLocked = new AtomicBoolean(false);
	public static AtomicBoolean MailLocked = new AtomicBoolean(false);

	public static class SavePhotoTask extends AsyncTask<byte[], String, String>
	{
		private Context mContext = null;
		private PhotoSettings mSettings = null;
		private ITextUpdater mTextUpdater = null;
		
		public SavePhotoTask(Context c, ITextUpdater tu, PhotoSettings s)
		{
			mContext = c;
			mTextUpdater = tu;
			mSettings = s;
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
		    	File path = new File(Environment.getExternalStorageDirectory() + "/MobileWebCam/");
		    	/*        if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 7)
		    	        	path = context.getExternalFilesDir(null); //***Environment.DIRECTORY_PICTURES);*/
    	    	boolean exists = path.exists();
    	    	if(!exists)
    	    	    exists = path.mkdirs();
    	    	if(exists)
    	    	{
    				Date date = new Date();
    	            SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
    	            String filename = sdf.format(date) + ".jpg";
    	            File file = new File(path, filename);
    				try
    		        {
    					FileOutputStream out = new FileOutputStream(file);
    					out.write(jpeg[0]);
    					out.flush();
    					out.close();
//***    					MediaStore.Images.Media.insertImage(mContext.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
    					
    					if(mSettings.mStoreGPS)
    						ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude);
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
			} catch (Exception e) {
				e.printStackTrace();
				publishProgress("Something went wrong!!!");
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

/*	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		tryOpenCam();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		tryOpenCam();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		if(mCamera != null)
		{
			mCamera = null;
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}*/
}