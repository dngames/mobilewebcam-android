package com.dngames.mobilewebcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class DrawOnTop extends View
{
    public DrawOnTop(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
		if(Preview.gPreview != null)
		{
			if(Preview.gPreview.mPreviewBitmap != null)
			{
				if(!Preview.gPreview.mPreviewBitmap.isRecycled())
				{
					Rect src = new Rect(0, 0, Preview.gPreview.mPreviewBitmap.getWidth(), Preview.gPreview.mPreviewBitmap.getHeight());
					Rect dst = new Rect(0, 0, this.getWidth(), this.getHeight());
					canvas.drawBitmap(Preview.gPreview.mPreviewBitmap, src, dst, null);
				}
				else
				{
					canvas.drawARGB(255, 255, 255, 0);
					Log.w("MobileWebCam", "setPreview: bitmap recycled");
				}
			}
			else
			{
				canvas.drawARGB(255, 255, 0, 255);
				Log.w("MobileWebCam", "setPreview: no bitmap");
			}
		}
		else
		{
			canvas.drawARGB(255, 128 + (int)(Math.random() * 128.0f), 0, 0);
			Log.w("MobileWebCam", "setPreview: no preview");
		}
        
        super.onDraw(canvas);
        
    }
}
