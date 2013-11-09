/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (c) 2012-2013 The Linux Foundation. All rights reserved.
 *
 * Not a Contribution.
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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.statusbar.policy.NetworkController;

import com.android.systemui.R;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;
    private SettingsObserver mObserver;

    private static final int SIGNAL_CLUSTER_STYLE_NORMAL = 0;

    private int mSignalClusterStyle;
    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mIsThemeDefaultEnabled = true;
    private boolean mIsSystemDefaultEnabled = false;
    private int mWifiIconNormalColor = 0xffaaaaaa;
    private int mWifiIconConnectedColor = 0xff33b5e5;
    private int mWifiActivityNormalColor = 0xffaaaaaa;
    private int mWifiActivityConnectedColor = 0xff33b5e5;
    private int mMobileNormalColor = 0xffaaaaaa;
    private int mMobileConnectedColor = 0xff33b5e5;
    private int mMobileNetworkTypeNormalColor = 0xffaaaaaa;
    private int mMobileNetworkTypeConnectedColor = 0xff33b5e5;
    private int mMobileActivityNormalColor = 0xffaaaaaa;
    private int mMobileActivityConnectedColor = 0xff33b5e5;

    private boolean mMobileVisible = false;
    private int mMobileStrengthId = 0, mMobileActivityId = 0;
    private int mMobileTypeId = 0, mNoSimIconId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private boolean mEtherVisible = false;
    private int mEtherIconId = 0;
    private String mWifiDescription, mMobileDescription, mMobileTypeDescription, mEtherDescription;

    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mMobile, mWifiActivity, mMobileActivity, mMobileType, mAirplane, mEther, mNoSimSlot;
    View mSpacer;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_TEXT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_WIFI_STYLE_ENABLE_DEFAULTS), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_NORMAL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_CONNECTED_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TYPE_NORMAL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TYPE_CONNECTED_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_NORMAL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_CONNECTED_COLOR), false, this);
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
        if (DEBUG) Slog.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mObserver.observe();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mNoSimSlot      = (ImageView) findViewById(R.id.no_sim);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        mEther          = (ImageView) findViewById(R.id.ethernet);

        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        mObserver.unobserve();

        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileActivity = null;
        mMobileType     = null;
        mNoSimSlot      = null;
        mSpacer         = null;
        mAirplane       = null;
        mEther          = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon) {
        mMobileVisible = visible;
        mMobileStrengthId = strengthIcon;
        mMobileActivityId = activityIcon;
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId = noSimIcon;

        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    @Override
    public void setEtherIndicators(boolean visible, int etherIcon, String contentDescription) {
        mEtherVisible = visible;
        mEtherIconId = etherIcon;
        mEtherDescription = contentDescription;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup.getContentDescription() != null)
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
            if (mIsThemeDefaultEnabled) {
                mWifi.setColorFilter(null);
                mWifiActivity.setColorFilter(null);
            } else if (mIsSystemDefaultEnabled) {
                mWifi.setColorFilter(getConnectedStatus(mWifiStrengthId) ?
                                         0xff33b5e5 : 0xffaaaaaa,
                                     Mode.MULTIPLY);
                mWifiActivity.setColorFilter(0xff000000, Mode.MULTIPLY);
            } else {
                mWifi.setColorFilter(getConnectedStatus(mWifiStrengthId) ?
                                         mWifiIconConnectedColor : mWifiIconNormalColor,
                                     Mode.MULTIPLY);
                mWifiActivity.setColorFilter(getConnectedStatus(mWifiStrengthId) ?
                                         mWifiActivityConnectedColor : mWifiActivityNormalColor,
                                     Mode.MULTIPLY);
            }
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);

            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            if (mIsThemeDefaultEnabled) {
                mMobile.setColorFilter(null);
                mMobileActivity.setColorFilter(null);
                mMobileType.setColorFilter(null);
            } else if (mIsSystemDefaultEnabled) {
                mMobile.setColorFilter(getConnectedStatus(mMobileStrengthId) ?
                                         0xff33b5e5 : 0xffaaaaaa,
                                     Mode.MULTIPLY);
                mMobileActivity.setColorFilter(0xff000000, Mode.MULTIPLY);
                mMobileType.setColorFilter(0xff808080, Mode.MULTIPLY);
            } else {
                mMobile.setColorFilter(getConnectedStatus(mMobileStrengthId) ?
                                         mMobileConnectedColor : mMobileNormalColor,
                                     Mode.MULTIPLY);
                mMobileActivity.setColorFilter(getConnectedStatus(mMobileStrengthId) ?
                                         mMobileActivityConnectedColor : mMobileActivityNormalColor,
                                     Mode.MULTIPLY);
                mMobileType.setColorFilter(getConnectedStatus(mMobileStrengthId) ?
                                         mMobileNetworkTypeConnectedColor : mMobileNetworkTypeNormalColor,
                                     Mode.MULTIPLY);
            }
            mMobile.setImageResource(mMobileStrengthId);
            mMobileActivity.setImageResource(mMobileActivityId);
            mMobileType.setImageResource(mMobileTypeId);

            mMobileGroup.setContentDescription(mMobileTypeDescription + " " + mMobileDescription);
            mMobileGroup.setVisibility(View.VISIBLE);
            mNoSimSlot.setImageResource(mNoSimIconId);
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mEtherVisible) {
            mEther.setVisibility(View.VISIBLE);
            mEther.setImageResource(mEtherIconId);
            mEther.setContentDescription(mEtherDescription);
        } else {
            mEther.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible &&
                ((mIsAirplaneMode) || (mNoSimIconId != 0))) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("mobile: %s sig=%d act=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId, mMobileActivityId, mMobileTypeId));

        mMobileType.setVisibility(
                !mWifiVisible ? View.VISIBLE : View.GONE);

        updateSignalClusterStyle();
    }

    private void updateSignalClusterStyle() {
        if (!mIsAirplaneMode) {
            mMobileGroup.setVisibility(mSignalClusterStyle !=
                    SIGNAL_CLUSTER_STYLE_NORMAL ? View.GONE : View.VISIBLE);
        }
    }

    private boolean getConnectedStatus(int strengthId) {
        String strengthValue = getResourceName(strengthId);
        boolean isConnected;
        if (strengthValue.contains("fully")) {
            isConnected = true;
        } else {
            isConnected = false;
        }
        return isConnected;
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "unknown";
            }
        } else {
            return "unknown";
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mSignalClusterStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_TEXT, SIGNAL_CLUSTER_STYLE_NORMAL,
                UserHandle.USER_CURRENT);
        mIsThemeDefaultEnabled = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_WIFI_STYLE_ENABLE_DEFAULTS, 0) == 0;
        mIsSystemDefaultEnabled = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_WIFI_STYLE_ENABLE_DEFAULTS, 0) == 1;
        mWifiIconNormalColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR, 0xffaaaaaa);
        mWifiIconConnectedColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR, 0xff33b5e5);
        mWifiActivityNormalColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR, 0xffaaaaaa);
        mWifiActivityConnectedColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR, 0xff33b5e5);
        mMobileNormalColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_NORMAL_COLOR, 0xffaaaaaa);
        mMobileConnectedColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_CONNECTED_COLOR, 0xff33b5e5);

        mMobileNetworkTypeNormalColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_TYPE_NORMAL_COLOR, 0xffaaaaaa);
        mMobileNetworkTypeConnectedColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_TYPE_CONNECTED_COLOR, 0xff33b5e5);

        mMobileActivityNormalColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_NORMAL_COLOR, 0xffaaaaaa);
        mMobileActivityConnectedColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SIGNAL_ACTIVITY_CONNECTED_COLOR, 0xff33b5e5);

        apply();
    }
}

