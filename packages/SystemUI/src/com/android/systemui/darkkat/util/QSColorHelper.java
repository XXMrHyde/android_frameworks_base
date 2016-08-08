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

public class QSColorHelper extends StatusBarExpandedColorHelper {

    public static final int WHITE             = 0xffffffff;
    public static final int BLACK             = 0xff000000;

    public static ColorStateList getBackgroundColorStateList(Context context) {
        return ColorStateList.valueOf(getBackgroundColor(context));
    }

    public static ColorStateList getDndModePanelBackgroundColor(Context context) {
        int colorLight = BLACK;
        int colorDark = WHITE;
        if (ColorHelper.isColorDark(getBackgroundColor(context))) {
            return ColorStateList.valueOf(colorDark);
        } else {
            return ColorStateList.valueOf(colorLight);
        }
    }

    public static ColorStateList getAccentColorStateList(Context context) {
        return ColorStateList.valueOf(getAccentColor(context));
    }

    public static ColorStateList getDndModeButtonTextColors(Context context) {
        int states[][] = new int[][] {
            new int[] { android.R.attr.state_selected },
            new int[]{}
        };
        int colors[] = new int[] {
            getAccentColor(context),
            getTextColor(context)
        };

        return new ColorStateList(states, colors);
    }

    public static ColorStateList getIconColorStateList(Context context) {
        return ColorStateList.valueOf(getIconColor(context));
    }

    public static ColorStateList getDndModeConditionsIconColors(Context context) {
        int states[][] = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[]{}
        };
        int colors[] = new int[] {
            getAccentColor(context),
            getIconColor(context)
        };

        return new ColorStateList(states, colors);
    }

    public static ColorStateList getRippleColorStateList(Context context) {
        return ColorStateList.valueOf(getRippleColor(context));
    }
}
