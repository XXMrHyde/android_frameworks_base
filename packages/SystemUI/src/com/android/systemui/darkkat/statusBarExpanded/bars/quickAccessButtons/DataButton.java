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
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;


public class DataButton extends QabButton {
    private static final Intent WIRELESS_SETTINGS = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

    private final DataCallback mDataCallback = new DataCallback();

    private final NetworkController mNetworkController;
    private final MobileDataController mMobileDataController;

    private boolean mEnabled;
    private boolean mAirplaneModeEnabled;

    public DataButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mNetworkController = mBar.getNetworkController();
        mMobileDataController = mNetworkController.getMobileDataController();
        mAirplaneModeEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        mEnabled = !mAirplaneModeEnabled && mMobileDataController.isMobileDataEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mNetworkController.addSignalCallback(mDataCallback);
        } else {
            mNetworkController.removeSignalCallback(mDataCallback);
        }
    }

    @Override
    public void handleClick() {
        if (!mAirplaneModeEnabled) {
            mMobileDataController.setMobileDataEnabled(!mEnabled);
        } else {
            mBar.startSettingsActivity(WIRELESS_SETTINGS);
        }
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$DataUsageSummaryActivity"));
        mBar.startSettingsActivity(intent);
    }

    private final class DataCallback extends SignalCallbackAdapter {
        @Override
        public void setMobileDataEnabled(boolean enabled) {
            mEnabled = !mAirplaneModeEnabled && enabled;
            updateState(mEnabled);
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mAirplaneModeEnabled = icon.visible;
            mEnabled = !mAirplaneModeEnabled && mMobileDataController.isMobileDataEnabled();
            updateState(mEnabled);
        }
    };
}
