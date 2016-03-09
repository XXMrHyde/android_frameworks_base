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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.provider.Settings;

public class StatusBarColorHelper {
    private static final int WHITE =
            0xffffffff;
    private static final int BLACK =
            0xff000000;
    private static final int MATERIAL_TEAL_500 =
            0xff009688;

    public static int getUserIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_MULTI_USER_SWITCH_ICON_COLOR, WHITE);
    }

    private static int getActiveUserTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_MULTI_USER_SWITCH_ACTIVE_TEXT_COLOR,
                MATERIAL_TEAL_500);
    }

    private static int getInactiveUserTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_MULTI_USER_SWITCH_INACTIVE_TEXT_COLOR,
                WHITE);
    }

    public static ColorStateList getUserTextColors(Context context) {
        int states[][] = new int[][] {
            new int[] { com.android.internal.R.attr.state_activated },
            new int[]{}
        };
        int colors[] = new int[] {
            getActiveUserTextColor(context),
            getInactiveUserTextColor(context)
        };
        return new ColorStateList(states, colors);
    }

    public static int getGreetingColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_GREETING_COLOR, WHITE);
    }

    public static int getGreetingColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_GREETING_COLOR_DARK_MODE, BLACK);
    }

    public static int getCarrierLabelColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_LABEL_COLOR, WHITE);
    }

    public static int getCarrierLabelColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_LABEL_COLOR_DARK_MODE, BLACK);
    }

    public static int getBatteryColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_BATTERY_COLOR, WHITE);
    }

    public static int getBatteryColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_BATTERY_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getBatteryTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR, WHITE);
    }

    public static int getBatteryTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getClockColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_DATE_COLOR, WHITE);
    }

    public static int getClockColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_DATE_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getWeatherTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_TEXT_COLOR, WHITE);
    }

    public static int getWeatherTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_TEXT_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getWeatherIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_ICON_COLOR, WHITE);
    }

    public static int getWeatherIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_ICON_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNetworkTrafficTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TEXT_COLOR, WHITE);
    }

    public static int getNetworkTrafficTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TEXT_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNetworkTrafficIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ICON_COLOR, WHITE);
    }

    public static int getNetworkTrafficIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ICON_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNetworkSignalColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_SIGNAL_COLOR, WHITE);
    }

    public static int getNetworkSignalColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_SIGNAL_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNoSimColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_NO_SIM_COLOR, WHITE);
    }

    public static int getNoSimColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_NO_SIM_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getAirplaneModeColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_AIRPLANE_MODE_COLOR, WHITE);
    }

    public static int getAirplaneModeColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_AIRPLANE_MODE_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getStatusIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_STATUS_ICONS_COLOR, WHITE);
    }

    public static int getStatusIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_STATUS_ICONS_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNotificationIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_ICONS_COLOR, WHITE);
    }

    public static int getNotificationIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_ICONS_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getTickerTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_TICKER_TEXT_COLOR, WHITE);
    }

    public static int getTickerTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_TICKER_TEXT_COLOR_DARK_MODE,
                BLACK);
    }
}
