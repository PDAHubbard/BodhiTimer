package org.yuttadhammo.BodhiTimer;

import android.content.Context;
import android.util.Log;

public class TimerUtils {
	

	private static String TAG = "TimerUtils";

	/**
	 * Returns the suggested text size for the string. A hack.
	 * @param str the time string
	 * @return the suggested text size to accommodate the string
	 */
	public static int textSize(String str)
	{
		if(str.length() > 7){ 
			return 50;
		}
		else if(str.length() == 7){
			return 55;
		}
		else{
			return 80;
		}
	}
 
	
	/** Converts a millisecond time to a string time 
	 * @param time is the time in milliseconds
	 * @return the formated string
	 */
	public static String[] time2str(int ms)
	{	
		int [] time = time2Array(ms);
 
		if(time[0] == 0 && time[1] == 0 && time[2] == 0){
			return new String[] {};
		}
		else if(time[0] == 0 && time[1] == 0){
			return new String[] {String.format("%01d",time[2])};
		}
		else if(time[0] == 0){
			return new String[] {String.format("%01d",time[1]),String.format("%02d",time[2])};
		}
		else {
			return new String[] {String.format("%01d",time[0]),String.format("%02d",time[1]),String.format("%02d",time[2])};
		}
	}
 
	/** Creates a time vector
	 *  @param ms the time in milliseconds
	 *  @return [hour,minutes,seconds,ms]
	 */
	public static int [] time2Array(int time)
	{
		int ms = time % 1000;
		int seconds = (int) (time / 1000);
		int minutes = seconds / 60;
		int hour = minutes / 60;
 
		minutes = minutes % 60;
   		seconds = seconds % 60;
   		
		int [] timeVec = new int[4];
		timeVec[0] = hour;
		timeVec[1] = minutes;
		timeVec[2] = seconds;
		timeVec[3] = ms;
		return timeVec;
	}
 
	public static String time2humanStr(Context context, int time)
	{
		int [] timeVec = time2Array(time);
		int hour = timeVec[0], minutes=timeVec[1], seconds=timeVec[2];
		
		//Log.v(TAG,"Times: "+hour+" "+minutes+" "+seconds);
   		
		String r = "";
   		
   		// string formating
   		if(hour != 0){	
   			if(hour != 1)
   				r += String.format(context.getString(R.string.x_hours), hour);
   			else
   				r += context.getString(R.string.one_hour);
   		}
   		if (minutes != 0) {
	   		if(r.length() != 0)
	   			r+= ",";
   			if(minutes != 1)
   				r += String.format(context.getString(R.string.x_mins), minutes);
   			else
   				r += context.getString(R.string.one_min);
   		}
   		if (seconds != 0) {
	   		if(r.length() != 0)
	   			r+= ",";
   			if(seconds != 1)
   				r += String.format(context.getString(R.string.x_secs), seconds);
   			else
   				r += context.getString(R.string.one_sec);
   		}
   		
   		return r;
	}


	public static String time2hms(int time) {
		String[] str = time2str(time);
		if(str.length == 3)
			return(str[0]+":"+str[1]+":"+str[2]);
		else if(str.length == 2)
			return(str[0]+":"+str[1]);
		else if(str.length == 1)
			return(str[0]);
		else
			return("");
	}
}