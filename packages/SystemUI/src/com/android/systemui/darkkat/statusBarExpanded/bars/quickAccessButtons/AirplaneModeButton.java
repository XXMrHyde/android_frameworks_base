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
import android.net.ConnectivityManager;
import android.provider.Settings;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;


public class AirplaneModeButton extends QabButton {
    private static final Intent WIRELESS_SETTINGS = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

    private final NetworkController mNetworkController;
    private final AirplaneModeCallback mAirplaneModeCallback = new AirplaneModeCallback();

    private boolean mEnabled;

    public AirplaneModeButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        mNetworkController = mBar.getNetworkController();
        mEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mNetworkController.addSignalCallback(mAirplaneModeCallback);
        } else {
            mNetworkController.removeSignalCallback(mAirplaneModeCallback);
        }
    }

    @Override
    public void handleClick() {
        final ConnectivityManager mgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mgr.setAirplaneMode(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(WIRELESS_SETTINGS);
    }

    private final class AirplaneModeCallback extends SignalCallbackAdapter {
        @Override
        public void setIsAirplaneMode(IconState icon) {
            mEnabled = icon.visible;
            updateState(mEnabled);
        }
    };
}
