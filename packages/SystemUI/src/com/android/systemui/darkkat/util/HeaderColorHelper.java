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

package com.android.systemui.darkkat.util;

import android.content.Context;
import android.content.res.ColorStateList;

import com.android.internal.util.darkkat.ColorHelper;
import com.android.internal.util.darkkat.StatusBarExpandedColorHelper;

public class HeaderColorHelper extends StatusBarExpandedColorHelper {

    private static final int SWITCH_TRACK_DISABLED_ALPHA = 77;
    private static final int TUNER_ICON_ALPHA            = 77;

    public static ColorStateList getBackgroundColorStateList(Context context) {
        return ColorStateList.valueOf(getHeaderBackgroundColor(context));
    }

    public static ColorStateList getAccentColorStateList(Context context) {
        return ColorStateList.valueOf(getHeaderAccentColor(context));
    }

    public static ColorStateList getDetailHeaderSwitchTrackColors(Context context) {
        int states[][] = new int[][] {
            new int[] { -android.R.attr.state_enabled },
            new int[] { android.R.attr.state_checked },
            new int[]{}
        };
        int colors[] = new int[] {
            (SWITCH_TRACK_DISABLED_ALPHA << 24)
            | (getHeaderIconColor(context) & 0x00ffffff),
            getHeaderAccentColor(context),
            getHeaderIconColor(context)
        };

        return new ColorStateList(states, colors);
    }

    public static ColorStateList getDetailHeaderSwitchThumbColors(Context context) {
        int states[][] = new int[][] {
            new int[] { -android.R.attr.state_enabled },
            new int[] { android.R.attr.state_checked },
            new int[]{}
        };
        int colors[] = new int[] {
            getHeaderIconColor(context),
            getHeaderAccentColor(context),
            getHeaderIconColor(context)
        };

        return new ColorStateList(states, colors);
    }

    public static ColorStateList getIconColorStateList(Context context) {
        return ColorStateList.valueOf(getHeaderIconColor(context));
    }

    public static ColorStateList getTunerIconColorStateList(Context context) {
        return ColorStateList.valueOf((TUNER_ICON_ALPHA << 24)
            | (getHeaderIconColor(context) & 0x00ffffff));
    }

    public static ColorStateList getRippleColorStateList(Context context) {
        return ColorStateList.valueOf(getHeaderRippleColor(context));
    }
}
