/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;
    private SettingsObserver mObserver;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private int mInetCondition = 0;
    private boolean mMobileVisible = false;
    private int mMobileStrengthId = 0, mMobileActivityId = 0, mMobileTypeId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileDescription, mMobileTypeDescription;

    private int mWifiIconNormalColor;
    private int mWifiIconConnectedColor;
    private int mWifiActivityNormalColor;
    private int mWifiActivityConnectedColor;
    private int mMobileNormalColor;
    private int mMobileConnectedColor;
    private int mMobileNetworkTypeNormalColor;
    private int mMobileNetworkTypeConnectedColor;
    private int mMobileActivityNormalColor;
    private int mMobileActivityConnectedColor;
    private int mAirplaneModeColor;

    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mMobile, mWifiActivity, mMobileActivity, mMobileType, mAirplane;
    View mSpacer;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_CONNECTED_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TYPE_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TYPE_CONNECTED_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_CONNECTED_COLOR),
                    false, this, UserHandle.USER_ALL);
           resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_AIRPLANE_MODE_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();
        mObserver = new SettingsObserver(mHandler);
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);

        mObserver.observe();
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileActivity = null;
        mMobileType     = null;
        mSpacer         = null;
        mAirplane       = null;

        mObserver.unobserve();

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int inetCondition, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mInetCondition = inetCondition;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        updateSettings();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon, int inetCondition, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription) {
        mMobileVisible = visible;
        mMobileStrengthId = strengthIcon;
        mInetCondition = inetCondition;
        mMobileActivityId = activityIcon;
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;

        updateSettings();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup != null && mMobileGroup.getContentDescription() != null)
            event.getText().add(mMobileGroup.getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
        }
        if (mWifiActivity != null) {
            mWifiActivity.setImageDrawable(null);
        }

        if (mMobile != null) {
            mMobile.setImageDrawable(null);
        }
        if (mMobileActivity != null) {
            mMobileActivity.setImageDrawable(null);
        }
        if (mMobileType != null) {
            mMobileType.setImageDrawable(null);
        }

        if(mAirplane != null) {
            mAirplane.setImageDrawable(null);
        }

        apply();
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifi.setImageResource(mWifiStrengthId);
            mWifi.setColorFilter(mInetCondition == 1
                    ? mWifiIconConnectedColor : mWifiIconNormalColor,
                    Mode.MULTIPLY);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiActivity.setColorFilter(mInetCondition == 1
                    ? mWifiActivityConnectedColor : mWifiActivityNormalColor,
                    Mode.MULTIPLY);

            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobile.setImageResource(mMobileStrengthId);
            mMobile.setColorFilter(mInetCondition == 1
                    ? mMobileConnectedColor : mMobileNormalColor,
                    Mode.MULTIPLY);
            mMobileActivity.setImageResource(mMobileActivityId);
            mMobileActivity.setColorFilter(mInetCondition == 1
                    ? mMobileActivityConnectedColor : mMobileActivityNormalColor,
                    Mode.MULTIPLY);
            mMobileType.setImageResource(mMobileTypeId);
            mMobileType.setColorFilter(mInetCondition == 1
                    ? mMobileNetworkTypeConnectedColor : mMobileNetworkTypeNormalColor,
                    Mode.MULTIPLY);

            mMobileGroup.setContentDescription(mMobileTypeDescription + " " + mMobileDescription);
            mMobileGroup.setVisibility(View.VISIBLE);
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setColorFilter(mAirplaneModeColor, Mode.MULTIPLY);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible && mIsAirplaneMode) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("mobile: %s sig=%d act=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId, mMobileActivityId, mMobileTypeId));

        mMobileType.setVisibility(
                !mWifiVisible ? View.VISIBLE : View.GONE);
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mWifiIconNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR,
                0xffff8800, UserHandle.USER_CURRENT);
        mWifiIconConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mWifiActivityNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR,
                0xff000000, UserHandle.USER_CURRENT);
        mWifiActivityConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR,
                0xff000000, UserHandle.USER_CURRENT);
        mMobileNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_NORMAL_COLOR,
                0xffff8800, UserHandle.USER_CURRENT);
        mMobileConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_CONNECTED_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mMobileNetworkTypeNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_TYPE_NORMAL_COLOR,
                0xffff8800, UserHandle.USER_CURRENT);
        mMobileNetworkTypeConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_TYPE_CONNECTED_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mMobileActivityNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_NORMAL_COLOR,
                0xff000000, UserHandle.USER_CURRENT);
        mMobileActivityConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_CONNECTED_COLOR,
                0xff000000, UserHandle.USER_CURRENT);
        mAirplaneModeColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_AIRPLANE_MODE_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);

        apply();
    }
}

