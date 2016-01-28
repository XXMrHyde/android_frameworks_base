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
import android.net.wifi.WifiManager;
import android.provider.Settings;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;


public class WifiButton extends QabButton {
    private static final Intent WIFI_SETTINGS = new Intent(Settings.ACTION_WIFI_SETTINGS);

    private final WifiSignalCallback mWifiSignalCallback = new WifiSignalCallback();

    private final NetworkController mNetworkController;

    private boolean mEnabled;

    public WifiButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mNetworkController = mBar.getNetworkController();
        final WifiManager wifiManager = (WifiManager)  mContext.getSystemService(Context.WIFI_SERVICE);
        mEnabled = wifiManager.isWifiEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mNetworkController.addSignalCallback(mWifiSignalCallback);
        } else {
            mNetworkController.removeSignalCallback(mWifiSignalCallback);
        }
    }

    @Override
    public void handleClick() {
        mNetworkController.setWifiEnabled(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(WIFI_SETTINGS);
    }

    private final class WifiSignalCallback extends SignalCallbackAdapter {
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mEnabled = enabled;
            updateState(mEnabled);
        }
    };
}
