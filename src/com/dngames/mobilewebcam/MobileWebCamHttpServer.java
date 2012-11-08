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

import java.io.*;
import java.util.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Debug;
import android.util.Log;
import android.widget.TextView;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import java.io.ByteArrayInputStream;

import com.dngames.mobilewebcam.PhotoSettings.Mode;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class MobileWebCamHttpServer extends NanoHTTPD
{
	Context mContext = null;
    public PhotoSettings mSettings = null;
	
	public MobileWebCamHttpServer(int port, Context context) throws IOException
	{
		super(port, new File(".").getAbsoluteFile());
		
		mContext = context;
		mSettings = new PhotoSettings(mContext);
	}

	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		StringBuilder sb = new StringBuilder();
		
		if(uri.contentEquals("/favicon.png"))
		{
			try {
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_OK, "image/png", mContext.getResources().getAssets().open("favicon.png"));
			} catch (IOException e) {
				e.printStackTrace();
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_HTML, e.getMessage());
			}
		}
		else if(uri.contentEquals("/current.jpg"))
		{
			if(MobileWebCamHttpService.gImageData != null)
				return new NanoHTTPD.Response(HTTP_OK, "image/jpeg", new ByteArrayInputStream(MobileWebCamHttpService.gImageData));
			else
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_HTML, "Error 404, file not found.");
		}
		else if(uri.contentEquals("/mjpeg"))
		{
			if(MobileWebCamHttpService.gImageData != null)
			{
				Response res = new NanoHTTPD.Response(HTTP_OK, "multipart/x-mixed-replace; boundary=" + MJpegInputStream.mBoundary, new MJpegInputStream());
				
				res.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0");
				res.addHeader("Cache-Control", "private");
				res.addHeader("Pragma", "no-cache");
				res.addHeader("Expires", "-1");
						
				return res;
			}
			else
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_HTML, "Error 404, file not found.");
		}
		
		if(uri.contentEquals("/start"))
		{
			Intent i = new Intent();
			i.setAction("com.dngames.mobilewebcam.START");
			mContext.sendBroadcast(i);

			Response res = new NanoHTTPD.Response(NanoHTTPD.HTTP_OTHER, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href='/'>" + "/" + "</a></body></html>");
			res.addHeader( "Location", "/");
			return res;
		}
		else if(uri.contentEquals("/stop"))
		{
			Intent i = new Intent();
			i.setAction("com.dngames.mobilewebcam.STOP");
			mContext.sendBroadcast(i);

			Response res = new NanoHTTPD.Response(NanoHTTPD.HTTP_OTHER, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href='/'>" + "/" + "</a></body></html>");
			res.addHeader( "Location", "/");
			return res;
		}
		else if(uri.contentEquals("/set"))
		{
			SharedPreferences prefs = mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
			SharedPreferences.Editor edit = prefs.edit();
			String refresh = parms.getProperty("refresh", mSettings.mRefreshDuration / 1000 + "");
			edit.putString("cam_refresh", refresh);
			edit.putBoolean("server_upload", parms.getProperty("http") != null);
			edit.putBoolean("ftpserver_upload", parms.getProperty("ftp") != null);
			edit.putBoolean("cam_storepictures", parms.getProperty("sdcard") != null);
			edit.putBoolean("cam_mailphoto", parms.getProperty("mail") != null);
			edit.putBoolean("dropbox_upload", parms.getProperty("dropbox") != null);
	    	String start = parms.getProperty("starttime", mSettings.mStartTime);
			edit.putString("activity_starttime", start);
			String end = parms.getProperty("endtime", mSettings.mEndTime);
			edit.putString("activity_endtime", end);
			edit.commit();

			Response res = new NanoHTTPD.Response(NanoHTTPD.HTTP_OTHER, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href='/'>" + "/" + "</a></body></html>");
			res.addHeader( "Location", "/");
			return res;
		}
		
		boolean active = mSettings.mMobileWebCamEnabled && (MobileWebCam.gIsRunning || MobileWebCam.gInSettings || mSettings.mMode == Mode.BROADCASTRECEIVER || mSettings.mMode == Mode.BACKGROUND || mSettings.mMode == Mode.HIDDEN);		
		String info_app = getVersionNumber(mContext);
		String info_device = android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE + " " + android.os.Build.DISPLAY;		
		
		String msg = "<html><head><link rel='icon' href='favicon.png' type='image/png'>";
		if(active)
			msg += "<meta http-equiv='refresh' content='" + mSettings.mRefreshDuration / 1000 + "'>";
		msg += "<title>MobileWebCam " + info_app + " " + info_device + "</title>";
		msg += "</head><body bgcolor='#000000'><font color='#ffffff' face='arial'>";
				
		msg += "<h1>MobileWebCam " + info_app + " " + info_device + "</h1>";
		
		msg += "<hr>";

		msg += "<table style='background-color: #000000; color: #FFFFFF; font-family: arial;' border='0' cellpadding='20'><tr><td width='50%'>";
		
		if(mSettings.mMode == Mode.MANUAL)
			msg += "Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "   Manual Mode active" + "<br>";
		else
			msg += "Pictures: " + MobileWebCam.gPictureCounter + "    Uploading: " + MobileWebCam.gUploadingCount + "<br>";
		msg += WorkImage.getBatteryInfo(mContext, mSettings.mImprintStatusInfo) + "<br>";
		msg += "Orientation: " + Preview.gOrientation + "<br>";;
		if(mSettings.mMode == Mode.MANUAL)
			msg += "Mode: " + mContext.getResources().getStringArray(R.array.entries_list_camera_mode)[0];
		else if(mSettings.mMode == Mode.NORMAL)
			msg += "Mode: " + mContext.getResources().getStringArray(R.array.entries_list_camera_mode)[1];
		else if(mSettings.mMode == Mode.BACKGROUND)
			msg += "Mode: " + mContext.getResources().getStringArray(R.array.entries_list_camera_mode)[2];
		else if(mSettings.mMode == Mode.HIDDEN)
			msg += "Mode: " + mContext.getResources().getStringArray(R.array.entries_list_camera_mode)[3];
		else if(mSettings.mMode == Mode.BROADCASTRECEIVER)
			msg += "Mode: " + mContext.getResources().getStringArray(R.array.entries_list_camera_mode)[4];
		float usedMegs = (float)Debug.getNativeHeapAllocatedSize() / (float)1048576L;
		msg += String.format("<br>Memory used: %.2f MB", usedMegs);
		msg += String.format("<br>Used upload image size: %d x %d (from %d x %d)", MobileWebCamHttpService.gImageWidth, MobileWebCamHttpService.gImageHeight, MobileWebCamHttpService.gOriginalImageWidth, MobileWebCamHttpService.gOriginalImageHeight);
		
		msg += "<hr>";
		
		if(!active)
		{
			msg += "<p><form  action='start' enctype='multipart/form-data' method='post'>";
		    msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' value='Start Camera' type='submit'>";
		    msg += "</form></p>";
		}
		else
		{
		    msg += "<p><form action='stop' enctype='multipart/form-data' method='post'>";
		    msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' value='Stop Camera' type='submit'>";
		    msg += "</form></p>";
		}
		
		msg += "<p><form action='set' enctype='multipart/form-data' method='post'>";
		msg += "Upload:<br>";
		msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' type='Checkbox' name='http' " + (mSettings.mUploadPictures ? "checked='checked'" : "") + "> http website post<br>";
		msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' type='Checkbox' name='ftp' " + (mSettings.mFTPPictures ? "checked='checked'" : "") + "> upload to ftp<br>";
		msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' type='Checkbox' name='mail' " + (mSettings.mMailPictures ? "checked='checked'" : "") + "> email picture<br>";
		msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' type='Checkbox' name='sdcard' " + (mSettings.mStorePictures ? "checked='checked'" : "") + "> store on SDCard<br>";
		msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' type='Checkbox' name='dropbox' " + (mSettings.mDropboxPictures ? "checked='checked'" : "") + "> upload to dropbox</p>";

	    msg += "<p>Refresh Duration: <input style='color: #FFFFFF; font-family: arial;background-color: #000000' name='refresh' value='" + mSettings.mRefreshDuration / 1000 + "' type='text'></p>";

	    msg += "<p>Activity Start Time: <input style='color: #FFFFFF; font-family: arial;background-color: #000000' name='starttime' value='" + mSettings.mStartTime + "' type='time'><br>";
	    msg += "Activity End Time: <input style='color: #FFFFFF; font-family: arial;background-color: #000000' name='endtime' value='" + mSettings.mEndTime + "' type='time'></p>";

	    msg += "<input style='color: #FFFFFF; font-family: arial;background-color: #000000' value='Set' type='submit'>";
	    msg += "</form><p>";
	    
	    msg += "<hr>";
		
		msg += "<p>MJPEG motion picture URL is:<br><a href=\'/mjpeg\'>http://" + SystemSettings.getLocalIpAddress(mContext) + "/mjpeg</a></p>";
		msg += "<p>To use your phone with Skype or other programs on your PC you need to install a mjpeg webcam driver like this one: <a href='http://www.webcamxp.com/download.aspx'>http://www.webcamxp.com/download.aspx</a> - then you enter the URL shown above there.</p>";		
		
		msg += "</td><td>";
		
		// hack
		if(active && MobileWebCamHttpService.gImageData != null)
		{
			msg += "<img src='current.jpg' name='refresh'>\n"; 
			msg += "  <script language='JavaScript' type='text/javascript'>\n"; 
			msg += "  <!-- \n"; 
			msg += "  image = 'current.jpg' //name of the image\n";  
			msg += "  function Reload() { \n"; 
			msg += "  tmp = new Date();\n";  
			msg += "  tmp = '?'+tmp.getTime()\n";  
			msg += "  document.images['refresh'].src = image+tmp\n";  
			msg += "  setTimeout('Reload()'," + Math.min(5000, mSettings.mRefreshDuration) + ")\n"; 
			msg += "  }\n"; 
			msg += "  Reload();\n";  
			msg += "// -->\n"; 
			msg += "</script>\n";  
		}
		else if(MobileWebCamHttpService.gImageData != null)
		{
			msg += "<img src='current.jpg' name='refresh' alt='last taken picture'>";
		}
		else
		{
			msg += "No camera with preview active or no picture taken yet!";
		}

		msg += "</td></tr></table>";
		
		msg += "<hr>";
		
		msg += "<table style='background-color: #000000; color: #FFFFFF; font-family: arial; font-size: 12px;' border='0' cellpadding='20'><tr><td width='50%'>";				
		
		int cnt = 0;
		int i = MobileWebCam.gCurLogInfos;
		while(cnt < MobileWebCam.gLogInfos.length)
		{
			if(MobileWebCam.gLogInfos[i] != null)
				msg += "<font color=#00FF00>Info:</font> " + MobileWebCam.gLogInfos[i] + "<br>";
			i++;
			if(i >= MobileWebCam.gLogInfos.length)
				i = 0;
			cnt++;
		}

		msg += "</td><td>";
		
		cnt = 0;
		i = MobileWebCam.gCurLogMessage;
		while(cnt < MobileWebCam.gLogMessages.length)
		{
			if(MobileWebCam.gLogMessages[i] != null)
				msg += "<font color=#FF0000>Error:</font> " + MobileWebCam.gLogMessages[i] + "<br>";
			i++;
			if(i >= MobileWebCam.gLogMessages.length)
				i = 0;
			cnt++;
		}

	    msg += "</td></tr></table></font></body></html>\n";
				
		return new NanoHTTPD.Response( HTTP_OK, MIME_HTML + "; charset=utf-8", msg );
	}

	public static String getVersionNumber(Context context) 
    {
        String version = "?";
        try 
        {
            PackageInfo packagInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packagInfo.versionName;
        } 
        catch (PackageManager.NameNotFoundException e)
        {
        }
        
        return version;
    }

	enum State { BOUND, TYPE, LENGTH, JPEG };
	
	public class MJpegInputStream extends InputStream
	{
		public static final String mBoundary = "my_jpeg";
		
		private static final String mContentType = "Content-type: image/jpeg\n";
		private static final String mContentLength = "Content-Length: %d\n\n";		
		public static final String mNext = "\n--" + mBoundary + "\n";

		private State mState;
		int len = 0;
		private String mLength;
		int mPos;
		
		private int mLastImageIdx = -1;
		
		public MJpegInputStream()
		{
			super();
			
			mState = State.TYPE;
			mPos = 0;

			Log.i("MobileWebCam", "HTTP - MJPEG: new input stream");
		}

		@Override
		public int available() throws IOException
		{
			boolean active = mSettings.mMobileWebCamEnabled && (MobileWebCam.gIsRunning || mSettings.mMode == Mode.BROADCASTRECEIVER || mSettings.mMode == Mode.BACKGROUND || mSettings.mMode == Mode.HIDDEN);
			if(!active)
				return -1;
			
			switch(mState)
			{
			case TYPE:
				if(mLastImageIdx == MobileWebCamHttpService.gImageIndex)
					return 0;
				
				return mContentType.length() - mPos;
			case LENGTH:
				return mLength.length() - mPos;
			case JPEG:
				return MobileWebCamHttpService.gImageData.length - mPos;
			case BOUND:
				return mNext.length() - mPos;
			}

			return 0;
		}

		@Override
		public int read() throws IOException
		{
			int res = 0;
			
			switch(mState)
			{
			case BOUND:
				res = mNext.charAt(mPos++);
				if(mPos >= mNext.length())
				{
					mState = State.TYPE;
					mPos = 0;
				}
				break;
			case TYPE:
				res = mContentType.charAt(mPos++);
				if(mPos >= mContentType.length())
				{
					Log.i("MobileWebCam", "HTTP - MJPEG: next image (" + MobileWebCamHttpService.gImageIndex + ")");
					
// TODO: lock image buffer from now on					
					synchronized(MobileWebCamHttpService.gImageDataLock)
					{
						len = MobileWebCamHttpService.gImageData.length;
						mLength = String.format(mContentLength, len);
					}
					
					mLastImageIdx = MobileWebCamHttpService.gImageIndex;

					mState = State.LENGTH;
					mPos = 0;
				}
				break;
			case LENGTH:
				res = mLength.charAt(mPos++);
				if(mPos >= mLength.length())
				{
					mState = State.JPEG;
					mPos = 0;
				}
				break;
			case JPEG:
				synchronized(MobileWebCamHttpService.gImageDataLock)
				{
					Log.i("MobileWebCam", "HTTP - MJPEG: gImageData " + mPos + " of " + MobileWebCamHttpService.gImageData.length);
					res = MobileWebCamHttpService.gImageData[mPos++];
					if(mPos >= MobileWebCamHttpService.gImageData.length)
					{
// TODO: unlock image buffer						
						mState = State.BOUND;
						mPos = 0;
					}
				}
				break;
			}
			return res;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			int copy = 0;
			
			switch(mState)
			{
			case BOUND:
				Log.i("MobileWebCam", "HTTP - MJPEG: next image");

				copy = Math.min(length, mNext.length() - mPos);
				System.arraycopy(mNext.getBytes(), mPos, buffer, 0, copy);
				mPos += copy;
				if(mPos >= mNext.length())
				{
					mPos = 0;
					mState = State.TYPE;
				}
				break;
			case TYPE:
				copy = Math.min(length, mContentType.length() - mPos);
				System.arraycopy(mContentType.getBytes(), mPos, buffer, 0, copy);
				mPos += copy;
				if(mPos >= mContentType.length())
				{
					// TODO: lock image buffer from now on					
// TODO: lock image buffer from now on					
					synchronized(MobileWebCamHttpService.gImageDataLock)
					{
						len = MobileWebCamHttpService.gImageData.length;
						mLength = String.format(mContentLength, len);
					}
					
					mLastImageIdx = MobileWebCamHttpService.gImageIndex;

					mState = State.LENGTH;
					mPos = 0;
				}
				break;
			case LENGTH:
				copy = Math.min(length, mLength.length() - mPos);
				System.arraycopy(mLength.getBytes(), mPos, buffer, 0, copy);
				mPos += copy;
				if(mPos >= mLength.length())
				{
					mState = State.JPEG;
					mPos = 0;
				}
				break;
			case JPEG:
				synchronized(MobileWebCamHttpService.gImageDataLock)
				{
					copy = Math.min(length, MobileWebCamHttpService.gImageData.length - mPos);
					
					Log.i("MobileWebCam", "HTTP - MJPEG: gImageData " + mPos + " of " + MobileWebCamHttpService.gImageData.length);
					
					if(copy <= 0)
					{
						mState = State.BOUND;
						mPos = 0;
						copy = -1;
					}
					else
					{
						System.arraycopy(MobileWebCamHttpService.gImageData, mPos, buffer, 0, copy);
						mPos += copy;
						if(mPos >= MobileWebCamHttpService.gImageData.length)
						{
// TODO: unlock image buffer						
							mState = State.BOUND;
							mPos = 0;
						}
					}
				}
				break;
			}
			return copy;
		}
	}
}