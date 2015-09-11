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

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.cm.WeatherControllerImpl;
import com.android.internal.util.darkkat.DeviceUtils;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkController;

import java.text.NumberFormat;

public class StatusBarHeaderExpandedPanel extends RelativeLayout implements
        NetworkController.NetworkSignalChangedCallback,
        WeatherController.Callback {

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    private final Context mContext;
    private final Handler mHandler;

    private ActivityStarter mActivityStarter;
    private NetworkController mNetworkController;
    private WeatherController mWeatherController;

    private View mWeatherView;

    private ImageView mCarrierIconView;
    private ImageView mWifiIconView;
    private BatteryMeterView mBatteryMeterView;

    private TextView mCarrierText;
    private TextView mWifiText;
    private TextView mBatteryPercentageText;
    private TextView mBatteryStatusText;
    private TextView mWeatherText;

    private ImageView mQsSettingsButton;

    private Drawable mMobileSignalIcon;
    private Drawable mWifiSignalIcon;

    private boolean mSupportsMobileData = true;
    private boolean mMobileNetworkEnabled = false;

    private boolean mWifiEnabled = false;
    private boolean mWifiConnected = false;

    private boolean mWeatherAvailable = false;

    private boolean mExpanded = false;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                updateBatteryInfo(intent);
            }
        }
    };

    public StatusBarHeaderExpandedPanel(Context context) {
        this(context, null);
    }

    public StatusBarHeaderExpandedPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandler = new Handler();

        if (!DeviceUtils.deviceSupportsMobileData(mContext)) {
            mSupportsMobileData = false;
        }
    }

    public void setUp(ActivityStarter starter, BatteryController battery, NetworkController network,
            WeatherController weather) {
        mActivityStarter = starter;
        mNetworkController = network;
        mWeatherController = weather;

        mBatteryMeterView.setBatteryController(battery);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWeatherView = findViewById(R.id.expanded_panel_weather);

        mCarrierIconView = (ImageView) findViewById(R.id.expanded_panel_carrier_icon);
        mWifiIconView = (ImageView) findViewById(R.id.expanded_panel_wifi_icon);
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.expanded_panel_battery_icon);

        mCarrierText = (TextView) findViewById(R.id.expanded_panel_carrier_label);
        mWifiText = (TextView) findViewById(R.id.expanded_panel_wifi_label);
        mBatteryPercentageText = (TextView) findViewById(R.id.expanded_panel_battery_percentage);
        mBatteryStatusText = (TextView) findViewById(R.id.expanded_panel_battery_status);
        mWeatherText = (TextView) findViewById(R.id.expanded_panel_weather_text);

        mQsSettingsButton = (ImageView) findViewById(R.id.qs_settings_button);

        mWeatherView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startForecastActivity();
            }
        });

        mQsSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startQsSettingsActivity();
            }
        });
    }

    public void setListening(boolean listening) {
        if (listening) {
            mContext.registerReceiver(mBatteryInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            mNetworkController.addNetworkSignalChangedCallback(this);
            if (showWeather()) {
                mWeatherController.addCallback(this);
            }
        } else {
            mContext.unregisterReceiver(mBatteryInfoReceiver);
            mNetworkController.removeNetworkSignalChangedCallback(this);
            mWeatherController.removeCallback(this);
        }
    }

    public void updateVisibilities() {
        if (!mSupportsMobileData) {
            findViewById(R.id.expanded_panel_mobile_network).setVisibility(View.INVISIBLE);
        }
        mWeatherView.setVisibility(showWeather() ? View.VISIBLE : View.INVISIBLE);
        mQsSettingsButton.setVisibility(showQsButton() ? View.VISIBLE : View.INVISIBLE);
    }

    public void updateClickTargets(boolean clickable) {
        mWeatherView.setClickable(showWeather() && clickable);
        mQsSettingsButton.setClickable(showQsButton() && clickable);
    }

    private boolean showWeather() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER, 0) == 1;
    }

    private boolean showQsButton() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_QS_BUTTON, 0) == 1;
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, boolean connected,
                int wifiSignalIconId, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
        Drawable icon = mContext.getResources().getDrawable(wifiSignalIconId);
        if (mWifiSignalIcon == null) {
            mWifiSignalIcon = icon;
            mWifiIconView.setImageDrawable(mWifiSignalIcon);
        } else if (mWifiSignalIcon != icon) {
            mWifiSignalIcon = icon;
            mWifiIconView.setImageDrawable(mWifiSignalIcon);
        }
        mWifiEnabled = enabled;
        mWifiConnected = connected;
        updateWifiText(description);
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description,
                boolean isDataTypeIconWide) {
        mMobileNetworkEnabled = enabled;
        if (mSupportsMobileData) {
            Drawable icon = mContext.getResources().getDrawable(mobileSignalIconId);
            if (mMobileSignalIcon == null) {
                mMobileSignalIcon = icon;
                mCarrierIconView.setImageDrawable(icon);
            } else if (mMobileSignalIcon != icon) {
                mMobileSignalIcon = icon;
                mCarrierIconView.setImageDrawable(icon);
            }
            updateCarrierlabel(description);
        }
    }

    @Override
    public  void onNoSimVisibleChanged(boolean visible) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
    }

    @Override
    public  void onMobileDataEnabled(boolean visible) {
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            mWeatherAvailable = true;
        }
        updateWeather(info);
    }

    private void updateWifiText(String description) {
        String wifiName = removeDoubleQuotes(description);
        if (!mWifiEnabled) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_wifi_off);
        } else if (!mWifiConnected) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_no_wifi);
        } else if (wifiName == null) {
            wifiName = "--";
        }
        mWifiText.setText(wifiName);
    }

    private void updateCarrierlabel(String description) {
        String carrierLabel = removeDoubleQuotes(description);
        if (!mMobileNetworkEnabled) {
            carrierLabel = mContext.getResources().getString(
                    R.string.quick_settings_rssi_emergency_only);
        } else if (carrierLabel == null) {
            carrierLabel = "--";
        }
        mCarrierText.setText(carrierLabel);
    }

    private void updateBatteryInfo(Intent intent) {
        int level = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);

        String batterylevel = getBatteryPercentageLevel(intent);
        String batteryStatus = getBatteryStatus(intent);
        mBatteryPercentageText.setText(batterylevel);
        mBatteryStatusText.setText((batteryStatus.isEmpty() ? "" : " (" + batteryStatus + ")"));
    }

    private void updateWeather(WeatherController.WeatherInfo info) {
        String weatherInfo = null;
        if (!mWeatherAvailable) {
            weatherInfo = mContext.getString(R.string.weather_info_not_available);
        } else {
            String city = info.city + ", ";
            String temp = info.temp;
            String condition = info.condition;
            weatherInfo = (showWeatherLocation() ? city : "") + temp + " - " + condition;
        }
        mWeatherText.setText(weatherInfo);
    }

    public void setIconColor(int color) {
        mWifiIconView.setColorFilter(color, Mode.MULTIPLY);
        mCarrierIconView.setColorFilter(color, Mode.MULTIPLY);
        mQsSettingsButton.setColorFilter(color, Mode.MULTIPLY);
    }

    public void setWeatherViewBackground(RippleDrawable background) {
        mWeatherView.setBackground(background);
    }

    public void setQsSettingsBackground(RippleDrawable background) {
        mQsSettingsButton.setBackground(background);
    }

    public void setTextColor(int color, boolean isOpaque) {
        if (isOpaque) {
            mCarrierText.setTextColor(color);
            mWifiText.setTextColor(color);
            mBatteryPercentageText.setTextColor(color);
            mWeatherText.setTextColor(color);
        } else {
            mBatteryStatusText.setTextColor(color);
        }
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private String getBatteryPercentageLevel(Intent intent) {
        int level = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        return NumberFormat.getPercentInstance().format((double) level / 100.0);
    }

    private String getBatteryStatus(Intent intent) {
        int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);

        String emptyString = "";
        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            return emptyString;
        }
        Resources settingsResources;
        try {
            settingsResources = pm.getResourcesForApplication(SETTINGS_METADATA_NAME);
        } catch (Exception e) {
            Log.e("StatusBarHeaderExpandedPanel:", "can't access settings resources",e);
            return emptyString;
        }

        int resId;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_ac", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_usb", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_wireless", null, null);
            } else {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging", null, null);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_discharging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_not_charging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_full", null, null);
        } else {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_unknown", null, null);
        }
        return resId > 0 ? settingsResources.getString(resId) : emptyString;
    }

    private boolean showWeatherLocation() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER_LOCATION, 1) == 1;
    }

    private void startForecastActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(WeatherControllerImpl.COMPONENT_WEATHER_FORECAST);
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startQsSettingsActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$StatusBarExpandedQsSettingsActivity"));
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }
}
