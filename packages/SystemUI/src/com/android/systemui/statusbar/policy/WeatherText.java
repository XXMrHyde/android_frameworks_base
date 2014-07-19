package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WeatherText extends TextView {

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_FORECAST_DATE = "forecast_date";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";
    
    private Context mContext;
    private String mCityText = "";
    private String mTempText = "";
    private String mConditionText = "";

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWeather(intent);
        }
    };

    public WeatherText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        ContentResolver resolver = mContext.getContentResolver();
        SettingsObserver so = new SettingsObserver(getHandler());

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter("com.android.settings.INTENT_WEATHER_UPDATE");
            mContext.registerReceiver(weatherReceiver, filter, null, getHandler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(weatherReceiver);
            mAttached = false;
        }
    }

    public void updateWeather(Intent intent) {
        mCityText = (String) intent.getCharSequenceExtra(EXTRA_CITY);
        mTempText = (String) intent.getCharSequenceExtra(EXTRA_TEMP);
        mConditionText = (String) intent.getCharSequenceExtra(EXTRA_CONDITION);

        updateSettings();
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION),
                    false, this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        boolean showLocation = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION, 1,
                UserHandle.USER_CURRENT) == 1;

        String wText = "";

        if (showLocation) {
            wText = (mCityText + ", " + mTempText + ", " + mConditionText);
        } else {
            wText = (mTempText + ", " + mConditionText);
        }

        setText(wText);
    }
}

