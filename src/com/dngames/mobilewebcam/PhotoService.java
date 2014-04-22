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

import java.util.Date;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("Instantiatable")
public class PhotoService
{
	private Handler mHandler = new Handler();
	
	private Context mContext = null;
	
	private PhotoSettings mSettings = null;
	
	private ITextUpdater mTextUpdater = null;
	
	public PhotoService(Context c, ITextUpdater tu)
	{
		mContext = c;
		mTextUpdater = tu;
		
		mSettings = new PhotoSettings(mContext);
	}
	
	public static volatile Camera mCamera = null;
	
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
	
	public void TakePicture(String event)
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
				mSettings.SetCameraParameters(params, false);
        		mCamera.setParameters(params);
			}
			
			MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();			
			
			Log.i("MobileWebCam", "takePicture");
/*			if(mSettings.mAutoFocus)
				mCamera.autoFocus(autofocusCallback);
			else*/
			try
			{
				photoCallback.mPhotoEvent = event;
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
	
	public static abstract class EventPictureCallback implements Camera.PictureCallback
	{
		public String mPhotoEvent = null;
	}

	EventPictureCallback photoCallback = new EventPictureCallback()
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
				work = new WorkImage(mContext, mTextUpdater, data, s, date, mPhotoEvent);
				MobileWebCam.gPictureCounter++;
				
				mTextUpdater.UpdateText();
				
//				mHandler.post(work);
				Log.i("MobileWebCam", "work to do " + MobileWebCam.gPictureCounter);

				
				// now start to work on the data
				if(work != null)
					new Thread(work).start();
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
					}
				});
		}
	};	
}