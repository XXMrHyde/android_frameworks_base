/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.cm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class WeatherControllerImpl implements WeatherController {

    private static final String TAG = WeatherController.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final ComponentName COMPONENT_WEATHER_FORECAST = new ComponentName(
            "com.cyanogenmod.lockclock", "com.cyanogenmod.lockclock.weather.ForecastActivity");
    public static final String ACTION_UPDATE_FINISHED
            = "com.cyanogenmod.lockclock.action.WEATHER_UPDATE_FINISHED";
    public static final String EXTRA_UPDATE_CANCELLED = "update_cancelled";
    public static final String ACTION_FORCE_WEATHER_UPDATE
            = "com.cyanogenmod.lockclock.action.FORCE_WEATHER_UPDATE";
    public static final Uri CURRENT_WEATHER_URI
            = Uri.parse("content://com.cyanogenmod.lockclock.weather.provider/weather");
    public static final String[] WEATHER_PROJECTION = new String[]{
            "city",
            "wind",
            "condition_code",
            "temperature",
            "humidity",
            "condition",
            "time_stamp",
            "forecast_low",
            "forecast_high",
            "forecast_condition",
            "forecast_condition_code"

    };
    public static final String LOCK_CLOCK_PACKAGE_NAME = "com.cyanogenmod.lockclock";

    private static final int WEATHER_ICON_MONOCHROME = 0;
    private static final int WEATHER_ICON_COLORED    = 1;
    private static final int WEATHER_ICON_VCLOUDS    = 2;

    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private final Receiver mReceiver = new Receiver();
    private final Context mContext;

    private WeatherInfo mCachedInfo = new WeatherInfo();

    public WeatherControllerImpl(Context context) {
        mContext = context;
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        queryWeather();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_FINISHED);
        mContext.registerReceiver(mReceiver, filter);
    }

    public void addCallback(Callback callback) {
        if (callback == null || mCallbacks.contains(callback)) return;
        if (DEBUG) Log.d(TAG, "addCallback " + callback);
        mCallbacks.add(callback);
        callback.onWeatherChanged(mCachedInfo); // immediately update with current values
    }

    public void removeCallback(Callback callback) {
        if (callback == null) return;
        if (DEBUG) Log.d(TAG, "removeCallback " + callback);
        mCallbacks.remove(callback);
    }

    private Drawable getIcon(int conditionCode) {
        return getIcon(conditionCode, 0 /* Monochrome icon */);
    }

    private Drawable getIcon(int conditionCode, int iconNameValue) {
        String iconName;

        if (iconNameValue == WEATHER_ICON_MONOCHROME) {
            iconName = "weather_";
        } else if (iconNameValue == WEATHER_ICON_COLORED) {
            iconName = "weather_color_";
        } else {
            iconName = "weather_vclouds_";
        }

        try {
            Resources resources =
                    mContext.createPackageContext(LOCK_CLOCK_PACKAGE_NAME, 0).getResources();
            return resources.getDrawable(resources.getIdentifier(iconName + conditionCode,
                    "drawable", LOCK_CLOCK_PACKAGE_NAME));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public WeatherInfo getWeatherInfo() {
        return mCachedInfo;
    }

    private void queryWeather() {
        Cursor c = mContext.getContentResolver().query(CURRENT_WEATHER_URI, WEATHER_PROJECTION,
                null, null, null);
        if (c == null) {
            if(DEBUG) Log.e(TAG, "cursor was null for temperature, forcing weather update");
            mContext.sendBroadcast(new Intent(ACTION_FORCE_WEATHER_UPDATE));
        } else {
            try {
                c.moveToFirst();
                mCachedInfo.city = c.getString(0);
                mCachedInfo.wind = c.getString(1);
                mCachedInfo.conditionCode = c.getInt(2);
                mCachedInfo.conditionDrawable = getIcon(mCachedInfo.conditionCode);
                mCachedInfo.conditionDrawableMonochrome =
                        getIcon(mCachedInfo.conditionCode, WEATHER_ICON_MONOCHROME);
                mCachedInfo.conditionDrawableColored =
                        getIcon(mCachedInfo.conditionCode, WEATHER_ICON_COLORED);
                mCachedInfo.conditionDrawableVClouds =
                        getIcon(mCachedInfo.conditionCode, WEATHER_ICON_VCLOUDS);
                mCachedInfo.temp = c.getString(3);
                mCachedInfo.humidity = c.getString(4);
                mCachedInfo.condition = c.getString(5);
                mCachedInfo.timeStamp = c.getString(6);
                c.moveToNext();
                mCachedInfo.forecasts.clear();
                DayForecast day1 = new DayForecast(c.getString(7), c.getString(8), c.getString(9), c.getInt(10),
                        getIcon(c.getInt(10)), getIcon(c.getInt(10), WEATHER_ICON_MONOCHROME),
                        getIcon(c.getInt(10), WEATHER_ICON_COLORED), getIcon(c.getInt(10), WEATHER_ICON_VCLOUDS));
                mCachedInfo.forecasts.add(day1);
                c.moveToNext();
                DayForecast day2 = new DayForecast(c.getString(7), c.getString(8), c.getString(9), c.getInt(10),
                        getIcon(c.getInt(10)), getIcon(c.getInt(10), WEATHER_ICON_MONOCHROME),
                        getIcon(c.getInt(10), WEATHER_ICON_COLORED), getIcon(c.getInt(10), WEATHER_ICON_VCLOUDS));
                mCachedInfo.forecasts.add(day2);
                c.moveToNext();
                DayForecast day3 = new DayForecast(c.getString(7), c.getString(8), c.getString(9), c.getInt(10),
                        getIcon(c.getInt(10)), getIcon(c.getInt(10), WEATHER_ICON_MONOCHROME),
                        getIcon(c.getInt(10), WEATHER_ICON_COLORED), getIcon(c.getInt(10), WEATHER_ICON_VCLOUDS));
                mCachedInfo.forecasts.add(day3);
                c.moveToNext();
                DayForecast day4 = new DayForecast(c.getString(7), c.getString(8), c.getString(9), c.getInt(10),
                        getIcon(c.getInt(10)), getIcon(c.getInt(10), WEATHER_ICON_MONOCHROME),
                        getIcon(c.getInt(10), WEATHER_ICON_COLORED), getIcon(c.getInt(10), WEATHER_ICON_VCLOUDS));
                mCachedInfo.forecasts.add(day4);
                c.moveToNext();
                DayForecast day5 = new DayForecast(c.getString(7), c.getString(8), c.getString(9), c.getInt(10),
                        getIcon(c.getInt(10)), getIcon(c.getInt(10), WEATHER_ICON_MONOCHROME),
                        getIcon(c.getInt(10), WEATHER_ICON_COLORED), getIcon(c.getInt(10), WEATHER_ICON_VCLOUDS));
                mCachedInfo.forecasts.add(day5);
            } finally {
                c.close();
            }
        }
    }

    private void fireCallback() {
        for (Callback callback : mCallbacks) {
            callback.onWeatherChanged(mCachedInfo);
        }
    }

    private final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "onReceive " + intent.getAction());
            if (intent.hasExtra(EXTRA_UPDATE_CANCELLED)) {
                if (intent.getBooleanExtra(EXTRA_UPDATE_CANCELLED, false)) {
                    // no update
                    return;
                }
            }
            queryWeather();
            fireCallback();
        }
    }

    @Override
    public void updateWeather() {
        queryWeather();
        fireCallback();
    }
}
