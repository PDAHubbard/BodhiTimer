package org.yuttadhammo.BodhiTimer.widget;

import java.util.Set;

import org.yuttadhammo.BodhiTimer.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/** Dialog box with an arbitrary number of number pickers */
public class NNumberPickerDialog extends Dialog implements OnClickListener,OnLongClickListener {

    public interface OnNNumberPickedListener {
        void onNumbersPicked(int[] number);
    }

    private final OnNNumberPickedListener mCallback;

    private int hsel;
    private int msel;
    private int ssel;

	private Gallery hour;
	private Gallery min;
	private Gallery sec;

	private String i1;

	private String i2;

	private String i3;

	private String i4;

	private SharedPreferences prefs;

	private Context context;
	
    /** Instantiate the dialog box.
     *
     * @param context
     * @param callBack callback function to get the numbers you requested
     * @param title title of the dialog box
     */
    public NNumberPickerDialog(Context context, OnNNumberPickedListener callBack,
            String title)
    {
        super(context, R.style.NPTheme);
        this.context = context;
        mCallback = callBack;
        

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.n_number_picker_dialog, null);
        setContentView(view);
        
		String [] numbers = new String[61];
		for(int i = 0; i < 61; i++) {
			numbers[i] = Integer.toString(i);
		}
		hour = (Gallery) view.findViewById(R.id.gallery_hour);
		min = (Gallery) view.findViewById(R.id.gallery_min);
		sec = (Gallery) view.findViewById(R.id.gallery_sec);
		
		ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(context, R.layout.gallery_item, numbers);        

		hour.setAdapter(adapter1);
		min.setAdapter(adapter1);
		sec.setAdapter(adapter1);
		
		Button cancel = (Button) findViewById(R.id.btnCancel);
		Button ok = (Button) findViewById(R.id.btnOk);
		cancel.setOnClickListener(this);
		ok.setOnClickListener(this);

		Button pre1 = (Button) findViewById(R.id.btn1);
		Button pre2 = (Button) findViewById(R.id.btn2);
		Button pre3 = (Button) findViewById(R.id.btn3);
		Button pre4 = (Button) findViewById(R.id.btn4);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		i1 = prefs.getString("pre1", null);
		i2 = prefs.getString("pre2", null);
		i3 = prefs.getString("pre3", null);
		i4 = prefs.getString("pre4", null);

		if(i1 != null)
			pre1.setText(i1);
		if(i2 != null)
			pre2.setText(i2);
		if(i3 != null)
			pre3.setText(i3);
		if(i4 != null)
			pre4.setText(i4);
		
		pre1.setOnClickListener(this);
		pre2.setOnClickListener(this);
		pre3.setOnClickListener(this);
		pre4.setOnClickListener(this);
		pre1.setOnLongClickListener(this);
		pre2.setOnLongClickListener(this);
		pre3.setOnLongClickListener(this);
		pre4.setOnLongClickListener(this);		
    }

	/** {@inheritDoc} */
	public void onClick(View v) 
	{
		
		switch(v.getId()){
			case R.id.btnOk:
		        if (mCallback != null) {
					
					hsel = hour.getSelectedItemPosition();
					msel = min.getSelectedItemPosition();
					ssel = sec.getSelectedItemPosition();
					
		            int[] values = {hsel,msel,ssel};
		            mCallback.onNumbersPicked(values);
		        }
		        dismiss();
		        break;
			case R.id.btnCancel:
	            mCallback.onNumbersPicked(null);
				dismiss();
				break;
			case R.id.btn1:
				setFromPre(i1);
				break;
			case R.id.btn2:
				setFromPre(i2);
				break;
			case R.id.btn3:
				setFromPre(i3);
				break;
			case R.id.btn4:
				setFromPre(i4);
				break;
		}
		
	}
    
	private void setFromPre(String ts) {
		if(ts == null)
			return;
		
        if (mCallback != null) {
    		int h = Integer.parseInt(ts.substring(0, 2));
    		int m = Integer.parseInt(ts.substring(3, 5));
    		int s = Integer.parseInt(ts.substring(6, 8));
			
    		if(h != 0 || m != 0 || s != 0) {
	            int[] values = {h,m,s};
	            mCallback.onNumbersPicked(values);
    		}
        	dismiss();
        }
	}

	/** {@inheritDoc} 
	 * @return */
	public boolean onLongClick(View v) 
	{
		String h = hour.getSelectedItemPosition()+"";
		if (h.length() == 1)
			h = "0"+h;
		String m = min.getSelectedItemPosition()+"";
		if (m.length() == 1)
			m = "0"+m;
		String s = sec.getSelectedItemPosition()+"";
		if (s.length() == 1)
			s = "0"+s;
		
		String vals = h+":"+m+":"+s;
		switch(v.getId()){
			case R.id.btn1:
				i1 = vals;
				setPre(v,1,vals);
				return true;
			case R.id.btn2:
				i2 = vals;
				setPre(v,2,vals);
				return true;
			case R.id.btn3:
				i3 = vals;
				setPre(v,3,vals);
				return true;
			case R.id.btn4:
				i4 = vals;
				setPre(v,4,vals);
				return true;
			default:
				return false;
		}		
	}

	private void setPre(View v, int i,String s) {
		String t = s;
		if(s.equals("00:00:00")) {
			s = null;
			t = context.getString(R.string.pre1);
			switch(i) {
				case 2:
					t = context.getString(R.string.pre2);
				case 3:
					t = context.getString(R.string.pre3);
				case 4:
					t = context.getString(R.string.pre4);
			}
		}
		((TextView) v).setText(t);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("pre"+i, s);
		editor.commit();
	}
}
