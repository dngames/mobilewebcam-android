package com.dngames.mobilewebcam;

import android.graphics.Bitmap;

public interface ITextUpdater
{
	public void UpdateText();
	
	public void Toast(String msg, int length);
	
	public void SetPreview(Bitmap image);
	
	public void JobFinished();
}