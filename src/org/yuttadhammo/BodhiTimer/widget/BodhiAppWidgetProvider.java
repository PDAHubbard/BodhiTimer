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

	private static Bitmap bmp;
    
    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    	Log.i("Timer","update");
    	startTicking(context);
    }
   

	/**
	* Sending this broadcast intent will cause the clock widgets to update.
	*/
	public static String ACTION_CLOCK_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	
		alarmManager.cancel(createUpdate(context));
	}

    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d("Timer Widget", "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            AppWidgetConfigure.deletePref(context, appWidgetIds[i]);
        }
    }
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	
		final String action = intent.getAction();
	
		if (ACTION_CLOCK_UPDATE.equals(action)){
            Log.d("Timer Widget", "received broadcast");
			final ComponentName appWidgets = new ComponentName(context.getPackageName(), getClass().getName());
			final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			final int ids[] = appWidgetManager.getAppWidgetIds(appWidgets);
			if (ids.length > 0){
	            final int N = ids.length;
	            for (int i=0; i<N; i++) {
	                updateAppWidget(context, appWidgetManager, ids[i]);
	            }
			}
		}
	}

	/**
	* Schedules an alarm to update the clock every minute, at the top of the minute.
	*
	* @param context
	*/
	private void startTicking(Context context){
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	
		// schedules updates so they occur on the top of the second
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.SECOND,1);

		alarmManager.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), 1000, createUpdate(context));
	}

	/**
	* Creates an intent to update the clock(s).
	*
	* @param context
	* @return
	*/
	private static PendingIntent createUpdate(Context context){
		return PendingIntent.getBroadcast(context, 0,
				new Intent(ACTION_CLOCK_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        Log.d("Timer Widget", "updateAppWidget appWidgetId=" + appWidgetId);

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        mSettings = PreferenceManager.getDefaultSharedPreferences(context);

		// set image
		
       	int di = mSettings.getInt("DrawingIndex",0);
       	
       	if(di >= 0) {
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                bmp = BitmapFactory.decodeStream(imageStream);
    		}
       	}
        

        
		long timeStamp = mSettings.getLong("TimeStamp", -1);
        
		Date now = new Date();
		Date then = new Date(timeStamp);

		int delta = (int)(then.getTime() - now.getTime());		
        
        int mLastTime = mSettings.getInt("LastTime",0); 
        
        state = mSettings.getInt("State",0);
    	
        // We still have a timer running!
		if(then.after(now) && state == TimerActivity.RUNNING){
    		
			float p = (mLastTime != 0) ? (delta/(float)mLastTime) : 0;
			bmp = adjustOpacity(bmp,(int)(255-(255*p)));
	   		views.setTextViewText(R.id.time, getTime(delta));
    	}
		else{
            views.setTextViewText(R.id.time, "");
    		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    		
    		alarmManager.cancel(createUpdate(context));
    	}
    	
    	views.setImageViewBitmap(R.id.mainImage, bmp);
		
        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        Intent intent = new Intent(context, TimerActivity.class);
        intent.putExtra("set", "true");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
    	views.setOnClickPendingIntent(R.id.mainImage, pendingIntent);
    	
        // set background
        int themeid = mSettings.getInt("widget_theme_"+appWidgetId, R.drawable.widget_background_black_square);
    	views.setImageViewResource(R.id.backImage, themeid);

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