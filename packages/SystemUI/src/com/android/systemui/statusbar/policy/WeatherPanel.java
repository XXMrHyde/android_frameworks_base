
package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.PorterDuff.Mode;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class WeatherPanel extends FrameLayout {

    private static final String TAG = "WeatherPanel";

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_CONDITION_CODE = "condition_code";
    public static final String EXTRA_FORECAST_DATE = "forecast_date";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";

    private TextView mHighTemp;
    private TextView mSlash;
    private TextView mLowTemp;
    private TextView mCurrentTemp;
    private TextView mCity;
    private TextView mHumidity;
    private TextView mWinds;
    private TextView mCondition;
    private ImageView mConditionImage;
    private Context mContext;
    private String mCondition_code = "";
    private ContentObserver mContentObserver;
    private ContentResolver mContentResolver;

    private ContentResolver mResolver;

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWeather(intent);
        }
    };

    public WeatherPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResolver = mContext.getContentResolver();
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHighTemp = (TextView) this.findViewById(R.id.high_temp);
        mLowTemp = (TextView) this.findViewById(R.id.low_temp);
        mCurrentTemp = (TextView) this.findViewById(R.id.current_temp);
        mCity = (TextView) this.findViewById(R.id.city);
        mHumidity = (TextView) this.findViewById(R.id.humidity);
        mWinds = (TextView) this.findViewById(R.id.winds);
        mCondition = (TextView) this.findViewById(R.id.condition);
        mConditionImage = (ImageView) this.findViewById(R.id.condition_image);
        mSlash = (TextView) this.findViewById(R.id.weatherpanel_slash);

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter("com.android.settings.INTENT_WEATHER_UPDATE");
            getContext().registerReceiver(weatherReceiver, filter, null, getHandler());

            updateSettings();
            mContentResolver = getContext().getContentResolver();
            mContentObserver = new ContentObserver(getHandler()) {
                @Override
                public void onChange(boolean selfChange) {
                    updateSettings();
                }
            };
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION),
                    false, mContentObserver);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_COLOR),
                    false, mContentObserver);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_PRESSED_COLOR),
                    false, mContentObserver);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON),
                    false, mContentObserver);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_COLOR),
                    false, mContentObserver);
        }
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(weatherReceiver);
            mAttached = false;
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    public void updateWeather(Intent intent) {
        mCondition_code = (String) intent.getCharSequenceExtra(EXTRA_CONDITION_CODE);
        if (mCurrentTemp != null)
            mCurrentTemp.setText(intent.getCharSequenceExtra(EXTRA_TEMP));
        if (mHighTemp != null)
            mHighTemp.setText(intent.getCharSequenceExtra(EXTRA_HIGH));
        if (mLowTemp != null)
            mLowTemp.setText(intent.getCharSequenceExtra(EXTRA_LOW));
        if (mCity != null)
            mCity.setText(intent.getCharSequenceExtra(EXTRA_CITY));
        if (mHumidity != null)
            mHumidity.setText(intent.getCharSequenceExtra(EXTRA_HUMIDITY));
        if (mWinds != null)
            mWinds.setText(intent.getCharSequenceExtra(EXTRA_WIND));
        if (mCondition != null)
            mCondition.setText(intent.getCharSequenceExtra(EXTRA_CONDITION));
        if (mConditionImage != null) {
            int level = 100;
            try {
                level = Integer.parseInt(mCondition_code);
            } catch (Exception e) {
            }
            mConditionImage.setImageLevel(level);
        }
    }

    public void setTextColor(int color)
    {
        if (mCurrentTemp != null)
            mCurrentTemp.setTextColor(color);
        if (mHighTemp != null)
            mHighTemp.setTextColor(color);
        if (mLowTemp != null)
            mLowTemp.setTextColor(color);
        if (mCity != null)
            mCity.setTextColor(color);
        if (mHumidity != null)
            mHumidity.setTextColor(color);
        if (mWinds != null)
            mWinds.setTextColor(color);
        if (mCondition != null)
            mCondition.setTextColor(color);
        if (mSlash != null)
            mSlash.setTextColor(color);
    }

    public void updateSettings() {

        boolean showLocation = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION, 0,
                UserHandle.USER_CURRENT) == 1;
        int iconType = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON, 0,
                UserHandle.USER_CURRENT);
        int iconColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        int textColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_TEXT_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);

        if (mCity != null) {
            mCity.setVisibility(showLocation ? View.VISIBLE : View.GONE);
        }

        if (mConditionImage != null) {
            if (iconType == 0) {
                mConditionImage.setImageResource(R.drawable.weather_condition);
                mConditionImage.setColorFilter(iconColor, Mode.MULTIPLY);
            } else if (iconType == 1) {
                mConditionImage.setImageResource(R.drawable.weather_condition_color);
                mConditionImage.setColorFilter(null);
            } else {
                mConditionImage.setImageResource(R.drawable.weather_condition_vclouds);
                mConditionImage.setColorFilter(null);
            }
        }
        setTextColor(textColor);
        updateBackground();
    }

    private void updateBackground() {
        int bgColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_COLOR,
                0xff191919, UserHandle.USER_CURRENT);
        int bgPressedColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_PRESSED_COLOR,
                0xff323232, UserHandle.USER_CURRENT);

        ColorDrawable bgDrawable = new ColorDrawable(bgColor);
        ColorDrawable bgPresDrawable = new ColorDrawable(bgPressedColor);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, bgPresDrawable);
        states.addState(new int[] {}, bgDrawable);
        setBackground(states);
    }
}
