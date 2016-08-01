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

package com.android.systemui.darkkat.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.android.internal.util.darkkat.DeviceUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;

public class StatusBarNetworkNames extends TextView {
    private Context mContext;
    private final Resources mResources;

    private NetworkController mNetworkController;
    private final NetworkNamesCallback mNetworkNamesCallback = new NetworkNamesCallback();

    private final boolean mSupportsMobileData;

    private String mCarrierName = "";
    private String mWifiName = "";

    private final int mTextSizeSingleLine;
    private final int mTextSizeDualLine;

    private boolean mNoSimsVisible = false;
    private boolean mIsAirplaneMode = false;
    private boolean mUseCustomText = false;
    private boolean mWifiConnected = false;
    private boolean mWifiEnabled = false;

    private boolean mIsEmergencyCallCapable = true;

    private boolean mIsClockCentered = true;
    private boolean mShowCarrier = false;
    private boolean mShowWifi = false;

    public StatusBarNetworkNames(Context context) {
        this(context, null);
    }

    public StatusBarNetworkNames(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResources = mContext.getResources();

        mSupportsMobileData = DeviceUtils.deviceSupportsMobileData(mContext);

        mTextSizeSingleLine = mResources.getDimensionPixelSize(R.dimen.network_names_single_text_size);
        mTextSizeDualLine = mResources.getDimensionPixelSize(R.dimen.network_names_dual_text_size);
        mIsEmergencyCallCapable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mNetworkController != null) {
            mNetworkController.addSignalCallback(mNetworkNamesCallback);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mNetworkController != null) {
            mNetworkController.removeSignalCallback(mNetworkNamesCallback);
        }
    }

    public void setNetworkController(NetworkController nc) {
        mNetworkController = nc;
    }

    private void updateNames() {
        String carrierText = "";
        String wifiText = "";
        int textSize = mTextSizeSingleLine;

        if (mIsAirplaneMode) {
            carrierText = mResources.getString(R.string.airplane_mode);
        } else if (mNoSimsVisible) {
            carrierText = getNoSimsText();
        } else {
            carrierText = removeTrailingPeriod(mCarrierName);
        }
        if (carrierText == null || !mShowCarrier) {
            carrierText = "";
        }
        if (mWifiEnabled) {
            if (!mWifiConnected) {
                wifiText = mResources.getString(R.string.network_names_not_connected);
            } else {
               wifiText = removeDoubleQuotes(mWifiName);
            }
        } else {
            wifiText = mResources.getString(R.string.network_names_wifi_off);
        }
        if (wifiText == null || !mShowWifi) {
            wifiText = "";
        }

        if (!carrierText.isEmpty() && !wifiText.isEmpty()) {
            textSize = mTextSizeDualLine;
            setText(carrierText + "\n" + wifiText);
        } else if (!carrierText.isEmpty()) {
            setText(carrierText);
        } else if (!wifiText.isEmpty()) {
            setText(wifiText);
        } else {
            setText("");
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) textSize);
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private String getNoSimsText() {
        String textToUse;
        final String emergencyOnlyText = mResources.getString(R.string.quick_settings_rssi_emergency_only);
        final String missingSimsText = mResources.getString(R.string.keyguard_missing_sim_message_short);
        final String seperator = mResources.getString(com.android.internal.R.string.kg_text_message_separator);

        if (mIsEmergencyCallCapable) {
            if (mIsClockCentered) {
                textToUse = emergencyOnlyText;
            } else {
                textToUse = missingSimsText + seperator + emergencyOnlyText;
            }
        } else {
            textToUse = missingSimsText;
        }
        return textToUse;
    }

    public void setShowNames(boolean showCarrier, boolean showWifi) {
        mShowCarrier = showCarrier;
        mShowWifi = showWifi;
        updateNames();
    }

    public void setCenteredClock(boolean centered) {
        mIsClockCentered = centered;
        updateNames();
    }

    private final class NetworkNamesCallback extends SignalCallbackAdapter {
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mWifiEnabled = enabled;
            mWifiConnected = enabled && (statusIcon.icon > 0) && (description != null);
            mWifiName = removeDoubleQuotes(description);
            updateNames();
        }

        @Override
        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
                int qsType, boolean activityIn, boolean activityOut, String typeContentDescription,
                String description, boolean isWide, int subId) {
            if (statusIcon == null || !mSupportsMobileData) {
                return;
            }
            mCarrierName = description;
            updateNames();
        }

        @Override
        public void setNoSims(boolean show) {
            mNoSimsVisible = show;
            updateNames();
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            if (!mSupportsMobileData) {
                return;
            }
            mIsAirplaneMode = icon.visible;
            updateNames();
        }
    };
}

