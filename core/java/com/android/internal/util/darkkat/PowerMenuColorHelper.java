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

import com.android.internal.R;

public class PowerMenuColorHelper {

    private static final int STATE_NORMAL            = 0;
    private static final int STATES_NORMAL_ENABLED   = 1;
    private static final int STATES_NORMAL_SELECTED  = 2;

    public static ColorStateList getBackgroundColorList(Context context) {
        return ColorStateList.valueOf(getBackgroundColor(context));
    }

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_BACKGROUND_COLOR,
                context.getResources().getColor(
                R.color.global_actions_background));
    }

    public static ColorStateList getIconColors(Context context, int iconStates) {
        if (iconStates == STATE_NORMAL) {
            return ColorStateList.valueOf(getIconNormalColor(context));
        } else {
            int state;
            if (iconStates == STATES_NORMAL_ENABLED) {
                state = R.attr.state_enabled;
            } else if (iconStates == STATES_NORMAL_SELECTED) {
                state = R.attr.state_selected;
            } else {
                state = R.attr.state_checked;
            }
            int states[][] = new int[][] {
                new int[] { state },
                new int[]{}
            };
            int colors[] = new int[] {
                getIconEnabledSelectedColor(context),
                getIconNormalColor(context)
            };
            return new ColorStateList(states, colors);
        }
    }

    public static int getIconNormalColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_ICON_NORMAL_COLOR,
                context.getResources().getColor(
                R.color.global_actions_icon_normal));
    }

    public static int getIconEnabledSelectedColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_ICON_ENABLED_SELECTED_COLOR,
                context.getResources().getColor(
                R.color.global_actions_icon_enabled_selected));
    }

    public static ColorStateList getRippleColorList(Context context) {
        return ColorStateList.valueOf(getRippleColor(context));
    }

    public static int getRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_RIPPLE_COLOR,
                context.getResources().getColor(
                R.color.global_actions_ripple));
        int colorToUse =  (51 << 24) | (color & 0x00ffffff);
        return colorToUse;
    }

    public static int getTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_TEXT_COLOR,
                context.getResources().getColor(
                R.color.global_actions_text));
    }

    public static int getSecondaryTextColor(Context context) {
        return (179 << 24) | (getTextColor(context) & 0x00ffffff);
    }
}
