/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconEmptyViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
    private ArrayList<TextView> mIconLabelViews = new ArrayList<TextView>();

    public static final int BATTERY_STYLE_NORMAL_CUST    = 0;
    public static final int BATTERY_STYLE_PERCENT        = 1;
    public static final int BATTERY_STYLE_ICON_MINI_CUST = 2;

    public static final int BATTERY_STYLE_ABM            = 4;
    public static final int BATTERY_STYLE_HCOMB          = 5;
    /***
     * BATTERY_STYLE_CIRCLE* cannot be handled in this controller, since we cannot get views from
     * statusbar here. Yet it is listed for completion and not to confuse at future updates
     * See CircleBattery.java for more info
     *
     * set to public to be reused by CircleBattery
     */
    public static final int BATTERY_STYLE_CIRCLE        = 3;

    private static final int BATTERY_TEXT_STYLE_NORMAL  = R.string.status_bar_settings_battery_meter_format;
    private static final int BATTERY_TEXT_STYLE_MIN     = R.string.status_bar_settings_battery_meter_min_format;

    private boolean mBatteryPlugged = false;
    private boolean mShowBatteryStatus = true;
    private int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private int mBatteryLevel = 0;
    private int mBatteryStyle;
    private boolean mEnableThemeDefault;
    private boolean mEnableIconText;
    private int mFillColor;
    private int mEmptyColor;
    private int mTextColor;
    private int mTextChargingColor;

    Handler mHandler;

    private final boolean mUiController;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_ENABLE_THEME_DEFAULT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_EMPTY_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_FILL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR), false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, int status);
    }

    public BatteryController(Context context) {
        this(context, true);
    }

    public BatteryController(Context context, boolean ui) {
        mContext = context;
        mHandler = new Handler();
        mUiController = ui;

        if (mUiController) {
            SettingsObserver settingsObserver = new SettingsObserver(mHandler);
            settingsObserver.observe();
            updateSettings();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void addIconEmptyView(ImageView v) {
        mIconEmptyViews.add(v);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void addIconLabelView(TextView v) {
        mIconLabelViews.add(v);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
        // trigger initial update
        cb.onBatteryLevelChanged(getBatteryLevel(), getBatteryStatus());
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    // Allow override battery icons
    public int getIconStyleUnknown() {
        return R.drawable.stat_sys_battery;
    }
    public int getIconStyleNormal() {
        return R.drawable.stat_sys_battery;
    }
    public int getIconStyleCharge() {
        return R.drawable.stat_sys_battery_charge;
    }
    public int getIconEmptyStyleNormal() {
        return R.drawable.stat_sys_battery_empty;
    }
    public int getIconStyleNormalCust() {
        return R.drawable.stat_sys_battery_cust;
    }
    public int getIconStyleChargeCust() {
        return R.drawable.stat_sys_battery_charge_cust;
    }
    public int getIconStyleNormalMin() {
        return R.drawable.stat_sys_battery_min;
    }
    public int getIconStyleChargeMin() {
        return R.drawable.stat_sys_battery_charge_min;
    }
    public int getIconEmptyStyleNormalMin() {
        return R.drawable.stat_sys_battery_empty_min;
    }
    public int getIconStyleNormalMinCust() {
        return R.drawable.stat_sys_battery_min_cust;
    }
    public int getIconStyleChargeMinCust() {
        return R.drawable.stat_sys_battery_charge_min_cust;
    }
    public int getIconStyleNormalABM() {
        return R.drawable.stat_sys_battery_abm;
    }
    public int getIconStyleChargeABM() {
        return R.drawable.stat_sys_battery_charge_abm;
    }
    public int getIconStyleNormalHCOMB() {
        return R.drawable.stat_sys_battery_hcomb;
    }
    public int getIconStyleChargeHCOMB() {
        return R.drawable.stat_sys_battery_charge_hcomb;
    }

    protected int getBatteryLevel() {
        return mBatteryLevel;
    }

    protected int getBatteryStyle() {
        return mBatteryStyle;
    }

    protected int getBatteryStatus() {
        return mBatteryStatus;
    }

    protected boolean isBatteryPlugged() {
        return mBatteryPlugged;
    }

    protected boolean isBatteryPresent() {
        // the battery widget always is shown.
        return true;
    }

    protected boolean isBatteryStatusUnknown() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_UNKNOWN;
    }

    protected boolean isBatteryStatusCharging() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    protected boolean isUiController() {
        return mUiController;
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                                                BatteryManager.BATTERY_STATUS_UNKNOWN);
            updateViews();
            if (mUiController) {
                updateBattery();
            }
        }
    }

    protected void updateViews() {
        int level = getBatteryLevel();
        if (mUiController) {
            int N = mIconEmptyViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconEmptyViews.get(i);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                    v.setText(mContext.getString(BATTERY_TEXT_STYLE_NORMAL, level));
                } else {
                    v.setText(mContext.getString(BATTERY_TEXT_STYLE_MIN, level));
                }
            }
            N = mIconLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mIconLabelViews.get(i);
                v.setText(mContext.getString(BATTERY_TEXT_STYLE_MIN, level));
            }

            for (BatteryStateChangeCallback cb : mChangeCallbacks) {
                cb.onBatteryLevelChanged(level, getBatteryStatus());
            }
            updateBattery();
        }
    }

    protected void updateBattery() {
        int showIcon = View.GONE;
        int iconStyle = getIconStyleNormal();
        int showEmptyIcon = View.GONE;
        int iconEmptyStyle = getIconEmptyStyleNormal();
        int showText = View.GONE;
        int showIconText = View.GONE;
        int textStyle = BATTERY_TEXT_STYLE_NORMAL;
        int pxTextPadding = 0;
        float textSize = 9.0f;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;

        if (isBatteryPresent() && mShowBatteryStatus) {
            if (isBatteryStatusUnknown() &&
                (mBatteryStyle == BATTERY_STYLE_NORMAL_CUST ||
                 mBatteryStyle == BATTERY_STYLE_PERCENT ||
                 mBatteryStyle == BATTERY_STYLE_ICON_MINI_CUST ||
				 mBatteryStyle == BATTERY_STYLE_ABM ||
				 mBatteryStyle == BATTERY_STYLE_HCOMB)) {

                // Unknown status doesn't relies on any style
                showIcon = (View.VISIBLE);
                iconStyle = getIconStyleUnknown();
            } else if (mBatteryStyle == BATTERY_STYLE_NORMAL_CUST) {
                showEmptyIcon = mEnableThemeDefault ? View.GONE : View.VISIBLE;
                showIcon = (View.VISIBLE);
                if (mEnableThemeDefault) {
                    iconStyle = isBatteryStatusCharging() ?
                                    getIconStyleCharge() : getIconStyleNormal();
                } else {
                    iconStyle = isBatteryStatusCharging() ?
                                    getIconStyleChargeCust() : getIconStyleNormalCust();
                    showIconText = (mEnableIconText ? View.VISIBLE : View.GONE);
                    textStyle = BATTERY_TEXT_STYLE_MIN;
                    textSize = mBatteryLevel == 100 ? 6.0f : 9.0f;
                }
            } else if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                showText = (View.VISIBLE);
                textStyle = BATTERY_TEXT_STYLE_NORMAL;
                pxTextPadding = (int) (4 * logicalDensity + 0.5);
            } else if (mBatteryStyle == BATTERY_STYLE_ICON_MINI_CUST) {
                showEmptyIcon = mEnableThemeDefault ? View.GONE : View.VISIBLE;
                iconEmptyStyle = getIconEmptyStyleNormalMin();
                showIcon = (View.VISIBLE);
                if (mEnableThemeDefault) {
                    iconStyle = isBatteryStatusCharging() ?
                                    getIconStyleChargeMin() : getIconStyleNormalMin();
                } else {
                    iconStyle = isBatteryStatusCharging() ?
                                    getIconStyleChargeMinCust() : getIconStyleNormalMinCust();
                }
                showText = (mEnableIconText ? View.VISIBLE : View.GONE);
            } else if (mBatteryStyle == BATTERY_STYLE_ABM) {
                showIcon = (View.VISIBLE);
                iconStyle = isBatteryStatusCharging() ?
                                getIconStyleChargeABM() : getIconStyleNormalABM();
                showIconText = (mEnableIconText ? View.VISIBLE : View.GONE);
                textStyle = BATTERY_TEXT_STYLE_MIN;
                textSize = mBatteryLevel == 100 ? 6.0f : 8.0f;
            } else if (mBatteryStyle == BATTERY_STYLE_HCOMB) {
                showIcon = (View.VISIBLE);
                iconStyle = isBatteryStatusCharging() ?
                                getIconStyleChargeHCOMB() : getIconStyleNormalHCOMB();
                showIconText = (mEnableIconText ? View.VISIBLE : View.GONE);
                textStyle = BATTERY_TEXT_STYLE_MIN;
                textSize = mBatteryLevel == 100 ? 6.0f : 8.0f;
            }
        }

        int batteryWarningColor = mContext.getResources().getColor(
                com.android.internal.R.color.holo_red_light);

        int N = mIconEmptyViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconEmptyViews.get(i);
            v.setVisibility(showEmptyIcon);
            v.setColorFilter(mEmptyColor, Mode.SRC_ATOP);
            v.setImageResource(iconEmptyStyle);
        }
        N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setVisibility(showIcon);
            if (mBatteryStyle == BATTERY_STYLE_NORMAL_CUST ||
                mBatteryStyle == BATTERY_STYLE_ICON_MINI_CUST) {
                if (!mEnableThemeDefault) {
                    // turn battery fill color red at 14% when not on charger - same level android battery warning appears
                    // if no custom color is defined or theme default is enabled && over 14% use default color
                    // else use custom user color
                    if (mBatteryLevel <= 14 && !mBatteryPlugged) {
                        v.setColorFilter(batteryWarningColor, Mode.SRC_ATOP);
                    } else {
                        v.setColorFilter(mFillColor, Mode.SRC_ATOP);
                    }
                } else {
                    v.setColorFilter(null);
                }
            } else {
                v.setColorFilter(null);
            }
            v.setImageResource(iconStyle);
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setVisibility(showText);
            v.setText(mContext.getString(textStyle, mBatteryLevel));
            v.setPadding(v.getPaddingLeft(),v.getPaddingTop(),pxTextPadding,v.getPaddingBottom());

            // turn text red at 14% when not on charger - same level android battery warning appears
            // if no custom color is defined or theme default is enabled && over 14% use default color
            // else use custom user color
            if (mBatteryLevel <= 14 && !mBatteryPlugged) {
                v.setTextColor(batteryWarningColor);
            } else if (mBatteryPlugged) {
                v.setTextColor(mTextChargingColor);
            } else {
                v.setTextColor(mTextColor);
            }
        }
        N = mIconLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mIconLabelViews.get(i);
            v.setVisibility(showIconText);
            v.setText(mContext.getString(textStyle, mBatteryLevel));
            v.setTextSize(textSize);

            // turn text red at 14% when not on charger - same level android battery warning appears
            // if no custom color is defined or theme default is enabled && over 14% use default color
            // else use custom user color
            if (mBatteryLevel <= 14 && !mBatteryPlugged) {
                v.setTextColor(batteryWarningColor);
            } else if (mBatteryPlugged) {
                v.setTextColor(mTextChargingColor);
            } else {
                v.setTextColor(mTextColor);
            }
        }   
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mShowBatteryStatus = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS, 1) == 1;
        mBatteryStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, BATTERY_STYLE_CIRCLE, UserHandle.USER_CURRENT);
        mEnableThemeDefault = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_ENABLE_THEME_DEFAULT, 1) == 1;
        mEnableIconText = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT, 1) == 1;
        int defaultEmptyColor = mContext.getResources().getColor(R.color.battery_empty_color);
        int defaultFillColor = mContext.getResources().getColor(
                com.android.internal.R.color.holo_blue_dark);
        int defaultTextColor = mContext.getResources().getColor(
                com.android.internal.R.color.white);
        int defaultTextChargingColor = Color.GREEN;
        int fillColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_FILL_COLOR, defaultFillColor);
        int emptyColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_EMPTY_COLOR, defaultEmptyColor);
        int textColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, defaultTextColor);
        int textChargingColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, defaultTextChargingColor);

        mEmptyColor = mEnableThemeDefault ? defaultEmptyColor : emptyColor;
        mFillColor = mEnableThemeDefault ? defaultFillColor : fillColor;
        mTextColor = mEnableThemeDefault ? defaultTextColor : textColor;
        mTextChargingColor = mEnableThemeDefault ? defaultTextChargingColor : textChargingColor;

        updateBattery();
    }
}
