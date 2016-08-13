/*
* Copyright (C) 2016 DarkKat
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
import android.provider.Settings;

import com.android.internal.R;

public class StatusBarExpandedColorHelper {

    public static final int SYSTEMUI_SECONDARY     = 0xff384248;
    public static final int DARKKAT_BLUE_BLUE_GREY = 0xff1F2429;
    public static final int DEEP_TEAL_200          = 0xff80CBC4;
    public static final int DEEP_TEAL_500          = 0xff009688;
    public static final int WHITE                  = 0xffffffff;
    public static final int HOLO_BLUE_LIGHT        = 0xff33b5e5;
    public static final int SYSTEMUI_PRIMARY       = 0xff263238;
    public static final int DARKKAT_BLUE_GREY      = 0xff1b1f23;

    private static final int FULLY_OPAQUE_ALPHA = 255;
    private static final int RIPPLE_ALPHA       = 51;

    public static int getHeaderBackgroundColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_BACKGROUND_COLOR, SYSTEMUI_SECONDARY);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getHeaderAccentColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_ACCENT_COLOR, DEEP_TEAL_200);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getHeaderTextColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_TEXT_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getHeaderIconColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_ICON_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getHeaderRippleColor(Context context) {
        int color = color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_RIPPLE_COLOR, WHITE);
        return (RIPPLE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getHeaderBatteryTextColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HEADER_BATTERY_TEXT_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getQSBackgroundColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BACKGROUND_COLOR, SYSTEMUI_PRIMARY);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getQSAccentColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_ACCENT_COLOR, DEEP_TEAL_200);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getQSTextColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_TEXT_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getQSIconColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_ICON_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getQSRippleColor(Context context) {
        int color = color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_RIPPLE_COLOR, WHITE);
        return (RIPPLE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getNoNotificationsTextColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.NO_NOTIFICATIONS_TEXT_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getNotificationsDismissAllIconColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DISMISS_ALL_NOTIFICATIONS_ICON_COLOR, WHITE);
        return (FULLY_OPAQUE_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getNotificationsDismissAllRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DISMISS_ALL_NOTIFICATIONS_RIPPLE_COLOR, WHITE);
        return (RIPPLE_ALPHA << 24) | (color & 0x00ffffff);
    }
}
