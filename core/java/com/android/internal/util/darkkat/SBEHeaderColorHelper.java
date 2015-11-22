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
import android.provider.Settings;

public class SBEHeaderColorHelper {

    private static final int SYSTEMUI_SECONDARY = 0xff384248;
    private static final int WHITE = 0xffffffff;

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_BG_COLOR,
                SYSTEMUI_SECONDARY);
    }

    public static int getIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR, WHITE);
    }

    public static int getNoSimIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_NO_SIM_COLOR, WHITE);
    }

    public static int getAirplaneModeIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_AIRPLANE_MODE_COLOR,
                WHITE);
    }

    public static int getRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_RIPPLE_COLOR, WHITE);
        int colorToUse =  (51 << 24) | (color & 0x00ffffff);
        return colorToUse;
    }

    public static int getTextColor(Context context, int alpha) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR, WHITE);
        int colorToUse =  (alpha << 24) | (color & 0x00ffffff);
        return colorToUse;
    }
}
