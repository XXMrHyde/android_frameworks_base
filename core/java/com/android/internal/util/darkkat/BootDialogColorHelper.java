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
import android.provider.Settings;

public class BootDialogColorHelper {

    private static final int APP_TEXT_COLOR_MODE_CUSTOM = 0;
    private static final int APP_TEXT_COLOR_MODE_RANDOM = 1;

    private static final int DARKKAT_BLUE_GREY =
            0xff1b1f23;
    private static final int WHITE =
            0xffffffff;
    private static final int MATERIAL_TEAL_500 =
            0xff009688;

    public static ColorStateList getBackgroundColorList(Context context) {
        return ColorStateList.valueOf(getBackgroundColor(context));
    }

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.BOOT_DIALOG_BACKGROUND_COLOR,
                DARKKAT_BLUE_GREY);
    }

    public static int getTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.BOOT_DIALOG_TEXT_COLOR, WHITE);
    }

    public static int getSecondaryTextColor(Context context) {
        return (179 << 24) | (getTextColor(context) & 0x00ffffff);
    }

    public static String getAppTextColor(Context context) {
        final int appTextColorMode = getAppTextColorMode(context);
        int color;
        if (appTextColorMode == APP_TEXT_COLOR_MODE_CUSTOM) {
            color = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BOOT_DIALOG_APP_TEXT_COLOR,
                    MATERIAL_TEAL_500);
        } else if (appTextColorMode == APP_TEXT_COLOR_MODE_RANDOM) {
            color = ColorHelper.getRandomColor();
        } else {
            color = getTextColor(context);
        }
        return ColorHelper.convertToColorHexString(color, false);
    }

    private static int getAppTextColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.BOOT_DIALOG_APP_TEXT_COLOR_MODE,
                APP_TEXT_COLOR_MODE_CUSTOM);
    }
}
