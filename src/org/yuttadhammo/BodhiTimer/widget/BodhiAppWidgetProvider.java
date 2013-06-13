package org.yuttadhammo.BodhiTimer.widget;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

import org.yuttadhammo.BodhiTimer.R;
import org.yuttadhammo.BodhiTimer.TimerActivity;
import org.yuttadhammo.BodhiTimer.TimerUtils;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import java.util.Calendar;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;

public class BodhiAppWidgetProvider extends AppWidgetProvider {

	private static SharedPreferences mSettings;

    private static int state;

	private static Bitmap bmp = null;

	private ComponentName appWidgets;

	private AppWidgetManager appWidgetManager;

	/** debug string */
	private final static String TAG = "BodhiAppWidgetProvider";
  
    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    	//Log.i(TAG,"update");
		doUpdate(context);
    }
   

	/**
	* Sending this broadcast intent will cause the clock widgets to update.
	*/
	public static String ACTION_CLOCK_START = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_START";
	public static String ACTION_CLOCK_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";
	public static String ACTION_CLOCK_CANCEL = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_CANCEL";

	private static RemoteViews views;

	private static long timeStamp;

	private static int mLastTime;

	private static AlarmManager alarmManager;

	private static int themeid;
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
    	Log.i(TAG,"enabled");
		doUpdate(context);

	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	
	
	}

    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            AppWidgetConfigure.deletePref(context, appWidgetIds[i]);
        }
    }
	
	@Override
	public void onReceive(Context context, Intent i) {
		super.onReceive(context, i);

		final String action = i.getAction();
	
		appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgets = new ComponentName(context.getPackageName(), getClass().getName());
		
		if (!ACTION_CLOCK_UPDATE.equals(action))
			doUpdate(context);

        //Log.d(TAG, "received broadcast");
		final int ids[] = appWidgetManager.getAppWidgetIds(appWidgets);
		if (ids.length > 0){
            for (int idx=0; idx<ids.length; idx++) {
                updateAppWidget(context, appWidgetManager, ids[idx]);
            }
		}
	}
	
	private void doUpdate(Context context) {
    	Log.i(TAG,"updating");

    	mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        
    	if(!mSettings.getBoolean("custom_bmp", false) || mSettings.getString("bmp_url","").length() == 0) {
			Resources resources = context.getResources();
			bmp = BitmapFactory.decodeResource(resources, R.drawable.leaf);
		}
		else {
			String bmpUrl = mSettings.getString("bmp_url", "");
			Uri selectedImage = Uri.parse(bmpUrl);
            InputStream imageStream = null;
			try {
				imageStream = context.getContentResolver().openInputStream(selectedImage);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
            bmp = BitmapFactory.decodeStream(imageStream);
		}
    
   		timeStamp = mSettings.getLong("TimeStamp", -1);
		mLastTime = mSettings.getInt("LastTime",0); 
		state = mSettings.getInt("State",TimerActivity.STOPPED); 
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgets = new ComponentName(context.getPackageName(), getClass().getName());

    	Intent intent = new Intent(context, TimerActivity.class);
        intent.putExtra("set", "true");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		final int ids[] = appWidgetManager.getAppWidgetIds(appWidgets);
		if (ids.length > 0){
            for (int idx=0; idx<ids.length; idx++) {

        		views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
        		
        		// Get the layout for the App Widget and attach an on-click listener
                // to the button
            	views.setOnClickPendingIntent(R.id.mainImage, pendingIntent);
            	
            	
                // set background
                themeid = mSettings.getInt("widget_theme_"+ids[idx], R.drawable.widget_background_black_square);
            	views.setImageViewResource(R.id.backImage, themeid);
                appWidgetManager.updateAppWidget(ids[idx], views);
            }
		}
	}
	
	
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        //Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId);

		views = new RemoteViews(context.getPackageName(), R.layout.appwidget);

		Date now = new Date();
		Date then = new Date(timeStamp);

		int delta = (int)(then.getTime() - now.getTime());		
        
        //Log.d(TAG, "Delta: "+delta);
    	
		float p = (mLastTime != 0) ? (delta/(float)mLastTime) : 0;
		
		// We still have a timer running!
		if(then.after(now) && state == TimerActivity.RUNNING){
	        Log.d(TAG, "running");
    		
	   		views.setTextViewText(R.id.time, getTime(delta-1000));
    	}
		else if(state == TimerActivity.PAUSED){
	        Log.d(TAG, "paused");

			Integer time = mSettings.getInt("CurrentTime",0);
	        int rtime = Math.round(((float) time)/1000)*1000;  // round to seconds
            views.setTextViewText(R.id.time, TimerUtils.time2hms(rtime));
		}
		else {
	        Log.d(TAG, "stopped");
            views.setTextViewText(R.id.time, "");
    		
    	}
    	
    	views.setImageViewBitmap(R.id.mainImage, then.after(now) && state == TimerActivity.RUNNING?adjustOpacity(bmp,(int)(255-(255*p))):bmp);
		
        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    
	/**
	 * @param bitmap The source bitmap.
	 * @param opacity a value between 0 (completely transparent) and 255 (completely
	 * opaque).
	 * @return The opacity-adjusted bitmap.  If the source bitmap is mutable it will be
	 * adjusted and returned, otherwise a new bitmap is created.
	 */
	private static Bitmap adjustOpacity(Bitmap bitmap, int opacity)
	{
	    Bitmap mutableBitmap = bitmap.isMutable()
	                           ? bitmap
	                           : bitmap.copy(Bitmap.Config.ARGB_8888, true);
	    Canvas canvas = new Canvas(mutableBitmap);
	    int colour = (opacity & 0xFF) << 24;
	    canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
	    return mutableBitmap;
	}

	/**
     * Updates the text label with the given time
     * @param time in milliseconds
     */
	public static String getTime(int time){
        time += 999;  // round seconds upwards
		String[] str = TimerUtils.time2str(time);
		if(str.length == 3)
			return (str[0]+":"+str[1]+":"+str[2]);
		else if(str.length == 2)
			return (str[0]+":"+str[1]);
		else if(str.length == 1)
			return (str[0]);
		else
			return ("");

	}

}