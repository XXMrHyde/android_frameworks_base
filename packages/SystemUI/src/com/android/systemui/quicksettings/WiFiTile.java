/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2013 CyanogenMod Project
 * Copyright (C) 2013 The SlimRoms Project
 * Copyright (C) 2013 DarkKat
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

package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.ContentResolver;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.util.TypedValue;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.policy.NetworkController;

public class WiFiTile extends NetworkTile {
    private boolean mWifiConnected;
    private boolean mWifiNotConnected;
    private int mWifiSignalIconId;
    private String mDescription;
    private int mInetCondition = 0;

    public WiFiTile(Context context, QuickSettingsController qsc, NetworkController controller) {
        super(context, qsc, controller, R.layout.quick_settings_tile_wifi);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wfm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                wfm.setWifiEnabled(!wfm.isWifiEnabled());
            }
        };
        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
                return true;
            }
        };
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.QUICK_TILES_USE_DIFFERENT_ACTIVITY_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.QUICK_TILES_WIFI_ACTIVITY_NORMAL_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.QUICK_TILES_WIFI_ACTIVITY_CONNECTED_COLOR), this);
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateTile();
    }

    @Override
    protected void updateTile() {
        ContentResolver resolver = mContext.getContentResolver();

        boolean useDifferentActivityColor = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_TILES_USE_DIFFERENT_ACTIVITY_COLOR, 1,
                UserHandle.USER_CURRENT) == 1;
        int wifiIconNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_NORMAL_COLOR, 0xffff8800,
                UserHandle.USER_CURRENT);
        int wifiIconConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ICON_CONNECTED_COLOR, 0xffffffff,
                UserHandle.USER_CURRENT);
        int sbWifiActivityNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_NORMAL_COLOR, 0xff000000,
                UserHandle.USER_CURRENT);
        int sbWifiActivityConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WIFI_ACTIVITY_CONNECTED_COLOR, 0xff000000,
                UserHandle.USER_CURRENT);
        int qsWifiActivityNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_TILES_WIFI_ACTIVITY_NORMAL_COLOR, 0xffff8800,
                UserHandle.USER_CURRENT);
        int qsWifiActivityConnectedColor = Settings.System.getIntForUser(resolver,
                Settings.System.QUICK_TILES_WIFI_ACTIVITY_CONNECTED_COLOR, 0xffffffff,
                UserHandle.USER_CURRENT);
        int wifiActivityNormalColor = useDifferentActivityColor ?
                qsWifiActivityNormalColor : sbWifiActivityNormalColor;
        int wifiActivityConnectedColor = useDifferentActivityColor ?
                qsWifiActivityConnectedColor : sbWifiActivityConnectedColor;


        if (mWifiConnected) {
            mDrawable = mWifiSignalIconId;
            mLabel = mDescription.substring(1, mDescription.length()-1);
        } else if (mWifiNotConnected) {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label);
        } else {
            mDrawable = R.drawable.ic_qs_wifi_no_network;
            mLabel = mContext.getString(R.string.quick_settings_wifi_off_label);
        }

        ImageView image = (ImageView) mTile.findViewById(R.id.image);
        if (image != null) {
            image.setColorFilter(mInetCondition == 1
                    ? wifiIconConnectedColor : wifiIconNormalColor,
                    Mode.MULTIPLY);
        }
        ImageView imageAi = (ImageView) mTile.findViewById(R.id.activity_in);
        if (imageAi != null) {
            imageAi.setColorFilter(mInetCondition == 1
                    ? wifiActivityConnectedColor : wifiActivityNormalColor,
                    Mode.MULTIPLY);
        }
        ImageView imageAo = (ImageView) mTile.findViewById(R.id.activity_out);
        if (imageAo != null) {
            imageAo.setColorFilter(mInetCondition == 1
                    ? wifiActivityConnectedColor : wifiActivityNormalColor,
                    Mode.MULTIPLY);
        }
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        if (tv != null) {
            tv.setText(mLabel);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
            tv.setPadding(0, mTileTextPadding, 0, 0);
            tv.setTextColor(mTileTextColor);
        }
        ImageView image = (ImageView) mTile.findViewById(R.id.image);
        if (image != null) {
            image.setImageResource(mDrawable);
        }
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
                int inetCondition, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
        mWifiConnected = enabled && (wifiSignalIconId > 0) && (description != null);
        mWifiNotConnected = (wifiSignalIconId > 0) && (description == null);
        mInetCondition = inetCondition;
        mWifiSignalIconId = wifiSignalIconId;
        mDescription = description;
        setActivity(activityIn, activityOut);
        updateResources();
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                int inetCondition, String mobileSignalContentDescriptionId,
                int dataTypeIconId, boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
    }
}
