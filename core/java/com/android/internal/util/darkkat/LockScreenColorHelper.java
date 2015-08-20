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

import com.android.internal.util.NotificationColorUtil;

public class LockScreenColorHelper {

    private static int WHITE = 0xffffffff;

    public static int getIconColor(Context context, Drawable d) {
        if (colorizeIcon(context, d)) {
           return getCustomIconColor(context);
        } else {
            return 0;
        }
    }

    public static int getRippleColor(Context context, Drawable d) {
        final int iconColorMode = getIconColorMode(context);
        final int rippleColorMode = getRippleColorMode(context);
        final boolean isGreyscale = isGrayscaleIcon(context, d);

        if (rippleColorMode == 0) {
            if (iconColorMode == 0) {
                return isGreyscale ? WHITE : 0; 
            } else if (iconColorMode == 1) {
                return isGreyscale ? getCustomIconColor(context) : 0;
            } else {
                return getCustomIconColor(context);
            }
        } else if (rippleColorMode == 1) {
            return getCustomRippleColor(context);
        } else {
            return WHITE;
        }
    }

    private static boolean colorizeIcon(Context context, Drawable d) {
        if (d == null) {
            return false;
        }

        final int iconColorMode = getIconColorMode(context);
        final boolean isGreyscale = isGrayscaleIcon(context, d);

        if (iconColorMode == 0) {
            return false;
        } else if (iconColorMode == 1) {
            return isGreyscale;
        } else {
            return true;
        }
    }

    public static boolean isGrayscaleIcon(Context context, Drawable d) {
        NotificationColorUtil cu = NotificationColorUtil.getInstance(context);
        return cu.isGrayscaleIcon(d);
    }

    public static int getIconColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTONS_BAR_ICON_COLOR_MODE, 0);
    }

    private static int getRippleColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTONS_BAR_RIPPLE_COLOR_MODE, 2);
    }

    private static int getCustomIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_SCREEN_ICON_COLOR, WHITE);
    }

    private static int getCustomRippleColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTONS_BAR_RIPPLE_COLOR, WHITE);
    }
}
