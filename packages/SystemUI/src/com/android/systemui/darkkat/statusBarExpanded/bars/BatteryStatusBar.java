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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;
import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.R;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.statusbar.policy.BatteryBar;
import com.android.systemui.statusbar.policy.BatteryController;

import java.text.NumberFormat;


public class BatteryStatusBar extends RelativeLayout {
    private static final String TAG = "BatteryStatusBar";
    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    private final Context mContext;

    private BatteryBar mBatteryBar;
    private BatteryMeterView mBatteryMeterView;
    private TextView mPrimaryText;
    private TextView mSecondaryText;
    private final String mEmptyString = "";
    private final String mSeparator;

    private boolean mReceiverRegistered = false;
    private Intent mBroadcast = null;

    private BatteryController mBatteryController;
    private PackageManager mPackageManager = null;
    private Resources mSettingsResources = null;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBroadcast = intent;
                updateBatteryInfo(intent);
            }
        }
    };

    public BatteryStatusBar(Context context) {
        this(context, null);
    }

    public BatteryStatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mSeparator = getResources().getString(
                com.android.internal.R.string.kg_text_message_separator);

        mPackageManager = mContext.getPackageManager();
        if (mPackageManager != null) {
            try {
                mSettingsResources = mPackageManager.getResourcesForApplication(SETTINGS_METADATA_NAME);
            } catch (Exception e) {
                Log.e(TAG, "can't access settings resources",e);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBatteryBar = (BatteryBar) findViewById(R.id.battery_status_bar_battery_bar);
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.battery_status_bar_icon);
        mPrimaryText = (TextView) findViewById(R.id.battery_status_bar_primary_text);
        mSecondaryText = (TextView) findViewById(R.id.battery_status_bar_secondary_text);
    }

    public void setBatteryController(BatteryController bc) {
        mBatteryController = bc;
        mBatteryBar.setBatteryController(bc);
        mBatteryMeterView.setBatteryController(bc);
    }

    public void setListening(boolean listening) {
        if (mBatteryController == null) {
            return;
        }
        mBatteryMeterView.setListening(listening);
        if (listening) {
            if (!mReceiverRegistered) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                mContext.registerReceiver(mBatteryInfoReceiver, filter);
                mReceiverRegistered = true;
                if (mBroadcast != null) {
                    updateBatteryInfo(mBroadcast);
                }
            }
        } else {
            if (mReceiverRegistered) {
                mContext.unregisterReceiver(mBatteryInfoReceiver);
                mReceiverRegistered = false;
            }
        }
    }

    public void setBatteryIndicator(int indicator) {
        mBatteryMeterView.updateBatteryIndicator(indicator);
    }

    public void setBatteryTextVisibility(boolean show) {
        mBatteryMeterView.setTextVisibility(show);
    }

    public void setBatteryCircleDots(int interval, int length) {
        mBatteryMeterView.updateCircleDots(interval, length);
    }

    public void setBatteryShowChargeAnimation(boolean show) {
        mBatteryBar.setShowChargeAnimation(show);
        mBatteryMeterView.setShowChargeAnimation(show);
    }

    public void setBatteryCutOutBatteryText(boolean cutOut) {
        mBatteryMeterView.setCutOutText(cutOut);
    }

    public void setBatteryTextColor(int textColor) {
        mBatteryMeterView.setTextColor(textColor);
    }

    public void setIconColor() {
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        mBatteryBar.setColor(iconColor);
        mBatteryMeterView.setBatteryColors(iconColor);
    }

    public void setTextColor() {
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mPrimaryText.setTextColor(textColorPrimary);
        mSecondaryText.setTextColor(textColorSecondary);
    }

    private void updateBatteryInfo(Intent intent) {
        final IBatteryStats batteryInfo = IBatteryStats.Stub.asInterface(
                ServiceManager.getService(BatteryStats.SERVICE_NAME));

        final String batterylevel = getBatteryPercentageLevel(intent);
        final String batteryStatus = getBatteryStatus(intent);
        String batteryTimeFormatted = "";
        String chargingTimeFormatted = "";
        String primaryText = batterylevel;

        long batteryTimeRemaining = 0;
        long chargingTimeRemaining = 0;

        try {
            batteryTimeRemaining = batteryInfo.computeBatteryTimeRemaining();

        } catch (RemoteException e) {
            Log.e(TAG, "Error calling IBatteryStats: ", e);
        }
        try {
            chargingTimeRemaining = batteryInfo.computeChargeTimeRemaining();

        } catch (RemoteException e) {
            Log.e(TAG, "Error calling IBatteryStats: ", e);
        }

        if (batteryTimeRemaining > 0) {
            batteryTimeFormatted = Formatter.formatShortElapsedTimeRoundingUpToMinutes(
                mContext, batteryTimeRemaining);
            primaryText = getRemainingText(false, batterylevel, batteryTimeFormatted);
        }
        if (chargingTimeRemaining > 0) {
            chargingTimeFormatted = Formatter.formatShortElapsedTimeRoundingUpToMinutes(
                mContext, chargingTimeRemaining);
            primaryText = getRemainingText(true, batterylevel, chargingTimeFormatted);
        }

        mPrimaryText.setText(primaryText);
        mSecondaryText.setText(batteryStatus);
    }

    private String getBatteryPercentageLevel(Intent intent) {
        final int level = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        return NumberFormat.getPercentInstance().format((double) level / 100.0);
    }

    private String getBatteryStatus(Intent intent) {
        if (mSettingsResources == null) {
            return mEmptyString;
        }

        final int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);

        int resId;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_ac", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_usb", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_wireless", null, null);
            } else {
                resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging", null, null);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_discharging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_not_charging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_full", null, null);
        } else {
            resId = mSettingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_unknown", null, null);
        }
        return resId > 0 ? mSettingsResources.getString(resId) : mEmptyString;
    }

    private String getRemainingText(boolean isCharging, String level, String timeRemaining) {
        if (mSettingsResources == null) {
            return mEmptyString;
        }

        final int resId = isCharging
                    ? (mSettingsResources.getIdentifier(SETTINGS_METADATA_NAME
                            + ":string/power_charging_duration", null, null))
                    : (mSettingsResources.getIdentifier(SETTINGS_METADATA_NAME
                            + ":string/power_discharging_duration", null, null));
        return resId > 0 ? mSettingsResources.getString(resId, level, timeRemaining) : mEmptyString;
    }
}
