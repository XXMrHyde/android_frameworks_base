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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;


public class WifiBar extends LinearLayout {
    private final Context mContext;

    private final WifiSignalCallback mWifiSignalCallback = new WifiSignalCallback();
    private NetworkController mNetworkController;
    private WifiManager mWifiManager;

    private ImageView mStrengthIcon;
    private TextView mPrimaryText;
    private TextView mSecondaryText;

    private boolean mWifiEnabled = false;
    private boolean mWifiConnected = false;
    private boolean mIsAirPlaneMode = false;

    public WifiBar(Context context) {
        this(context, null);
    }

    public WifiBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mStrengthIcon = (ImageView) findViewById(R.id.wifi_bar_strength_icon);
        mPrimaryText = (TextView) findViewById(R.id.wifi_bar_primary_text);
        mSecondaryText = (TextView) findViewById(R.id.wifi_bar_secondary_text);
    }

    public void setNetworkController(NetworkController nc) {
        mNetworkController = nc;
    }

    public void setListening(boolean listening) {
        if (mNetworkController == null) {
            return;
        }
        if (listening) {
            mNetworkController.addSignalCallback(mWifiSignalCallback);
        } else {
            mNetworkController.removeSignalCallback(mWifiSignalCallback);
        }
    }

    private void updateWifiText(String description) {
        final Resources res = mContext.getResources();
        String name = "";
        String linkSpeed = "";
        if (mWifiEnabled) {
            if (mWifiConnected) {
                name = removeDoubleQuotes(description);
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    linkSpeed = wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
                }
            } else {
                name = res.getString(R.string.accessibility_no_wifi);
            }
        } else {
            name = res.getString(R.string.accessibility_wifi_off);
        }
        mPrimaryText.setText(name);
        if (mIsAirPlaneMode && !mWifiConnected) {
            linkSpeed = res.getString(
                        R.string.accessibility_quick_settings_airplane_on);
        }
        if (linkSpeed.isEmpty()) {
            mSecondaryText.setVisibility(View.GONE);
        } else {
            mSecondaryText.setVisibility(View.VISIBLE);
        }
        mSecondaryText.setText(linkSpeed);
    }

    public void setIconColor() {
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        mStrengthIcon.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    public void setTextColor() {
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mPrimaryText.setTextColor(textColorPrimary);
        mSecondaryText.setTextColor(textColorSecondary);
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private final class WifiSignalCallback extends SignalCallbackAdapter {
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mWifiEnabled = enabled;
            mWifiConnected = enabled && (qsIcon.icon > 0) && (description != null);
            final int strengthIconId = qsIcon.icon;
            mStrengthIcon.setImageResource(strengthIconId);
            updateWifiText(description);
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mIsAirPlaneMode = icon.visible;
        }

    };
}
