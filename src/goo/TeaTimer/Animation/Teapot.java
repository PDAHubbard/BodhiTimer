package goo.TeaTimer.Animation;

import goo.TeaTimer.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;

class Teapot implements TimerAnimation.TimerDrawing
{	
	private Bitmap mCupBitmap;

	private int mWidth = 0;
	private int mHeight = 0;
	
	private Paint mProgressPaint = null;

	private Bitmap mBitmap = null;

	public Teapot(Context context)
	{
		Resources resources = context.getResources();
		
		mProgressPaint = new Paint();
		mProgressPaint.setColor(Color.BLACK);
		mProgressPaint.setAlpha(255);
		mProgressPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		
		mCupBitmap = BitmapFactory.decodeResource(resources, R.drawable.leaf);	
		mHeight = mCupBitmap.getHeight();
		mWidth = mCupBitmap.getWidth();

		mBitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888);
	}

	/**
	 * Updates the image to be in sync with the current time
	 * @param time in milliseconds
	 * @param max the original time set in milliseconds
	 */
	public void updateImage(Canvas canvas, int time, int max) {
	
		canvas.save();
		int w = canvas.getClipBounds().width();
		int h = canvas.getClipBounds().height();
		
		Rect rs = new Rect(0, 0, mWidth, mHeight);
		Rect rd;
		
		if(w < h) {
			rd = new Rect(0,0,w,w);
			canvas.translate(0,(h-w)/2);
		}
		else {
			rd = new Rect(0,0,h,h);
			canvas.translate((w-h)/2,0);
		}
		
		
		canvas.drawBitmap(mCupBitmap, rs, rd, null);
		
		float p = (max != 0) ? (time/(float)max) : 0;
		
		//if(p == 0) p = 1;
		
		RectF fill = new RectF(0,0,canvas.getWidth(),canvas.getHeight());
		mProgressPaint.setAlpha((int)(255-(255*p)));
		canvas.drawRect(fill,mProgressPaint);	
		
		canvas.restore();
	}

	public void configure() {
		// TODO Auto-generated method stub
		
	}
}