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

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.darkkat.ColorHelper;

public class NotificationColorHelper {
    final static int DARKKAT_BLUE_GREY      = 0xff1b1f23;
    final static int WHITE                  = 0xffffffff;
    final static int TRANSLUCENT_WHITE      = 0x4dffffff;
    final static int DEFAULT_MEDIA_BG       = 0xff424242;
    final static int ACTION_LIST_BG_GREY    = 0xcc808080;
    final static int ACTION_LIST_BG_DARK    = 0xcc111111;
    final static int ACTION_LIST_BG_LIGHT   = 0xcceeeeee;
    final static int SPEED_BUMP_LINE_DARK   = 0x6f222222;
    final static int SPEED_BUMP_LINE_LIGHT  = 0x6fdddddd;
    final static int ACTION_DIVIDER_LIGHT   = 0x31000000;
    final static int ACTION_DIVIDER_DARK    = 0x31ffffff;

    public static int getNotificationMediaBgColor(Context context, int bgColor) {
        if (getMediaBgMode(context) == 0) {
            return bgColor;
        } else if (getMediaBgMode(context) == 1) {
            return bgColor != DEFAULT_MEDIA_BG ? bgColor
                    : getCustomNotificationBgColor(context);
        } else {
            return getCustomNotificationBgColor(context);
        }
    }

    public static int getAppIconBgColor(Context context, int notificationColor) {
        if (colorizeIconBackground(context, notificationColor)) {
           return (255 << 24) | (getCustomAppIconBgColor(context) & 0x00ffffff);
        } else if (notificationColor != Notification.COLOR_DEFAULT) {
            return notificationColor;
        } else {
            if (ColorHelper.isColorDark(getCustomNotificationBgColor(context))) {
                return Notification.COLOR_DEFAULT;
            } else {
                return Color.BLACK;
            }
        }
    }

    public static int getAppIconBgAlpha(Context context, int notificationColor) {
        if (colorizeIconBackground(context, notificationColor)) {
            return Color.alpha(getCustomAppIconBgColor(context));
        } else if (notificationColor != Notification.COLOR_DEFAULT) {
            return 255;
        } else {
            return 77;
        }
    }

    public static int getRippleColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_RIPPLE_COLOR, WHITE);
        return (51 << 24) | (color & 0x00ffffff);
    }

    public static int getIconColor(Context context, Drawable icon) {
        if (colorizeIcon(context, icon)) {
           return getCustomIconColor(context);
        } else {
            return 0;
        }
    }

    public static int getActionListLayoutBgColor(int iconColor, int textColor) {
        int color;
        if (ColorHelper.isColorDark(iconColor) != ColorHelper.isColorDark(textColor)) {
            color = ACTION_LIST_BG_GREY;
        } else {
            color = ColorHelper.isColorDark(iconColor)
                    ? ACTION_LIST_BG_LIGHT : ACTION_LIST_BG_DARK;
        }
        return color;
    }
    public static int getSpeedBumpLineColor(Context context) {
        if (ColorHelper.isColorDark(getCustomNotificationBgColor(context))) {
            return SPEED_BUMP_LINE_DARK;
        } else {
            return SPEED_BUMP_LINE_LIGHT;
        }
    }

    public static int getActionDividerColor(int bgColor) {
        if (ColorHelper.isColorDark(bgColor)) {
            return ACTION_DIVIDER_DARK;
        } else {
            return ACTION_DIVIDER_LIGHT;
        }
    }

    private static boolean colorizeIconBackground(Context context, int notificationColor) {
        final int appIconBgMode = getAppIconBgMode(context);
        if (appIconBgMode == 0) {
            return false;
        } else if (appIconBgMode == 1) {
            return notificationColor == Notification.COLOR_DEFAULT;
        } else {
            return true;
        }
    }

    public static boolean colorizeIcon(Context context, Drawable d) {
        if (d == null) {
            return false;
        }

        NotificationColorUtil cu = NotificationColorUtil.getInstance(context);
        final int iconColorMode = getIconColorMode(context);
        final boolean isGreyscale = cu.isGrayscaleIcon(d);

        if (iconColorMode == 0) {
            return false;
        } else if (iconColorMode == 1) {
            return isGreyscale;
        } else {
            return true;
        }
    }

    private static int getMediaBgMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_MEDIA_BG_MODE, 0);
    }

    private static int getAppIconBgMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_APP_ICON_BG_MODE, 0);
    }

    private static int getIconColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, 0);
    }

    public static int getCustomNotificationBgColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_BG_COLOR, DARKKAT_BLUE_GREY);
    }

    private static int getCustomAppIconBgColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_APP_ICON_BG_COLOR, TRANSLUCENT_WHITE);
    }

    public static int getCustomIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_ICON_COLOR, WHITE);
    }

    public static int getCustomTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_TEXT_COLOR, WHITE);
    }
}
