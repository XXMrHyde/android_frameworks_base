/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.widget.LockPatternUtils;

import java.text.NumberFormat;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    private TextView mAlarmStatusView;
    private TextClock mDateView;
    private TextClock mClockView;
    private View mAmbientDisplayWeatherLayout;
    private TextView mAmbientDisplayWeatherLT;
    private TextView mAmbientDisplayWeatherC;
    private ImageView mAmbientDisplayWeatherIcon;
    private TextView mAmbientDisplayBatteryView;
    private TextView mOwnerInfo;
    private KeyguardButtonBar mButtonBar;

    private boolean mDozing = false;

    private final int mWarningColor = 0xfff4511e; // deep orange 600
    private int mIconColor = 0xffffffff;
    private int mPrimaryTextColor;
    private int mSecondaryTextColor = 0xb3ffffff;;

    private WeatherController mWeatherController;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            refresh();
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        mDateView = (TextClock) findViewById(R.id.date_view);
        mClockView = (TextClock) findViewById(R.id.clock_view);
        mDateView.setShowCurrentUserTime(true);
        mClockView.setShowCurrentUserTime(true);
        mAmbientDisplayWeatherLayout = findViewById(R.id.ambient_display_weather_layout);
        mAmbientDisplayWeatherLT = (TextView) findViewById(R.id.ambient_display_weather_location_temp);
        mAmbientDisplayWeatherIcon = (ImageView) findViewById(R.id.ambient_display_weather_icon);
        mAmbientDisplayWeatherC = (TextView) findViewById(R.id.ambient_display_weather_condition);
        mAmbientDisplayBatteryView = (TextView) findViewById(R.id.ambient_display_battery_view);
        mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        mButtonBar = (KeyguardButtonBar) findViewById(R.id.button_bar);

        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
    }

    public void setWeatherController(WeatherController controller) {
        mWeatherController = controller;
    }

    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
        updateColors();
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            mAlarmStatusView.setVisibility(View.VISIBLE);
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
            mButtonBar.setOwnerInfoVisibility(true);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
            mButtonBar.setOwnerInfoVisibility(false);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
    }

    private String getOwnerInfo() {
        ContentResolver res = getContext().getContentResolver();
        String info = null;
        final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                KeyguardUpdateMonitor.getCurrentUser());
        if (ownerInfoEnabled) {
            info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
        }
        return info;
    }

    private void updateColors() {
        final ContentResolver resolver = getContext().getContentResolver();

        mIconColor = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_ICON_COLOR, 0xffffffff);
        mPrimaryTextColor = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_TEXT_COLOR, 0xffffffff);
        // mPrimaryTextColor with a transparency of 70%
        mSecondaryTextColor = (179 << 24) | (mPrimaryTextColor & 0x00ffffff);

        mAlarmStatusView.setTextColor(mSecondaryTextColor);
        mAlarmStatusView.setCompoundDrawableTintList(ColorStateList.valueOf(mSecondaryTextColor));
        mDateView.setTextColor(mPrimaryTextColor);
        mClockView.setTextColor(mPrimaryTextColor);
        mOwnerInfo.setTextColor(mSecondaryTextColor);
        refreshWeatherInfo();
    }

    private void refreshBatteryInfo() {
        if (!mDozing) {
            if (mAmbientDisplayBatteryView.getVisibility() != View.GONE) {
                mAmbientDisplayBatteryView.setVisibility(View.GONE);
            }
        } else if (showBatteryOnAmbientDisplay()) {
            if (mAmbientDisplayBatteryView.getVisibility() != View.VISIBLE) {
                mAmbientDisplayBatteryView.setVisibility(View.VISIBLE);
            }
            final Resources res = getContext().getResources();
            KeyguardUpdateMonitor.BatteryStatus batteryStatus =
                    KeyguardUpdateMonitor.getInstance(mContext).getBatteryStatus();

            String percentage = "";
            int resId = 0;
            final int lowLevel = res.getInteger(
                    com.android.internal.R.integer.config_lowBatteryWarningLevel);
            final boolean useWarningColor = batteryStatus == null || batteryStatus.status == 1
                    || (batteryStatus.level <= lowLevel && !batteryStatus.isPluggedIn());

            if (batteryStatus != null) {
                percentage = NumberFormat.getPercentInstance().format((double) batteryStatus.level / 100.0);
            }
            if (batteryStatus == null || batteryStatus.status == 1) {
                resId = R.drawable.ic_battery_unknown;
            } else {
                if (batteryStatus.level >= 96) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_full : R.drawable.ic_battery_full;
                } else if (batteryStatus.level >= 90) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_90 : R.drawable.ic_battery_90;
                } else if (batteryStatus.level >= 80) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_80 : R.drawable.ic_battery_80;
                } else if (batteryStatus.level >= 60) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_60 : R.drawable.ic_battery_60;
                } else if (batteryStatus.level >= 50) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_50 : R.drawable.ic_battery_50;
                } else if (batteryStatus.level >= 30) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_30 : R.drawable.ic_battery_30;
                } else if (batteryStatus.level >= lowLevel) {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_20 : R.drawable.ic_battery_20;
                } else {
                    resId = batteryStatus.isPluggedIn()
                            ? R.drawable.ic_battery_charging_20 : R.drawable.ic_battery_alert;
                }
            }
            Drawable icon = resId > 0 ? res.getDrawable(resId).mutate() : null;
            if (icon != null) {
                icon.setTintList(ColorStateList.valueOf(useWarningColor ? mWarningColor : mIconColor));
            }

            mAmbientDisplayBatteryView.setText(percentage);
            mAmbientDisplayBatteryView.setTextColor(useWarningColor
                    ? mWarningColor : mPrimaryTextColor);
            mAmbientDisplayBatteryView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
        }
    }

    private void refreshWeatherInfo() {
        if (mWeatherController == null) {
            mAmbientDisplayWeatherLayout.setVisibility(View.GONE);
            return;
        }

        final boolean visible = (showWeather() && !mDozing) || (showWeatherOnAmbientDisplay() && mDozing);
        if (visible && mAmbientDisplayWeatherLayout.getVisibility() != View.VISIBLE) {
            mAmbientDisplayWeatherLayout.setVisibility(View.VISIBLE);
        } else if (!visible && mAmbientDisplayWeatherLayout.getVisibility() != View.GONE) {
            mAmbientDisplayWeatherLayout.setVisibility(View.GONE);
        }

        WeatherController.WeatherInfo info = mWeatherController.getWeatherInfo();
        if (info.temp != null && info.condition != null && info.conditionDrawableMonochrome != null) {
            String locationTemp = (showWeatherLocation() ? info.city + ", " : "") + info.temp;
            Drawable icon = info.conditionDrawableMonochrome.getConstantState().newDrawable();
            mAmbientDisplayWeatherLT.setText(locationTemp);
            mAmbientDisplayWeatherC.setText(info.condition);
            mAmbientDisplayWeatherLT.setTextColor(mPrimaryTextColor);
            mAmbientDisplayWeatherC.setTextColor(mSecondaryTextColor);
            mAmbientDisplayWeatherIcon.setImageDrawable(icon);
            mAmbientDisplayWeatherIcon.setColorFilter(mIconColor, Mode.MULTIPLY);
        } else {
            mAmbientDisplayWeatherLT.setText("");
            mAmbientDisplayWeatherC.setText("");
            mAmbientDisplayWeatherIcon.setImageDrawable(null);
            mAmbientDisplayWeatherLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setDozing(boolean dozing) {
        mDozing = dozing;
        refreshBatteryInfo();
        refreshWeatherInfo();
    }

    private boolean showWeather() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0) == 1;
    }

    private boolean showWeatherLocation() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1;
    }

    private boolean showBatteryOnAmbientDisplay() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AMBIENT_DISPLAY_SHOW_BATTERY, 1) == 1;
    }

    private boolean showWeatherOnAmbientDisplay() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AMBIENT_DISPLAY_SHOW_WEATHER, 0) == 1;
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }
}
