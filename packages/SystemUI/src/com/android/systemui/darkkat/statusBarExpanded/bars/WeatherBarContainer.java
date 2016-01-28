/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.darkkat.statusBarExpanded.bars;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.cm.WeatherController.DayForecast;
import com.android.internal.util.cm.WeatherControllerImpl;
import com.android.internal.util.darkkat.SBEPanelColorHelper;
import com.android.internal.util.darkkat.SBEPanelWeatherHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherBarContainer extends FrameLayout implements
        WeatherController.Callback {

    private final Context mContext;
    private PhoneStatusBar mStatusBar;
    private final WeatherControllerImpl mWeatherController;

    private LinearLayout mWeatherBar;
    private TextView mNoWeather;

    private boolean mWeatherAvailable = false;
    private boolean mListening = false;

    public WeatherBarContainer(Context context) {
        this(context, null);
    }

    public WeatherBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mWeatherController = new WeatherControllerImpl(mContext);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNoWeather = (TextView) findViewById(R.id.weather_bar_no_weather);
        mWeatherBar = 
                (LinearLayout) findViewById(R.id.status_bar_expanded_weather_bar);
        mWeatherBar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                startForecastActivity();
                return true;
            }
        });
    }

    public void setStatusBar(PhoneStatusBar statusBar) {
        mStatusBar = statusBar;
    }

    public void setListening(boolean listening) {
        if (!isLockClockInstalled()) {
            return;
        }
        if (listening && !mListening) {
            mListening = true;
            mWeatherController.addCallback(this);
        }
    }

    public void updateClickTargets(boolean clickable) {
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            mWeatherAvailable = true;
        } else {
            mWeatherAvailable = false;
        }
        updateWeather(info);
        if (mWeatherAvailable) {
            createItems(info);
        }
    }

    private void updateWeather(WeatherController.WeatherInfo info) {
        if (!mWeatherAvailable) {
            mNoWeather.setVisibility(View.VISIBLE);
        } else {
            mNoWeather.setVisibility(View.GONE);
        }
    }

    private void createItems(WeatherController.WeatherInfo info) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TimeZone myTimezone = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(myTimezone);
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mWeatherBar.removeAllViews();
        mNoWeather.setTextColor(textColorSecondary);

        boolean isToday = false;
        if (SBEPanelWeatherHelper.showCurrent(mContext)) {
            View currentItem = inflater.inflate(R.layout.weather_bar_current_item, null);

            TextView updateTime = (TextView) currentItem.findViewById(R.id.weather_update_time);
            updateTime.setText(getUpdateTime(info));
            updateTime.setTextColor(textColorPrimary);
            calendar.roll(Calendar.DAY_OF_WEEK, true);

            ImageView currentImage = (ImageView) currentItem.findViewById(R.id.weather_image);
            currentImage.setImageDrawable(
                    SBEPanelWeatherHelper.getCurrentConditionDrawable(mContext, info));
            if (SBEPanelWeatherHelper.getIconType(mContext) == 0) {
                currentImage.setColorFilter(iconColor, Mode.MULTIPLY);
            }
            TextView temp = (TextView) currentItem.findViewById(R.id.weather_temp);
            temp.setText(info.temp);
            temp.setTextColor(textColorPrimary);

            mWeatherBar.addView(currentItem,
                  new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            isToday = true;
        }

        ArrayList<DayForecast> forecasts = info.forecasts;

        for (DayForecast d : forecasts) {
            if (!isToday) {
                View forecastItem = inflater.inflate(R.layout.weather_bar_forecast_item, null);

                TextView day = (TextView) forecastItem.findViewById(R.id.forecast_day);
                day.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
                day.setTextColor(textColorPrimary);
                calendar.roll(Calendar.DAY_OF_WEEK, true);

                ImageView image = (ImageView) forecastItem.findViewById(R.id.weather_image);
                image.setImageDrawable(SBEPanelWeatherHelper.getForcastConditionDrawable(mContext, d));
                if (SBEPanelWeatherHelper.getIconType(mContext) == 0) {
                    image.setColorFilter(iconColor, Mode.MULTIPLY);
                }
                TextView temps = (TextView) forecastItem.findViewById(R.id.forecast_temps);
                temps.setText(isToday ? info.temp : d.low + " | " + d.high);
                temps.setTextColor(isToday ? textColorPrimary : textColorSecondary);

                mWeatherBar.addView(forecastItem,
                      new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            } else {
                isToday = false;
            }
        }
    }

    public void updateItems() {
        if (mWeatherAvailable) {
            createItems(mWeatherController.getWeatherInfo());
        }
    }

    public void setRippleColor() {
        RippleDrawable background =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_rectangle).mutate();
        final int color = SBEPanelColorHelper.getRippleColor(mContext);
        background.setColor(ColorStateList.valueOf(color));
        mWeatherBar.setBackground(background);
    }

    private void startForecastActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(WeatherControllerImpl.COMPONENT_WEATHER_FORECAST);
        mStatusBar.startActivity(intent, true /* dismissShade */);
    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private String getUpdateTime(WeatherController.WeatherInfo info) {
        if (info.timeStamp != null) {
            Date lastUpdate = new Date(info.timeStamp);
            StringBuilder sb = new StringBuilder();
            sb.append(DateFormat.getTimeFormat(mContext).format(lastUpdate));
            return sb.toString();
        } else {
            String empty = "";
            return empty;
        }
    }

    private boolean isLockClockInstalled() {
        PackageManager pm = mContext.getPackageManager();
        boolean installed = false;
        try {
           pm.getPackageInfo("com.cyanogenmod.lockclock", PackageManager.GET_ACTIVITIES);
           installed = true;
        } catch (PackageManager.NameNotFoundException e) {
           installed = false;
        }
        return installed;
    }
}
