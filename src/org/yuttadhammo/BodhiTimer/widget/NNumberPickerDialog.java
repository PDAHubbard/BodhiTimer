package org.yuttadhammo.BodhiTimer.widget;

/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import org.yuttadhammo.BodhiTimer.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ArrayAdapter;
import android.view.GestureDetector;

/** Dialog box with an arbitrary number of number pickers */
public class NNumberPickerDialog extends AlertDialog implements OnClickListener {

    public interface OnNNumberPickedListener {
        void onNumbersPicked(int[] number);
    }

    private final OnNNumberPickedListener mCallback;

    private int hsel;
    private int msel;
    private int ssel;

    private GestureDetector gestureDetector;


	private Gallery hour;
	private Gallery min;
	private Gallery sec;

    /** Instantiate the dialog box.
     *
     * @param context
     * @param callBack callback function to get the numbers you requested
     * @param title title of the dialog box
     */
    public NNumberPickerDialog(Context context, OnNNumberPickedListener callBack,
            String title)
    {
        super(context);
        
        mCallback = callBack;
        
        setButton(context.getText(android.R.string.ok), this);
        setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.n_number_picker_dialog, null);
        setView(view);

        //setTitle(title);
        LayoutParams npLayout = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        npLayout.gravity = 1;
        LayoutParams sepLayout = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
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
	}



    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
			
			hsel = hour.getSelectedItemPosition();
			msel = min.getSelectedItemPosition();
			ssel = sec.getSelectedItemPosition();
			
            int[] values = {hsel,msel,ssel};
            mCallback.onNumbersPicked(values);
        }
    }
}
