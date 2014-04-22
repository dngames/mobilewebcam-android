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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;

// httppost
public class UploadPhoto extends Upload
{
	public UploadPhoto(Context c, ITextUpdater tu, PhotoSettings s, byte[] jpeg, Date date, String event)
	{
		super(c, tu, s, jpeg, date, event);
	}
	
	@Override
	public void run()
	{
		doInBackgroundBegin();

		if(mSettings.mRefreshDuration >= 10)
			publishProgress(mContext.getString(R.string.uploading, mSettings.mURL));
		
		try
		{
			HttpClient client = new DefaultHttpClient();  
			if(mSettings.mLogin.length() > 0)
			{
				try
				{
//					URL url = new URL(mSettings.mURL);
					((AbstractHttpClient) client).getCredentialsProvider().setCredentials(
    						new AuthScope(null, -1),
    						new UsernamePasswordCredentials(mSettings.mLogin, mSettings.mPassword));
				}
				catch(Exception e)
				{
					e.printStackTrace();
					if(e.getMessage() != null)
						MobileWebCam.LogE("http login " + e.getMessage());
					else
						MobileWebCam.LogE("http: unable to log in");
				}
			}
			HttpPost post = new HttpPost(mSettings.mURL);
			ByteArrayBody bin;
			if(mSettings.mTimeStampedFilePostName)
			{
				long curtime = mDate.getTime();
				bin = new ByteArrayBody(mJpeg, "image/jpeg", curtime + ".jpg");
			}
			else
			{
				bin = new ByteArrayBody(mJpeg, "image/jpeg", mSettings.mDefaultname);
			}
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
			reqEntity.addPart("time", new StringBody(mDate.toString()));
			reqEntity.addPart("battery", new StringBody(WorkImage.getBatteryInfo(mContext, "%d")));
			if(mPhotoEvent != null)
				reqEntity.addPart("event", new StringBody(mPhotoEvent));
			if(mSettings.mLogUpload)
				reqEntity.addPart("log", new StringBody(MobileWebCam.GetLog(mContext, mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0), mSettings)));				
			reqEntity.addPart("userfile", bin);
			if(mSettings.mStoreGPS)
			{
				reqEntity.addPart("latitude", new StringBody(String.format("%f", WorkImage.gLatitude)));
				reqEntity.addPart("longitude", new StringBody(String.format("%f", WorkImage.gLongitude)));
				reqEntity.addPart("altitude", new StringBody(String.format("%f", WorkImage.gAltitude)));
			}
			post.setEntity(reqEntity);
			HttpResponse response = client.execute(post);  
			HttpEntity resEntity = response.getEntity();  
			if (resEntity != null)
			{
				String message = EntityUtils.toString(resEntity);
				if(mSettings.mRefreshDuration >= 10)
				{
					MobileWebCam.LogI("http RESPONSE: " + message.trim().replaceAll("\r\n", ""));
					publishProgress(mContext.getString(R.string.result, message));
				}
			}
		} catch (Exception e)
		{
			String ename = e.getClass().toString().replace("class java.io.", "");
			if(e.getMessage() != null)
				publishProgress(ename + ":\nPicture not posted to http.\n" + e.getMessage());
			else
				publishProgress(ename + ":\nPicture not posted to http.");
	    	if(e.getMessage() != null)
		    	MobileWebCam.LogE("Could not post picture to http: " + e.getMessage());
	    	else
		    	MobileWebCam.LogE("Could not post picture to http:");
	    	Log.e("MobileWebCam", e.toString());
	    	e.printStackTrace();
		}
		
		doInBackgroundEnd(true);
	}
	
	public static class ByteArrayBody extends AbstractContentBody
	{
		private final byte[] bytes;
		private final String fileName;

		public ByteArrayBody(byte[] bytes, String mimeType, String fileName) {
				super(mimeType);
				this.bytes = bytes;
				this.fileName = fileName;
		}

		@Override
		public String getFilename() {
				return fileName;
		}

		@Override
		public void writeTo(OutputStream out) throws IOException {
				out.write(bytes);
		}

		@Override
		public String getCharset() {
				return null;
		}

		@Override
		public long getContentLength() {
				return bytes.length;
		}

		@Override
		public String getTransferEncoding() {
				return MIME.ENC_BINARY;
		}
	}
}