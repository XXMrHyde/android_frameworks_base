/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.telephony.SignalStrength;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

public class SignalClusterTextView extends LinearLayout implements
        NetworkController.NetworkSignalChangedCallback,
        NetworkController.SignalStrengthChangedCallback {

    private boolean mAirplaneMode;
    private int mInetCondition = 0;
    private int mDBm = 0;
    private int mSignalClusterStyle = SignalClusterView.STYLE_NORMAL;

    private TextView mMobileSignalText;
    private ImageView mMobileSignalTextIcon;

    public SignalClusterTextView(Context context) {
        this(context, null);
    }

    public SignalClusterTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mMobileSignalText = (TextView) findViewById(R.id.mobile_signal_text);
        mMobileSignalTextIcon = (ImageView) findViewById(R.id.mobile_signal_text_icon);
        updateSignalText();
        updateColors();
    }

    public void setStyle(int style) {
        mSignalClusterStyle = style;
        updateSignalText();
    }

    private String getSignalLevelString(int dBm) {
        if (dBm == 0 || dBm == SignalStrength.INVALID) {
            return "-\u221e"; // -oo ('minus infinity')
        }
        return Integer.toString(dBm);
    }

    private void updateSignalText() {
        if (mMobileSignalText == null) {
            return;
        }
        if (mAirplaneMode || mDBm == 0) {
            setVisibility(View.GONE);
        } else if (mSignalClusterStyle == SignalClusterView.STYLE_TEXT) {
            setVisibility(View.VISIBLE);
            mMobileSignalText.setText(getSignalLevelString(mDBm));
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId, int inetCondition,
            boolean activityIn, boolean activityOut,
            String wifiSignalContentDescriptionId, String description) {
        mInetCondition = inetCondition;
        updateColors();
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId, int inetCondition,
            String mobileSignalContentDescriptionId, int dataTypeIconId,
            boolean activityIn, boolean activityOut,
            String dataTypeContentDescriptionId, String description) {
        mInetCondition = inetCondition;
        updateColors();
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
        mAirplaneMode = enabled;
        updateSignalText();
        updateColors();
    }

    @Override
    public void onPhoneSignalStrengthChanged(int dbm) {
        mDBm = dbm;
        updateSignalText();
    }

    public void updateColors() {
        ContentResolver resolver = mContext.getContentResolver();

        int defaultColor = 0xffffffff;
        int defaultTextColor = mContext.getResources().getColor(
                    R.color.status_bar_clock_color);
        int normalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ICONS_NORMAL_COLOR,
                defaultColor, UserHandle.USER_CURRENT);
        int fullyColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ICONS_FULLY_COLOR,
                defaultColor, UserHandle.USER_CURRENT);
        int currentStateColor = mInetCondition == 1 ?
                fullyColor : normalColor;

        if (mMobileSignalText != null) {
            mMobileSignalText.setTextColor(currentStateColor == defaultColor ?
                    defaultTextColor : currentStateColor);
        }
        if (mMobileSignalTextIcon != null) {
            if (currentStateColor == defaultColor) {
                mMobileSignalTextIcon.setColorFilter(null);
            } else {
                mMobileSignalTextIcon.setColorFilter(currentStateColor, Mode.MULTIPLY);
            }
        }
    }
}
