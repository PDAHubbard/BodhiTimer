package org.yuttadhammo.BodhiTimer;


import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.Toast;

public class TimerPrefActivity extends PreferenceActivity 
{
	private static final String TAG = TimerPrefActivity.class.getSimpleName();
	private SharedPreferences settings;
	private Context context;
	private static Activity activity;
	private MediaPlayer player;
	private Preference play;
	private int SELECT_RINGTONE = 123;
	private int SELECT_PHOTO = 456;
	private int SELECT_FILE;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        context = this;
        activity = this;

        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if(settings.getBoolean("FULLSCREEN", false))
			getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
       
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        // Load the sounds
        ListPreference tone = (ListPreference)findPreference("NotificationUri");
        play = (Preference)findPreference("playSound");
    	
    	CharSequence [] entries = {"No Sound","Three Bells","One Bell","Gong","Singing Bowl","System Tone","Sound File"};
    	CharSequence [] entryValues = {"","android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell1,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.gong,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bowl,"system","file"};
    	
    	//Default value
    	if(tone.getValue() == null) tone.setValue((String)entryValues[1]);
    	tone.setDefaultValue((String)entryValues[1]);
    	
		tone.setEntries(entries);
		tone.setEntryValues(entryValues);
		
    	player = new MediaPlayer();
    	
    	tone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
    	    	if(player.isPlaying()) {
    				play.setTitle(context.getString(R.string.play_sound));
    				play.setSummary(context.getString(R.string.play_sound_desc));
    	    		player.stop();      
    	    	}
    	    	if(newValue.toString().equals("system")) {
    	    		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
    	    		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
    	    		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
    	    		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
    	    		activity.startActivityForResult(intent, SELECT_RINGTONE);
    	    	}
    	    	else if(newValue.toString().equals("file")) {

    	            Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
    	            intent.setType("audio/*"); 
    	            intent.addCategory(Intent.CATEGORY_OPENABLE);

    	            try {
    	            	activity.startActivityForResult(Intent.createChooser(intent, "Select Sound File"), SELECT_FILE);
    	            } 
    	            catch (ActivityNotFoundException ex) {
    	                Toast.makeText(activity, "Please install a File Manager.", 
    	                        Toast.LENGTH_SHORT).show();
    	            }
    	        }
				return true;
			}

    	});

    	play.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
    		@Override
			public boolean onPreferenceClick(final Preference preference) {
    			if(player.isPlaying()) {
    				player.stop();
    				preference.setTitle(context.getString(R.string.play_sound));
    				preference.setSummary(context.getString(R.string.play_sound_desc));
    				return false;
    			}
    			
                try {
                    String notificationUri = settings.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
					if(notificationUri.equals("system"))
						notificationUri = settings.getString("SystemUri", "");
					else if(notificationUri.equals("file"))
						notificationUri = settings.getString("FileUri", "");
                    player.reset();
                    player.setDataSource(context, Uri.parse(notificationUri));
                    player.prepare();
	                player.setLooping(false);
	                player.setOnCompletionListener(new OnCompletionListener(){
						@Override
						public void onCompletion(MediaPlayer mp) {
		    				preference.setTitle(context.getString(R.string.play_sound));
		    				preference.setSummary(context.getString(R.string.play_sound_desc));
						}
	                });
	                player.start();
                } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				preference.setTitle(context.getString(R.string.playing_sound));
				preference.setSummary(context.getString(R.string.playing_sound_desc));
				
                return false;
			}

    	});
    	
        Preference about = (Preference)findPreference("aboutPref");

        about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
    		@Override
			public boolean onPreferenceClick(Preference preference) {
				LayoutInflater li = LayoutInflater.from(context);
	            View view = li.inflate(R.layout.about, null);
				WebView wv = (WebView) view.findViewById(R.id.about_text);
			    wv.loadData(getString(R.string.about_text), "text/html", "utf-8");
				
				Builder p = new AlertDialog.Builder(context).setView(view);
	            final AlertDialog alrt = p.create();
	            alrt.setIcon(R.drawable.icon);
	            alrt.setTitle(getString(R.string.about_title));
	            alrt.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close),
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog,
	                                int whichButton) {
	                        }
	                    });
	            alrt.show();
	            return true;
				   			
			}

    	});

        final Preference bmpUrl = (Preference)findPreference("bmp_url");
        CheckBoxPreference customBmp = (CheckBoxPreference)findPreference("custom_bmp");
        if(!customBmp.isChecked())
        	bmpUrl.setEnabled(false);
        
        bmpUrl.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
    			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
    			photoPickerIntent.setType("image/*");
    			startActivityForResult(photoPickerIntent, SELECT_PHOTO );
	            return true;
				   			
			}

    	});
    	

        customBmp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				if(newValue.toString().equals("true"))
					bmpUrl.setEnabled(true);
				else
					bmpUrl.setEnabled(false);
				return true;
			}

    	});
        
        CheckBoxPreference full = (CheckBoxPreference)findPreference("FULLSCREEN");
        
        full.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				if(newValue.toString().equals("true"))
					getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
		    	else
		        	getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN); 				
				return true;
			}

    	});

        final Preference indexPref = (Preference)findPreference("DrawingIndex");
		int dIndex = settings.getInt("DrawingIndex", 0);
		if(dIndex == 0)
			indexPref.setSummary(getString(R.string.is_bitmap));
		else
			indexPref.setSummary(getString(R.string.is_circle));
        
        indexPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				int dIndex = settings.getInt("DrawingIndex", 0);
				dIndex++;
				dIndex %= 2;
				
				if(dIndex == 0) {
					indexPref.setSummary(getString(R.string.is_bitmap));
				}
				else {
					indexPref.setSummary(getString(R.string.is_circle));
				}
		        Editor mSettingsEdit = settings.edit();
        		mSettingsEdit.putInt("DrawingIndex", dIndex);
        		mSettingsEdit.commit();
	            return true;
				   			
			}

    	});

    }

    @Override
    public void onPause() {
    	if(player.isPlaying()) {
    		player.stop();
			play.setTitle(context.getString(R.string.play_sound));
			play.setSummary(context.getString(R.string.play_sound_desc));
    	}
		super.onPause();
    }    
    @Override
    public void onResume() {
		if(settings.getBoolean("FULLSCREEN", false))
			getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
    	else
        	getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN); 

		super.onResume();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
    {
        Uri uri = null;
        Editor mSettingsEdit = settings.edit();
		if (resultCode == Activity.RESULT_OK && requestCode == SELECT_RINGTONE )
        {
        	uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            Log.i("Timer","Got ringtone "+uri.toString()); 
        	if (uri != null)
        		mSettingsEdit.putString("SystemUri", uri.toString());
        	else
        		mSettingsEdit.putString("SystemUri", "");
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == SELECT_FILE) {
            // Get the Uri of the selected file 
            uri = intent.getData();
            Log.d(TAG, "File Path: " + uri);
        	if (uri != null)
        		mSettingsEdit.putString("FileUri", uri.toString());
        	else
        		mSettingsEdit.putString("FileUri", "");
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PHOTO ){
            uri = intent.getData();
        	if (uri != null)
        		mSettingsEdit.putString("bmp_url", uri.toString());
        	else
        		mSettingsEdit.putString("bmp_url", "");
        }
		mSettingsEdit.commit();  
    }

}