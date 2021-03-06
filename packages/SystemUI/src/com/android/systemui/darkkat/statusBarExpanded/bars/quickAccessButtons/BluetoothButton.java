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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BluetoothController;


public class BluetoothButton extends QabButton implements
        BluetoothController.Callback {
    private static final Intent BLUETOOTH_SETTINGS = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);

    private final BluetoothController mBluetoothController;

    private boolean mEnabled;

    public BluetoothButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mBluetoothController = mBar.getBluetoothController();
        mEnabled = mBluetoothController.isBluetoothEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mBluetoothController.addStateChangedCallback(this);
        } else {
            mBluetoothController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mBluetoothController.setBluetoothEnabled(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(BLUETOOTH_SETTINGS);
    }

    @Override
    public void onBluetoothStateChange(boolean enabled) {
        mEnabled = enabled;
        updateState(enabled);
    }

    @Override
    public void onBluetoothDevicesChanged() {
    }
}
