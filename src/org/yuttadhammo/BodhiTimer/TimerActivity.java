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
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
// import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ImageView;
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

	public static final int PAUSED=2;
	
	/** Should the logs be shown */
	private final static boolean LOG = true;
	
	/** Macros for our dialogs */
	private final static int ALERT_DIALOG = 1;
	/** debug string */
	private final String TAG = getClass().getSimpleName();
	
	/** Update rate of the internal timer */
	public final static int TIMER_TIC = 100;
	
	/** The timer's current state */
	public static int mCurrentState = -1;
	
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

	private static PendingIntent mPendingIntent;

	private AudioManager mAudioMgr;

	private SharedPreferences mSettings;
    
	// for canceling notifications
	
	public NotificationManager mNM;

	private boolean widget;
	private boolean isPaused;

	private int[] lastTimes;

	private TimerActivity context;

	private int animationIndex;

	private ImageView blackView;

	private MediaPlayer prePlayer;

	private long timeStamp;
	
	public static final String BROADCAST_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";
	public static final String BROADCAST_STOP = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_CANCEL";
	
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
		
		blackView = (ImageView)findViewById(R.id.black);
		
        // Store some useful values
        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mAlarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAudioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mNM = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // get last times
        
        lastTimes = new int[3];
        
        //enterState(STOPPED);

		mSettings.registerOnSharedPreferenceChangeListener(this);
    }

	/** { @inheritDoc} */
    @Override 
    public void onPause()
    {
    	super.onPause();
    	isPaused = true; // tell gui timer to stop
		sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update
		
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
	        	Log.i(TAG,"pause while running: "+new Date().getTime() + mTime);
	        	break;
        	case STOPPED:
        		cancelNotification();
        	case PAUSED:
        		editor.putLong("TimeStamp", 1);
        		break;
        }
        
        editor.commit();

    }
   

    /** {@inheritDoc} */
	@SuppressLint("NewApi")
	@Override 
    public void onResume()
    {
    	super.onResume();
		Log.d(TAG,"Resuming");
    	isPaused = false;
		sendBroadcast(new Intent(BROADCAST_STOP)); // tell widgets to stop updating
        mTimer = new Timer();

        lastTimes[0] = mSettings.getInt("last_hour", 0);
        lastTimes[1] = mSettings.getInt("last_min", 0);
        lastTimes[2] = mSettings.getInt("last_sec", 0);
		
		// register receiver to update the GUI
		
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
		
		setLowProfile();
		if(mSettings.getBoolean("WakeLock", false))
			getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

    	if(mSettings.getBoolean("FULLSCREEN", false))
			getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
    	else
        	getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN); 
		
    	// check the timestamp from the last update and start the timer.
    	// assumes the data has already been loaded?   
        mLastTime = mSettings.getInt("LastTime",0);    
		
        Log.d(TAG,"Last Time: "+mLastTime);
       
        animationIndex = mSettings.getInt("DrawingIndex",0);
        
        mTimerAnimation.setIndex(animationIndex);
        int state = mSettings.getInt("State",STOPPED);
    	if(state == STOPPED)
    		cancelNotification();

    	switch(state)
        {
        	case RUNNING:
	        	Log.i(TAG,"Resume while running: "+mSettings.getLong("TimeStamp", -1));
        		timeStamp = mSettings.getLong("TimeStamp", -1);
                
        		Date now = new Date();
        		Date then = new Date(timeStamp);
            	
            	// We still have a timer running!
            	if(then.after(now)){
    	        	if(LOG) Log.i(TAG,"Still have a timer");
    	    		mTime = (int) (then.getTime() - now.getTime());

            		enterState(RUNNING);
            		
            		doTick();
            		
            	// All finished
            	}else{
            		cancelNotification();
            		timerStop();
            	}
            	break;
        	
        	case STOPPED:
                mNM.cancelAll();
        		timerStop();
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

	@SuppressLint("NewApi")
	private void setLowProfile() {
    	if(android.os.Build.VERSION.SDK_INT >= 14) {
	    	View rootView = getWindow().getDecorView();
	    	rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }		
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
    	if(animationIndex != 0) {
    		blackView.setVisibility(View.GONE);
    		mTimerAnimation.updateImage(mTime,mLastTime);
    	}
    	else {
    		float p = (mLastTime != 0) ? (mTime/(float)mLastTime) : 0;
    		int alpha = (int)(255*p);
    		String alphas = Integer.toHexString(alpha);
    		alphas = alphas.length() == 1?"0"+alphas:alphas;
    		
    		int color = Color.parseColor("#"+alphas+"000000");
    		blackView.setBackgroundColor(color);
    		blackView.setVisibility(View.VISIBLE);
    	}
    }
	
	
    /**
     * Updates the text label with the given time
     * @param time in milliseconds
     */
	public void updateLabel(int time){
		if(time == 0)
			time = mLastTime;
		
        int rtime = (int) (Math.ceil(((float) time)/1000)*1000);  // round to seconds

        //Log.v(TAG,"rounding time: "+time+" "+rtime);
        
        mTimerLabel.setText(TimerUtils.time2hms(rtime));

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
		mTime = mLastTime;
		Log.v(TAG,"Picked time: "+mLastTime);

		onUpdateTime();

		lastTimes = new int[3];
		
		lastTimes[0] = hour;
		lastTimes[1] = min;
		lastTimes[2] = sec;
		
		// put last set time to prefs
		
		Editor mSettingsEdit = mSettings.edit();
		mSettingsEdit.putInt("LastTime", mLastTime);
		mSettingsEdit.putInt("last_hour", lastTimes[0]);
		mSettingsEdit.putInt("last_min", lastTimes[1]);
		mSettingsEdit.putInt("last_sec", lastTimes[2]);
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
		
		playPreSound();
		timerStart(mLastTime,true);
		
		if(widget == true) {
			sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update
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
			
			if(LOG) Log.v(TAG,"From/to states: "+mCurrentState+" "+state);
	        SharedPreferences.Editor editor = mSettings.edit();
	        editor.putInt("State", state);
	        editor.commit();
			mCurrentState = state;		
		}
		
		switch(state)
		{
			case RUNNING:
				mSetButton.setVisibility(View.GONE);
				mCancelButton.setVisibility(View.VISIBLE);
				mPauseButton.setVisibility(View.VISIBLE);
				mPauseButton.setImageBitmap(mPauseBitmap);
				setButtonAlpha(127);
				break;
			case STOPPED:
				mNM.cancelAll();
				mPauseButton.setImageBitmap(mPlayBitmap);
				mCancelButton.setVisibility(View.GONE);
				mSetButton.setVisibility(View.VISIBLE);	
				clearTime();
				setButtonAlpha(255);
				break;
	
			case PAUSED:
				mSetButton.setVisibility(View.GONE);
				mPauseButton.setVisibility(View.VISIBLE);
				mCancelButton.setVisibility(View.VISIBLE);
				mPauseButton.setImageBitmap(mPlayBitmap);
				setButtonAlpha(255);
				break;	
		}
	}
	
	private void setButtonAlpha(int i) {
		mPauseButton.setAlpha(i);		
		mCancelButton.setAlpha(i);		
		mPrefButton.setAlpha(i);		
	}
	
	/**
	 * Starts the timer at the given time
	 * @param time with which to count down
	 * @param service whether or not to start the service as well
	 */
	private void timerStart(int time,boolean service)
	{
		if(LOG) Log.v(TAG,"Starting the timer: "+time);
		
		enterState(RUNNING);

		mTime = time;
		
		timeStamp = new Date().getTime() + mTime;
		
        SharedPreferences.Editor editor = mSettings.edit();
		editor.putLong("TimeStamp", timeStamp);
        editor.commit();

		// Start external service
		if(service){
		    if(LOG) Log.v(TAG,"Starting the timer service: "+ TimerUtils.time2humanStr(context, mLastTime));
		    Intent intent = new Intent( this, TimerReceiver.class);
		    intent.putExtra("SetTime",mLastTime);
		    mPendingIntent = PendingIntent.getBroadcast( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		    mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + time, mPendingIntent);	    
		}

		// send start broadcast for widgets
		
		// start ticking
	    if(LOG) Log.v(TAG,"Start ticking...");

		mTimer.schedule( 
			new TimerTask(){
	        	public void run() {
	        		if(mHandler != null){
	        			mHandler.sendEmptyMessage(0);
	        		}
	        	}
	      	},
	      	TIMER_TIC
		);
		
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.SECOND,1);
		//mAlarmMgr.cancel(tickIntent);
		//mAlarmMgr.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), TIMER_TIC, tickIntent);

	}

	
	/**
	 * Stops the timer
	 */
	private void timerStop()
	{		
		if(LOG) Log.v(TAG,"Timer stopped");
		
		clearTime();
		
		// Stop our timer service
		enterState(STOPPED);		
		
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

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("CurrentTime",mTime);
        editor.commit();
        
		stopAlarmTimer();
		
		enterState(PAUSED);
	}
	
	/** Clears the time, sets the image and label to zero */
	private void clearTime()
	{
		mTime = 0;
		onUpdateTime();
	}


	/**
	 * Cancels the alarm portion of the timer
	 */
	private void stopAlarmTimer(){
		if(LOG) Log.v(TAG,"Stopping the alarm timer ...");		
		mAlarmMgr.cancel(mPendingIntent);
		mNM.cancelAll();
	}

	
	/** plays a sound before timer starts */
	private void playPreSound() {
        String uriString = mSettings.getString("PreSoundUri", "");
		
		if(uriString.equals("system"))
			uriString = mSettings.getString("PreSystemUri", "");
		else if(uriString.equals("file"))
			uriString = mSettings.getString("PreFileUri", "");

        if(uriString.equals(""))
        	return;
		
		Log.v(TAG,"preplay uri: "+uriString);

		try {
			prePlayer = new MediaPlayer();
			Uri uri = Uri.parse(uriString);

			int currVolume = mSettings.getInt("tone_volume", 0);
        	if(currVolume != 0) {
	        	float log1=(float)(Math.log(100-currVolume)/Math.log(100));
	            prePlayer.setVolume(1-log1,1-log1);
        	}
        	prePlayer.setDataSource(context, uri);
        	prePlayer.prepare();
        	prePlayer.setLooping(false);
        	prePlayer.setOnCompletionListener(new OnCompletionListener(){

				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mp.release();
				}
	        	
	        });
        	prePlayer.start();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/** {@inheritDoc} */
	public void onClick(View v) 
	{

		setLowProfile();
		
		if(mCurrentState == STOPPED) {
			if(prePlayer != null) {
				prePlayer.release();
			}

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
						playPreSound();
						timerStart(mLastTime,true);
						break;
				}
				break;
			
			case R.id.cancelButton:
				
				stopAlarmTimer();
				// We need to be careful to not cancel timers
				// that are not running (e.g. if we're paused)
				switch(mCurrentState){
					case RUNNING:
						if(prePlayer != null) {
							prePlayer.release();
						}
						cancelNotification();
						timerStop();
						break;
					case PAUSED:
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
			if(mSettings.getBoolean("WakeLock", false))
				getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
			else
				getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
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
	
	private Timer mTimer;

	private void doTick() {
		//Log.w(TAG,"ticking");

		if(mCurrentState != RUNNING || isPaused)
			return;
		
		Date now = new Date();
		Date then = new Date(timeStamp);

		mTime = (int)(then.getTime() - now.getTime());	
		
		if(mTime <= 0){
			
			Log.e(TAG,"Time up");
			
			timerStop();
			
			if(mSettings.getBoolean("AutoRestart", false)) {
				if(LOG) Log.v(TAG,"Restarting at " + mLastTime);
				mTime = mLastTime;
				timerStart(mLastTime,false);
			}
			
		// Update the time
		}else{
			// Internal thread to properly update the GUI
			mTimer.schedule( new TimerTask(){
		        	public void run() {
		        		if(mHandler != null){
		        			mHandler.sendEmptyMessage(0);
		        		}
		        	}
		      	},
		      	TIMER_TIC
			);
		}
	}
	
	/** Handler for the message from the timer service */
	private Handler mHandler = new Handler() {
		
		@Override
        public void handleMessage(Message msg) {
			onUpdateTime();
			doTick();
		}
    };
}
