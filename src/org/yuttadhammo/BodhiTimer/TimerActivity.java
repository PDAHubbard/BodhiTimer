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
import org.yuttadhammo.BodhiTimer.widget.NNumberPickerDialog;
import org.yuttadhammo.BodhiTimer.widget.NNumberPickerDialog.OnNNumberPickedListener;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main activity which shows the timer and allows the user to set the time
 * @author Ralph Gootee (rgootee@gmail.com)
 */
public class TimerActivity extends Activity implements OnClickListener,OnNNumberPickedListener,OnSharedPreferenceChangeListener
{
	/** All possible timer states */
	final static int RUNNING=0;

	private static final int STOPPED=1;

	private static final int PAUSED=2;
	
	/** Should the logs be shown */
	private final static boolean LOG = true;
	
	/** Macros for our dialogs */
	private final static int NUM_PICKER_DIALOG = 0, ALERT_DIALOG = 1;
	/** debug string */
	private final String TAG = getClass().getSimpleName();
	
	/** Update rate of the internal timer */
	private final int TIMER_TIC = 100;
	
	/** The timer's current state */
	private int mCurrentState = -1;
	
	/** The maximum time */
	private int mLastTime = 0;
	
	/** The current timer time */
	private int mTime = 0;
	
	/** Internal increment class for the timer */
	private Timer mTimer = null;

	/** Handler for the message from the timer service */
	private Handler mHandler = new Handler() {
		
		@Override
        public void handleMessage(Message msg) {
			
			// The timer is finished
			if(msg.arg1 <= 0){
				
				if(mTimer != null){
					if(LOG) Log.v(TAG,"rcvd a <0 msg = " + msg.arg1);
					
					Context context = getApplicationContext();
					CharSequence text = getResources().getText(R.string.Notification);
					Toast.makeText(context, text,Toast.LENGTH_SHORT).show();
					timerStop();
				}
				
			// Update the time
			}else{
				mTime = msg.arg1;
				
				//enterState(RUNNING);
				onUpdateTime();
			}
		}
    };

	/** To save having to traverse the view tree */
	private ImageButton mPauseButton, mCancelButton, mSetButton, mPrefButton;

	private TimerAnimation mTimerAnimation;
	private TextView mTimerLabel1;
	
	private Bitmap mPlayBitmap,mPauseBitmap;

	private AlarmManager mAlarmMgr;

	private PendingIntent mPendingIntent;

	private AudioManager mAudioMgr;

	private SharedPreferences mSettings;

	private WakeLock mWakeLock;
    
	// for canceling notifications
	
	private NotificationManager mNM;
	
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
   
		mTimerLabel1 = (TextView)findViewById(R.id.text_top);

		mTimerAnimation = (TimerAnimation)findViewById(R.id.mainImage);
		mTimerAnimation.setMaxHeight(mTimerAnimation.getMeasuredHeight());
		mTimerAnimation.setMaxWidth(mTimerAnimation.getMeasuredWidth());
		
        enterState(STOPPED);
        
      
        // Store some useful values
        mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mAlarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAudioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mNM = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
       
		mSettings.registerOnSharedPreferenceChangeListener(this);
		
		if (mSettings.getBoolean("hideTime", false))
			mTimerLabel1.setVisibility(View.INVISIBLE);

		if(mSettings.getBoolean("FULLSCREEN", false))
				getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
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
        
        switch(mCurrentState){
        
        	case RUNNING:
        	{
	        	if(mTimer != null){
	        		mTimer.cancel();
	        		editor.putLong("TimeStamp", new Date().getTime() + mTime);
	        	}	
        	}break;
        	
        	case STOPPED:
                mNM.cancelAll();
        	case PAUSED:
        	{
        		editor.putLong("TimeStamp", 1);
        	}break;
        }
        
        editor.commit();
        
        releaseWakeLock();
    }
   

    /** {@inheritDoc} */
	@SuppressLint("NewApi")
	@Override 
    public void onResume()
    {
    	super.onResume();
		
    	if(mCurrentState == STOPPED)
			mNM.cancelAll();
        
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
        
        mTimerAnimation.setIndex(mSettings.getInt("DrawingIndex",0));
        int state = mSettings.getInt("State",0);
        
        switch(state)
        {
        	case RUNNING:
        		long timeStamp = mSettings.getLong("TimeStamp", -1);
                
        		Date now = new Date();
        		Date then = new Date(timeStamp);
            	
            	// We still have a timer running!
            	if(then.after(now)){
            		int delta = (int)(then.getTime() - now.getTime());		
            		timerStart(delta,false);
            		mCurrentState = RUNNING;
            	// All finished
            	}else{
            		clearTime();
            		enterState(STOPPED);
            	}
            	break;
        	
        	case STOPPED:
                mNM.cancelAll();
        		enterState(STOPPED);
        		break;
        	
        	case PAUSED:
        		mTime = mSettings.getInt("CurrentTime",0);
        		onUpdateTime();
        		enterState(PAUSED);
        		break;  	
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

    
    /**
     * Updates the time 
     */
	public void onUpdateTime(){
		
    	updateLabel(mTime);
    	mTimerAnimation.updateImage(mTime,mLastTime);  	
    }
	
	
    /**
     * Updates the text label with the given time
     * @param time in milliseconds
     */
	public void updateLabel(int time){
        time += 999;  // round seconds upwards
		String[] str = TimerUtils.time2str(time);
		if(str.length == 3)
			mTimerLabel1.setText(str[0]+":"+str[1]+":"+str[2]);
		else if(str.length == 2)
			mTimerLabel1.setText(str[0]+":"+str[1]);
		else if(str.length == 1)
			mTimerLabel1.setText(str[0]);
		else
			mTimerLabel1.setText("");

		//mTimerLabel2.setText(str[1]);
	}

	
	/** {@inheritDoc} */
	@Override
	protected Dialog onCreateDialog(int id) 
	{
		Dialog d = null;
		
		switch(id){
		
			case NUM_PICKER_DIALOG:
				d = new NNumberPickerDialog(this, this, getResources().getString(R.string.InputTitle));
				break;
			
			case ALERT_DIALOG:
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getResources().getText(R.string.warning_text))
				       .setCancelable(false)
				       .setPositiveButton(getResources().getText(R.string.warning_button), null)
				       .setTitle(getResources().getText(R.string.warning_title));
				       
				d = builder.create();
				
			}break;	
		}
		
		return d;
	}
	
	
	/** 
	 * Callback for the number picker dialog
	 */
	public void onNumbersPicked(int[] number)
	{
		int hour = number[0];
		int min = number[1];
		int sec = number[2];
		
		mLastTime = hour*60*60*1000 + min*60*1000 + sec*1000;
		
		// put last set time to prefs
		
		Editor mSettingsEdit = mSettings.edit();
		mSettingsEdit.putInt("LAST_SET_TIME", mLastTime);
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
	}


	/** 
	 * This only refers to the visual state of the application, used to manage
	 * the view coming back into focus.
	 * 
	 * @param state the visual state that is being entered
	 */
	private void enterState(int state){
		
		if(mCurrentState != state){
			
			setButtonAlpha(255);
			mCurrentState = state;		
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
					mPauseButton.setVisibility(View.GONE);
					mCancelButton.setVisibility(View.GONE);
					mSetButton.setVisibility(View.VISIBLE);	
					clearTime();
				
				}break;
		
				case PAUSED:
				{
					mSetButton.setVisibility(View.GONE);
					mPauseButton.setVisibility(View.VISIBLE);
					mCancelButton.setVisibility(View.VISIBLE);
					mPauseButton.setImageBitmap(mPlayBitmap);
				}break;	
			}
		}
	}
	
	private void setButtonAlpha(int i) {
		mPauseButton.setAlpha(i);		
		mCancelButton.setAlpha(i);		
		mPrefButton.setAlpha(i);		
	}

	/**
	 * Cancels the alarm portion of the timer
	 */
	private void stopAlarmTimer(){
		if(LOG) Log.v(TAG,"Stopping the alarm timer ...");		
		mAlarmMgr.cancel(mPendingIntent);
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
		mTimer.cancel();
		
		releaseWakeLock();
	}
	
	private void releaseWakeLock(){
		// Remove the wakelock
		if(mWakeLock != null && mWakeLock.isHeld()) {
			if(LOG) Log.v(TAG,"Releasing wake lock...");
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
				PowerManager.SCREEN_DIM_WAKE_LOCK
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
		
		// Start external service
		if(service){
		    if(LOG) Log.v(TAG,"Starting the timer service ...");
		    Intent intent = new Intent( getApplicationContext(), TimerReceiver.class);
		    intent.putExtra("SetTime",mLastTime);
		    mPendingIntent = PendingIntent.getBroadcast( getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		    mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + time, mPendingIntent);	    
		}
		
		// Internal thread to properly update the GUI
		mTimer = new Timer();	
		mTime = time;
		mTimer.scheduleAtFixedRate( new TimerTask(){
	        	public void run() {
	          		timerTic();
	        	}
	      	},
	      	0,
	      	TIMER_TIC);
		
		aquireWakeLock();
	}
	
	/** Resume the time after being paused */
	private void resumeTimer() 
	{
		if(LOG) Log.v(TAG,"Resuming the timer...");
			
		timerStart(mTime,true);
		enterState(RUNNING);
	}
	
	/** Pause the timer and stop the timer service */
	private void pauseTimer()
	{
		if(LOG) Log.v(TAG,"Pausing the timer...");
		
		mTimer.cancel();
		mTimer = null;
		
		stopAlarmTimer();
		
		enterState(PAUSED);
	}

	/** Called whenever the internal timer is updated */
	protected void timerTic() 
	{
		mTime -= TIMER_TIC;
		
		if(mHandler != null){
			Message msg = new Message();
			msg.arg1 = mTime;			
			mHandler.sendMessage(msg);
		}
	}
	
	/** Clears the time, sets the image and label to zero */
	private void clearTime()
	{
		mTime = 0;
		onUpdateTime();
	}

	/** {@inheritDoc} */
	public void onClick(View v) 
	{
		if(mCurrentState == STOPPED)
			mNM.cancelAll();
		
		switch(v.getId()){
		
			case R.id.setButton:
			{
				showDialog(NUM_PICKER_DIALOG);		
			}break;

			case R.id.prefButton:
			{
				Log.i("Timer","pref button clicked");
				startActivity(new Intent(this, TimerPrefActivity.class));	
			}break;
			
			
			case R.id.pauseButton:
			{
				switch(mCurrentState){
					case RUNNING:
						pauseTimer();
						break;
					case PAUSED:
						resumeTimer();
						break;
				}		
			}break;
			
			case R.id.cancelButton:
			{
				mNM.cancelAll();
			    Intent intent = new Intent( getApplicationContext(), TimerReceiver.class);
			    mPendingIntent = PendingIntent.getBroadcast( getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
				
				// We need to be careful to not cancel timers
				// that are not running (e.g. if we're paused)
				switch(mCurrentState){
					case RUNNING:
						timerStop();
						stopAlarmTimer();
						break;
					case PAUSED:
						clearTime();
						enterState(STOPPED);
						break;
				}	
			}break;
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
		
}
