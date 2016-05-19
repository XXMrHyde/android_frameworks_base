/*
 * Copyright (C) 2016 The OmniROM Project
 *
 * Copyright (C) 2016 DarkKat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.android.internal.util.darkkat;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeatherServiceControllerImpl implements WeatherServiceController {
    private static final String TAG = "WeatherService:WeatherServiceController";
    public static final String PACKAGE_NAME = "net.darkkatroms.weather";

    public static final ComponentName COMPONENT_WEATHER_FORECAST = new ComponentName(
            "net.darkkatroms.weather", "net.darkkatroms.weather.ForecastActivity");

    private static final Uri WEATHER_URI
            = Uri.parse("content://net.darkkatroms.weather.provider/weather");
    private static final Uri SETTINGS_URI
            = Uri.parse("content://net.darkkatroms.weather.provider/settings");
    private static final String[] WEATHER_PROJECTION = new String[] {
            "city",
            "wind_speed",
            "wind_direction",
            "condition_code",
            "temperature",
            "humidity",
            "condition",
            "forecast_low",
            "forecast_high",
            "forecast_condition",
            "forecast_condition_code",
            "time_stamp",
    };
    private static final String[] SETTINGS_PROJECTION = new String[] {
            "enabled",
            "units"
    };

    private static final boolean DEBUG = false;

    private final Context mContext;
    private final Handler mHandler;
    private final ContentResolver mResolver;
    private final WeatherObserver mWeatherObserver;

    private ArrayList<Callback> mCallbacks;
    private WeatherInfo mCachedInfo;

    private static final DecimalFormat mNoDigitsFormat = new DecimalFormat("0");
    private boolean mMetric;

    public WeatherServiceControllerImpl(Context context) {
        mContext = context;
        mHandler = new Handler();
        mResolver = mContext.getContentResolver();
        mWeatherObserver = new WeatherObserver(mHandler);

        mCallbacks = new ArrayList<Callback>();
        mCachedInfo = new WeatherInfo();

        if (WeatherHelper.isWeatherServiceAvailable(mContext)) {
            Intent updateIntent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(PACKAGE_NAME, PACKAGE_NAME + ".WeatherService");
            updateIntent.setAction(PACKAGE_NAME + ".ACTION_UPDATE");
            updateIntent.putExtra("force", true);
            mContext.startService(updateIntent);
            mWeatherObserver.observe();
            queryWeather();
        }
    }

    @Override
    public void addCallback(Callback callback) {
        if (callback == null || mCallbacks.contains(callback)) return;
        if (DEBUG) Log.d(TAG, "addCallback " + callback);
        mCallbacks.add(callback);
        callback.onWeatherChanged(mCachedInfo); // immediately update with current values
    }

    @Override
    public void removeCallback(Callback callback) {
        if (callback == null) return;
        if (DEBUG) Log.d(TAG, "removeCallback " + callback);
        mCallbacks.remove(callback);
    }

    @Override
    public void updateWeather() {
        queryWeather();
        fireCallback();
    }

    @Override
    public WeatherInfo getWeatherInfo() {
        return mCachedInfo;
    }

    public void queryWeather() {
        if (!isServiceEnabled()) {
            return;
        }
        Cursor c = mContext.getContentResolver().query(WEATHER_URI, WEATHER_PROJECTION,
                null, null, null);
        if (c != null) {
            try {
                if (c.getCount() > 0) {
                    List<DayForecast> forecastList = new ArrayList<DayForecast>();
                    for (int i = 0; i < 6; i++) {
                        c.moveToPosition(i);
                        if (i == 0) {
                            mCachedInfo.city = c.getString(0);
                            mCachedInfo.wind = c.getString(1);
                            mCachedInfo.conditionCode = c.getInt(3);
                            mCachedInfo.conditionDrawableMonochrome = getIcon(mCachedInfo.conditionCode,
                                    WeatherHelper.ICON_MONOCHROME);
                            mCachedInfo.conditionDrawableColored = getIcon(mCachedInfo.conditionCode,
                                    WeatherHelper.ICON_COLORED);
                            mCachedInfo.conditionDrawableVClouds = getIcon(mCachedInfo.conditionCode,
                                    WeatherHelper.ICON_VCLOUDS);
                            mCachedInfo.temp = c.getString(4);
                            mCachedInfo.humidity = c.getString(5);
                            mCachedInfo.condition = c.getString(6);
                            mCachedInfo.timeStamp = c.getString(11);
                        } else {
                            DayForecast day = new DayForecast();
                            day.low = c.getString(7);
                            day.high = c.getString(8);
                            day.condition = c.getString(9);
                            day.conditionCode = c.getInt(10);
                            day.conditionDrawableMonochrome = getIcon(day.conditionCode,
                                    WeatherHelper.ICON_MONOCHROME);
                            day.conditionDrawableColored = getIcon(day.conditionCode,
                                    WeatherHelper.ICON_COLORED);
                            day.conditionDrawableVClouds = getIcon(day.conditionCode,
                                    WeatherHelper.ICON_VCLOUDS);
                            forecastList.add(day);
                        }
                    }
                    mCachedInfo.forecasts = forecastList;
                }
            } finally {
                c.close();
            }
        }
        if (DEBUG) Log.d(TAG, "queryWeather " + mCachedInfo);
    }

    public boolean isServiceEnabled() {
        if (!WeatherHelper.isWeatherServiceAvailable(mContext)) {
            return false;
        }
        final Cursor c = mContext.getContentResolver().query(SETTINGS_URI, SETTINGS_PROJECTION,
                null, null, null);
        if (c != null) {
            int count = c.getCount();
            if (count == 1) {
                c.moveToPosition(0);
                boolean enabled = c.getInt(0) == 1;
                return enabled;
            }
        }
        return true;
    }

    private Drawable getIcon(int conditionCode, int iconNameValue) {
        String iconName;

        if (iconNameValue == WeatherHelper.ICON_MONOCHROME) {
            iconName = "weather_";
        } else if (iconNameValue == WeatherHelper.ICON_COLORED) {
            iconName = "weather_color_";
        } else {
            iconName = "weather_vclouds_";
        }

        try {
            Resources resources =
                    mContext.createPackageContext(PACKAGE_NAME, 0).getResources();
            return resources.getDrawable(resources.getIdentifier(iconName + conditionCode,
                    "drawable", PACKAGE_NAME));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void fireCallback() {
        for (Callback callback : mCallbacks) {
            callback.onWeatherChanged(mCachedInfo);
        }
    }

    class WeatherObserver extends ContentObserver {
        WeatherObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(WEATHER_URI, false, this);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            queryWeather();
            fireCallback();
        }
    }
}
