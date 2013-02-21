package org.yuttadhammo.BodhiTimer;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver {
	private final static String TAG = TimerReceiver.class.getSimpleName();
    private final static String CANCEL_NOTIFICATION = "CANCEL_NOTIFICATION";
	
	@Override
	public void onReceive(Context context, Intent pintent) 
    {
        MediaPlayer player = new MediaPlayer();

		NotificationManager mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel notification and return...
        if (CANCEL_NOTIFICATION.equals(pintent.getAction())) {
            Log.v(TAG,"Cancelling notification...");
            player.stop();
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
		
        Log.v(TAG,"notification uri: "+notificationUri);

		if(notificationUri.equals("system"))
			notificationUri = settings.getString("SystemUri", "");
		else if(notificationUri.equals("file"))
			notificationUri = settings.getString("FileUri", "");


		Log.v(TAG,"notification uri: "+notificationUri);
		
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

        


      	if(settings.getBoolean("overrideSound", false)) {
      		
      		//remove notification sound
      		mBuilder.setSound(null);
			
	        try {
				player.setDataSource(context, uri);
		        player.prepare();
		        player.setLooping(false);
		        player.setOnCompletionListener(new OnCompletionListener(){

					@Override
					public void onCompletion(MediaPlayer mp) {
						// TODO Auto-generated method stub
						mp.release();
					}
		        	
		        });
		        player.start();
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      	}
      	
        if (settings.getBoolean("AutoClear", false)) {
            // Determine duration of notification sound
            int duration = 5000;
            if (uri != null) {
                MediaPlayer cancelPlayer = new MediaPlayer();
                try {
                	cancelPlayer.setDataSource(context, uri);
                	cancelPlayer.prepare();
                    duration = Math.max(duration, cancelPlayer.getDuration() + 2000);
                }
                catch (java.io.IOException ex) {
                    Log.e(TAG, "Cannot get sound duration: " + ex);
                    duration = 30000; // on error, default to 30 seconds
                }
                finally {
                	cancelPlayer.release();
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

        if (settings.getBoolean("AutoRestart", false)) {
        	int time = pintent.getIntExtra("SetTime",0);
        	if (time != 0) {
                mNM.cancel(0);
    		    Log.v(TAG,"Restarting the timer service ...");
    		    Intent rintent = new Intent( context, TimerReceiver.class);
    		    rintent.putExtra("SetTime",time);
    		    PendingIntent mPendingIntent = PendingIntent.getBroadcast( context, 0, rintent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mAlarmMgr = (AlarmManager)context
                        .getSystemService(Context.ALARM_SERVICE);
                mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + time, mPendingIntent);

                // Save new time
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong("TimeStamp", new Date().getTime() + time);
                editor.commit();
                
        	}
        }
                
		// mId allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());


		
	}
}