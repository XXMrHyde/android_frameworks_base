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

package com.android.systemui.darkkat.statusBarExpanded;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.darkkat.DeviceUtils;
import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.darkkat.NetworkTrafficController;
import com.android.systemui.darkkat.statusBarExpanded.bars.BatteryStatusBar;
import com.android.systemui.darkkat.statusBarExpanded.bars.BrightnessSliderBar;
import com.android.systemui.darkkat.statusBarExpanded.bars.MobileBar;
import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.darkkat.statusBarExpanded.bars.WeatherBarContainer;
import com.android.systemui.darkkat.statusBarExpanded.bars.WifiBar;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BatteryBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.LocationController;

public class BarsController {
    private final Context mContext;

    private Handler mHandler;
    private SettingsObserver mSettingsObserver;
    private final ContentResolver mResolver;

    private QSContainer mQSContainer;
    private final View mBarsContainer;
    private View mQABarContainer;
    private QuickAccessBar mQuickAccessBar;
    private BrightnessSliderBar mBrightnessSliderBar;
    private WifiBar mWifiBar;
    private MobileBar mMobileBar;
    private BatteryStatusBar mBatteryStatusBar;
    private BatteryBar mBatteryBar;
    private WeatherBarContainer mWeatherBarContainer;
    private View mSpaceQAB;
    private View mSpaceBrightness;
    private View mSpaceWifi;
    private View mSpaceMobile;
    private View mSpaceBattery;
    private View mSpaceWeather;

    private boolean mShowQuickAccessBar;
    private boolean mShowBrightnessSliderBar;
    private boolean mShowWifiBar;
    private boolean mShowMobileBar;
    private boolean mShowBatteryStatusBar;
    private boolean mShowBatteryBar;
    private boolean mShowWeatherBar;

    private final boolean mSupportsMobileData;

    public BarsController(Context context, View barsContainer) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);

        mBarsContainer = barsContainer;
        mQSContainer = (QSContainer) barsContainer.getParent();
        mQABarContainer = mBarsContainer.findViewById(R.id.expanded_bars_quick_access_bar_container);
        mQuickAccessBar = (QuickAccessBar) mBarsContainer.findViewById(R.id.quick_access_bar);
        mBrightnessSliderBar =
                (BrightnessSliderBar) mBarsContainer.findViewById(R.id.expanded_bars_brightness_bar);
        mWifiBar = (WifiBar) mBarsContainer.findViewById(R.id.expanded_bars_wifi_bar);
        mMobileBar = (MobileBar) mBarsContainer.findViewById(R.id.expanded_bars_mobile_bar);
        mBatteryStatusBar =
                (BatteryStatusBar) mBarsContainer.findViewById(R.id.expanded_bars_battery_status_bar);
        mBatteryBar = (BatteryBar) mBatteryStatusBar.findViewById(R.id.battery_status_bar_battery_bar);
        mWeatherBarContainer =
                (WeatherBarContainer) mBarsContainer.findViewById(R.id.expanded_bars_weather_bar_container);
        mSpaceQAB = mBarsContainer.findViewById(R.id.expanded_bars_space_quick_access_bar);
        mSpaceBrightness = mBarsContainer.findViewById(R.id.expanded_bars_space_brightness);
        mSpaceWifi = mBarsContainer.findViewById(R.id.expanded_bars_space_wifi);
        mSpaceMobile = mBarsContainer.findViewById(R.id.expanded_bars_space_mobile);
        mSpaceBattery = mBarsContainer.findViewById(R.id.expanded_bars_space_battery);
        mSpaceWeather = mBarsContainer.findViewById(R.id.expanded_bars_space_weather);
        mSupportsMobileData = DeviceUtils.deviceSupportsMobileData(mContext);

        mShowQuickAccessBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB, 1) == 1;
        mShowBrightnessSliderBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER, 1) == 1;
        mShowWifiBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_WIFI_BAR, 0) == 1;
        mShowMobileBar = mSupportsMobileData && Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_MOBILE_BAR, 0) == 1;
        mShowBatteryStatusBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BATTERY_STATUS_BAR, 0) == 1;
        mShowBatteryBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_BATTERY_BAR, 0) == 1;
        mShowWeatherBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_WEATHER, 0) == 1;
    }

    public void setUp(PhoneStatusBar statusBar, BluetoothController bluetooth, NetworkController network,
            NetworkTrafficController networkTraffic, RotationLockController rotationLock,
            LocationController location, HotspotController hotspot, FlashlightController flashlight,
            BatteryController battery, WeatherController weather) {
        mQuickAccessBar.setUp(statusBar, bluetooth, network, rotationLock, location, hotspot, flashlight);
        mWifiBar.setNetworkController(network);
        mWifiBar.setNetworkTrafficController(networkTraffic);
        mMobileBar.setNetworkController(network);
        mMobileBar.setNetworkTrafficController(networkTraffic);
        mBatteryStatusBar.setBatteryController(battery);
        mWeatherBarContainer.setUp(statusBar, weather);

        applyVisibilitySettings();
        mobileWifiBarSetBitByte();
        applyAdvancedBatteryStatusSettings();
        applyColors();
    }

    private void applyVisibilitySettings() {
        mQABarContainer.setVisibility(mShowQuickAccessBar ? View.INVISIBLE : View.GONE);
        mSpaceQAB.setVisibility(mShowQuickAccessBar ? View.INVISIBLE : View.GONE);
        mBrightnessSliderBar.setVisibility(mShowBrightnessSliderBar ? View.INVISIBLE : View.GONE);
        mSpaceBrightness.setVisibility(mShowBrightnessSliderBar ? View.INVISIBLE : View.GONE);
        mWifiBar.setVisibility(mShowWifiBar ? View.INVISIBLE : View.GONE);
        mSpaceWifi.setVisibility(mShowWifiBar ? View.INVISIBLE : View.GONE);
        mMobileBar.setVisibility(mShowMobileBar ? View.INVISIBLE : View.GONE);
        mSpaceMobile.setVisibility(mShowMobileBar ? View.INVISIBLE : View.GONE);
        mBatteryStatusBar.setVisibility(mShowBatteryStatusBar ? View.INVISIBLE : View.GONE);
        mBatteryBar.setVisibility(mShowBatteryStatusBar && mShowBatteryBar
                ? View.INVISIBLE : View.GONE);
        mSpaceBattery.setVisibility(mShowBatteryStatusBar ? View.INVISIBLE : View.GONE);
        mWeatherBarContainer.setVisibility(mShowWeatherBar ? View.INVISIBLE : View.GONE);
        mSpaceWeather.setVisibility(mShowWeatherBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void applyAdvancedBatteryStatusSettings() {
        batteryStatusSetIndicator();
        batteryStatusSetIconTextVisibility();
        batteryStatusSetIconCircleDots();
        batteryStatusSetShowChargeAnimation();
        batteryStatusSetIconCutOutText();
        batteryStatusSetIconTextColor();
    }

    private void applyColors() {
        setBackgroundColor();
        setIconColor();
        setTextColor();
        setRippleColor();
    }

    private void setShowQuickAccessBar() {
        mShowQuickAccessBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB, 1) == 1;
        mQABarContainer.setVisibility(mShowQuickAccessBar ? View.INVISIBLE : View.GONE);
        mSpaceQAB.setVisibility(mShowQuickAccessBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void setShowBrightnessSliderBar() {
        mShowBrightnessSliderBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER, 1) == 1;
        mBrightnessSliderBar.setVisibility(mShowBrightnessSliderBar ? View.INVISIBLE : View.GONE);
        mSpaceBrightness.setVisibility(mShowBrightnessSliderBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void setShowWifiBar() {
        mShowWifiBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_WIFI_BAR, 0) == 1;
        mWifiBar.setVisibility(mShowWifiBar ? View.INVISIBLE : View.GONE);
        mSpaceWifi.setVisibility(mShowWifiBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void setShowMobileBar() {
        mShowMobileBar = mSupportsMobileData && Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_MOBILE_BAR, 0) == 1;
        mMobileBar.setVisibility(mShowMobileBar ? View.INVISIBLE : View.GONE);
        mSpaceMobile.setVisibility(mShowMobileBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void setShowBatteryStatusBar() {
        mShowBatteryStatusBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BATTERY_STATUS_BAR, 0) == 1;
        mBatteryStatusBar.setVisibility(mShowBatteryStatusBar ? View.INVISIBLE : View.GONE);
        mBatteryBar.setVisibility(mShowBatteryStatusBar && mShowBatteryBar
                ? View.INVISIBLE : View.GONE);
        mSpaceBattery.setVisibility(mShowBatteryStatusBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private void setShowWeatherBar() {
        mShowWeatherBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_WEATHER, 0) == 1;
        mWeatherBarContainer.setVisibility(mShowWeatherBar ? View.INVISIBLE : View.GONE);
        mSpaceWeather.setVisibility(mShowWeatherBar ? View.INVISIBLE : View.GONE);
        mBarsContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
        mQSContainer.setVisibility(showAnyBar() ? View.INVISIBLE : View.GONE);
    }

    private boolean showAnyBar() {
        return mShowQuickAccessBar
                || mShowBrightnessSliderBar
                || mShowWifiBar
                || mShowMobileBar
                || mShowBatteryStatusBar
                || mShowWeatherBar;
    }

    private void batteryStatusSetIndicator() {
        final int indicator = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_ICON_INDICATOR, 0);
        mBatteryStatusBar.setBatteryIndicator(indicator);
    }

    private void batteryStatusSetIconTextVisibility() {
        final boolean show = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_TEXT, 0) == 1;
        mBatteryStatusBar.setBatteryTextVisibility(show);
    }

    private void batteryStatusSetIconCircleDots() {
        final int interval = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_INTERVAL, 0);
        final int length = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_LENGTH, 0);
        mBatteryStatusBar.setBatteryCircleDots(interval, length);
    }

    private void batteryStatusSetShowChargeAnimation() {
        final boolean show = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_CHARGE_ANIMATION, 0) == 1;
        mBatteryStatusBar.setBatteryShowChargeAnimation(show);
    }

    private void batteryStatusSetIconCutOutText() {
        final boolean cutOut = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CUT_OUT_TEXT, 1) == 1;
        mBatteryStatusBar.setBatteryCutOutBatteryText(cutOut);
    }

    private void batteryStatusSetShowBatteryBar() {
        mShowBatteryBar = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_BATTERY_BAR, 0) == 1;
        mBatteryBar.setVisibility(mShowBatteryStatusBar && mShowBatteryBar
                ? View.INVISIBLE : View.GONE);
    }

    private void mobileWifiBarSetBitByte() {
        final int bitByte = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_BARS_TRAFFIC_BIT_BYTE, 1);
        mWifiBar.setBitByte(bitByte);
        mMobileBar.setBitByte(bitByte);
    }

    private void batteryStatusSetIconTextColor() {
        final int color = SBEPanelColorHelper.getBatteryTextColor(mContext);
        mBatteryStatusBar.setBatteryTextColor(color);
    }

    private void setBackgroundColor() {
        final int color = SBEPanelColorHelper.getBackgroundColor(mContext);
        mQSContainer.getBgView().getBackground().setColorFilter(color, Mode.SRC_IN);
        mQABarContainer.getBackground().setColorFilter(color, Mode.SRC_IN);
        mBrightnessSliderBar.getBackground().setColorFilter(color, Mode.SRC_IN);
        mWifiBar.getBackground().setColorFilter(color, Mode.SRC_IN);
        mMobileBar.getBackground().setColorFilter(color, Mode.SRC_IN);
        mBatteryStatusBar.getBackground().setColorFilter(color, Mode.SRC_IN);
        mWeatherBarContainer.getBackground().setColorFilter(color, Mode.SRC_IN);
    }

    private void setIconColor() {
        mQuickAccessBar.setColors();
        mBrightnessSliderBar.setIconColor();
        mWifiBar.setIconColor();
        mMobileBar.setIconColor();
        mBatteryStatusBar.setIconColor();
        weatherupdateItems();
    }

    private void setTextColor() {
        mWifiBar.setTextColor();
        mMobileBar.setTextColor();
        mBatteryStatusBar.setTextColor();
        weatherupdateItems();
    }

    private void setRippleColor() {
        mQuickAccessBar.setColors();
        mBrightnessSliderBar.setRippleColor();
        mWeatherBarContainer.setRippleColor();
    }

    private void weatherupdateItems() {
        mWeatherBarContainer.updateItems();
    }

    public void updateVisibility(boolean keyguardShowing, boolean visible) {
        if (!showAnyBar()) {
            return;
        }
        mBarsContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mQSContainer.setVisibility(
                keyguardShowing && !visible ? View.INVISIBLE : View.VISIBLE);
        if (mShowQuickAccessBar) {
            mQABarContainer.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
            mSpaceQAB.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowBrightnessSliderBar) {
            mBrightnessSliderBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            mSpaceBrightness.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowWifiBar) {
            mWifiBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            mSpaceWifi.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowMobileBar) {
            mMobileBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            mSpaceMobile.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowBatteryStatusBar) {
            mBatteryStatusBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            if (mShowBatteryBar) {
                mBatteryBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
            mSpaceBattery.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowWeatherBar) {
            mWeatherBarContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            mSpaceWeather.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setObserving(boolean observe) {
        if (observe) {
            mSettingsObserver.observe();
        } else {
            mSettingsObserver.unobserve();
        }
    }

    public void setListening(boolean listening) {
        mQuickAccessBar.setListening(listening && mShowQuickAccessBar);
        mBrightnessSliderBar.setListening(listening && mShowBrightnessSliderBar);
        mWifiBar.setListening(listening && mShowWifiBar);
        mMobileBar.setListening(listening && mShowMobileBar);
        mBatteryStatusBar.setListening(listening && mShowBatteryStatusBar);
        mBatteryBar.setListening(listening && mShowBatteryStatusBar && mShowBatteryBar);
        mWeatherBarContainer.setListening(listening && mShowWeatherBar);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_WIFI_BAR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_MOBILE_BAR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_BATTERY_STATUS_BAR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_WEATHER),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_ICON_INDICATOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_TEXT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_INTERVAL),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_LENGTH),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_CHARGE_ANIMATION),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CUT_OUT_TEXT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_BATTERY_BAR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_TEXT_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_CURRENT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_TYPE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_TRAFFIC_BIT_BYTE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BACKGROUND_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_ICON_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_RIPPLE_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_TEXT_COLOR),
                    false, this);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB))) {
                setShowQuickAccessBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER))) {
                setShowBrightnessSliderBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_WIFI_BAR))) {
                setShowWifiBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_MOBILE_BAR))) {
                setShowMobileBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_BATTERY_STATUS_BAR))) {
                setShowBatteryStatusBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_SHOW_WEATHER))) {
                setShowWeatherBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_ICON_INDICATOR))) {
                batteryStatusSetIndicator();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_TEXT))) {
                batteryStatusSetIconTextVisibility();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_INTERVAL))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CIRCLE_DOT_LENGTH))) {
                batteryStatusSetIconCircleDots();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_CHARGE_ANIMATION))) {
                batteryStatusSetShowChargeAnimation();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_CUT_OUT_TEXT))) {
                batteryStatusSetIconCutOutText();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_SHOW_BATTERY_BAR))) {
                batteryStatusSetShowBatteryBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_BATTERY_TEXT_COLOR))) {
                batteryStatusSetIconTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_CURRENT))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_TYPE))) {
                weatherupdateItems();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BARS_TRAFFIC_BIT_BYTE))) {
                mobileWifiBarSetBitByte();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BACKGROUND_COLOR))) {
                setBackgroundColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_ICON_COLOR))) {
                setIconColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_TEXT_COLOR))) {
                setTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_RIPPLE_COLOR))) {
                setRippleColor();
            }
        }
    }
}
