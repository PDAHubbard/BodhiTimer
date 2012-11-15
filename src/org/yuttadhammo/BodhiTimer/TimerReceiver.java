package org.yuttadhammo.BodhiTimer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver 
{
	private final static String TAG = TimerReceiver.class.getSimpleName();
    private final static String CANCEL_NOTIFICATION = "CANCEL_NOTIFICATION";
	
	@Override
	public void onReceive(Context context, Intent pintent) 
    {

		NotificationManager mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel notification and return...
        if (CANCEL_NOTIFICATION.equals(pintent.getAction())) {
            Log.v(TAG,"Cancelling notification...");
            mNM.cancel(0);
            return;
        }

        // ...or display a new one

		Log.v(TAG,"Showing notification...");
		
        int setTime = pintent.getIntExtra("SetTime",0);
		String setTimeStr = TimerUtils.time2humanStr(setTime);
		
		CharSequence text = context.getText(R.string.Notification);
		CharSequence textLatest = context.getText(R.string.timer_for) + setTimeStr;

		// Load the settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean led = settings.getBoolean("LED",true);
        boolean vibrate = settings.getBoolean("Vibrate",true);
        String notificationUri = settings.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);

		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(context)
		        .setSmallIcon(R.drawable.notification)
		        .setContentTitle(text)
		        .setContentText(textLatest);

		Uri uri = null;
        // Play a sound!
        if(notificationUri != ""){
			uri = Uri.parse(notificationUri);
	      	mBuilder.setSound(uri);
        }
		
        // Vibrate
        if(vibrate){
        	mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);    	
        }

        // Have a light
        if(led){
        	mBuilder.setLights(0xff00ff00, 300, 1000);
        }
        
        mBuilder.setAutoCancel(true);
        
		// Creates an explicit intent for an Activity in your app
      	Intent resultIntent = new Intent(context,TimerActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(TimerActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		// Create intent for cancelling the notification
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, TimerReceiver.class);
        intent.setAction(CANCEL_NOTIFICATION);

        // Cancel the pending cancellation and create a new one
        PendingIntent pendingCancelIntent =
            PendingIntent.getBroadcast(appContext, 0, intent,
                                       PendingIntent.FLAG_CANCEL_CURRENT);
		
        if (settings.getBoolean("AutoClear", false)) {
            // Determine duration of notification sound
            int duration = 5000;
            if (uri != null) {
                MediaPlayer player = new MediaPlayer();
                try {
                    player.setDataSource(context, uri);
                    player.prepare();
                    duration = Math.max(duration, player.getDuration() + 2000);
                }
                catch (java.io.IOException ex) {
                    Log.e(TAG, "Cannot get sound duration: " + ex);
                    duration = 30000; // on error, default to 30 seconds
                }
                finally {
                    player.release();
                }
            }
            Log.v(TAG, "Notification duration: " + duration + " ms");
            // Schedule cancellation
            AlarmManager alarmMgr = (AlarmManager)context
                .getSystemService(Context.ALARM_SERVICE);
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                         SystemClock.elapsedRealtime() + duration,
                         pendingCancelIntent);
        }
		
		
		// mId allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());
		
	}

}