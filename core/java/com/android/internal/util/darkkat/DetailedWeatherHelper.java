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
    public static final String DAY_INDEX = "day_index";

    public static final int THEME_MATERIAL       = 0;
    public static final int THEME_MATERIAL_LIGHT = 1;
    public static final int THEME_DARKKAT        = 2;
    public static final int THEME_DARKKAT_BLUE   = 3;

    public static final int MATERIAL_DEEP_TEAL_500 = 0xff009688;
    public static final int MATERIAL_DEEP_TEAL_200 = 0xff80cbc4;
    public static final int MATERIAL_BLUE_700      = 0xff1976d2;
    public static final int MATERIAL_BLUE_500      = 0xff2196f3;
    public static final int MATERIAL_GREY_850      = 0xff303030;
    public static final int MATERIAL_GREY_800      = 0xff424242;
    public static final int DARKKAT_BLUE_GREY      = 0xff1b1f23;
    public static final int DARKKAT_BLUE_BLUE_GREY = 0xff1f2429;
    public static final int MATERIAL_GREY_50       = 0xfffafafa;
    public static final int WHITE                  = 0xffffffff;
    public static final int HOLO_BLUE_LIGHT        = 0xff33b5e5;
    public static final int BLACK                  = 0xff000000;

    public static final int ACCENT_ALPHA                        = 255;
    public static final int BACKGROUND_ALPHA                    = 255;
    public static final int PRIMARY_TEXT_MATERIAL_ALPHA         = 255;
    public static final int SECONDARY_TEXT_MATERIAL_ALPHA       = 179;
    public static final int PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA   = 222;
    public static final int SECONDARY_TEXT_MATERIAL_LIGHT_ALPHA = 138;
    public static final int ICON_MATERIAL_ALPHA                 = 179;
    public static final int ICON_MATERIAL_LIGHT_ALPHA           = 138;
    public static final int ICON_DARKKAT_ALPHA                  = 255;
    public static final int RIPPLE_MATERIAL_ALPHA               = 51;
    public static final int RIPPLE_MATERIAL_LIGHT_ALPHA         = 31;
    public static final int RIPPLE_MATERIAL_COLORED_ALPHA       = 66;
    public static final int DIVIDER_MATERIAL_ALPHA              = 51;
    public static final int DIVIDER_MATERIAL_LIGHT_ALPHA        = 31;
    public static final int DIVIDER_MATERIAL_COLORED_ALPHA      = 66;

    public static final int INDEX_ACCENT_COLOR       = 0;
    public static final int INDEX_CONTENT_BG_COLOR   = 1;
    public static final int INDEX_CARDS_BG_COLOR     = 2;
    public static final int INDEX_CARDS_TEXT_COLOR   = 3;
    public static final int INDEX_CARDS_ICON_COLOR   = 4;
    public static final int INDEX_AB_RIPPLE_COLOR    = 5;
    public static final int INDEX_CARDS_RIPPLE_COLOR = 6;

    public static int[][] DEFAULT_COLORS = {
        { MATERIAL_DEEP_TEAL_200,
          MATERIAL_GREY_850,
          MATERIAL_GREY_800,
          WHITE,
          (ICON_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff),
          (RIPPLE_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff),
          (RIPPLE_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff) },
        { MATERIAL_DEEP_TEAL_500,
          MATERIAL_GREY_50,
          WHITE,
          (PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff),
          (ICON_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff),
          (RIPPLE_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff),
          (RIPPLE_MATERIAL_LIGHT_ALPHA << 24) | (BLACK & 0x00ffffff) },
        { MATERIAL_DEEP_TEAL_500,
          DARKKAT_BLUE_GREY,
          DARKKAT_BLUE_BLUE_GREY,
          WHITE,
          WHITE,
          (RIPPLE_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff),
          (RIPPLE_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff) },
        { MATERIAL_DEEP_TEAL_500,
          DARKKAT_BLUE_GREY,
          DARKKAT_BLUE_BLUE_GREY,
          HOLO_BLUE_LIGHT,
          HOLO_BLUE_LIGHT,
          (RIPPLE_MATERIAL_ALPHA << 24) | (WHITE & 0x00ffffff),
          (RIPPLE_MATERIAL_COLORED_ALPHA << 24) | (HOLO_BLUE_LIGHT & 0x00ffffff) }
    };

    public static boolean showLocation(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_SHOW_LOCATION, 1) == 1;
    }

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

    public static int getAccentColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACCENT_COLOR, 
                DEFAULT_COLORS[getTheme(context)][INDEX_ACCENT_COLOR]);
        return (ACCENT_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getStatusBarBgColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_STATUS_BAR_BG_COLOR, MATERIAL_BLUE_700);
        return (BACKGROUND_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getActionBarBgColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_BG_COLOR, MATERIAL_BLUE_500);
        return (BACKGROUND_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getContentBgColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CONTENT_BG_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CONTENT_BG_COLOR]);
        return (BACKGROUND_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getCardsBgColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CARDS_BG_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CARDS_BG_COLOR]);
        return (BACKGROUND_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getActionBarTextColor(Context context, boolean isPrimary) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_TEXT_COLOR, WHITE);

        int alpha;
        if (isPrimary) {
            alpha = PRIMARY_TEXT_MATERIAL_ALPHA;
        } else {
            alpha = SECONDARY_TEXT_MATERIAL_ALPHA;
        }

        return (alpha << 24) | (color & 0x00ffffff);
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

    public static int getCardsTextColor(Context context, boolean isPrimary) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CARDS_TEXT_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CARDS_TEXT_COLOR]);
        return (getTextColorAlpha(context, isPrimary) << 24) | (color & 0x00ffffff);
    }

    public static int getActionBarIconColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_ICON_COLOR, WHITE);
        return (PRIMARY_TEXT_MATERIAL_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getConditionImageColor(Context context) {
        if (getConditionIconType(context) != WeatherHelper.ICON_MONOCHROME) {
            return 0;
        } else {
            return getCardsIconColor(context);
        }
    }

    public static int getCardsIconColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CARDS_ICON_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CARDS_ICON_COLOR]);

        int alpha;
        if (getTheme(context) == THEME_MATERIAL) {
            alpha = ICON_MATERIAL_ALPHA;
        } else if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            alpha = ICON_MATERIAL_LIGHT_ALPHA;
        } else {
            alpha = ICON_DARKKAT_ALPHA;
        }

        return (alpha << 24) | (color & 0x00ffffff);
    }

    public static int getActionBarRippleColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_ACTION_BAR_RIPPLE_COLOR, WHITE);
        return (RIPPLE_MATERIAL_ALPHA << 24) | (color & 0x00ffffff);
    }

    public static int getCardsRippleColor(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DETAILED_WEATHER_CARDS_RIPPLE_COLOR,
                DEFAULT_COLORS[getTheme(context)][INDEX_CARDS_RIPPLE_COLOR]);

        int alpha;
        if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            alpha = RIPPLE_MATERIAL_LIGHT_ALPHA;
        } else if (getTheme(context) == THEME_DARKKAT_BLUE) {
            alpha = RIPPLE_MATERIAL_COLORED_ALPHA;
        } else {
            alpha = RIPPLE_MATERIAL_ALPHA;
        }

        return (alpha << 24) | (color & 0x00ffffff);
    }

    public static int getDividerAlpha(Context context) {
        int alpha;
        if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            alpha = DIVIDER_MATERIAL_LIGHT_ALPHA;
        } else if (getTheme(context) == THEME_DARKKAT_BLUE) {
            alpha = DIVIDER_MATERIAL_COLORED_ALPHA;
        } else {
            alpha = DIVIDER_MATERIAL_ALPHA;
        }

        return alpha;
    }

    private static int getTextColorAlpha(Context context, boolean isPrimary) {
        if (getTheme(context) == THEME_MATERIAL_LIGHT) {
            return isPrimary ? PRIMARY_TEXT_MATERIAL_LIGHT_ALPHA
                    : SECONDARY_TEXT_MATERIAL_LIGHT_ALPHA;
        } else {
            return isPrimary ? PRIMARY_TEXT_MATERIAL_ALPHA
                    : SECONDARY_TEXT_MATERIAL_ALPHA;
        }
    }
}
