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
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.internal.util.cm.WeatherController.DayForecast;
import com.android.internal.util.cm.WeatherController.WeatherInfo;

public class SBEPanelWeatherHelper {

    private static final int ICON_MONOCHROME = 0;
    private static final int ICON_COLORED    = 1;
    private static final int ICON_VCLOUDS    = 2;

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
