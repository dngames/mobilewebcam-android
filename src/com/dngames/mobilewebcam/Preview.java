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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.BadSurfaceTypeException;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Date;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
//***import android.graphics.ImageFormat;
//***import android.graphics.YuvImage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.os.Handler;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.OutputStream;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.commons.net.ftp.FTPClient;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.content.SharedPreferences;

public class Preview extends SurfaceView implements SurfaceHolder.Callback, ITextUpdater
{
	SurfaceHolder mHolder = null;
	Camera mCamera = null;

	private Handler mHandler = new Handler();

	private CamActivity mActivity = null;

	private PhotoSettings mSettings = null;

	private long mSetupServerMessage_lasttime = 0;
	
	public static Preview gPreview = null;
	
	public static AtomicBoolean mPhotoLock = new AtomicBoolean(false);
	public static long mPhotoLockTime = 0;
	
	public static int gOrientation = 0;
	
	private final Runnable mPostPicture = new Runnable()
	{
		public void run()
		{
			Log.i("MobileWebCam", "mPostPicture.run");
			
			if(mSettings.mReboot > 0 && MobileWebCam.gPictureCounter >= mSettings.mReboot)
			{
				try {
				    Runtime.getRuntime().exec("su");
				    Runtime.getRuntime().exec(new String[]{"su","-c","reboot now"});
				}
				catch(IOException e)
				{
					try
					{
						Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot now"});
					}
					catch(IOException e1)
					{
						if(e1.getMessage() != null)
							Toast.makeText(mActivity, "Root for device reboot!\n" + e1.getMessage(), Toast.LENGTH_LONG).show(); 		
						e1.printStackTrace();
					}
				}
				Toast.makeText(mActivity, "Root for device reboot!", Toast.LENGTH_LONG).show(); 		
           
			}
			
			if(!mSettings.mLowBatteryPause || WorkImage.getBatteryLevel(mActivity) >= 15)
			{
				if(!mPhotoLock.getAndSet(true))
				{
					mPhotoLockTime = System.currentTimeMillis();
					if((mSettings.mMode == Mode.MANUAL && MobileWebCam.gUploadingCount < 3) || (mSettings.mMode != Mode.MANUAL && MobileWebCam.gUploadingCount <= 6)) // wait if too slow
					{
						Log.i("MobileWebCam", "mPostPicture.run.tryOpenCam");
						
						tryOpenCam();
						
						if(mCamera != null)
						{
							Log.i("MobileWebCam", "mPostPicture.run mCamera");
							
							if (mSettings.mFTPPictures && !mSettings.mFTP.equals(mSettings.mDefaultFTPurl) || mSettings.mStorePictures)
							{
								boolean ignoreinactivity = false; 
								long sincelastalive = System.currentTimeMillis() - MobileWebCam.gLastMotionKeepAliveTime;
								if(mSettings.mMotionDetectKeepAliveRefresh > 0 && sincelastalive >= mSettings.mMotionDetectKeepAliveRefresh)
								{
									MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
									MobileWebCam.LogI("Taking keep alive picture!");
									ignoreinactivity = true;
								}
								
								Date date = new Date();
								if(CheckInTime(date, mSettings.mStartTime, mSettings.mEndTime, false) || ignoreinactivity)
								{
									Log.i("MobileWebCam", "mPostPicture.try");
									if(!mSettings.mShutterSound)
									{
										AudioManager mgr = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
										mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
									}
									try
									{
										Log.i("MobileWebCam", "mPostPicture.ok");
										mCamera.setPreviewCallback(null);
										
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
											try
											{
												mCamera.setParameters(params);
											}
											catch(RuntimeException e)
											{
												MobileWebCam.LogE("Camera parameter set failed! Invalid settings?");
												e.printStackTrace();
											}
										}
										
										MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
										
										if(!MobileWebCam.gIsRunning && mSettings.mDelayAfterCameraOpened > 0)
										{
											// in semi background mode some devices may need to wait for camera to be ready
											// but to keep the ui thread running take the picture in another thread
											new Thread(new Runnable()
											{ 
												@Override
												public void run()
												{
													try {
														Thread.sleep(mSettings.mDelayAfterCameraOpened);
													} catch (InterruptedException e) {
														e.printStackTrace();												
													}

													if(mCamera != null)
													{
														if(mSettings.mAutoFocus)
															mCamera.autoFocus(autofocusCallback);
														else
															mCamera.takePicture(shutterCallback, null, photoCallback);
													}
												}
											}).run();
										}
										else
										{
											// finally take the picture
											if(mSettings.mAutoFocus)
												mCamera.autoFocus(autofocusCallback);
											else
												mCamera.takePicture(shutterCallback, null, photoCallback);
										}
									}
									catch(RuntimeException e)
									{
										if(e.getMessage() != null)
											MobileWebCam.LogE(e.getMessage());
										e.printStackTrace();
										Log.v("MobileWebCam", "PhotoLock released!");
										mPhotoLock.set(false);
										JobFinished();
									}
								}
								else
								{
									// no time
									mPhotoLock.set(false);
									Log.v("MobileWebCam", "No active time! PhotoLock released!");
									if(mSettings.mMode == Mode.MANUAL)
										MobileWebCam.LogI("No active time!");
	
									JobFinished();
								}
							}
							else
							{
								if(System.currentTimeMillis() - mSetupServerMessage_lasttime > 10000)
								{
									Toast.makeText(mActivity, "Setup your server or email or local store!\n\nSettings: MENU", Toast.LENGTH_LONG).show();
									mSetupServerMessage_lasttime = System.currentTimeMillis();
								}
								MobileWebCam.LogE("Setup your server or email or local store!");
								JobFinished();
							}
	
							if(mSettings.mMode != Mode.MANUAL)
							{
								// set time for next picture
								int slowdown = Math.max(0, MobileWebCam.gUploadingCount * MobileWebCam.gUploadingCount - 1) * 1000;
								int time = mSettings.mRefreshDuration + slowdown;
								if(!mSettings.mMotionDetect || (System.currentTimeMillis() - MobileWebCam.gLastMotionTime < time))
								{
									mHandler.removeCallbacks(mPostPicture);
									mHandler.postDelayed(mPostPicture, time);
								}
								else if(mSettings.mMotionDetect)
								{
									if(mActivity.mMotionTextView != null)
										mActivity.mMotionTextView.setText("No motion detected!");
								}
							}
						}
						else
						{
							// no camera
							mPhotoLock.set(false);
							MobileWebCam.LogE("No cam! PhotoLock released!");
	
			    			JobFinished();						
						}
					}
					else// if(mSettings.mMode != Mode.MANUAL)
					{
						Log.i("MobileWebCam", "mPostPicture try soon");
						
						mPhotoLock.set(false);
						Log.v("MobileWebCam", "PhotoLock released!");
	
						// try again soon
						mHandler.removeCallbacks(mPostPicture);
						mHandler.postDelayed(mPostPicture, Math.min(Math.max(mSettings.mRefreshDuration, 1), 5000));
					}
				}
				else
				{
					Log.w("MobileWebCam", "Photo locked!");
					
					mPhotoLock.set(false);
					Log.v("MobileWebCam", "PhotoLock released!");
	
					// try again soon
					mHandler.removeCallbacks(mPostPicture);
					mHandler.postDelayed(mPostPicture, Math.min(Math.max(mSettings.mRefreshDuration, 1), 5000));
				}
			}
			else
			{
				// no battery power
				MobileWebCam.LogI("Battery low ... pause");
				// try again next picture time
				mHandler.removeCallbacks(mPostPicture);
				mHandler.postDelayed(mPostPicture, mSettings.mRefreshDuration);
			}
		};
	};
	
	// calc minutes of the day from hours:minutes time string
	private static int getDayMinutes(String tstr, int hour, String use)
	{
		String[] time = tstr.split(":");
		int h = hour;
		int m = 0;
		try
		{
			if(time.length > 0)
				h = Integer.parseInt(time[0]);
			if(time.length > 1)
				m = Integer.parseInt(time[1]);
		}
		catch(NumberFormatException e)
		{
			MobileWebCam.LogE("Invalid activity " + use + " time format!");
		}
		
		return h * 60 + m;
	}
	
	// Check if the given date time is inside the start-end time interval in daily hours.
	public static boolean CheckInTime(Date date, String starttime, String endtime, boolean night)
	{
		if(starttime.equals(endtime))
			return true;
		
		int cur_dayminutes = date.getHours() * 60 + date.getMinutes();
		if(night)
		{
			int check_dayminutes = getDayMinutes(starttime, 24, "night start"); 
			if(cur_dayminutes >= check_dayminutes)
				return true;
				
			check_dayminutes = getDayMinutes(endtime, 0, "night end");
			if(cur_dayminutes < check_dayminutes)
				return true;
		}
		else
		{
			int check_dayminutes = getDayMinutes(starttime, 0, "start"); 
			if(cur_dayminutes >= check_dayminutes)
			{
				check_dayminutes = getDayMinutes(endtime, 24, "end");
				if(cur_dayminutes < check_dayminutes)
					return true;
			}
		}
		
		return false;
	}
	
	private OrientationEventListener mOrientationListener = null;
    
	public Preview(Context context, AttributeSet attrs)
	{
		super(context, attrs);
        
        mActivity = (CamActivity)context;
        
        mOrientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL)
        {
            @Override
            public void onOrientationChanged(int orientation)
            {
            	gOrientation = orientation;
            }
        };
		
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		Log.i("MobileWebCam", "Preview(...)");				
	}
	
	public void SetSettings(PhotoSettings s)
	{
		mSettings = s;
	}
	
	public void TakePhoto()
	{
		mHandler.removeCallbacks(mPostPicture);
		mHandler.post(mPostPicture);
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
    	if((mSettings.mMode == Mode.MANUAL || !mSettings.mMobileWebCamEnabled) && event.getAction() == MotionEvent.ACTION_DOWN)
    	{
    		mSettings.EnableMobileWebCam(true);
    		TakePhoto();
    		return true;
    	}
    	
    	return super.onTouchEvent(event);
    }
    
    public void onResume()
	{
		gPreview = this;
        mOrientationListener.enable();        

		if(mSettings.mMode == Mode.NORMAL && mSettings.mMobileWebCamEnabled)
		{
			mHandler.removeCallbacks(mPostPicture);
			mHandler.postDelayed(mPostPicture, 5000);
		}

		UpdateText();

		Log.i("MobileWebCam", "Preview.onResume()");				
	}
	
	public void onPause()
	{
		mActivity.releaseLocks();
		BackgroundPhoto.releaseWakeLocks();
		
        mOrientationListener.disable();        
		mHandler.removeCallbacks(mPostPicture);

		shutDownCamAsync();
		
		if(mPreviewBitmap != null)
			mPreviewBitmap.recycle();
		mPreviewBitmap = null;
		gPreview = null;	

		Log.i("MobileWebCam", "Preview.onPause()");				
	}

	public void onDestroy()
	{
		mActivity.releaseLocks();
		BackgroundPhoto.releaseWakeLocks();
		
		mOrientationListener = null;
		mHandler.removeCallbacks(mPostPicture);

		shutDownCamAsync();
		
		gPreview = null;	

		Log.i("MobileWebCam", "Preview.onDestroy()");				
	}
	
	// wait until cam can close
	private synchronized void shutDownCamAsync()
	{
		if(mCamera != null)
		{
			if(mPhotoLock.get())
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						// close cam when no longer required
						while(mPhotoLock.get())
						{
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
						}
						
						if(mCamera != null)
						{
							Camera shutdown = mCamera;
							mCamera = null;
							shutdown.setPreviewCallback(null);
							shutdown.stopPreview();
							shutdown.release();
						}
					}
				}).start();
			}
			else
			{
				Camera shutdown = mCamera;
				mCamera = null;
				shutdown.setPreviewCallback(null);
				shutdown.stopPreview();
				shutdown.release();
			}
		}
	}	
	
	private void tryOpenCam()
	{
		Log.i("MobileWebCam", "Preview.tryOpenCam()");
		
		if(mCamera == null)
		{
			try
			{
				if(mSettings.mFrontCamera || NewCameraFunctions.getNumberOfCameras() == 1 && NewCameraFunctions.isFrontCamera(0))
				{
					Log.v("MobileWebCam", "Trying to open CAMERA 1!");
					mCamera = NewCameraFunctions.openFrontCamera();
				}
				
				if(mCamera == null)
				{
					Log.i("MobileWebCam", "Camera.open()");
					mCamera = Camera.open();
				}
				if(mCamera != null)
				{
					Log.i("MobileWebCam", "mCamera.setErrorCallback()");
					mCamera.setErrorCallback(new Camera.ErrorCallback()
					{
						@Override
						public void onError(int error, Camera camera)
						{
							if(error != 0) // Samsung Galaxy S returns 0? https://groups.google.com/forum/?fromgroups=#!topic/android-developers/ZePJqveaExk
							{
								MobileWebCam.LogE("Camera error: " + error);
								if(mCamera != null)
								{
									mCamera = null;
									camera.setPreviewCallback(null);
									camera.stopPreview();
									camera.release();
									
									System.gc();
								}
								
								mPhotoLock.set(false);
								Log.v("MobileWebCam", "PhotoLock released!");
								JobFinished();
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
					Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
					MobileWebCam.LogE(e.getMessage());
				}
				else
				{
					Toast.makeText(mActivity, "No access to camera!", Toast.LENGTH_SHORT).show();
					MobileWebCam.LogE("No access to camera!");
				}
				mCamera = null;
				mPhotoLock.set(false);
				Log.v("MobileWebCam", "PhotoLock released!");
				JobFinished();
			}
			
			if(mCamera != null)
			{
				Log.i("MobileWebCam", "mCamera.setPreviewDisplay()");
				try {
				   mCamera.setPreviewDisplay(mHolder);
				} catch (IOException exception) {
					mCamera.release();
					mCamera = null;
					if(exception.getMessage() != null)
					{
						Toast.makeText(mActivity, exception.getMessage(), Toast.LENGTH_SHORT).show();            
						MobileWebCam.LogE(exception.getMessage());
					}
					else
					{
						Toast.makeText(mActivity, "No camera preview!", Toast.LENGTH_SHORT).show();            
						MobileWebCam.LogE("No camera preview!");
					}
					mPhotoLock.set(false);
					Log.v("MobileWebCam", "PhotoLock released!");
					JobFinished();
				}
				
				// Now that the size is known, set up the camera parameters and begin
				// the preview.
				if(mCamera != null)
				{
					Camera.Parameters parameters = mCamera.getParameters();
					if(parameters != null)
					{
						parameters.set("orientation", "landscape");						
		        		mCamera.setParameters(parameters);
		        		mSettings.SetCameraParameters(parameters);
						try
						{
							mCamera.setParameters(parameters);
						}
						catch(RuntimeException e)
						{
							e.printStackTrace();
						}
					}

		//***			setCameraDisplayOrientation(MobileWebCam.this, 0, mCamera);
					
					if(mSettings.mMode != Mode.HIDDEN)
					{
						// show preview window
						try
						{
							mCamera.startPreview();
						}
						catch(RuntimeException e)
						{
							if(e.getMessage() != null)
								MobileWebCam.LogE(e.getMessage());
							else
								e.printStackTrace();
						}
					}
					
					if(mSettings.mMotionDetect)
					{
						Log.i("MobileWebCam", "mCamera.setPreviewCallback()");
						mCamera.setPreviewCallback(new PreviewCallback() {
							@Override
							public void onPreviewFrame(byte[] data, Camera camera)
							{
								if(mPreviewChecker != null)
								{
									int d = (mPreviewChecker.mDataLockIdx + 1) % mPreviewChecker.DATACOUNT;
									if(mPreviewChecker.mData[d] == null)
										mPreviewChecker.mData[d] = new byte[data.length];
									System.arraycopy(data, 0, mPreviewChecker.mData[d], 0, data.length);
									if(MobileWebCam.DEBUG_MOTIONDETECT && mActivity.mDrawOnTop != null)
										mActivity.mDrawOnTop.invalidate();
								}
							}
						});

						mPreviewChecker = new PreviewImageChecker(); 
						mPreviewChecker.start();
					}
				}
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.i("MobileWebCam", "surfaceCreated");
		
		tryOpenCam();

		if(mCamera != null && mSettings.mMobileWebCamEnabled)
		{
			if(mSettings.mMode == Mode.NORMAL)
			{
				mHandler.removeCallbacks(mPostPicture);
				mHandler.postDelayed(mPostPicture, 5000);
			}
			else if(mSettings.mMode == Mode.BACKGROUND)
			{
				// from takehiddenpicture
				TakePhoto();
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		mHandler.removeCallbacks(mPostPicture);
		
		if(mPreviewChecker != null)
			mPreviewChecker.mStop = true;
		mPreviewChecker = null;

		shutDownCamAsync();
	}
	
	// reinitialize everything
	public void RestartCamera()
	{
		offline(false);
		
		if(mCamera != null)
		{
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		tryOpenCam();
		if(mCamera != null)
			mSettings.SetCameraParameters(mCamera);
		
		online();
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		tryOpenCam();		
	}
	
	public AtomicBoolean mPreviewBitmapLock = new AtomicBoolean(false);
	public Bitmap mPreviewBitmap = null;
	
	private class PreviewImageChecker extends Thread
	{
		public boolean mStop = false;
		public int mDataLockIdx = 0;
		public final static int DATACOUNT = 2;
		public byte[][] mData = new byte[DATACOUNT][];
		
		public void run()
		{
			if(mCamera == null)
				return;
			
			try
			{
				Camera.Parameters parameters = mCamera.getParameters();
		    	int w = parameters.getPreviewSize().width;
		    	int h = parameters.getPreviewSize().height;
				int format = parameters.getPreviewFormat();
		    	int[] pixels = new int[w * h];
		    	final int samplesize = 50;
		    	byte[] last = new byte[samplesize * samplesize];
				byte[] erode = new byte[samplesize * samplesize];			
	
		    	while(!mStop)
				{
		    		if(mSettings.mMotionDetect)
		    		{
				    	if(mData[mDataLockIdx] != null)
						{
			    		// YUV formats require more conversion
	/*					if (format == ImageFormat.NV21 || format == ImageFormat.YUY2)// || format == ImageFormat.NV16)
						{
					    	// Get the YuV image
					    	YuvImage yuv_image = new YuvImage(mMyData, format, w, h, null);
					    	// Convert YuV to Jpeg
							Rect rect = new Rect(0, 0, w, h);
							ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
							yuv_image.compressToJpeg(rect, 100, output_stream);
							// Convert from Jpeg to Bitmap
							mPreviewBitmap = BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size());
						}
						// Jpeg and RGB565 are supported by BitmapFactory.decodeByteArray
						else// if (format == ImageFormat.JPEG || format == ImageFormat.RGB_565)
						{
							mPreviewBitmap = BitmapFactory.decodeByteArray(mMyData, 0, mMyData.length);
						}
						if(mPreviewBitmap != null)
							next = mPreviewBitmap.copy(mPreviewBitmap.getConfig(), false);*/
					
							MobileWebCam.decodeYUV420SPGrayscale(pixels, mData[mDataLockIdx], w, h);
						}
			    		mDataLockIdx = (mDataLockIdx + 1) % DATACOUNT;
	
			    		if(MobileWebCam.DEBUG_MOTIONDETECT)
			    			mPreviewBitmapLock.set(true);
						{
				    		if(MobileWebCam.DEBUG_MOTIONDETECT)
				    		{
								if(mPreviewBitmap == null)
									mPreviewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
								mPreviewBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
				    		}
					
							float sx = (float)w / (float)samplesize;
							float sy = (float)h / (float)samplesize;
							
							// erode
							for(int x = 0; x < samplesize; x++)
							{
								for(int y = 0; y < samplesize; y++)
								{
									int color = 0;
									for(int nx = x - 1; nx < x + 1; nx++)
									{
										for(int ny = y - 1; ny < y + 1; ny++)
										{
											int c = pixels[(int)(x * sx) + (int)(y * sy) * w] & 0xFF;
											color = Math.max(color, c);
										}
									}
									erode[x + y * samplesize] = (byte)color;
								}
							}
							// diff
							int diffcnt = 0;
							for(int x = 0; x < samplesize; x++)
							{
								for(int y = 0; y < samplesize; y++)
								{
									int diff = Math.abs((int)erode[x + y * samplesize] - (int)last[x + y * samplesize]);
									if(diff > mSettings.mMotionColorChange)
									{
							    		if(MobileWebCam.DEBUG_MOTIONDETECT)
							    			mPreviewBitmap.setPixel((int)(x * sx), (int)(y * sy), Color.argb(255, 255, 0, 0));
										diffcnt++;
									}
								}
							}
							if(diffcnt > mSettings.mMotionPixels) // attention depends on samplesize of 50
							{
	
								long lasttime = MobileWebCam.gLastMotionTime; 
								MobileWebCam.gLastMotionKeepAliveTime = MobileWebCam.gLastMotionTime = System.currentTimeMillis();
								if(MobileWebCam.gLastMotionTime - lasttime >= mSettings.mRefreshDuration * 2)
								{
									mHandler.removeCallbacks(mPostPicture);
									mHandler.post(mPostPicture);
	
									mHandler.post(new Runnable() {
										@Override
										public void run()
										{
											if(mActivity.mMotionTextView != null)
												mActivity.mMotionTextView.setText("Motion Detected!");
											if(!mSettings.mNoToasts)
												Toast.makeText(mActivity, "Motion detected!", Toast.LENGTH_SHORT).show();
										}
									});
								}
								else
								{
									final int d = diffcnt;
									mHandler.post(new Runnable() {
										@Override
										public void run()
										{
											if(mActivity.mMotionTextView != null)
												mActivity.mMotionTextView.setText("Moved pixels: " + d + "  motion detected right now!");
										}
									});
								}
							}
							else
							{
								final long sincelastmotion = System.currentTimeMillis() - MobileWebCam.gLastMotionKeepAliveTime;
								if(mSettings.mMotionDetectKeepAliveRefresh > 0 && sincelastmotion >= mSettings.mMotionDetectKeepAliveRefresh)
								{
									MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
									MobileWebCam.LogI("Taking keep alive picture!");

									// keep alive picture
									mHandler.removeCallbacks(mPostPicture);
									mHandler.post(mPostPicture);
								}
								final int d = diffcnt;
								mHandler.post(new Runnable() {
									@Override
									public void run()
									{
										if(mActivity.mMotionTextView != null)
											mActivity.mMotionTextView.setText("Moved pixels: " + d + "  last movement: " + sincelastmotion);
									}
								});
							}
						}
			    		if(MobileWebCam.DEBUG_MOTIONDETECT)
			    			mPreviewBitmapLock.set(false);
						
						System.arraycopy(erode, 0, last, 0, samplesize * samplesize);
		    		}
		    		else
		    		{
		    			try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
		    		}
				}
		    	
		    	// finish
	    		if(MobileWebCam.DEBUG_MOTIONDETECT)
	    		{
					mPreviewBitmapLock.set(true);
					{
						if(mPreviewBitmap != null)
							mPreviewBitmap.recycle();
						mPreviewBitmap = null;
					}
					mPreviewBitmapLock.set(false);
	    		}
			}
			catch(OutOfMemoryError e)
			{
				MobileWebCam.LogE("Out of memory for preview image checker!");
			}
		}
	}
	
	private PreviewImageChecker mPreviewChecker = null;
	
	Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean success, Camera camera)
		{
			Log.i("MobileWebCam", "mPostPicture.autofocus");
			// take picture now
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if(mCamera != null)
					{
						Log.i("MobileWebCam", "mPostPicture.autofocus.run");
						try
						{
							mCamera.takePicture(shutterCallback, null, photoCallback);
						}
						catch(RuntimeException e)
						{
							MobileWebCam.LogE("Error: mCamera.takePicture crashed!");
							e.printStackTrace();
						}
					}
					else
					{
						Log.w("MobileWebCam", "mPostPicture.autofocus.run no cam");
					}
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

			Log.i("MobileWebCam", "mPostPicture.photoCallback");
			
			WorkImage work = null;
			Camera.Size size = null;
			if(mCamera != null)
			{
				Camera.Parameters parameters = camera.getParameters();
				size = parameters.getPictureSize();
			}
			if(size != null)
			{
				work = new WorkImage(mActivity, Preview.this, data, size, date);
				MobileWebCam.gPictureCounter++;
				
				UpdateText();
				
				mHandler.post(work);
			}
			else
			{
				mPhotoLock.set(false);				
				Log.v("MobileWebCam", "PhotoLock released!");
			}
			
			if(mSettings.mMode != Mode.HIDDEN)
			{
				if(mCamera != null)
				{
					try
					{
						mCamera.startPreview();
					}
					catch(RuntimeException e)
					{
						e.printStackTrace();
						if(e.getMessage() != null)
						{
							Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
							MobileWebCam.LogE(e.getMessage());
						}
					}
				
					if(mSettings.mMotionDetect)
					{
						mCamera.setPreviewCallback(new PreviewCallback() {
							@Override
							public void onPreviewFrame(byte[] data, Camera camera)
							{
								if(mPreviewChecker != null && mPreviewChecker.mData != null)
								{
									int d = (mPreviewChecker.mDataLockIdx + 1) % mPreviewChecker.DATACOUNT;
									if(mPreviewChecker.mData[d] == null)
										mPreviewChecker.mData[d] = new byte[data.length];
									System.arraycopy(data, 0, mPreviewChecker.mData[d], 0, data.length);
								}
								if(MobileWebCam.DEBUG_MOTIONDETECT && mActivity.mDrawOnTop != null)
									mActivity.mDrawOnTop.invalidate();
							}
						});
					}
				}
			}
			
			if(mCamera != null && ControlReceiver.takePicture())
			{
				// PHOTO intent requested several pictures!
				mCamera.takePicture(shutterCallback, null, photoCallback);
				Log.i("MobileWebCam", "another takePicture done");
				return; // do not yet shut camera down!
			}

			if(!mSettings.mShutterSound)
			{
				AudioManager mgr = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
				mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			}
			
//			if(work != null)
//				new Thread(work).start();
		}
	};

	Camera.PictureCallback sharePhotoCallback = new Camera.PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			if(mCamera != null)
			{
				if(mSettings.mMode != Mode.HIDDEN)
					mCamera.startPreview();
			}
		}
	};
	
	public static boolean sharePictureNow = false;
	
	public void shareNextPicture()
	{
		if(mCamera != null)
		{
			MobileWebCam.gInSettings = true; // block alarms!
			sharePictureNow = true;
			mHandler.removeCallbacks(mPostPicture);
			mHandler.postDelayed(mPostPicture, 100);
		}
	}
	
	public static void sharePicture(Context c, PhotoSettings s, byte[] data)
	{
		if(c == null)
		{
			MobileWebCam.LogE("Unable to share picture: Context is null!");
			return;
		}
		
		sharePictureNow = false;
		
		FileOutputStream out = null;
		try {
			out = c.openFileOutput("current.jpg", Context.MODE_WORLD_READABLE);
			out.write(data);
			out.flush();
    		out.close();
    		
			if(s.mStoreGPS)
				ExifWrapper.addCoordinates("file:///data/data/com.dngames.mobilewebcam/files/current.jpg", WorkImage.gLatitude, WorkImage.gLongitude);
    		
			final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("image/jpeg");
			shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file:///data/data/com.dngames.mobilewebcam/files/current.jpg"));
			c.startActivity(Intent.createChooser(shareIntent, "Share picture ...").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(OutOfMemoryError e)
		{
			e.printStackTrace();
		}

		MobileWebCam.gInSettings = false;
	}
	
	// set camera back online - start to take pictures now
	public void online()
	{
		if(mSettings.mMode == Mode.NORMAL)
		{
			mHandler.removeCallbacks(mPostPicture);
			mHandler.postDelayed(mPostPicture, 2000);
		}
		else if(mSettings.mMode == Mode.BACKGROUND || mSettings.mMode == Mode.HIDDEN)
		{
			AlarmManager alarmMgr = (AlarmManager)mActivity.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(mActivity, PhotoAlarmReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mActivity, 0, intent, 0);
			alarmMgr.cancel(pendingIntent);
			Calendar time = Calendar.getInstance();
			time.setTimeInMillis(System.currentTimeMillis());
			alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
		}

		CustomReceiverService.start(mActivity);
		
		if(mCamera != null)
			mSettings.SetCameraParameters(mCamera);
	}
	
	// camera goes offline - if wanted upload offline picture now
	public void offline(boolean setofflinepicture)
	{
		mHandler.removeCallbacks(mPostPicture);
		
		if(mSettings.mMode == Mode.BACKGROUND || mSettings.mMode == Mode.HIDDEN)
		{
			AlarmManager alarmMgr = (AlarmManager)mActivity.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(mActivity, PhotoAlarmReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mActivity, 0, intent, 0);
			alarmMgr.cancel(pendingIntent);
		}

		CustomReceiverService.stop(mActivity);
		
		if(setofflinepicture)
		{
			byte[] data = null;
	
			File path = new File(Environment.getExternalStorageDirectory() + "/" + mActivity.getString(R.string.app_name) + "/");
	    	if(path.exists())
	    	{
				File file = new File(path, "offline.png");
				try
				{
					FileInputStream in = new FileInputStream(file);
					data = new byte[(int)file.length()];
					in.read(data);
					in.close();
				}
				catch(IOException e)
				{
					Toast.makeText(mActivity, "Error: unable to read offline bitmap " + file.getPath() + "! Using default.", Toast.LENGTH_LONG).show();
					data = null;
				}
				catch(OutOfMemoryError e)
				{
					Toast.makeText(mActivity, "Error: offline bitmap " + file.getName() + " too large!", Toast.LENGTH_LONG).show();
					data = null;				
				}
				if(data == null)
				{
					try
					{
						DataInputStream in = new DataInputStream(mActivity.getResources().openRawResource(R.drawable.offline));
						data = null;
						byte[] b = new byte[8192];
						int len = in.read(b);
						int read = 0;
						while(len != -1)
						{
							if(data == null || data.length < read + len)
							{
								byte[] tmp = new byte[read + len];
								if(data != null && read > 0)
									System.arraycopy(data, 0, tmp, 0, read);
								data = tmp;
							}
							System.arraycopy(b, 0, data, read, len);
							len = in.read(b);
						}
						in.close();
					}
					catch(OutOfMemoryError e)
					{
						Toast.makeText(mActivity, "Error: default offline bitmap too large!", Toast.LENGTH_LONG).show();
						data = null;
					} catch (IOException e) {
						Toast.makeText(mActivity, "Error: unable to read default offline bitmap!", Toast.LENGTH_LONG).show();
						data = null;
					}
				}
				
				Camera.Size size = null;
				if(mCamera != null)
				{
					Camera.Parameters parameters = mCamera.getParameters();
					if(parameters != null)
						size = parameters.getPictureSize();
				}
				
				final WorkImage work = new WorkImage(mActivity, this, data, size, new Date());
				mHandler.post(work);
	    	}
    	}
	}

	@Override
	public void UpdateText()
	{
		mActivity.runOnUiThread(new Runnable()
		{
		    public void run()
		    {
		    	if(mActivity.mTextView != null)
		    	{
			    	if(MobileWebCam.gUploadingCount > 3)
						mActivity.mTextView.setTextColor(Color.argb(0xff, 0xff, 0x80, 0x80));
					else
						mActivity.mTextView.setTextColor(Color.WHITE);
					int slowdown = Math.max(0, MobileWebCam.gUploadingCount * MobileWebCam.gUploadingCount - 1) * 1000;
					if(mSettings.mMode == Mode.MANUAL)
						mActivity.mTextView.setText("Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "   Manual Mode active");
					else
						mActivity.mTextView.setText("Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "   Refresh: " + (mSettings.mRefreshDuration + slowdown) / 1000 + " s");
		    	}
				
		    	if(mActivity.mCamNameViewFrame != null)
		    		mActivity.mCamNameViewFrame.setBackgroundColor(mSettings.mTextBackgroundColor);
		    	if(mActivity.mTextViewFrame != null)
		    		mActivity.mTextViewFrame.setBackgroundColor(mSettings.mDateTimeBackgroundColor);
		    }
		});
	}

	@Override
	public void Toast(String msg, int length)
	{
		if(!mSettings.mNoToasts)
		{
			Toast.makeText(mActivity, msg, length).show();
			
/*			Toast myToast = new Toast(mActivity);
			// Creating our custom text view, and setting text/rotation
			CustomTextView text = new CustomTextView(mActivity);
			text.SetText(msg);
			text.SetRotation(-90, 120, 90);
			myToast.setView(text);
			// Setting duration and displaying the toast
			myToast.setDuration(length);
			myToast.show(); */			
		}
	}
	
	@Override
	public void SetPreview(Bitmap image)
	{
		if(!MobileWebCam.gIsRunning)
			return;
		
		if(mActivity.mDrawOnTop != null && mSettings.mMode == Mode.HIDDEN)
		{		
			mPreviewBitmapLock.set(true);
	
			if(mPreviewBitmap != null)
				mPreviewBitmap.recycle();
			mPreviewBitmap = null;
			
			int w = Math.min(image.getWidth(), 384);
			float scale = (float)image.getWidth() / (float)w;
			int h = (int)((float)image.getHeight() * scale);
			if(w < h)
			{
				h = Math.min(image.getHeight(), 384);
				scale = (float)image.getHeight() / (float)h;
				w = (int)((float)image.getWidth() * scale);
			}
	
			try
			{
				mPreviewBitmap = Bitmap.createScaledBitmap(image, w, h, true);
			}
			catch(OutOfMemoryError e)
			{
				MobileWebCam.LogI("Not enough memory for fullsize preview!");
				try
				{
					mPreviewBitmap = Bitmap.createScaledBitmap(image, image.getWidth() / 20, image.getHeight() / 20, true);
				}
				catch(OutOfMemoryError e1)
				{
					e1.printStackTrace();
				}
			}
	
			mActivity.mDrawOnTop.setVisibility(VISIBLE);
			invalidate();
			mActivity.mDrawOnTop.invalidate();

			mPreviewBitmapLock.set(false);
		}
	}
	
	@Override
	public void JobFinished()
	{
		if(ControlReceiver.PhotoCount.get() > 0)
			return; // still pictures to do!
		
        mActivity.releaseLocks();
		BackgroundPhoto.releaseWakeLocks();

		SharedPreferences prefs = mActivity.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
        PhotoSettings.Mode mode = PhotoSettings.getCamMode(prefs);
		if(mode == Mode.BACKGROUND)
		{
			if(!MobileWebCam.gIsRunning)
				mActivity.finish();
		}
	}
}