/* @file TimerActivity.java
 * 
 * TeaTimer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version. More info: http://www.gnu.org/licenses/
 *  
 * Copyright 2009 Ralph Gootee <rgootee@gmail.com>
 *  
 */

package org.yuttadhammo.BodhiTimer;

import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation;
import org.yuttadhammo.BodhiTimer.NNumberPickerDialog.OnNNumberPickedListener;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
// import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main activity which shows the timer and allows the user to set the time
 * @author Ralph Gootee (rgootee@gmail.com)
 */
public class TimerActivity extends Activity implements OnClickListener,OnNNumberPickedListener,OnSharedPreferenceChangeListener
{
	/** All possible timer states */
	public final static int RUNNING=0;

	public static final int STOPPED=1;

	private static final int PAUSED=2;
	
	/** Should the logs be shown */
	private final static boolean LOG = true;
	
	/** Macros for our dialogs */
	private final static int ALERT_DIALOG = 1;
	/** debug string */
	private final String TAG = getClass().getSimpleName();
	
	/** Update rate of the internal timer */
	private final int TIMER_TIC = 100;
	
	/** The timer's current state */
	public int mCurrentState = -1;
	
	/** The maximum time */
	private int mLastTime = 0;
	
	/** The current timer time */
	private int mTime = 0;

	/** To save having to traverse the view tree */
	private ImageButton mPauseButton, mCancelButton, mSetButton, mPrefButton;

	private TimerAnimation mTimerAnimation;
	private TextView mTimerLabel;
	
	private Bitmap mPlayBitmap,mPauseBitmap;

	private AlarmManager mAlarmMgr;

	private PendingIntent mPendingIntent;

	private AudioManager mAudioMgr;

	private SharedPreferences mSettings;

	private WakeLock mWakeLock;
    
	// for canceling notifications
	
	public NotificationManager mNM;

	private boolean widget;

	private int[] lastTimes;

	private TimerActivity context;

	private PendingIntent pi;

	public static final String BROADCAST = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";
	
	/** Called when the activity is first created.
     *	{ @inheritDoc} 
     */
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //RelativeLayout main = (RelativeLayout)findViewById(R.id.mainLayout);
        
        context = this;
        
        if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 11) {
        	this.getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        }

        mCancelButton = (ImageButton)findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);
        
		mSetButton = (ImageButton)findViewById(R.id.setButton);
        mSetButton.setOnClickListener(this);
       
        mPauseButton = (ImageButton)findViewById(R.id.pauseButton);
        mPauseButton.setOnClickListener(this);

        mPrefButton = (ImageButton)findViewById(R.id.prefButton);
        mPrefButton.setOnClickListener(this);
        
        mPauseBitmap = BitmapFactory.decodeResource(
        		getResources(), R.drawable.pause);
        
        mPlayBitmap = BitmapFactory.decodeResource(
        		getResources(), R.drawable.play);
   
		mTimerLabel = (TextView)findViewById(R.id.text_top);

		mTimerAnimation = (TimerAnimation)findViewById(R.id.mainImage);
		mTimerAnimation.setOnClickListener(this);
		
        // Store some useful values
        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mAlarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAudioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mNM = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // get last times
        
        lastTimes = new int[3];
        
        lastTimes[0] = mSettings.getInt("last_hour", 0);
        lastTimes[1] = mSettings.getInt("last_min", 0);
        lastTimes[2] = mSettings.getInt("last_sec", 0);
 
        //enterState(STOPPED);
        
		mSettings.registerOnSharedPreferenceChangeListener(this);
    }

	/** { @inheritDoc} */
    @Override 
    public void onPause()
    {
    	super.onPause();

    	BitmapDrawable drawable = (BitmapDrawable)mTimerAnimation.getDrawable();
    	if(drawable != null) {
		    Bitmap bitmap = drawable.getBitmap();
		    bitmap.recycle();
    	}
    	
    	// Save our settings
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("LastTime", mLastTime);
        editor.putInt("CurrentTime",mTime);
        editor.putInt("DrawingIndex",mTimerAnimation.getIndex());
        editor.putInt("State", mCurrentState);

        editor.putInt("last_hour", lastTimes[0]);
        editor.putInt("last_min", lastTimes[1]);
        editor.putInt("last_sec", lastTimes[2]);
        
        switch(mCurrentState){
        
        	case RUNNING:
        	{
	        	Log.i(TAG,"pause while running: "+new Date().getTime() + mTime);
        		//editor.putLong("TimeStamp", new Date().getTime() + mTime);
        		
        	}break;
        	
        	case STOPPED:
        		cancelNotification();
        	case PAUSED:
        	{
        		editor.putLong("TimeStamp", 1);
        	}break;
        }
        
        editor.commit();

        releaseWakeLock();
        unregisterReceiver(onTick);
    }
   

    /** {@inheritDoc} */
	@SuppressLint("NewApi")
	@Override 
    public void onResume()
    {
    	super.onResume();
		Log.d(TAG,"Resuming");

		// register receiver to update the GUI
		IntentFilter filter=new IntentFilter(BROADCAST);
		filter.setPriority(2);
		registerReceiver(onTick, filter);

		
		if(getIntent().hasExtra("set")) {
			Log.d(TAG,"Create From Widget");
			widget = true;
			getIntent().removeExtra("set");
		}
    	
    	try {
			mTimerAnimation.resetAnimationList();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if (mSettings.getBoolean("hideTime", false))
			mTimerLabel.setVisibility(View.INVISIBLE);
		else
			mTimerLabel.setVisibility(View.VISIBLE);

		if(mSettings.getBoolean("FULLSCREEN", false))
				getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
		
    	if(mCurrentState == STOPPED)
    		cancelNotification();
    	
    	if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 11) {
	    	View rootView = getWindow().getDecorView();
	    	rootView.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
	    	rootView.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        }

    	if(mSettings.getBoolean("FULLSCREEN", false))
			getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
    	else
        	getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN); 
		
    	// check the timestamp from the last update and start the timer.
    	// assumes the data has already been loaded?   
        mLastTime = mSettings.getInt("LastTime",0);    
		
        Log.d(TAG,"Last Time: "+mLastTime);
       
        mTimerAnimation.setIndex(mSettings.getInt("DrawingIndex",0));
        int state = mSettings.getInt("State",0);
        
        switch(state)
        {
        	case RUNNING:
	        	Log.i(TAG,"Resume while running: "+mSettings.getLong("TimeStamp", -1));
        		long timeStamp = mSettings.getLong("TimeStamp", -1);
                
        		Date now = new Date();
        		Date then = new Date(timeStamp);
            	
            	// We still have a timer running!
            	if(then.after(now)){
    	        	Log.i(TAG,"Still have a timer");
    	    		mTime = (int) (then.getTime() - now.getTime());

            		mCurrentState = RUNNING;
            		aquireWakeLock();
            	// All finished
            	}else{
            		timerStop();
            	}
            	break;
        	
        	case STOPPED:
                mNM.cancelAll();
        		enterState(STOPPED);
        		if(widget) {
        			showNumberPicker();
        			return;
        		}
        		break;
        	
        	case PAUSED:
        		mTime = mSettings.getInt("CurrentTime",0);
        		onUpdateTime();
        		enterState(PAUSED);
        		break;  	
        }
		widget = false;
	}

	@Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        mNM.cancelAll();
        switch(keycode) {
        case KeyEvent.KEYCODE_MENU:
    		startActivity(new Intent(this, TimerPrefActivity.class));	
            return true;
        }
        return super.onKeyDown(keycode, e);
    }

	protected void  onActivityResult (int requestCode, int resultCode, Intent  data) {
		if(LOG) Log.v(TAG,"Got result");
		if(resultCode == Activity.RESULT_OK) {
			int[] values = data.getIntArrayExtra("times");
			onNumbersPicked(values);
			if(widget) {
				finish();
			}
		}
		widget = false;
	}

    private void showNumberPicker() {
		Intent i = new Intent(this, NNumberPickerDialog.class);
		i.putExtra("times", lastTimes);
    	startActivityForResult(i, 1);
	}

	
    /**
     * Updates the time 
     */
	public void onUpdateTime(){
		if(mCurrentState == STOPPED)
			mTime = 0;
    	updateLabel(mTime);
    	mTimerAnimation.updateImage(mTime,mLastTime);  	
    }
	
	
    /**
     * Updates the text label with the given time
     * @param time in milliseconds
     */
	public void updateLabel(int time){
		if(time == 0)
			time = mLastTime;
		
        time += 999;  // round seconds upwards
		String[] str = TimerUtils.time2str(time);
		if(str.length == 3)
			mTimerLabel.setText(str[0]+":"+str[1]+":"+str[2]);
		else if(str.length == 2)
			mTimerLabel.setText(str[0]+":"+str[1]);
		else if(str.length == 1)
			mTimerLabel.setText(str[0]);
		else
			mTimerLabel.setText("");

		//mTimerLabel2.setText(str[1]);
	}

	
	/** 
	 * Callback for the number picker dialog
	 */
	public void onNumbersPicked(int[] number)
	{
		if(number == null) {
			widget = false;
			return;
		}
			
		int hour = number[0];
		int min = number[1];
		int sec = number[2];
		
		mLastTime = hour*60*60*1000 + min*60*1000 + sec*1000;

		Log.v(TAG,"Picked numbers: "+mLastTime);
		
		lastTimes = new int[3];
		
		lastTimes[0] = hour;
		lastTimes[1] = min;
		lastTimes[2] = sec;
		
		// put last set time to prefs
		
		Editor mSettingsEdit = mSettings.edit();
		mSettingsEdit.putInt("LastTime", mLastTime);
		mSettingsEdit.commit();
		
		// Check to make sure the phone isn't set to silent
		boolean silent = (mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
		String noise = mSettings.getString("NotificationUri","");
		boolean vibrate = mSettings.getBoolean("Vibrate",true);
        boolean nag = mSettings.getBoolean("NagSilent",true);
       
        // If the conditions are _just_ right show a nag screen
		if(nag && silent && (noise != "" || vibrate) ){
			showDialog(ALERT_DIALOG);
		}
		
		timerStart(mLastTime,true);
		
		if(widget == true) {
			finish();		
		}
	}


	/** 
	 * This only refers to the visual state of the application, used to manage
	 * the view coming back into focus.
	 * 
	 * @param state the visual state that is being entered
	 */
	private void enterState(int state){

		if(mCurrentState != state){

			// update preference for widget, notification
			
	        SharedPreferences.Editor editor = mSettings.edit();
	        editor.putInt("State", state);
	        editor.commit();
			
			if(LOG) Log.v(TAG,"Set current state = " + mCurrentState);
			
			switch(state)
			{
				case RUNNING:
				{
					mSetButton.setVisibility(View.GONE);
					mCancelButton.setVisibility(View.VISIBLE);
					mPauseButton.setVisibility(View.VISIBLE);
					mPauseButton.setImageBitmap(mPauseBitmap);
					setButtonAlpha(127);
				}break;
		
				case STOPPED:
				{	
					mNM.cancelAll();
					mPauseButton.setImageBitmap(mPlayBitmap);
					mCancelButton.setVisibility(View.GONE);
					mSetButton.setVisibility(View.VISIBLE);	
					clearTime();
					setButtonAlpha(255);
				
				}break;
		
				case PAUSED:
				{
					mSetButton.setVisibility(View.GONE);
					mPauseButton.setVisibility(View.VISIBLE);
					mCancelButton.setVisibility(View.VISIBLE);
					mPauseButton.setImageBitmap(mPlayBitmap);
					setButtonAlpha(255);
				}break;	
			}
			mCurrentState = state;		
		}
		
	}
	
	private void setButtonAlpha(int i) {
		mPauseButton.setAlpha(i);		
		mCancelButton.setAlpha(i);		
		mPrefButton.setAlpha(i);		
	}
	
	private void releaseWakeLock(){
		// Remove the wakelock
		if(mWakeLock != null && mWakeLock.isHeld()) {
			if(LOG) Log.v(TAG,"Releasing wakelock...");
			mWakeLock.release();
			mWakeLock = null;
		}
	}
	/**
	 * Only aquires the wake lock _if_ it is set in the settings. 
	 */
	private void aquireWakeLock(){
		// We're going to start a wakelock
		if(mSettings.getBoolean("WakeLock", false)){
			if(LOG) Log.v(TAG,"Issuing a wakelock...");
			
			PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
			if(mWakeLock != null) Log.e(TAG,"There's already a wakelock... Shouldn't be there!");
			
			mWakeLock= pm.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
	            | PowerManager.ON_AFTER_RELEASE,
	            TAG);
			mWakeLock.acquire();
		}		
	}
	
	/**
	 * Starts the timer at the given time
	 * @param time with which to count down
	 * @param service whether or not to start the service as well
	 */
	private void timerStart(int time,boolean service)
	{
		if(LOG) Log.v(TAG,"Starting the timer...");
		
		enterState(RUNNING);

		mTime = time;
		
        SharedPreferences.Editor editor = mSettings.edit();
		editor.putLong("TimeStamp", new Date().getTime() + mTime);
        editor.commit();

		
		// Start external service
		if(service){
		    if(LOG) Log.v(TAG,"Starting the timer service ...");
		    Intent intent = new Intent( this, TimerReceiver.class);
		    intent.putExtra("SetTime",mLastTime);
		    mPendingIntent = PendingIntent.getBroadcast( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		    mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + time, mPendingIntent);	    
		}

		// start broadcasting ticks
	    if(LOG) Log.v(TAG,"Start ticking...");

		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.SECOND,1);
		
		pi = PendingIntent.getBroadcast(this, 1,
				new Intent("org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE"), PendingIntent.FLAG_UPDATE_CURRENT);
		mAlarmMgr.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), 100, pi);

		aquireWakeLock();
	}

	
	/**
	 * Stops the timer
	 */
	private void timerStop()
	{		
		if(LOG) Log.v(TAG,"Timer stopped");
		stopAlarmTimer();
		clearTime();
		
		// Stop our timer service
		enterState(STOPPED);		
		
		releaseWakeLock(); 
	}
	
	/** Resume the time after being paused */
	private void timerResume() 
	{
		if(LOG) Log.v(TAG,"Resuming the timer...");
			
		timerStart(mTime,true);
		enterState(RUNNING);
	}
	
	/** Pause the timer and stop the timer service */
	private void timerPause()
	{
		if(LOG) Log.v(TAG,"Pausing the timer...");

		
		stopAlarmTimer();
		
		enterState(PAUSED);
	}
	
	/** Clears the time, sets the image and label to zero */
	private void clearTime()
	{
		mTime = 0;
		onUpdateTime();
		Intent broadcast = new Intent();
        broadcast.setAction("org.yuttadhammo.BodhiTimer.ACTION_CLOCK_CANCEL");
		sendBroadcast(broadcast);
	}


	/**
	 * Cancels the alarm portion of the timer
	 */
	private void stopAlarmTimer(){
		if(LOG) Log.v(TAG,"Stopping the alarm timer ...");		
		mAlarmMgr.cancel(mPendingIntent);
		mAlarmMgr.cancel(pi);
		mNM.cancelAll();
	}

	
	/** {@inheritDoc} */
	public void onClick(View v) 
	{
		if(mCurrentState == STOPPED) {
			cancelNotification();
		}
		
		switch(v.getId()){
			case R.id.setButton:
				Log.i("Timer","set button clicked");
				showNumberPicker();
				break;

			case R.id.prefButton:
				Log.i("Timer","pref button clicked");
				widget = false;
				startActivity(new Intent(this, TimerPrefActivity.class));	
				break;
			
			
			case R.id.pauseButton:
				switch(mCurrentState){
					case RUNNING:
						timerPause();
						break;
					case PAUSED:
						timerResume();
						break;
					case STOPPED:
						timerStart(mLastTime,true);
						break;
				}
				break;
			
			case R.id.cancelButton:
				
				// We need to be careful to not cancel timers
				// that are not running (e.g. if we're paused)
				switch(mCurrentState){
					case RUNNING:
						cancelNotification();
						timerStop();
						break;
					case PAUSED:
						mNM.cancelAll();
						clearTime();
						enterState(STOPPED);
						break;
				}	

			    break;
		}
	}
	
	/** 
	 * Mostly used for the wakelock currently -- should be used for the visual components eventually
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		// We need to check if the 
		if(key == "WakeLock"){
			if(mSettings.getBoolean("WakeLock", false)) aquireWakeLock();
			else releaseWakeLock();
		}
	}
	
	private void cancelNotification() {
		// Create intent for cancelling the notification
        Intent intent = new Intent(this, TimerReceiver.class);
        intent.setAction(TimerReceiver.CANCEL_NOTIFICATION);

        // Cancel the pending cancellation and create a new one
        PendingIntent pendingCancelIntent =
            PendingIntent.getBroadcast(this, 0, intent,
                                       PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                         SystemClock.elapsedRealtime(),
                         pendingCancelIntent);

	}
	
	private BroadcastReceiver onTick = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent i) {
			mTime -= TIMER_TIC;
			
			if(mTime <= TIMER_TIC){
				
				Log.e(TAG,"Time up");
				
				timerStop();
				
				if(mSettings.getBoolean("AutoRestart", false)) {
					if(LOG) Log.v(TAG,"Restarting at " + mLastTime);
					mTime = mLastTime;
					timerStart(mLastTime,false);
				}
				
			// Update the time
			}else{
				//enterState(RUNNING);
				onUpdateTime();
			}
		}
	};
}
