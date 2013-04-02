/**
 * Copyright CMW Mobile.com, 2011.
 */

package com.dngames.mobilewebcam;

import android.app.AlertDialog.Builder;

import android.content.Context;
import android.content.res.TypedArray;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Shader;

import android.preference.DialogPreference;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * The ColorDialogPreference class is responsible for displaying a color
 * palet where the user can select a color from. 
 * @author Casper Wakkers
 */
public class ColorDialogPreference extends DialogPreference {
	/**
	 * The validation expression for this preference
	 */
	private static final String VALIDATION_EXPRESSION = "#.*";
 
	/**
	 * The default value for this preference
	 */
	private String defaultValue = "#FF000000";

	private int color = 0;
	// Color change listener.
	private OnColorChangedListener dialogColorChangedListener = null;
	private OnSeekBarChangeListener dialogSeekBarChangeListener = null;
	
	private Context mContext;
	private EditText mColorCode;
	private SeekBar mAlphaSlider;

	/**
	 * Interface describing a color change listener.
	 */
	public interface OnColorChangedListener {
		/**
		 * Method colorChanged is called when a new color is selected.
		 * @param color new color.
		 */
		void colorChanged(int color);
	}

	/**
	 * Inner class representing the color chooser.
	 */
	public class ColorPickerView extends View
	{
		private int CENTER_X = 100;
		private int CENTER_Y = 100;
		private int CENTER_RADIUS = 32;

		private Paint paint = null;
		private Paint centerPaint = null;
		private Paint centerBGPaint = null;
//		private boolean trackingCenter = false;
		private final int[] colors = new int[] {
//			0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000,
//			0xFF00FFFF, 0xFF00FF00,	0xFFFFFF00, 0xFFFF0000 };

			0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
			0xFFFFFF00, 0xFFFF0000 };

		/**
		 * @param context
		 * @param listener
		 * @param color
		 */
		ColorPickerView(Context context, OnColorChangedListener listener, int color)
		{
			super(context);

			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setShader(new SweepGradient(0, 0, colors, null));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(32);

			centerBGPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			centerBGPaint.setColor(Color.WHITE);
			centerBGPaint.setStrokeWidth(5);
			BitmapShader shader = new BitmapShader(BitmapFactory.decodeResource(context.getResources(), R.drawable.checkered), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			centerBGPaint.setShader(shader);

			centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			centerPaint.setColor(color);
			centerPaint.setStrokeWidth(5);

			DisplayMetrics metrics = new DisplayMetrics();
			WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();			
			display.getMetrics(metrics);

			if(metrics.widthPixels * 2 / 3 < CENTER_X * 2 || metrics.heightPixels * 2 / 3 < CENTER_Y * 2)
			{
				CENTER_X = CENTER_X * 2 / 3;
				CENTER_Y = CENTER_Y * 2 / 3;
				CENTER_RADIUS = CENTER_RADIUS * 2 / 3;
			}
		}
		
		public void setColor(int color)
		{
			centerPaint.setColor(color);
			mAlphaSlider.setProgress(Color.alpha(ColorDialogPreference.this.color));			
			invalidate();
		}
		
		protected void onDraw(Canvas canvas)
		{
			int centerX = getRootView().getWidth() / 2 - (int)(paint.getStrokeWidth() / 2);
			float r = CENTER_X - paint.getStrokeWidth() * 0.5f;

			canvas.translate(centerX, CENTER_Y);
			canvas.drawOval(new RectF(-r, -r, r, r), paint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, centerBGPaint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, centerPaint);

/*			if (trackingCenter)
			{
				int c = centerPaint.getColor();
				centerPaint.setStyle(Paint.Style.STROKE);
				centerPaint.setAlpha(0x80);

				canvas.drawCircle(0, 0, CENTER_RADIUS + centerPaint.getStrokeWidth(), centerPaint);

				centerPaint.setStyle(Paint.Style.FILL);
				centerPaint.setColor(c);
			}*/
		}
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int width = getRootView().getWidth();

			if (width == 0)
			{
				width = CENTER_X * 2 + CENTER_X / 2;
			}

			setMeasuredDimension(width, CENTER_Y * 2);
		}
		/**
		 * @param s
		 * @param d
		 * @param p
		 * @return
		 */
		private int ave(int s, int d, float p)
		{
			return s + Math.round(p * (d - s));
		}
		/**
		 * @param colors
		 * @param unit
		 * @return
		 */
		private int interpColor(int colors[], float unit)
		{
			if (unit <= 0)
			{
				return colors[0];
			}

			if (unit >= 1)
			{
				return colors[colors.length - 1];
			}

			float p = unit * (colors.length - 1);
			int i = (int) p;
			p -= i;

			// now p is just the fractional part [0...1) and i is the index
			int c0 = colors[i];
			int c1 = colors[i + 1];
			int a = ave(Color.alpha(c0), Color.alpha(c1), p);
			int r = ave(Color.red(c0), Color.red(c1), p);
			int g = ave(Color.green(c0), Color.green(c1), p);
			int b = ave(Color.blue(c0), Color.blue(c1), p);

			return Color.argb(a, r, g, b);
		}
		public boolean onTouchEvent(MotionEvent event)
		{
			float x = event.getX() - getRootView().getWidth()/2;
			float y = event.getY() - CENTER_Y;

			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				float angle = (float) java.lang.Math.atan2(y, x);
				// need to turn angle [-PI ... PI] into unit [0....1]
				float unit = (float)(angle / (2 * Math.PI));

				if (unit < 0)
				{
					unit += 1;
				}

				if(x * x + y * y < CENTER_RADIUS * CENTER_RADIUS)
				{
					if(event.getAction() == MotionEvent.ACTION_DOWN)
					{
						if(centerPaint.getColor() != Color.BLACK)
							centerPaint.setColor(Color.BLACK);
						else
							centerPaint.setColor(Color.WHITE);
					}
				}
				else if(x * x + y * y < CENTER_X * CENTER_X + centerPaint.getStrokeWidth() * centerPaint.getStrokeWidth())
				{
					centerPaint.setColor(interpColor(colors, unit));
				}
				else
				{
					return false;
				}
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				ColorDialogPreference.this.color = centerPaint.getColor();
				mColorCode.setText(toHex(ColorDialogPreference.this.color));
				mAlphaSlider.setProgress(Color.alpha(ColorDialogPreference.this.color));
				break;
			}

			return true;
		}
	}

	/**
	 * ColorDialogPreference constructor.
	 * @param context of this class.
	 * @param attrs custom xml attributes.
	 */
	public ColorDialogPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize(context);
	}
	
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public ColorDialogPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initialize(context);
	}	
	
	/**
	 * Initialize this preference
	 */
	private void initialize(Context context)
	{
		setPersistent(true);

		mContext = context;
	}
	
	// convert integer argb color to hex string
	private String toHex(int color)
	{
		String colstr = Integer.toHexString(color);
		for(int i = colstr.length(); i < 8; i++)
			colstr = "0" + colstr;
		return colstr.toUpperCase();
	}
	
	protected void onDialogClosed(boolean positiveResult)
	{
		// Persist the color after the ok button is clicked.
		if (positiveResult)
		{
			String colstr = toHex(color);
			persistString("#" + colstr);
			callChangeListener("#" + colstr);
		}

		super.onDialogClosed(positiveResult);
	}
	
	@Override
	public void setDefaultValue(Object defaultValue)
	{
		// BUG this method is never called if you use the 'android:defaultValue' attribute in your XML preference file, not sure why it isn't		
 
		super.setDefaultValue(defaultValue);
 
		if (!(defaultValue instanceof String)) {
			return;
		}

		try
		{
			this.color = Color.parseColor((String)defaultValue);
		}
		catch(IllegalArgumentException e)
		{
			return;
		}
		mColorCode.setText(toHex(color));
		mAlphaSlider.setProgress(Color.alpha(color));
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		final String value = a.getString(index);
 
		if (value == null || !value.matches(VALIDATION_EXPRESSION))
		{
			return null;
		}
 
		this.defaultValue = value;
		if(mColorCode != null)
			mColorCode.setText(value);

		if(mAlphaSlider != null)
		{
			try
			{
				int c = Color.parseColor((String)defaultValue);
				mAlphaSlider.setProgress(Color.alpha(c));
			}
			catch(IllegalArgumentException e) {	}
		}
		return value;
	}	
	
	protected void onPrepareDialogBuilder(Builder builder)
	{
		String colstr = getPersistedString(this.defaultValue);
		color = Color.BLACK;
		try
		{
			color = Color.parseColor(colstr);
		}
		catch(IllegalArgumentException e)
		{
			MobileWebCam.LogE("Wrong color: '" + colstr + "'");
		}
		
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View dialoglayout = inflater.inflate(R.layout.colordialog, null);
		LinearLayout layout = (LinearLayout)dialoglayout.findViewById(R.id.colordialoglinear);
		final ColorPickerView colorpicker = new ColorPickerView(getContext(), dialogColorChangedListener, color);

		dialogColorChangedListener = new OnColorChangedListener()
		{
			public void colorChanged(int c)
			{
				ColorDialogPreference.this.color = c;
				mColorCode.setText(toHex(ColorDialogPreference.this.color));
				mAlphaSlider.setProgress(Color.alpha(ColorDialogPreference.this.color));
			}
		};

		dialogSeekBarChangeListener = new OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if(fromUser)
				{
					int c = ColorDialogPreference.this.color;
					ColorDialogPreference.this.color = Color.argb(progress, Color.red(c), Color.green(c), Color.blue(c));
					mColorCode.setText(toHex(ColorDialogPreference.this.color));
					colorpicker.setColor(ColorDialogPreference.this.color);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
		};
		
		mAlphaSlider = (SeekBar)dialoglayout.findViewById(R.id.alphaslider);
		mAlphaSlider.setOnSeekBarChangeListener(dialogSeekBarChangeListener);
		mAlphaSlider.setProgress(Color.alpha(ColorDialogPreference.this.color));
		mColorCode = (EditText)dialoglayout.findViewById(R.id.colorpickercolorcode);
		mColorCode.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				String code = mColorCode.getText().toString();
				if(code.length() == 8 && !code.matches("[^0-9a-fA-F]"))
				{
					try
					{
						int c = (int) (Long.parseLong(code, 16) & 0xFFFFFFFF);
						ColorDialogPreference.this.color = c;
						mAlphaSlider.setProgress(Color.alpha(c));
						colorpicker.setColor(c);
					}
					catch(NumberFormatException e)
					{
						MobileWebCam.LogE("Wrong color: '" + code + "'");
					}
				}
				return false;
			} });
/*		mColorCode.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if(!hasFocus)
				{
					try
					{
						ColorDialogPreference.this.color = Color.parseColor(mColorCode.getText().toString());
					}
					catch(IllegalArgumentException e)
					{
						MobileWebCam.LogE("Wrong color: '" + mColorCode.getText() + "'");
						e.printStackTrace();
					}
				}
			} });*/
		mColorCode.setText(toHex(color));
		layout.addView(colorpicker, 1);
		builder.setView(dialoglayout);
//		builder.setView(new ColorPickerView(getContext(), dialogColorChangedListener, color));
//		builder.setMessage("Select color in circle. Tap center for black and white!");

		super.onPrepareDialogBuilder(builder);
	}
}
