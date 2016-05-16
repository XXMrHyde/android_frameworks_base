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

package com.android.internal.util.darkkat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;

import com.android.internal.util.darkkat.WeatherServiceController.DayForecast;
import com.android.internal.util.darkkat.WeatherServiceController.WeatherInfo;

public class WeatherHelper {
    public static final int ICON_MONOCHROME = 0;
    public static final int ICON_COLORED    = 1;
    public static final int ICON_VCLOUDS    = 2;

    public static final int PACKAGE_ENABLED  = 0;
    public static final int PACKAGE_DISABLED = 1;
    public static final int PACKAGE_MISSING  = 2;

    public static int getWeatherServiceAvailability(Context context) {
        boolean isInstalled = false;
        int availability = PACKAGE_MISSING;

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(WeatherServiceControllerImpl.PACKAGE_NAME,
                    PackageManager.GET_ACTIVITIES);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }

        if (isInstalled) {
            final int enabledState = pm.getApplicationEnabledSetting(
                    WeatherServiceControllerImpl.PACKAGE_NAME);
            if (enabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || enabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                availability = PACKAGE_DISABLED;
            } else {
                availability = PACKAGE_ENABLED;
            }
        }
        return availability;
    }

    public static boolean isWeatherServiceAvailable(Context context) {
        return getWeatherServiceAvailability(context)
                == PACKAGE_ENABLED;
    }

    public static Intent getWeatherServiceAppDetailSettingsIntent() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + WeatherServiceControllerImpl.PACKAGE_NAME));
        return i;
    }

    public static Intent getWeatherServiceSettingsIntent() {
        Intent settings = new Intent(Intent.ACTION_MAIN)
                .setClassName(WeatherServiceControllerImpl.PACKAGE_NAME,
                WeatherServiceControllerImpl.PACKAGE_NAME + ".SettingsActivity");
        return settings;
    }

    public static boolean showCurrent(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_CURRENT, 1) == 1;
    }

    public static int getIconType(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_TYPE,
                ICON_MONOCHROME);
    }

    public static Drawable getCurrentConditionDrawable(Context context, WeatherInfo info) {
        if (getIconType(context) == ICON_MONOCHROME) {
            return info.conditionDrawableMonochrome;
        } else if (getIconType(context) == ICON_COLORED) {
            return info.conditionDrawableColored;
        } else {
            return info.conditionDrawableVClouds;
        }
    }

    public static Drawable getForcastConditionDrawable(Context context, DayForecast forcast) {
        if (getIconType(context) == ICON_MONOCHROME) {
            return forcast.conditionDrawableMonochrome;
        } else if (getIconType(context) == ICON_COLORED) {
            return forcast.conditionDrawableColored;
        } else {
            return forcast.conditionDrawableVClouds;
        }
    }
}
