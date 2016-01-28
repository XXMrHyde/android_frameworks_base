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

package com.android.systemui.darkkat.statusBarExpanded.bars;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;

public class BrightnessSliderBar extends LinearLayout {
    private final Context mContext;

    private BrightnessController mBrightnessController;

    public BrightnessSliderBar(Context context) {
        this(context, null);
    }

    public BrightnessSliderBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ImageView brightnessIcon = (ImageView) findViewById(R.id.brightness_bar_mode_icon);
        mBrightnessController = new BrightnessController(mContext,
                brightnessIcon,
                (ToggleSlider) findViewById(R.id.brightness_bar_slider));

    }

    public void setListening(boolean listening) {
        if (mBrightnessController == null) {
            return;
        }
        if (listening) {
            mBrightnessController.registerCallbacks();
        } else {
            mBrightnessController.unregisterCallbacks();
        }
    }

    public void setIconColor() {
        if (mBrightnessController == null) {
            return;
        }
        mBrightnessController.setIconColor();
    }

    public void setRippleColor() {
        if (mBrightnessController == null) {
            return;
        }
        mBrightnessController.setRippleColor();
    }
}
