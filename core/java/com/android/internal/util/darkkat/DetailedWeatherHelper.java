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
import android.graphics.Color;
import android.provider.Settings;

public class DetailedWeatherHelper {
    public static final int THEME_MATERIAL       = 0;
    public static final int THEME_DARKKAT        = 1;
    public static final int THEME_MATERIAL_LIGHT = 2;

    public static final int MATERIAL_BLUE_700 = 0xff1976d2;
    public static final int MATERIAL_BLUE_500 = 0xff2196f3;
    public static final int MATERIAL_GREY_850 = 0xff303030;
    public static final int DARKKAT_BLUE_GREY = 0xff1b1f23;
    public static final int MATERIAL_GREY_50  = 0xfffafafa;
    public static final int White             = 0xffffffff;
    public static final int HOLO_BLUE_LIGHT   = 0xff33b5e5;
    public static final int BLACK             = 0xff000000;

    public static final int PRIMARY_TEXT_MATERIAL_ALPHA         = 255;
    public static final int SECONDARY_TEXT_MATERIAL_ALPHA       = 179;
    public static final int PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA   = 222;
    public static final int SECONDARY_TEXT_MATERIAL_LIGHT_ALPHA = 138;
    public static final int ICON_MATERIAL_ALPHA                 = 255;
    public static final int ICON_MATERIAL_LIGHT_ALPHA           = 138;

    public static final int INDEX_CONTENT_BG_COLOR      = 0;
    public static final int INDEX_CONTENT_TEXT_COLOR    = 1;
    public static final int INDEX_CONDITION_IMAGE_COLOR = 2;

    public static int[][] DEFAULT_COLORS = {
        { MATERIAL_GREY_850,
          White,
          White },
        { DARKKAT_BLUE_GREY,
          HOLO_BLUE_LIGHT,
          HOLO_BLUE_LIGHT },
        { MATERIAL_GREY_50,
          (PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff),
          (ICON_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff) }
    };

    public static int getTheme(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_THEME, THEME_MATERIAL);
    }

    public static int getConditionIconType(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CONDITION_ICON, WeatherHelper.ICON_MONOCHROME);
    }

    public static boolean customizeColors(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CUSTOMIZE_COLORS, 0) == 1;
    }

    public static int getStatusBarBgColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_STATUS_BAR_BG_COLOR, MATERIAL_BLUE_700);
    }

    public static int getActionBarBgColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_BG_COLOR, MATERIAL_BLUE_500);
    }

    public static int getContentBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CONTENT_BG_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CONTENT_BG_COLOR]);
    }

    public static int getActionBarTextColor(Context context, boolean isPrimary) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_TEXT_COLOR, White);
        int colorToUse;
        if (isPrimary) {
            colorToUse = (PRIMARY_TEXT_MATERIAL_ALPHA << 24) | (color & 0x00ffffff);
        } else {
            colorToUse = (SECONDARY_TEXT_MATERIAL_ALPHA << 24) | (color & 0x00ffffff);
        }
        return colorToUse;
    }

    public static ColorStateList getActionBarTabTextColors(Context context) {
        int states[][] = new int[][] {
            new int[] { com.android.internal.R.attr.state_selected },
            new int[]{}
        };
        int colors[] = new int[] {
            getActionBarTextColor(context, true),
            getActionBarTextColor(context, false)
        };

        return new ColorStateList(states, colors);
    }

    public static int getContentTextColor(Context context, boolean isPrimary) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CONTENT_TEXT_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CONTENT_TEXT_COLOR]);
        int colorToUse;
        if (isPrimary) {
            colorToUse = (getTextColorPrimaryAlpha(context) << 24) | (color & 0x00ffffff);
        } else {
            colorToUse = (getTextColorSecondaryAlpha(context) << 24) | (color & 0x00ffffff);
        }
        return colorToUse;
    }

    public static int getActionBarIconColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_ICON_COLOR, White);

        return (ICON_MATERIAL_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getConditionImageColor(Context context) {
        if (getConditionIconType(context) != WeatherHelper.ICON_MONOCHROME) {
            return 0;
        } else {
            final int color = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.DETAILED_WEATHER_CONDITION_IMAGE_COLOR,
                    DEFAULT_COLORS[getTheme(context)][INDEX_CONDITION_IMAGE_COLOR]);
            int alpha;
            if (getTheme(context) == THEME_MATERIAL_LIGHT) {
                alpha = ICON_MATERIAL_LIGHT_ALPHA;
            } else {
                alpha = ICON_MATERIAL_ALPHA;
            }
            return (alpha << 24) | (color & 0x00ffffff);
        }
    }

    private static int getTextColorPrimaryAlpha(Context context) {
        if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            return PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA;
        } else {
            return PRIMARY_TEXT_MATERIAL_ALPHA;
        }
    }

    private static int getTextColorSecondaryAlpha(Context context) {
        if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            return SECONDARY_TEXT_MATERIAL_LIGHT_ALPHA;
        } else {
            return SECONDARY_TEXT_MATERIAL_ALPHA;
        }
    }
}
