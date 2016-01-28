/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.darkkat.statusBarExpanded.bars.quickAccessButtons;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.FlashlightController;

public class TorchButton extends QabButton implements
        FlashlightController.FlashlightListener {

    private final FlashlightController mFlashlightController;
    private boolean mFlashlightAvailable;
    private boolean mOn = false;


    public TorchButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mFlashlightController = mBar.getFlashlightController();
        mFlashlightAvailable = mFlashlightController.isAvailable();
        if (mFlashlightAvailable) {
            mOn = mFlashlightController.isEnabled();
        }
        updateState(mOn);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mFlashlightController.addListener(this);
        } else {
            mFlashlightController.removeListener(this);
        }
    }

    @Override
    public void handleClick() {
        if (mFlashlightAvailable) {
            mFlashlightController.setFlashlight(!mOn);
        }
    }

    @Override
    public void onFlashlightChanged(boolean on) {
        mOn = on;
        updateState(mOn);
    }

    @Override
    public void onFlashlightError() {
        updateState(false);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        mFlashlightAvailable = available;
    }
}
