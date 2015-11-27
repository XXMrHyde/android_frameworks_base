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

package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.systemui.R;

import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;

public class StatusBarCarrierText extends TextView {

    private NetworkControllerImpl mNetworkController;
    private final CarrierTextCallback mCarrierTextCallback = new CarrierTextCallback();

    private boolean mNoSimsVisible = false;
    private boolean mIsAirplaneMode = false;
    private boolean mUseCustomText = false;

    private boolean mIsClockCentered = false;
    private boolean mIsEmergencyCallCapable = true;


    private String mCarrierText = "";
    private String mCustomText = "";

    private Context mContext;

    public StatusBarCarrierText(Context context) {
        this(context, null);
    }

    public StatusBarCarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mIsEmergencyCallCapable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
        updateCarrierTextSettings();
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        mNetworkController = nc;
        mNetworkController.addSignalCallback(mCarrierTextCallback);
    }

    public void updateCarrierText() {
        String text;

        if (mUseCustomText && !mCustomText.isEmpty()) {
            text = mCustomText;
        } else {
            text = removeTrailingPeriod(mCarrierText);
        }
        if (mIsAirplaneMode) {
            text = mContext.getString(R.string.airplane_mode);
        } else if (mNoSimsVisible) {
            text = getNoSimsText();
        }
        if (text == null) {
            setText("");
        } else {
            setText(text);
        }
    }

    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private String getNoSimsText() {
        String textToUse;
        final String emergencyOnlyText = mContext.getString(R.string.quick_settings_rssi_emergency_only);
        final String missingSimsText = mContext.getString(R.string.keyguard_missing_sim_message_short);
        final String seperator = mContext.getString(com.android.internal.R.string.kg_text_message_separator);

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

    public void updateCarrierTextSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mUseCustomText = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CARRIER_LABEL_USE_CUSTOM, 0) == 1;
        mCustomText = Settings.System.getString(resolver,
                Settings.System.STATUS_BAR_CARRIER_LABEL_CUSTOM_LABEL);
        if (mCustomText == null || mCustomText.isEmpty()) {
            mCustomText = mContext.getResources().getString(
                    com.android.internal.R.string.default_custom_label);
        }
    }

    public void setCenteredClock(boolean isCentered) {
        mIsClockCentered = isCentered;
        updateCarrierText();
    }

    private final class CarrierTextCallback extends SignalCallbackAdapter {
        @Override
        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
                int qsType, boolean activityIn, boolean activityOut, String typeContentDescription,
                String description, boolean isWide, int subId) {
            mCarrierText = description;
            updateCarrierText();
        }

        @Override
        public void setNoSims(boolean show) {
            mNoSimsVisible = show;
            updateCarrierText();
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mIsAirplaneMode = icon.visible;
            updateCarrierText();
        }
    };
}
