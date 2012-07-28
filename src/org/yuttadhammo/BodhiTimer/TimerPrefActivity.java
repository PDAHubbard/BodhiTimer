package org.yuttadhammo.BodhiTimer;


import java.io.IOException;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;

public class TimerPrefActivity extends PreferenceActivity 
{
	private static final String TAG = TimerPrefActivity.class.getSimpleName();
	private SharedPreferences settings;
	private Context context;
	private MediaPlayer player;
	private Preference play;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        context = this;

        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if(settings.getBoolean("FULLSCREEN", false))
			getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
       
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        // Load the sounds
        ListPreference tone = (ListPreference)findPreference("NotificationUri");
        play = (Preference)findPreference("playSound");
       	
    	String [] cols = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE};
    	
    	// Lets check out the media provider
        Cursor cursor = managedQuery(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
        		cols, 
        		"is_ringtone OR is_notification",
        		null, "is_ringtone, title_key");
         int i=0;
   
         CharSequence[] items = null;
         CharSequence[] itemUris = null;
         
        if(cursor != null && cursor.getCount() > 0){
        	
            items = new CharSequence[cursor.getCount()];
            itemUris = new CharSequence[cursor.getCount()];
        
        	int colTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);;
        	int colId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);;
	        
        	while(!cursor.isLast()){
	        	cursor.moveToNext();
	        	items[i] = cursor.getString(colTitle);
	        	itemUris[i] = MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "/" + cursor.getLong(colId);
	        	i++;
        	}
        
	        cursor.close();   
		}
        
    	CharSequence [] entries = {"No Sound","Three Bells","One Bell","Gong","Singing Bowl"};
    	CharSequence [] entryValues = {"","android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell1,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.gong,"android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bowl};
    	
    	//Default value
    	if(tone.getValue() == null) tone.setValue((String)entryValues[1]);
    	tone.setDefaultValue((String)entryValues[1]);
    	
    	if( items != null && items.length > 0){
    		tone.setEntries(concat(entries,items));
    		tone.setEntryValues(concat(entryValues,itemUris));
    	}else{
    		tone.setEntries(entries);
    		tone.setEntryValues(entryValues);
    	}

    	player = new MediaPlayer();
    	
    	tone.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
    		@Override
			public boolean onPreferenceClick(final Preference preference) {
    	    	if(player.isPlaying()) {
    				play.setTitle(context.getString(R.string.play_sound));
    				play.setSummary(context.getString(R.string.play_sound_desc));
    	    		player.stop();      
    	    	}
    	    	return false;
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

        Preference full = (Preference)findPreference("FULLSCREEN");
        
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
        
    }
    static private CharSequence [] concat( CharSequence[] A, CharSequence[] B) 
    {		
    	CharSequence[] C= new CharSequence[A.length+B.length];
    	System.arraycopy(A, 0, C, 0, A.length);
    	System.arraycopy(B, 0, C, A.length, B.length);

    	   return C;
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
}