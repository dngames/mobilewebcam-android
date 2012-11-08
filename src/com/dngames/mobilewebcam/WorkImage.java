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

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;

/*import magick.ColorspaceType;
import magick.CompositeOperator;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;
import magick.PixelPacket;
import magick.QuantizeInfo;
import magick.util.MagickBitmap;
import fakeawt.Dimension;
import fakeawt.Rectangle;
*/
import com.dngames.mobilewebcam.PhotoSettings.ImageScaleMode;
import com.dngames.mobilewebcam.PhotoSettings.Mode;

import android.content.Context;
import android.view.WindowManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Color;
import android.hardware.Camera;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.Display;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;

public class WorkImage implements Runnable, LocationListener
{
	private WorkImage()
	{
	}

	public WorkImage(Context c, ITextUpdater tu, byte[] data, Camera.Size s)
	{
		mTextUpdater = tu;
		mContext = c;
		mData = data;
		size = s;
		
		mSettings = new PhotoSettings(c);
	}
	
	private ITextUpdater mTextUpdater = null;
	private Context mContext = null;
	private byte[] mData = null;
	private static Bitmap gBmp = null;	
	private Camera.Size size = null;
	private PhotoSettings mSettings = null;
	
	private LocationManager locationManager;
	private Geocoder geocoder;	
	
	public static String getBatteryInfo(Context context, String format)
	{
		Context c = context.getApplicationContext();
		if(c == null)
			c = context;
		Intent batteryIntent = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int rawlevel = batteryIntent.getIntExtra("level", -1);
		double bscale = batteryIntent.getIntExtra("scale", -1);
		int temp = batteryIntent.getIntExtra("temperature", 0);
		double level = -1;
		if (rawlevel >= 0 && bscale > 0)
		    level = rawlevel / bscale;
		String txt;
		try
		{
			txt = String.format(format, (int)(level * 100.0), (float)temp / 10.0f);
		}
		catch(IllegalFormatException e)
		{
				txt = e.getMessage();
		}
		
		return txt;
	}
	
/*	public static Bitmap loadScaled(byte[] imgdata, int target_w, int target_h, boolean keepsize, boolean matchtarget, Bitmap.Config cfg)
	{
		BitmapFactory.Options opts = new BitmapFactory.Options();
		
		opts.inTempStorage = new byte[16 * 1024];
		opts.inJustDecodeBounds = true;
		try
		{
    		BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length, opts);
		}
		catch(OutOfMemoryError err)
		{
			MobileWebCam.LogE("Not enough memory to detect picture size! Keeping size.");
			keepsize = true;
		}
		
		int width_tmp = opts.outWidth, height_tmp = opts.outHeight;
		
		//Find the correct scale value. It should be the power of 2.
		float desiredScaleW = 1.0f;
		float desiredScaleH = 1.0f;
		opts = new BitmapFactory.Options();
		if(!keepsize)
		{
			int scale = 1;
			int trycount = 10;
			while(true && trycount > 0)
			{
				if(width_tmp / 2 < target_w || height_tmp / 2 < target_h)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
				trycount--;
			}
			
			desiredScaleW = (float) target_w / width_tmp;
			desiredScaleH = (float) target_w / width_tmp;

			opts.inSampleSize = scale;
//***				opts.inMutable = true;
		}
		else
		{
			opts.inSampleSize = 1;
		}
//		opts.inScaled = false;
		opts.inDither = false;
		opts.inPreferredConfig = cfg;
		
		boolean toolarge = false;
		Bitmap tmp = null;
		int trycount = 10;
		do
		{
			try
			{
				tmp = BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length, opts);
				opts.inSampleSize *= 2;
			}
			catch(OutOfMemoryError e)
			{
				toolarge = true;
				e.printStackTrace();
				MobileWebCam.LogE("Not enough memory for wanted picture size! (1/" + opts.inSampleSize + ")");
			}
			trycount--;
		}
		while(toolarge && trycount > 0);
		
		System.gc();
		
		if(tmp != null && matchtarget)
		{
			Bitmap old = tmp;
			try
			{
				// Resize
				Matrix matrix = new Matrix();
				matrix.postScale(desiredScaleW, desiredScaleH);
				tmp = Bitmap.createBitmap(old, 0, 0, old.getWidth(), old.getHeight(), matrix, true);
					
//				tmp = Bitmap.createScaledBitmap(old, mSettings.mCustomImageW, mSettings.mCustomImageH, true);
			}
			catch(OutOfMemoryError e)
			{
				e.printStackTrace();
				tmp = old;
			}

			if(old != tmp)
			{
				old.recycle();
				old = null;
			}
		}			
		
		return tmp;
	}*/	
	
/*	private Bitmap applyFilters(Bitmap tmp, boolean rotate)
	{
		System.gc();
		
        try
        {
			MagickImage m = MagickBitmap.fromBitmap(tmp);
			tmp.recycle();
			tmp = null;

			System.gc();
			
			switch(mSettings.mFilterType)
			{
			case 0:
				// grayscale
				QuantizeInfo qi = new QuantizeInfo();
				qi.setColorspace(ColorspaceType.GRAYColorspace);
				m.quantizeImage(qi);
				
//				qi = new QuantizeInfo();
//				qi.setColorspace(ColorspaceType.RGBColorspace);
//				m.quantizeImage(qi);
				break;
			case 1:
				// contrast
				m.contrastImage(true);
				break;
			case 2:
				// enhance
				m.enhanceImage();
				break;
			case 3:
				// equalize
				m.equalizeImage();
				break;
			case 5:
				// lomo
				Log.i("MobileWebCam", "Lomo -------------------------------------------------");
				m.levelImage("33%,33%");
				Log.i("MobileWebCam", "Lomo - level done");

				System.gc();
				
				Log.i("MobileWebCam", "Lomo - gc done");
				// no break -> lomo also has vignette
			case 4:
				// vignette
				double crop_factor = 1.5f;
				int crop_x = (int)Math.floor(m.getWidth() * crop_factor);
		        int crop_y = (int)Math.floor(m.getHeight() * crop_factor);
				
				MagickImage radial = new MagickImage();
				ImageInfo ri = new ImageInfo("radial-gradient:none-black");
				ri.setSize(crop_x + "x" + crop_y);
				radial.readImage(ri);

				Log.i("MobileWebCam", "Vignette - readimage done");
				
				Rectangle r = new Rectangle();
				r.setBounds(crop_x / 2 - m.getWidth() / 2, crop_y / 2 - m.getHeight() / 2, m.getWidth(), m.getHeight());

				Log.i("MobileWebCam", "Vignette - rect");
				Log.i("MobileWebCam", "Vignette - width = " + r.width);
				
				radial.cropImage(r);
				
				Log.i("MobileWebCam", "Vignette - cropped");
				
				System.gc();						
				
				Log.i("MobileWebCam", "Vignette - gc done");
				m.compositeImage(CompositeOperator.MultiplyCompositeOp, radial, m.getWidth() / 2 - radial.getWidth() / 2,  m.getHeight() / 2 - radial.getHeight() / 2);

				Log.i("MobileWebCam", "Vignette - finished -------------------------------------------");
				break;
			case 6:
				// charcoal
				m = m.charcoalImage(0.5, 0.5);
				break;
			case 7:
				// oilpaint
				m.oilPaintImage(10.0f);
				break;
			case 8:
				// tilt shift
//					    # TILT SHIFT
//			    public function tilt_shift()
//			    {
//			        $this->tempfile();

//			        $this->execute("convert 
//			        ( $this->_tmp -gamma 0.75 -modulate 100,130 -contrast ) 
//			        ( +clone -sparse-color Barycentric '0,0 black 0,%h white' -function polynomial 4,-4,1 -level 0,50% ) 
//			        -compose blur -set option:compose:args 5 -composite 
//			        $this->_tmp");

//			        $this->output();
//			    }
				break;
			}		
			
			System.gc();					
			
			if(rotate)
				m = m.rotateImage(90.0);
			
			System.gc();					
			
			tmp = MagickBitmap.ToBitmap(m);
		}
        catch (MagickException e)
        {
			e.printStackTrace();
			MobileWebCam.LogE("Unable to process image fx!");
		}
        catch(OutOfMemoryError e)
        {
        	e.printStackTrace();
			MobileWebCam.LogE("Not enough memory to process image!");
        }

        System.gc();
        
        return tmp;
	} */	
	
	@Override
	public void run()
	{
		if(mData == null)
		{
			Preview.mPhotoLock.set(false);
			Log.v("MobileWebCam", "PhotoLock released!");
			mTextUpdater.JobFinished();
			return;
		}
		
		Log.v("MobileWebCam", "WorkImage.run()");
		
		if(mSettings.mStoreGPS || mSettings.mImprintGPS)
		{
	    	locationManager = (LocationManager)mContext.getSystemService(android.content.Context.LOCATION_SERVICE);
	    	if(locationManager != null)
	    	{
		    	geocoder = new Geocoder(mContext);
		
		    	gLocation = null;
		    	
		    	Location location = null;
		    	try
		    	{
		    		location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		    	}
		    	catch(IllegalArgumentException e)
		    	{
		    		e.printStackTrace();
		    	}
		    	if (location != null)
		    	{
		    		this.onLocationChanged(location);
		        }
				else
				{
		    		location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		    		if (location != null)
		    		{
		    			this.onLocationChanged(location);
					}
				}
		
		       	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 50, this);
	    	}
	    	else
	    	{
	    		gLocation = null;
	    	}
		}
		
		// something left here from before?
		if(gBmp != null && !gBmp.isRecycled())
		{
			Log.e("MobileWebCam", "gBmp left from before!");
			gBmp.recycle();
		}
		gBmp = null;		

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BitmapFactory.Options opts = new BitmapFactory.Options();
		
		int final_w = -1;
		int final_h = -1;
		
		int target_w = 512;
		int target_h = 384;
		if(mSettings.mImageSize == 2)
		{
			target_w = 640;
			target_h = 480;
		}
		else if(mSettings.mImageSize == 3)
		{
			target_w = 1024;
			target_h = 768;
		}
		else if(mSettings.mImageSize == 5)
		{
			target_w = mSettings.mCustomImageW;
			target_h = mSettings.mCustomImageH;
		}
		else if(mSettings.mImageSize == 0)
		{
			target_w = 320;
			target_h = 240;
		}
		
		//Find the correct scale value. It should be the power of 2.
		float desiredScaleW = 1.0f;
		float desiredScaleH = 1.0f;
		opts = new BitmapFactory.Options();
		if(size != null && mSettings.mImageSize != 4)
		{
			int width_tmp = size.width, height_tmp = size.height;
			int scale = 1;
			int trycount = 10;
			while(true && trycount > 0)
			{
				if(width_tmp / 2 < target_w || height_tmp / 2 < target_h)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
				trycount--;
			}
			opts.inSampleSize = scale;
//***				opts.inMutable = true;
			
			if(mSettings.mCustomImageScale == ImageScaleMode.LETTERBOX)
			{
				desiredScaleH = desiredScaleW = Math.min((float)target_w / (float)width_tmp, (float)target_h / (float)height_tmp);
			}
			else if(mSettings.mCustomImageScale == ImageScaleMode.CROP)
			{
				desiredScaleH = desiredScaleW = Math.max((float)target_w / (float)width_tmp, (float)target_h / (float)height_tmp);
			}
			else if(mSettings.mCustomImageScale == ImageScaleMode.NOSCALE)
			{
				desiredScaleW = 1.0f;
				desiredScaleH = 1.0f;
			}
			else // if(mSettings.mCustomImageScale == ImageScaleMode.STRETCH)
			{
				desiredScaleW = (float)target_w / (float)width_tmp;
				desiredScaleH = (float)target_h / (float)height_tmp;
			}
			
//			MobileWebCam.LogI("Custom image size scaling: " + desiredScaleW + ", " + desiredScaleH);
		}
		else
		{
			opts.inSampleSize = 1;
		}
//		opts.inScaled = false;
		opts.inDither = false;
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		
		Log.v("MobileWebCam", "opts.inSampleSize = " + opts.inSampleSize);		
				
		boolean toolarge = false;
		int trycount = 10;
		do
		{
			try
			{
				gBmp = BitmapFactory.decodeByteArray(mData, 0, mData.length, opts);
				opts.inSampleSize *= 2;
			}
			catch(OutOfMemoryError e)
			{
				toolarge = true;
				e.printStackTrace();
				MobileWebCam.LogE("Not enough memory for wanted picture size! (1/" + opts.inSampleSize + ")");
			}
			trycount--;
		}
		while(toolarge && trycount > 0);
		
		Log.v("MobileWebCam", "gBmp = " + gBmp + " opts.inSampleSize = " + opts.inSampleSize);		
		
		if(gBmp != null)
		{
			mData = null; // first decode successfull no longer need original data (otherwise keep as fallback)
			System.gc();
			
			if(mSettings.mImageSize == 5)
			{
				Bitmap old = gBmp;
				try
				{
					// Resize
					Matrix matrix = new Matrix();
					matrix.postScale(desiredScaleW, desiredScaleH);
					int crop_x = 0;
					int crop_y = 0;
					int crop_w = old.getWidth();
					int crop_h = old.getHeight();
					if(mSettings.mCustomImageScale == ImageScaleMode.CROP)
					{
						int w = crop_w;
						int h = crop_h;
						crop_w = w - (w - (int)((float)target_w / desiredScaleW));
						crop_h = h - (h - (int)((float)target_h / desiredScaleH));
						crop_x = (w - (int)((float)target_w / desiredScaleW)) / 2;
						crop_y = (h - (int)((float)target_h / desiredScaleH)) / 2;
					}
					gBmp = Bitmap.createBitmap(old, crop_x, crop_y, crop_w, crop_h, matrix, true);
				}
				catch(OutOfMemoryError e)
				{
					e.printStackTrace();
					gBmp = old;
				}
	
				if(old != gBmp)
				{
					old.recycle();
					old = null;
				}
			}
		}
		
		if(gBmp != null)
		{
			boolean ignoreinactivity = false; 
			long sincelastalive = System.currentTimeMillis() - MobileWebCam.gLastMotionKeepAliveTime;
			if(sincelastalive >= mSettings.mMotionDetectKeepAliveRefresh)
			{
				MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
				ignoreinactivity = true;
			}

// TODO: do night detection on own downsampled image from mData to 100x100 for all sizes if! gBmp == null
			if(mSettings.mNightDetect && isNightImage(gBmp) && !ignoreinactivity)
			{
				Preview.mPhotoLock.set(false);
				MobileWebCam.LogI("Dropping night image.");
				Log.i("MobileWebCam", "PhotoLock released!");
				mTextUpdater.JobFinished();
				if(locationManager != null)
					locationManager.removeUpdates(this);
				gBmp.recycle();
				gBmp = null;
				return;
			}
			
			boolean rotate = mSettings.mForcePortraitImages && !mSettings.mFrontCamera;
			if(!rotate && Preview.gOrientation != -1 && mSettings.mAutoRotateImages)
			{
				if((Preview.gOrientation > 315 || Preview.gOrientation < 45) || (Preview.gOrientation > 135 && Preview.gOrientation < 225))
					rotate = true;
			}
			
			// imagemagick fx				
/*			if(mSettings.mFilterPicture)
				applyFilters(gBmp, rotate);
	*/		
			boolean mutable = true;
			Bitmap old = gBmp;
			try
			{
				if(rotate)
					gBmp = Bitmap.createBitmap(old.getHeight(), old.getWidth(), Bitmap.Config.ARGB_8888);
				else
					gBmp = old.copy(Bitmap.Config.ARGB_8888, true);
			}
			catch(OutOfMemoryError e)
			{
				e.printStackTrace();
				gBmp = old;
				mutable = false;
				MobileWebCam.LogE("Not enough memory to process image!");
			}
			
			int oldw = old.getWidth();
			int oldh = old.getHeight();
			
			if(old != gBmp && !rotate && gBmp != null && mutable)
			{
				// try to save some memory except when rotate is needed -> then release there later
				old.recycle();
				old = null;
			}			

			if(gBmp != null)
			{
				if(mSettings.mFlipImages && mutable)
				{
					// some front camera images are flipped upside down ...
					Canvas canvas = new Canvas(gBmp);
					Matrix matrix = new Matrix();
					matrix.preScale(-1f, 1f);
					matrix.postTranslate(gBmp.getWidth(), 0);
					canvas.drawBitmap(gBmp, matrix, new Paint());
				}
			
				if(size != null)
					Log.v("MobileWebCam", "Picture size: " + size.width + "x" + size.height + " -> " + gBmp.getWidth() + "x" + gBmp.getHeight());
			}
			else
			{
				// use mData as we got it as a last resort
				MobileWebCam.LogI("Using unmodified original size picture!");
				
/*				Preview.mPhotoLock.set(false);
				Log.v("MobileWebCam", "PhotoLock released!");
				MobileWebCam.LogE("No image!?");
				mTextUpdater.JobFinished();
				if(locationManager != null)
					locationManager.removeUpdates(this);
				return;*/
			}
			
			if(mSettings.mStoreGPS || mSettings.mImprintGPS)
			{
				int cnt = 0;
				// wait until location got
				while(gLocation == null && cnt < 60)
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					cnt++;
				}
				if(locationManager != null)
					locationManager.removeUpdates(this);
			}

			if(gBmp != null && mutable)
			{
				float scale = gBmp.getWidth() / 320.0f;
				float textsize = 6.0f * scale;
				if(mSettings.mImageSize == 5 && (mSettings.mCustomImageW < 480 || mSettings.mCustomImageH < 480))
					textsize += 2.0f;
				else if(mSettings.mImageSize == 0)
					textsize += 1.0f;
				
				Canvas canvas = new Canvas(gBmp);				

				if(rotate && old != null)
				{
					Matrix m = new Matrix();
					m.setRotate(90, oldw / 2, oldh / 2);
					m.postTranslate(gBmp.getWidth() / 2 - oldw / 2, gBmp.getHeight() / 2 - oldh / 2);
					canvas.drawBitmap(old, m, null); //gBmp.getWidth() / 2 - old.getHeight() / 2, gBmp.getHeight() / 2 - old.getWidth() / 2, null);
					
					if(old != gBmp)
					{
						old.recycle();
						old = null;
					}					
				}
				
				if(mSettings.mImprintPicture && mSettings.mImprintBitmap != null)
				{
					Rect src = new Rect(0, 0, mSettings.mImprintBitmap.getWidth(), mSettings.mImprintBitmap.getHeight());
					Rect dst = new Rect(0, 0,canvas.getWidth(), canvas.getHeight());
					canvas.drawBitmap(mSettings.mImprintBitmap, src, dst, null);
				}
				
				Paint p = new Paint();
				if(mSettings.mImprintDateTime.length() > 0)
				{
					Date date = new Date();
					SimpleDateFormat sdf = null;
					try
					{
						sdf = new SimpleDateFormat(mSettings.mImprintDateTime);
					}
					catch(IllegalArgumentException e)
					{
						MobileWebCam.LogE(e.toString());
						sdf = new SimpleDateFormat("format error! yyyy/MM/dd   HH:mm:ss");
					}
        			p.setAntiAlias(true);
        			p.setTextSize(textsize);
        			String txt = "";
        			try
        			{
        				txt = sdf.format(date); 
        			}
        			catch(IllegalArgumentException e)
        			{
						MobileWebCam.LogE(e.toString());
						txt = "format error!";
        			}
        			
        			float textWidth = p.measureText(txt);
        			float textHeight = p.descent() - p.ascent();
        			int rectX = (int)(gBmp.getWidth() * mSettings.mDateTimeX / 100 - textWidth / 2);
        			int tx = rectX;
        		    int ty = (int)(gBmp.getHeight() * mSettings.mDateTimeY / 100 - textHeight / 2);
        		    int rectWidth = (int)textWidth;
        		    if(mSettings.mDateTimeBackgroundLine)
        		    {
        		    	rectX = 0;
            		    rectWidth = canvas.getWidth();
        		    }
        		    
        		    p.setColor(mSettings.mDateTimeBackgroundColor);
        		    canvas.drawRect(rectX - textHeight / 2, ty + textHeight / 2, rectX + rectWidth + textHeight / 2, ty - textHeight, p);        			
        			
    				p.setColor(mSettings.mDateTimeShadowColor);
        			canvas.drawText(txt, tx + 1, ty + 1, p);
    				p.setColor(mSettings.mDateTimeColor);
        			canvas.drawText(txt, tx, ty, p);
				}
				
				if(mSettings.mImprintText.length() > 0)
				{
        			p.setAntiAlias(true);
        			p.setTextSize(textsize);
        			
        			float textWidth =  p.measureText(mSettings.mImprintText);
        			float textHeight = p.descent() - p.ascent();
        			int rectX = (int)(gBmp.getWidth() * mSettings.mTextX / 100 - textWidth / 2);
        			int tx = rectX;
        		    int ty = (int)(gBmp.getHeight() * mSettings.mTextY / 100 - textHeight / 2);
        		    int rectWidth = (int)textWidth;
        		    if(mSettings.mTextBackgroundLine)
        		    {
        		    	rectX = 0;
            		    rectWidth = canvas.getWidth();
        		    }
        		    
        		    p.setColor(mSettings.mTextBackgroundColor);
        		    canvas.drawRect(rectX - textHeight / 2, ty + textHeight / 2, rectX + rectWidth + textHeight / 2, ty - textHeight, p);        			
        			
    				p.setColor(mSettings.mTextShadowColor);
        			canvas.drawText(mSettings.mImprintText, tx + 1, ty + 1, p);
    				p.setColor(mSettings.mTextColor);
        			canvas.drawText(mSettings.mImprintText, tx, ty, p);
				}
				
				if(mSettings.mImprintStatusInfo.length() > 0)
				{
					String txt = getBatteryInfo(mContext, mSettings.mImprintStatusInfo);

					p.setAntiAlias(true);
        			p.setTextSize(textsize);
        			
        			float textWidth = p.measureText(txt);
        			float textHeight = p.descent() - p.ascent();
        			int rectX = (int)(gBmp.getWidth() * mSettings.mStatusInfoX / 100 - textWidth / 2);
        			int tx = rectX;
        		    int ty = (int)(gBmp.getHeight() * mSettings.mStatusInfoY / 100 - textHeight / 2);
        		    int rectWidth = (int)textWidth;
        		    if(mSettings.mDateTimeBackgroundLine)
        		    {
        		    	rectX = 0;
            		    rectWidth = canvas.getWidth();
        		    }
        		    
        		    p.setColor(mSettings.mDateTimeBackgroundColor);
        		    canvas.drawRect(rectX - textHeight / 2, ty + textHeight / 2, rectX + rectWidth + textHeight / 2, ty - textHeight, p);        			
        			
    				p.setColor(mSettings.mDateTimeShadowColor);
        			canvas.drawText(txt, tx + 1, ty + 1, p);
    				p.setColor(mSettings.mDateTimeColor);
        			canvas.drawText(txt, tx, ty, p);
				}
				
				if(mSettings.mImprintGPS)
				{
        			p.setAntiAlias(true);
        			p.setTextSize(textsize);
        			String txt = String.format("%10f, %10f", gLatitude, gLongitude);
        			if(mSettings.mImprintLocation && gLocation != null)
        				txt = gLocation;
        			
        			float textWidth = p.measureText(txt);
        			float textHeight = p.descent() - p.ascent();
        			int rectX = (int)(gBmp.getWidth() * mSettings.mGPSX / 100 - textWidth / 2);
        			int tx = rectX;
        		    int ty = (int)(gBmp.getHeight() * mSettings.mGPSY / 100 - textHeight / 2);
        		    int rectWidth = (int)textWidth;
        		    if(mSettings.mDateTimeBackgroundLine)
        		    {
        		    	rectX = 0;
            		    rectWidth = canvas.getWidth();
        		    }
        		    
        		    p.setColor(mSettings.mDateTimeBackgroundColor);
        		    canvas.drawRect(rectX - textHeight / 2, ty + textHeight / 2, rectX + rectWidth + textHeight / 2, ty - textHeight, p);        			
        			
    				p.setColor(mSettings.mDateTimeShadowColor);
        			canvas.drawText(txt, tx + 1, ty + 1, p);
    				p.setColor(mSettings.mDateTimeColor);
        			canvas.drawText(txt, tx, ty, p);
				}

				Log.v("MobileWebCam", "Workimage: imprint done");
				
				try
				{
					gBmp.compress(Bitmap.CompressFormat.JPEG, mSettings.mImageCompression, out);
					
					final_w = gBmp.getWidth();
					final_h = gBmp.getHeight();
					
					out.flush();
					out.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				catch(OutOfMemoryError e)
				{
					MobileWebCam.LogE("Error: unable to compress jpg - out of memory!");
				}

				if(MobileWebCam.gIsRunning)
				{
					try
					{
						mTextUpdater.SetPreview(gBmp);
					
						Log.v("MobileWebCam", "Workimage: preview set");
					}
					catch(OutOfMemoryError e)
					{
						Log.e("MobileWebCam", e.toString());
					}
				}

				gBmp.recycle();
				gBmp = null;
				
				mData = out.toByteArray();
			}
			else if(MobileWebCam.gIsRunning)
			{
				// set fallback preview image
				Bitmap smalltmp = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
				if(smalltmp != null)
				{
					Canvas c = new Canvas(smalltmp);
					if(c != null)
					{
						c.drawARGB(255, 255, 128, 128);
						Paint p = new Paint();
	        			p.setAntiAlias(true);
	        			p.setTextSize(12);
	    				p.setColor(mSettings.mDateTimeColor);
						c.drawText("memory", 25, 50, p);
					}

//***				Bitmap smalltmp = loadScaled(mData, 100, 100, false, false, Bitmap.Config.RGB_565);
					MobileWebCam.LogI("Smaller preview set from original image because modified version is lost!");
					mTextUpdater.SetPreview(smalltmp);
					
					smalltmp.recycle();
					smalltmp = null;
				}
			}
		}
		
		System.gc();

		if(mData != null && mData.length > 0)
		{
			// show browserinterface preview and pic info
			synchronized(MobileWebCamHttpService.gImageDataLock)
			{
				MobileWebCamHttpService.gImageData = mData;
				MobileWebCamHttpService.gImageWidth = final_w;
				MobileWebCamHttpService.gImageHeight = final_h;
				if(size != null)
				{
					MobileWebCamHttpService.gOriginalImageWidth = size.width;
					MobileWebCamHttpService.gOriginalImageHeight = size.height;
				}
				MobileWebCamHttpService.gImageIndex++;
			}

			// now send picture
			if(!Preview.sharePictureNow)
			{
				MobileWebCam.LogI("Picture taken ... uploading now!");
				
				if(mSettings.mFTPPictures && (MobileWebCam.gPictureCounter % mSettings.mFTPFreq) == 0)
					new PhotoService.UploadFTPPhotoTask(mContext, mTextUpdater, mSettings).execute(mData);
				if(mSettings.mStorePictures && !PhotoService.SaveLocked.get() && (MobileWebCam.gPictureCounter % mSettings.mStoreFreq) == 0)
					new PhotoService.SavePhotoTask(mContext, mTextUpdater, mSettings).execute(mData);
			}
			else
			{
				MobileWebCam.LogI("Picture taken ... sharing now!");
				
				Preview.sharePicture(mContext, mSettings, mData);
			}
		}
		else
		{
			MobileWebCam.LogE("Skipped image of size 0!");
		}
		
		Preview.mPhotoLock.set(false);
		Log.v("MobileWebCam", "PhotoLock released!");
	}

	private boolean isNightImage(Bitmap tmp)
	{
		if(tmp == null || tmp.isRecycled())
			return false;
			
		int w = tmp.getWidth();
		int h = tmp.getHeight();
		float sx = w / 100.0f;
		float sy = h / 100.0f;

		// make gray
		int[][] graym = new int[100][100];
		for(int x = 0; x < 100; x++)
		{
			for(int y = 0; y < 100; y++)
			{
				int c = tmp.getPixel((int)(x * sx), (int)(y * sy));
				int r = Color.red(c);
				int g = Color.green(c);
				int b = Color.blue(c);
				graym[x][y] = Math.max(Math.max(r, b), g);
			}
		}
			
		// erode
		int[][] erode = new int[100][100];			
		for(int x = 0; x < 100; x++)
		{
			for(int y = 0; y < 100; y++)
			{
				int color = 0;
				for(int nx = x - 1; nx < x + 1; nx++)
				{
					for(int ny = y - 1; ny < y + 1; ny++)
					{
						int c = graym[Math.min(100 - 1, Math.max(0, x + nx))][Math.min(100 - 1, Math.max(0, y + ny))];
						color = Math.max(color, c);
					}
				}
				erode[x][y] = color;
			}
		}
			
		int blackcount = 0;
		int isblack = (int)((100.0f * 100.0f) * 0.75f);
		for(int x = 0; x < 100; x++)
		{
			for(int y = 0; y < 100; y++)
			{
				int c = erode[x][y];
				if(c < 32)
				{
					blackcount++;
					if(blackcount > isblack)
						return true;
				}
			}
		}
			
		return false;
	}

	public static String gLocation = null;
	public static double gLatitude;
	public static double gLongitude;
	
	@Override
	public void onLocationChanged(Location location)
	{
		try
		{
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if(!addresses.isEmpty())
			{
				gLocation = "";
				if(addresses.get(0).getAddressLine(1) != null)
					gLocation += addresses.get(0).getAddressLine(1) + ", ";
				gLocation += addresses.get(0).getAddressLine(0);
			}

			gLatitude = location.getLatitude();
			gLongitude = location.getLongitude();

//			GeoPoint point = new GeoPoint(latitude, longitude);
//			mapController.animateTo(point);
        
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
			gLocation = "Unknown";
		}
		catch(IOException e)
		{
			MobileWebCam.LogE("Could not get Geocoder data!");
			Log.e("MobileWebCam", "Could not get Geocoder data!", e);
			gLocation = "Unknown";
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}