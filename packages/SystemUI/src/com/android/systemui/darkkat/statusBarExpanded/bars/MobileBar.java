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
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.R;
import com.android.systemui.darkkat.NetworkTrafficController;
import com.android.systemui.darkkat.statusBarExpanded.bars.BarNetworkTraffic;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;


public class MobileBar extends LinearLayout {
    private final Context mContext;

    private final MobileSignalCallback mMobileSignalCallback = new MobileSignalCallback();
    private NetworkController mNetworkController;

    private ImageView mMobileStrengthIcon;
    private TextView mPrimaryText;
    private TextView mSecondaryText;
    private BarNetworkTraffic mNetworkTraffic;

    private int mMobileStrengthIconId = 0;
    private String mDescription = null;
    private String mType = null;
    private String mTypeContentDescription = null;
    private String mSeparator;

    private boolean mMobileNetworkEnabled = false;
    private boolean mMobileDataEnabled = false;
    private boolean mIsAirPlaneMode = false;
    private boolean mIsNoSims = false;
    private boolean mWifiConnected = false;
    private boolean mListening = false;

    public MobileBar(Context context) {
        this(context, null);
    }

    public MobileBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMobileStrengthIcon = (ImageView) findViewById(R.id.mobile_bar_strength_icon);
        mPrimaryText = (TextView) findViewById(R.id.mobile_bar_primary_text);
        mSecondaryText = (TextView) findViewById(R.id.mobile_bar_secondary_text);
        mNetworkTraffic = (BarNetworkTraffic) findViewById(R.id.expanded_bars_network_traffic_container);
        mSeparator = getResources().getString(
                com.android.internal.R.string.kg_text_message_separator);
    }

    public void setNetworkController(NetworkController nc) {
        mNetworkController = nc;
    }

    public void setNetworkTrafficController(NetworkTrafficController ntc) {
        mNetworkTraffic.setNetworkTrafficController(ntc);
    }

    public void setListening(boolean listening) {
        mNetworkTraffic.setListening(listening && !mWifiConnected && mMobileDataEnabled);
        if (mNetworkController == null || mListening == listening) {
            return;
        }
        mListening = listening;
        if (mListening) {
            mNetworkController.addSignalCallback(mMobileSignalCallback);
        } else {
            mNetworkController.removeSignalCallback(mMobileSignalCallback);
        }
    }

    private void updateViews() {
        final Resources res = mContext.getResources();
        if (mIsNoSims) {
            mDescription = res.getString(
                        R.string.quick_settings_rssi_emergency_only);
            mType = res.getString(
                        R.string.keyguard_missing_sim_message_short);
        } else if (mIsAirPlaneMode) {
            mType = res.getString(
                        R.string.accessibility_quick_settings_airplane_on);
        }
        if (mMobileStrengthIconId <= 0) {
            mMobileStrengthIconId = R.drawable.ic_qs_signal_disabled;
        }
        if (mDescription == null || mDescription.isEmpty()) {
            mDescription = res.getString(com.android.internal.R.string.lockscreen_carrier_default);
        }
        if (mType == null || mType.isEmpty()) {
            mType = res.getString(R.string.accessibility_no_data);
        }
        mMobileStrengthIcon.setImageResource(mMobileStrengthIconId);
        mPrimaryText.setText(mDescription);
        mSecondaryText.setText(mType);
    }

    public void setBitByte(int bitByte) {
        mNetworkTraffic.setBitByte(bitByte);
    }

    public void setIconColor() {
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        mMobileStrengthIcon.setImageTintList(ColorStateList.valueOf(iconColor));
        mNetworkTraffic.setIconColor(iconColor);
    }

    public void setTextColor() {
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mPrimaryText.setTextColor(textColorPrimary);
        mSecondaryText.setTextColor(textColorSecondary);
        mNetworkTraffic.setTextColors(textColorPrimary, textColorSecondary);
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private final class MobileSignalCallback extends SignalCallbackAdapter {
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mWifiConnected = enabled && (qsIcon.icon > 0) && (description != null);
            if (mWifiConnected || !mMobileDataEnabled) {
                if (mTypeContentDescription != null && !mTypeContentDescription.isEmpty()) {
                    mType = mTypeContentDescription + mSeparator
                            + mContext.getResources().getString(R.string.accessibility_no_data);
                }
            }
            mNetworkTraffic.setListening(mListening && !mWifiConnected && mMobileDataEnabled);
            updateViews();
        }

        @Override
        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
                int qsType, boolean activityIn, boolean activityOut, String typeContentDescription,
                String description, boolean isWide, int subId) {
            if (qsIcon == null) {
                return;
            }
            mMobileNetworkEnabled = qsIcon.visible;
            mMobileStrengthIconId = qsIcon.icon;
            mMobileDataEnabled = mMobileNetworkEnabled && qsType > 0;
            mDescription = removeTrailingPeriod(description);
            mTypeContentDescription = typeContentDescription;
            mType = typeContentDescription;

            if (mWifiConnected || !mMobileDataEnabled) {
                if (typeContentDescription != null && !typeContentDescription.isEmpty()) {
                    mType = typeContentDescription + mSeparator
                            + mContext.getResources().getString(R.string.accessibility_no_data);
                }
            }
            mNetworkTraffic.setListening(mListening && !mWifiConnected && mMobileDataEnabled);
            updateViews();
        }

        @Override
        public void setNoSims(boolean show) {
            mIsNoSims = show;
            updateViews();
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mIsAirPlaneMode = icon.visible;
            updateViews();
        }
    };
}
