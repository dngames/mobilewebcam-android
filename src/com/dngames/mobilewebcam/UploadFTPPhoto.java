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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.net.ftp.FTPClient;

import android.content.Context;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class UploadFTPPhoto extends Upload
{
	public static class FTPConnection
	{
		public static FTPClient client		= null;
		public static Session 	session 	= null;
		public static Channel 	channel 	= null;
		public static ChannelSftp channelSftp = null;
	};
			
	protected UploadFTPPhoto(Context c, ITextUpdater tu, PhotoSettings s, byte[] jpeg, Date date, String event)
	{
		super(c, tu, s, jpeg, date, event);
	}

	@Override
	public void run()
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
		        buffIn = new BufferedInputStream(new ByteArrayInputStream(mJpeg));
		 
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
					ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude, WorkImage.gAltitude);
        			
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
					if(mSettings.mSFTP && FTPConnection.session == null)
					{
						JSch jsch = new JSch(); 
						FTPConnection.session = jsch.getSession(mSettings.mFTPLogin, mSettings.mFTP, mSettings.mFTPPort);
						FTPConnection.session.setPassword(mSettings.mFTPPassword);
						java.util.Properties config = new java.util.Properties();
						config.put("StrictHostKeyChecking", "no");
						FTPConnection.session.setConfig(config);
						FTPConnection.session.connect();
						FTPConnection.channel = FTPConnection.session.openChannel("sftp");
						FTPConnection.channel.connect();
						FTPConnection.channelSftp = (ChannelSftp)FTPConnection.channel;
						FTPConnection.channelSftp.cd(mSettings.mFTPDir);
					}	
					else if(!mSettings.mSFTP && FTPConnection.client == null)
					{
						FTPConnection.client = new FTPClient();  
						FTPConnection.client.connect(InetAddress.getByName(mSettings.mFTP), mSettings.mFTPPort);
						
						FTPConnection.client.login(mSettings.mFTPLogin, mSettings.mFTPPassword);
						if(!FTPConnection.client.getReplyString().contains("230"))
					    {
					    	publishProgress("wrong ftp login response: " + FTPConnection.client.getReplyString() + "\nAre your credentials correct?");
					    	MobileWebCam.LogE("wrong ftp login response: " + FTPConnection.client.getReplyString() + "\nAre your credentials correct?");
					    	FTPConnection.session = null;
					    	FTPConnection.client = null;
					    	break ftpupload;
					    }

						FTPConnection.client.changeWorkingDirectory(mSettings.mFTPDir);
						if(!FTPConnection.client.getReplyString().contains("250"))
					    {
					    	publishProgress("wrong ftp cwd response: " + FTPConnection.client.getReplyString() + "\nIs the directory correct?");
					    	MobileWebCam.LogE("wrong ftp cwd response: " + FTPConnection.client.getReplyString() + "\nIs the directory correct?");
					    	FTPConnection.session = null;
					    	FTPConnection.client = null;
					    	break ftpupload;
					    }

						FTPConnection.client.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

				        if(mSettings.mFTPPassive)
				        	FTPConnection.client.enterLocalPassiveMode();
				        else
				        	FTPConnection.client.enterLocalActiveMode();
					}
					
			        if(mSettings.mFTPKeepPics > 0)
			        {
			        	String replacename = filename.replace(".jpg", "");  
			        	// rename old pictures first
			        	for(int i = mSettings.mFTPKeepPics - 1; i > 0; i--)
			        	{
			        		try
			        		{
								if(mSettings.mSFTP)
									FTPConnection.channelSftp.rename(replacename + i + ".jpg", replacename + (i + 1) + ".jpg");
								else
									FTPConnection.client.rename(replacename + i + ".jpg", replacename + (i + 1) + ".jpg");
			        		}
			        		catch(IOException e)
			        		{
			        		}
			        	}
		        		try
		        		{
							if(mSettings.mSFTP)
								FTPConnection.channelSftp.rename(mSettings.mDefaultname, replacename + "1.jpg");
							else
								FTPConnection.client.rename(mSettings.mDefaultname, replacename + "1.jpg");
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
						if(mSettings.mSFTP)
							FTPConnection.channelSftp.put(buffIn, filename);
						else
							result = FTPConnection.client.storeFile(filename, buffIn);
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
							if(mSettings.mSFTP)
								FTPConnection.channelSftp.put(logIS, "log.txt");
							else
								result &= FTPConnection.client.storeFile("log.txt", logIS);
						}
					}
					
			        if(result || mSettings.mSFTP)
			        {
				    	publishProgress("ok");
				    	MobileWebCam.LogI("ok");
			        }
			        else
			        {
				    	publishProgress("ftp error: " + FTPConnection.client.getReplyString());
				    	MobileWebCam.LogE("ftp error: " + FTPConnection.client.getReplyString());
			        }

					if(!mSettings.mFTPKeepConnected)
					{
						if(mSettings.mSFTP)
						{
							FTPConnection.channelSftp.disconnect();
							FTPConnection.session = null;
						}
						else
						{
							FTPConnection.client.logout();
							FTPConnection.client.disconnect();
							FTPConnection.client = null;
						}
					}
				}
			}
			catch (SocketException e)
			{
				e.printStackTrace();
				publishProgress("Ftp socket exception!");
				MobileWebCam.LogE("Ftp socket exception!");
				FTPConnection.session = null;
				FTPConnection.client = null;
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
				publishProgress("Unknown ftp host!");
				MobileWebCam.LogE("Unknown ftp host!");
				FTPConnection.session = null;
				FTPConnection.client = null;
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
				FTPConnection.session = null;
				FTPConnection.client = null;
			}
			catch (JSchException e)
			{
				if(e.getMessage() != null)
				{
					publishProgress("IOException: sftp\n" + e.getMessage());
					MobileWebCam.LogE("IOException: sftp\n" + e.getMessage());
				}
				else
				{
					publishProgress("sftp IOException");
					MobileWebCam.LogE("sftp IOException");
				}
				FTPConnection.session = null;
				FTPConnection.client = null;
			}
			catch (SftpException e)
			{
				if(e.getMessage() != null)
				{
					publishProgress("IOException: sftp\n" + e.getMessage());
					MobileWebCam.LogE("IOException: sftp\n" + e.getMessage());
				}
				else
				{
					publishProgress("sftp IOException");
					MobileWebCam.LogE("sftp IOException");
				}
				FTPConnection.session = null;
				FTPConnection.client = null;
			}
			catch (NullPointerException e)
			{
				MobileWebCam.LogE("NullPointerException:\n" + e.getMessage());
				FTPConnection.session = null;
				FTPConnection.client = null;
			}
		}
			
		doInBackgroundEnd(mSettings.mFTPBatch == 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) == 0));
	}
}