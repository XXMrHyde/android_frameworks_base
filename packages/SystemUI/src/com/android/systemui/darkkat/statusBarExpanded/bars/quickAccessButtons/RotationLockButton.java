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
 * limitations under the License.
 */

package com.android.systemui.darkkat.statusBarExpanded.bars.quickAccessButtons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.RotationLockController;

public class RotationLockButton extends QabButton implements
        RotationLockController.RotationLockControllerCallback {
    private static final Intent DISPLAY_SETTINGS = new Intent(Settings.ACTION_DISPLAY_SETTINGS);

    private final RotationLockController mRotationLockController;

    private boolean mEnabled;

    public RotationLockButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mRotationLockController = mBar.getRotationLockController();
        mEnabled = !mRotationLockController.isRotationLocked();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mRotationLockController.addRotationLockControllerCallback(this);
        } else {
            mRotationLockController.removeRotationLockControllerCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mRotationLockController.setRotationLocked(mEnabled);
    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(DISPLAY_SETTINGS);
    }

    @Override
    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        mEnabled = !rotationLocked;
        updateState(mEnabled);
    }
}
