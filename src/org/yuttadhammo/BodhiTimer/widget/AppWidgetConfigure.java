package org.yuttadhammo.BodhiTimer.widget;

import org.yuttadhammo.BodhiTimer.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RemoteViews;

public class AppWidgetConfigure extends Activity implements OnClickListener {

	private int mAppWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
		    mAppWidgetId = extras.getInt(
		            AppWidgetManager.EXTRA_APPWIDGET_ID, 
		            AppWidgetManager.INVALID_APPWIDGET_ID);
		}

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
		
        setContentView(R.layout.widget_config);
        
        ImageButton ib1 = (ImageButton) findViewById(R.id.wtheme1);
        ImageButton ib2 = (ImageButton) findViewById(R.id.wtheme2);
        ImageButton ib3 = (ImageButton) findViewById(R.id.wtheme3);
        ImageButton ib4 = (ImageButton) findViewById(R.id.wtheme4);
        
        ib1.setOnClickListener(this);
        ib2.setOnClickListener(this);
        ib3.setOnClickListener(this);
        ib4.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		int theme = R.drawable.widget_background_black_square;
		switch(v.getId()) {
			case R.id.wtheme2:
				theme = R.drawable.widget_background_black;
				break;
			case R.id.wtheme3:
				theme = R.drawable.widget_background_white_square;
				break;
			case R.id.wtheme4:
				theme = R.drawable.widget_background_white;
				break;
		}
		
		final Context context = AppWidgetConfigure.this;
		savePref(context, mAppWidgetId,theme);
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.appwidget);
				appWidgetManager.updateAppWidget(mAppWidgetId, views);
		
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		this.sendBroadcast(new Intent(BodhiAppWidgetProvider.ACTION_CLOCK_UPDATE));
		finish();
	}
	
    // Write the prefix to the SharedPreferences object for this widget
    private void savePref(Context context, int appWidgetId, int theme) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putInt("widget_theme_" + appWidgetId, theme);
        prefs.commit();
    }
    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.remove("widget_theme_" + appWidgetId);
        prefs.commit();    	
    }
}
