package org.yuttadhammo.BodhiTimer;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;
public class SoundEffect {
	
	private SoundPool mSoundPool;
	private AudioManager  mAudioManager;
	private int mStream1 = 0;
	private int streamVolume;
	private int soundInt;
	final static int LOOP_1_TIME = 0;
	final static int SOUND_FX_01 = 1;
	
	public SoundEffect(Context context) {
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		
		String path = PreferenceManager.getDefaultSharedPreferences(context).getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
		
		soundInt = mSoundPool.load(path, 1);

		streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}
	public void play() {

		mSoundPool.stop(mStream1);
		mStream1= mSoundPool.play(soundInt, streamVolume, streamVolume, 1, LOOP_1_TIME, 1f);
	}
}
