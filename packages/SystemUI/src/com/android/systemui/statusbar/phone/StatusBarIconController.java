/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.darkkat.ColorHelper;
import com.android.internal.util.darkkat.DeviceUtils;
import com.android.internal.util.darkkat.StatusBarColorHelper;

import com.android.keyguard.CarrierText;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.darkkat.statusbar.StatusBarWeather;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.BatteryBar;
import com.android.systemui.statusbar.policy.NetworkTraffic;
import com.android.systemui.statusbar.policy.StatusBarCarrierText;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Controls everything regarding the icons in the status bar and on Keyguard, including, but not
 * limited to: notification icons, signal cluster, additional status icons, and clock in the status
 * bar.
 */
public class StatusBarIconController implements Tunable {

    public static final long DEFAULT_TINT_ANIMATION_DURATION = 120;

    public static final String ICON_BLACKLIST = "icon_blacklist";

    private static final int CLOCK_STYLE_DEFAULT  = 0;
    private static final int CLOCK_STYLE_CENTERED = 1;
    private static final int CLOCK_STYLE_HIDDEN   = 2;

    private static final int CARRIER_TEXT_COLOR          = 0;
    private static final int BATTERY_COLORS              = 1;
    private static final int CLOCK_COLOR                 = 2;
    private static final int WEATHER_COLORS              = 3;
    private static final int NETWORK_TRAFFIC_COLORS      = 4;
    private static final int STATUS_NETWORK_ICON_COLORS  = 5;

    private Context mContext;
    private View mStatusBar;
    private PhoneStatusBar mPhoneStatusBar;
    private KeyguardStatusBarView mKeyguardStatusBarView;
    private Interpolator mLinearOutSlowIn;
    private Interpolator mFastOutSlowIn;
    private DemoStatusIcons mDemoStatusIcons;
    private NotificationColorUtil mNotificationColorUtil;

    private LinearLayout mStatusBarContents;
    private LinearLayout mSystemIconArea;
    private LinearLayout mStatusIcons;
    private LinearLayout mStatusIconsKeyguard;
    private SignalClusterView mSignalCluster;
    private SignalClusterView mSignalClusterKeyguard;
    private StatusBarCarrierText mCarrierText;
    private CarrierText mCarrierTextKeyguard;
    private IconMerger mNotificationIcons;
    private View mNotificationIconArea;
    private ImageView mMoreIcon;
    private BatteryMeterView mBatteryMeterView;
    private BatteryMeterView mBatteryMeterViewKeyguard;
    private TextView mBatteryLevelKeyguard;
    private BatteryBar mBatteryBar;
    private BatteryBar mBatteryBarKeyguard;
    private Clock mClockDefault;
    private Clock mClockCentered;
    private LinearLayout mCenterClockLayout;
    private StatusBarWeather mWeatherLayout;
    private NetworkTraffic mNetworkTraffic;
    private NetworkTraffic mNetworkTrafficKeyguard;
    private Ticker mTicker;
    private View mTickerView;

    private int mCarrierTextColor;
    private int mCarrierTextColorOld;
    private int mCarrierTextColorTint;
    private int mBatteryColor;
    private int mBatteryColorOld;
    private int mBatteryColorTint;
    private int mBatteryTextColor;
    private int mBatteryTextColorOld;
    private int mBatteryTextColorTint;
    private int mClockColor;
    private int mClockColorOld;
    private int mClockColorTint;
    private int mWeatherTextColor;
    private int mWeatherTextColorOld;
    private int mWeatherTextColorTint;
    private int mWeatherIconColor;
    private int mWeatherIconColorOld;
    private int mWeatherIconColorTint;
    private int mNetworkTrafficTextColor;
    private int mNetworkTrafficTextColorOld;
    private int mNetworkTrafficTextColorTint;
    private int mNetworkTrafficIconColor;
    private int mNetworkTrafficIconColorOld;
    private int mNetworkTrafficIconColorTint;
    private int mNetworkSignalColor;
    private int mNetworkSignalColorOld;
    private int mNetworkSignalColorTint;
    private int mNoSimColor;
    private int mNoSimColorOld;
    private int mNoSimColorTint;
    private int mAirplaneModeColor;
    private int mAirplaneModeColorOld;
    private int mAirplaneModeColorTint;
    private int mStatusIconColor;
    private int mStatusIconColorOld;
    private int mStatusIconColorTint;
    private int mNotificationIconColor;
    private int mNotificationIconColorTint;
    private int mTickerTextColor;
    private int mTickerTextColorTint;
    private float mDarkIntensity;

    private int mIconSize;
    private int mIconHPadding;

    private boolean mTransitionPending;
    private boolean mTintChangePending;
    private float mPendingDarkIntensity;

    private Animator mColorTransitionAnimator;
    private ValueAnimator mTintAnimator;

    private boolean mShowBatteryBar;
    private int mClockStyle;
    private int mColorToChange;

    private boolean mShowTicker = false;
    private boolean mTicking;

    private final Handler mHandler;
    private boolean mTransitionDeferring;
    private long mTransitionDeferringStartTime;
    private long mTransitionDeferringDuration;

    private final ArraySet<String> mIconBlacklist = new ArraySet<>();

    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() {
        @Override
        public void run() {
            mTransitionDeferring = false;
        }
    };

    public StatusBarIconController(Context context, View statusBar, KeyguardStatusBarView keyguardStatusBar,
            PhoneStatusBar phoneStatusBar) {
        mContext = context;
        mStatusBar = statusBar;
        mPhoneStatusBar = phoneStatusBar;
        mKeyguardStatusBarView = keyguardStatusBar;
        mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        mStatusBarContents = (LinearLayout) statusBar.findViewById(R.id.status_bar_contents);
        mSystemIconArea = (LinearLayout) statusBar.findViewById(R.id.system_icon_area);
        mStatusIcons = (LinearLayout) statusBar.findViewById(R.id.statusIcons);
        mSignalCluster = (SignalClusterView) statusBar.findViewById(R.id.signal_cluster);
        mSignalClusterKeyguard = (SignalClusterView) keyguardStatusBar.findViewById(R.id.signal_cluster);
        mNotificationIconArea = statusBar.findViewById(R.id.notification_icon_area_inner);
        mCarrierText = (StatusBarCarrierText) statusBar.findViewById(R.id.status_bar_carrier_text);
        mCarrierTextKeyguard = (CarrierText) keyguardStatusBar.findViewById(R.id.keyguard_carrier_text);
        mNotificationIcons = (IconMerger) statusBar.findViewById(R.id.notificationIcons);
        mMoreIcon = (ImageView) statusBar.findViewById(R.id.moreIcon);
        mNotificationIcons.setOverflowIndicator(mMoreIcon);
        mStatusIconsKeyguard = (LinearLayout) keyguardStatusBar.findViewById(R.id.statusIcons);
        mBatteryMeterView = (BatteryMeterView) statusBar.findViewById(R.id.battery);
        mBatteryMeterViewKeyguard = (BatteryMeterView) keyguardStatusBar.findViewById(R.id.battery);
        mBatteryLevelKeyguard = ((TextView) keyguardStatusBar.findViewById(R.id.battery_level));
        mBatteryBar = (BatteryBar) statusBar.findViewById(R.id.battery_bar);
        mBatteryBarKeyguard = (BatteryBar) keyguardStatusBar.findViewById(R.id.battery_bar);
        mClockDefault = (Clock) statusBar.findViewById(R.id.clock);
        mClockCentered = (Clock) statusBar.findViewById(R.id.center_clock);
        mCenterClockLayout = (LinearLayout) statusBar.findViewById(R.id.center_clock_layout);
        mWeatherLayout = (StatusBarWeather) statusBar.findViewById(R.id.status_bar_weather_layout);
        mNetworkTraffic = (NetworkTraffic) statusBar.findViewById(R.id.network_traffic_layout);
        mNetworkTrafficKeyguard = (NetworkTraffic) keyguardStatusBar.findViewById(
                R.id.keyguard_network_traffic_layout);
        mLinearOutSlowIn = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.linear_out_slow_in);
        mFastOutSlowIn = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.fast_out_slow_in);
        mHandler = new Handler();
        updateResources();
        TunerService.get(mContext).addTunable(this, ICON_BLACKLIST);
        setUpCustomColors();
    }

    private void setUpCustomColors() {
        mCarrierTextColor = StatusBarColorHelper.getCarrierLabelColor(mContext);
        mCarrierTextColorOld = mCarrierTextColor;
        mCarrierTextColorTint = mCarrierTextColor;
        mBatteryColor = StatusBarColorHelper.getBatteryColor(mContext);
        mBatteryColorOld = mBatteryColor;
        mBatteryColorTint = mBatteryColor;
        mBatteryTextColor = StatusBarColorHelper.getBatteryTextColor(mContext);
        mBatteryTextColorOld = mBatteryTextColor;
        mBatteryTextColorTint = mBatteryTextColor;
        mClockColor = StatusBarColorHelper.getClockColor(mContext);
        mClockColorOld = mClockColor;
        mClockColorTint = mClockColor;
        mWeatherTextColor = StatusBarColorHelper.getWeatherTextColor(mContext);
        mWeatherTextColorOld = mWeatherTextColor;
        mWeatherTextColorTint = mWeatherTextColor;
        mWeatherIconColor = StatusBarColorHelper.getWeatherIconColor(mContext);
        mWeatherIconColorOld = mWeatherIconColor;
        mWeatherIconColorTint = mWeatherIconColor;
        mNetworkTrafficTextColor = StatusBarColorHelper.getNetworkTrafficTextColor(mContext);
        mNetworkTrafficTextColorOld = mNetworkTrafficTextColor;
        mNetworkTrafficTextColorTint = mNetworkTrafficTextColor;
        mNetworkTrafficIconColor = StatusBarColorHelper.getNetworkTrafficIconColor(mContext);
        mNetworkTrafficIconColorOld = mNetworkTrafficIconColor;
        mNetworkTrafficIconColorTint = mNetworkTrafficIconColor;
        mNetworkSignalColor = StatusBarColorHelper.getNetworkSignalColor(mContext);
        mNetworkSignalColorOld = mNetworkSignalColor;
        mNetworkSignalColorTint = mNetworkSignalColor;
        mNoSimColor = StatusBarColorHelper.getNoSimColor(mContext);
        mNoSimColorOld = mNoSimColor;
        mNoSimColorTint = mNoSimColor;
        mAirplaneModeColor = StatusBarColorHelper.getAirplaneModeColor(mContext);
        mAirplaneModeColorOld = mAirplaneModeColor;
        mAirplaneModeColorTint = mAirplaneModeColor;
        mStatusIconColor = StatusBarColorHelper.getStatusIconColor(mContext);
        mStatusIconColorOld = mStatusIconColor;
        mStatusIconColorTint = mStatusIconColor;
        mNotificationIconColor = StatusBarColorHelper.getNotificationIconColor(mContext);
        mNotificationIconColorTint = mNotificationIconColor;
        mTickerTextColor = StatusBarColorHelper.getTickerTextColor(mContext);
        mTickerTextColorTint = mTickerTextColor;

        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!ICON_BLACKLIST.equals(key)) {
            return;
        }
        mIconBlacklist.clear();
        mIconBlacklist.addAll(getIconBlacklist(newValue));
        ArrayList<StatusBarIconView> views = new ArrayList<StatusBarIconView>();
        // Get all the current views.
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            views.add((StatusBarIconView) mStatusIcons.getChildAt(i));
        }
        // Remove all the icons.
        for (int i = views.size() - 1; i >= 0; i--) {
            removeSystemIcon(views.get(i).getSlot(), i, i);
        }
        // Add them all back
        for (int i = 0; i < views.size(); i++) {
            addSystemIcon(views.get(i).getSlot(), i, i, views.get(i).getStatusBarIcon());
        }
    };

    public void updateResources() {
        mIconSize = mContext.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_icon_size);
        mIconHPadding = mContext.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_padding);
        FontSizeUtils.updateFontSize(mClockDefault, R.dimen.status_bar_clock_size);
        FontSizeUtils.updateFontSize(mClockCentered, R.dimen.status_bar_clock_size);
    }

    public void addSystemIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        boolean blocked = mIconBlacklist.contains(slot);
        StatusBarIconView view = new StatusBarIconView(mContext, slot, null, blocked);
        view.set(icon);
        mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize));
        view = new StatusBarIconView(mContext, slot, null, blocked);
        view.set(icon);
        mStatusIconsKeyguard.addView(view, viewIndex, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize));
        applyIconTint();
        updateStatusIconKeyguardColor();
    }

    public void updateSystemIcon(String slot, int index, int viewIndex,
            StatusBarIcon old, StatusBarIcon icon) {
        StatusBarIconView view = (StatusBarIconView) mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
        view = (StatusBarIconView) mStatusIconsKeyguard.getChildAt(viewIndex);
        view.set(icon);
        applyIconTint();
        updateStatusIconKeyguardColor();
    }

    public void removeSystemIcon(String slot, int index, int viewIndex) {
        mStatusIcons.removeViewAt(viewIndex);
        mStatusIconsKeyguard.removeViewAt(viewIndex);
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                mIconSize + 2*mIconHPadding, mPhoneStatusBar.getStatusBarHeight());

        ArrayList<NotificationData.Entry> activeNotifications =
                notificationData.getActiveNotifications();
        final int N = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(N);

        // Filter out ambient notifications and notification children.
        for (int i = 0; i < N; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            if (notificationData.isAmbient(ent.key)
                    && !NotificationData.showNotificationEvenIfUnprovisioned(ent.notification)) {
                continue;
            }
            if (!PhoneStatusBar.isTopLevelChild(ent)) {
                continue;
            }
            toShow.add(ent.icon);
        }

        ArrayList<View> toRemove = new ArrayList<>();
        for (int i=0; i<mNotificationIcons.getChildCount(); i++) {
            View child = mNotificationIcons.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        final int toRemoveCount = toRemove.size();
        for (int i = 0; i < toRemoveCount; i++) {
            mNotificationIcons.removeView(toRemove.get(i));
        }

        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mNotificationIcons.addView(v, i, params);
            }
        }

        // Resort notification icons
        final int childCount = mNotificationIcons.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View actual = mNotificationIcons.getChildAt(i);
            StatusBarIconView expected = toShow.get(i);
            if (actual == expected) {
                continue;
            }
            mNotificationIcons.removeView(expected);
            mNotificationIcons.addView(expected, i);
        }

        applyNotificationIconsTint();
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(mSystemIconArea, animate);
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            animateHide(mCenterClockLayout, animate);
        }
        if (mShowBatteryBar) {
            animateHide(mBatteryBar, animate);
        }
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(mSystemIconArea, animate);
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            animateShow(mCenterClockLayout, animate);
        }
        if (mShowBatteryBar) {
            animateShow(mBatteryBar, animate);
        }
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(mNotificationIconArea, animate);
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(mNotificationIconArea, animate);
    }

    public void setClockVisibility(boolean visible) {
        if (mClockStyle == CLOCK_STYLE_DEFAULT) {
            mClockDefault.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            mClockCentered.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void dump(PrintWriter pw) {
        int N = mStatusIcons.getChildCount();
        pw.println("  system icons: " + N);
        for (int i=0; i<N; i++) {
            StatusBarIconView ic = (StatusBarIconView) mStatusIcons.getChildAt(i);
            pw.println("    [" + i + "] icon=" + ic);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (mDemoStatusIcons == null) {
            mDemoStatusIcons = new DemoStatusIcons(mStatusIcons, mIconSize);
        }
        mDemoStatusIcons.dispatchDemoCommand(command, args);
    }

    public void dispatchClockDemoCommand(String command, Bundle args) {
        if (mClockStyle == CLOCK_STYLE_DEFAULT) {
            mClockDefault.dispatchDemoCommand(command, args);
        }
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            mClockCentered.dispatchDemoCommand(command, args);
        }
    }

    /**
     * Hides a view.
     */
    private void animateHide(final View v, boolean animate) {
        v.animate().cancel();
        if (!animate) {
            v.setAlpha(0f);
            v.setVisibility(View.INVISIBLE);
            return;
        }
        v.animate()
                .alpha(0f)
                .setDuration(160)
                .setStartDelay(0)
                .setInterpolator(PhoneStatusBar.ALPHA_OUT)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.setVisibility(View.INVISIBLE);
                    }
                });
    }

    /**
     * Shows a view, and synchronizes the animation with Keyguard exit animations, if applicable.
     */
    private void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(View.VISIBLE);
        if (!animate) {
            v.setAlpha(1f);
            return;
        }
        v.animate()
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(PhoneStatusBar.ALPHA_IN)
                .setStartDelay(50)

                // We need to clean up any pending end action from animateHide if we call
                // both hide and show in the same frame before the animation actually gets started.
                // cancel() doesn't really remove the end action.
                .withEndAction(null);

        // Synchronize the motion with the Keyguard fading if necessary.
        if (mPhoneStatusBar.isKeyguardFadingAway()) {
            v.animate()
                    .setDuration(mPhoneStatusBar.getKeyguardFadingAwayDuration())
                    .setInterpolator(mLinearOutSlowIn)
                    .setStartDelay(mPhoneStatusBar.getKeyguardFadingAwayDelay())
                    .start();
        }
    }

    public void setIconsDark(boolean dark, boolean animate) {
        if (!animate) {
            setIconTintInternal(dark ? 1.0f : 0.0f);
        } else if (mTransitionPending) {
            deferIconTintChange(dark ? 1.0f : 0.0f);
        } else if (mTransitionDeferring) {
            animateIconTint(dark ? 1.0f : 0.0f,
                    Math.max(0, mTransitionDeferringStartTime - SystemClock.uptimeMillis()),
                    mTransitionDeferringDuration);
        } else {
            animateIconTint(dark ? 1.0f : 0.0f, 0 /* delay */, DEFAULT_TINT_ANIMATION_DURATION);
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay,
            long duration) {
        if (mTintAnimator != null) {
            mTintAnimator.cancel();
        }
        if (mDarkIntensity == targetDarkIntensity) {
            return;
        }
        mTintAnimator = ValueAnimator.ofFloat(mDarkIntensity, targetDarkIntensity);
        mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setIconTintInternal((Float) animation.getAnimatedValue());
            }
        });
        mTintAnimator.setDuration(duration);
        mTintAnimator.setStartDelay(delay);
        mTintAnimator.setInterpolator(mFastOutSlowIn);
        mTintAnimator.start();
    }

    private void setIconTintInternal(float darkIntensity) {
        mDarkIntensity = darkIntensity;
        if (DeviceUtils.deviceSupportsMobileData(mContext)) {
            mCarrierTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                    mCarrierTextColor, StatusBarColorHelper.getCarrierLabelColorDark(mContext));
        }
        mBatteryColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mBatteryColor, StatusBarColorHelper.getBatteryColorDark(mContext));
        mBatteryTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mBatteryTextColor, StatusBarColorHelper.getBatteryTextColorDark(mContext));
        mClockColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mClockColor, StatusBarColorHelper.getClockColorDark(mContext));
        mWeatherTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mWeatherTextColor, StatusBarColorHelper.getWeatherTextColorDark(mContext));
        mWeatherIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mWeatherIconColor, StatusBarColorHelper.getWeatherIconColorDark(mContext));
        mNetworkTrafficTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNetworkTrafficTextColor, StatusBarColorHelper.getNetworkTrafficTextColorDark(mContext));
        mNetworkTrafficIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNetworkTrafficIconColor, StatusBarColorHelper.getNetworkTrafficIconColorDark(mContext));
        mNetworkSignalColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNetworkSignalColor, StatusBarColorHelper.getNetworkSignalColorDark(mContext));
        mNoSimColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNoSimColor, StatusBarColorHelper.getNoSimColorDark(mContext));
        mAirplaneModeColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mAirplaneModeColor, StatusBarColorHelper.getAirplaneModeColorDark(mContext));
        mStatusIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mStatusIconColor, StatusBarColorHelper.getStatusIconColorDark(mContext));
        mNotificationIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNotificationIconColor, StatusBarColorHelper.getNotificationIconColorDark(mContext));
        mTickerTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mTickerTextColor, StatusBarColorHelper.getTickerTextColorDark(mContext));

        applyIconTint();
    }

    private void deferIconTintChange(float darkIntensity) {
        if (mTintChangePending && darkIntensity == mPendingDarkIntensity) {
            return;
        }
        mTintChangePending = true;
        mPendingDarkIntensity = darkIntensity;
    }

    private void applyIconTint() {
        if (DeviceUtils.deviceSupportsMobileData(mContext)) {
            mCarrierText.setTextColor(mCarrierTextColorTint);
        }
        mBatteryMeterView.setBatteryColors(mBatteryColorTint);
        mBatteryMeterView.setTextColor(mBatteryTextColorTint);
        mBatteryBar.setColor(mBatteryColorTint);
        if (mClockStyle == CLOCK_STYLE_DEFAULT) {
            mClockDefault.setTextColor(mClockColorTint);
        }
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            mClockCentered.setTextColor(mClockColorTint);
        }
        mWeatherLayout.setTextColor(mWeatherTextColorTint);
        mWeatherLayout.setIconColor(mWeatherIconColorTint);
        mNetworkTraffic.setTextColor(mNetworkTrafficTextColorTint);
        mNetworkTraffic.setIconColor(mNetworkTrafficIconColorTint);
        mSignalCluster.setIconTint(
                mNetworkSignalColorTint, mNoSimColorTint, mAirplaneModeColorTint);
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mStatusIcons.getChildAt(i);
            v.setColorFilter(mStatusIconColorTint, Mode.MULTIPLY);
        }
        mMoreIcon.setColorFilter(mNotificationIconColorTint, Mode.MULTIPLY);
        applyNotificationIconsTint();
        if (mTicker != null && mTickerView != null) {
            mTicker.setTextColor(mTickerTextColorTint);
        }
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mNotificationIcons.getChildAt(i);
            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
            boolean colorize = !isPreL || isGrayscale(v);
            if (colorize) {
                v.setImageTintList(ColorStateList.valueOf(mNotificationIconColorTint));
            }
        }
        if (mTicker != null && mTickerView != null) {
            mTicker.setIconColorTint(ColorStateList.valueOf(mNotificationIconColorTint));
        }
    }

    private boolean isGrayscale(StatusBarIconView v) {
        Object isGrayscale = v.getTag(R.id.icon_is_grayscale);
        if (isGrayscale != null) {
            return Boolean.TRUE.equals(isGrayscale);
        }
        boolean grayscale = mNotificationColorUtil.isGrayscaleIcon(v.getDrawable());
        v.setTag(R.id.icon_is_grayscale, grayscale);
        return grayscale;
    }

    public void appTransitionPending() {
        mTransitionPending = true;
    }

    public void appTransitionCancelled() {
        if (mTransitionPending && mTintChangePending) {
            mTintChangePending = false;
            animateIconTint(mPendingDarkIntensity, 0 /* delay */, DEFAULT_TINT_ANIMATION_DURATION);
        }
        mTransitionPending = false;
    }

    public void appTransitionStarting(long startTime, long duration) {
        if (mTransitionPending && mTintChangePending) {
            mTintChangePending = false;
            animateIconTint(mPendingDarkIntensity,
                    Math.max(0, startTime - SystemClock.uptimeMillis()),
                    duration);

        } else if (mTransitionPending) {

            // If we don't have a pending tint change yet, the change might come in the future until
            // startTime is reached.
            mTransitionDeferring = true;
            mTransitionDeferringStartTime = startTime;
            mTransitionDeferringDuration = duration;
            mHandler.removeCallbacks(mTransitionDeferringDoneRunnable);
            mHandler.postAtTime(mTransitionDeferringDoneRunnable, startTime);
        }
        mTransitionPending = false;
    }

    public static ArraySet<String> getIconBlacklist(String blackListStr) {
        ArraySet<String> ret = new ArraySet<String>();
        if (blackListStr != null) {
            String[] blacklist = blackListStr.split(",");
            for (String slot : blacklist) {
                if (!TextUtils.isEmpty(slot)) {
                    ret.add(slot);
                }
            }
        }
        return ret;
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mColorToChange == CARRIER_TEXT_COLOR) {
                    final int blended = ColorHelper.getBlendColor(
                            mCarrierTextColorOld, mCarrierTextColor, position);
                    mCarrierText.setTextColor(blended);
                } else if (mColorToChange == BATTERY_COLORS) {
                    final int blended = ColorHelper.getBlendColor(
                            mBatteryColorOld, mBatteryColor, position);
                    final int blendedText = ColorHelper.getBlendColor(
                            mBatteryTextColorOld, mBatteryTextColor, position);
                    mBatteryMeterView.setBatteryColors(blended);
                    mBatteryMeterView.setTextColor(blendedText);
                    mBatteryBar.setColor(blended);
                } else if (mColorToChange == CLOCK_COLOR) {
                    final int blended = ColorHelper.getBlendColor(
                            mClockColorOld, mClockColor, position);
                    if (mClockStyle == CLOCK_STYLE_DEFAULT) {
                        mClockDefault.setTextColor(blended);
                    }
                    if (mClockStyle == CLOCK_STYLE_CENTERED) {
                        mClockCentered.setTextColor(blended);
                    }
                } else if (mColorToChange == WEATHER_COLORS) {
                    final int blendedText = ColorHelper.getBlendColor(
                            mWeatherTextColorOld, mWeatherTextColor, position);
                    final int blendedIcon = ColorHelper.getBlendColor(
                            mWeatherIconColorOld, mWeatherIconColor, position);
                    mWeatherLayout.setTextColor(blendedText);
                    mWeatherLayout.setIconColor(blendedIcon);
                } else if (mColorToChange == NETWORK_TRAFFIC_COLORS) {
                    final int blendedText = ColorHelper.getBlendColor(
                            mNetworkTrafficTextColorOld, mNetworkTrafficTextColor, position);
                    final int blendedIcon = ColorHelper.getBlendColor(
                            mNetworkTrafficIconColorOld, mNetworkTrafficIconColor, position);
                    mNetworkTraffic.setTextColor(blendedText);
                    mNetworkTraffic.setIconColor(blendedIcon);
                } else if (mColorToChange == STATUS_NETWORK_ICON_COLORS) {
                    final int blendedStatus = ColorHelper.getBlendColor(
                            mStatusIconColorOld, mStatusIconColor, position);
                    final int blendedSignal = ColorHelper.getBlendColor(
                            mNetworkSignalColorOld, mNetworkSignalColor, position);
                    final int blendedNoSim = ColorHelper.getBlendColor(
                            mNoSimColorOld, mNoSimColor, position);
                    final int blendedAirPlaneMode = ColorHelper.getBlendColor(
                            mAirplaneModeColorOld, mAirplaneModeColor, position);
                    for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
                        StatusBarIconView v = (StatusBarIconView) mStatusIcons.getChildAt(i);
                        v.setColorFilter(blendedStatus, Mode.MULTIPLY);
                    }
                    mSignalCluster.setIconTint(
                            blendedSignal, blendedNoSim, blendedAirPlaneMode);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mColorToChange == CARRIER_TEXT_COLOR) {
                    mCarrierTextColorOld = mCarrierTextColor;
                    mCarrierTextColorTint = mCarrierTextColor;
                } else if (mColorToChange == BATTERY_COLORS) {
                    mBatteryColorOld = mBatteryColor;
                    mBatteryTextColorOld = mBatteryTextColor;
                    mBatteryColorTint = mBatteryColor;
                    mBatteryTextColorTint = mBatteryTextColor;
                } else if (mColorToChange == CLOCK_COLOR) {
                    mClockColorOld = mClockColor;
                    mClockColorTint = mClockColor;
                } else if (mColorToChange == WEATHER_COLORS) {
                    mWeatherTextColorOld = mWeatherTextColor;
                    mWeatherIconColorOld = mWeatherIconColor;
                    mWeatherTextColorTint = mWeatherTextColor;
                    mWeatherIconColorTint = mWeatherIconColor;
                } else if (mColorToChange == NETWORK_TRAFFIC_COLORS) {
                    mNetworkTrafficTextColorOld = mNetworkTrafficTextColor;
                    mNetworkTrafficIconColorOld = mNetworkTrafficIconColor;
                    mNetworkTrafficTextColorTint = mNetworkTrafficTextColor;
                    mNetworkTrafficIconColorTint = mNetworkTrafficIconColor;
                } else if (mColorToChange == STATUS_NETWORK_ICON_COLORS) {
                    mStatusIconColorOld = mStatusIconColor;
                    mNetworkSignalColorOld = mNetworkSignalColor;
                    mNoSimColorOld = mNoSimColor;
                    mAirplaneModeColorOld = mAirplaneModeColor;
                    mStatusIconColorTint = mStatusIconColor;
                    mNetworkSignalColorTint = mNetworkSignalColor;
                    mNoSimColorTint = mNoSimColor;
                    mAirplaneModeColorTint = mAirplaneModeColor;
                }
            }
        });
        return animator;
    }

    public void updateCarrierTextVisibility(boolean show, boolean forceHide, int maxAllowedIcons) {
        boolean forceHideByNumberOfIcons = false;
        if (forceHide && mNotificationIcons.getChildCount() >= maxAllowedIcons) {
            forceHideByNumberOfIcons = true;
        }
        mCarrierText.setVisibility(show && !forceHideByNumberOfIcons ? View.VISIBLE : View.GONE);
    }

    public void updateCarrierTextKeyguardVisibility(boolean show) {
        mCarrierTextKeyguard.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void updateCarrierTextSettings() {
        mCarrierText.updateCarrierTextSettings();
        mCarrierTextKeyguard.updateCarrierTextSettings();
        mCarrierText.updateCarrierText();
        mCarrierTextKeyguard.updateCarrierText();
    }

    public void updateCarrierTextColor(boolean animate) {
        mCarrierTextColor = StatusBarColorHelper.getCarrierLabelColor(mContext);
        if (animate) {
            mColorToChange = CARRIER_TEXT_COLOR;
            mColorTransitionAnimator.start();
        } else {
            mCarrierText.setTextColor(mCarrierTextColor);
            mCarrierTextColorOld = mCarrierTextColor;
            mCarrierTextColorTint = mCarrierTextColor;
        }
        mCarrierTextKeyguard.setTextColor(mCarrierTextColor);
    }

    public void updateBatteryIndicator(int indicator) {
        mBatteryMeterView.updateBatteryIndicator(indicator);
        mBatteryMeterViewKeyguard.updateBatteryIndicator(indicator);
        mKeyguardStatusBarView.updateBatteryLevelVisibility();
    }

    public void updateBatteryBarVisibility(boolean show, boolean showOnKeyguard) {
        mShowBatteryBar = show;
        mBatteryBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mBatteryBarKeyguard.setVisibility(showOnKeyguard ? View.VISIBLE : View.GONE);
    }

    public void updateBatteryTextVisibility(boolean show) {
        mBatteryMeterView.setTextVisibility(show);
        mBatteryMeterViewKeyguard.setTextVisibility(show);
        mKeyguardStatusBarView.updateBatteryLevelVisibility();
    }

    public void updateBatteryCircleDots(int interval, int length) {
        mBatteryMeterView.updateCircleDots(interval, length);
        mBatteryMeterViewKeyguard.updateCircleDots(interval, length);
    }

    public void updateShowChargeAnimation(boolean show) {
        mBatteryMeterView.setShowChargeAnimation(show);
        mBatteryMeterViewKeyguard.setShowChargeAnimation(show);
        mBatteryBar.setShowChargeAnimation(show);
        mBatteryBarKeyguard.setShowChargeAnimation(show);
    }

    public void updateCutOutBatteryText(boolean cutOut) {
        mBatteryMeterView.setCutOutText(cutOut);
        mBatteryMeterViewKeyguard.setCutOutText(cutOut);
    }

    public void updateBatteryColors(boolean animate) {
        mBatteryColor = StatusBarColorHelper.getBatteryColor(mContext);
        mBatteryTextColor = StatusBarColorHelper.getBatteryTextColor(mContext);
        if (animate) {
            mColorToChange = BATTERY_COLORS;
            mColorTransitionAnimator.start();
        } else {
            mBatteryMeterView.setBatteryColors(mBatteryColor);
            mBatteryMeterView.setTextColor(mBatteryTextColor);
            mBatteryBar.setColor(mBatteryColor);
            mBatteryColorOld = mBatteryColor;
            mBatteryTextColorOld = mBatteryTextColor;
            mBatteryColorTint = mBatteryColor;
            mBatteryTextColorTint = mBatteryTextColor;
        }
        mBatteryMeterViewKeyguard.setBatteryColors(mBatteryColor);
        mBatteryMeterViewKeyguard.setTextColor(mBatteryTextColor);
        mBatteryBarKeyguard.setColor(mBatteryColor);
        mBatteryLevelKeyguard.setTextColor(mBatteryTextColor);
    }

    public void updateClockStyle(int clockStyle) {
        mClockStyle = clockStyle;

        switch (mClockStyle) {
            case CLOCK_STYLE_DEFAULT:
                mClockCentered.setVisibility(View.GONE);
                mCenterClockLayout.setVisibility(View.GONE);
                mClockDefault.setVisibility(View.VISIBLE);
                break;
            case CLOCK_STYLE_CENTERED:
                mClockDefault.setVisibility(View.GONE);
                mCenterClockLayout.setVisibility(View.VISIBLE);
                mClockCentered.setVisibility(View.VISIBLE);
                break;
            case CLOCK_STYLE_HIDDEN:
                mClockDefault.setVisibility(View.GONE);
                mCenterClockLayout.setVisibility(View.GONE);
                mClockCentered.setVisibility(View.GONE);
                break;
        }
        mNotificationIcons.setCenteredClock(mClockStyle == CLOCK_STYLE_CENTERED);
        if (DeviceUtils.deviceSupportsMobileData(mContext)) {
            mCarrierText.setCenteredClock(mClockStyle == CLOCK_STYLE_CENTERED);
        }
    }

    public void updateClockSettings() {
        mClockDefault.updateSettings();
        mClockCentered.updateSettings();
    }

    public void updateClockColor(boolean animate) {
        mClockColor = StatusBarColorHelper.getClockColor(mContext);
        if (animate) {
            mColorToChange = CLOCK_COLOR;
            mColorTransitionAnimator.start();
            if (mClockStyle == CLOCK_STYLE_DEFAULT) {
                mClockCentered.setTextColor(mClockColor);
            }
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                mClockDefault.setTextColor(mClockColor);
            }
        } else {
            mClockCentered.setTextColor(mClockColor);
            mClockDefault.setTextColor(mClockColor);
            mClockColorOld = mClockColor;
            mClockColorTint = mClockColor;
        }
    }

    public void updateWeatherVisibility(boolean show, boolean forceHide, int maxAllowedIcons) {
        boolean forceHideByNumberOfIcons = false;
        if (forceHide && mNotificationIcons.getChildCount() >= maxAllowedIcons) {
            forceHideByNumberOfIcons = true;
        }
        mWeatherLayout.setShow(show && !forceHideByNumberOfIcons);
    }

    public void setShowWeather(boolean show) {
        mWeatherLayout.setShow(show);
    }

    public void setWeatherType(int type) {
        mWeatherLayout.setType(type);
    }

    public void updateWeatherColors(boolean animate) {
        mWeatherTextColor = StatusBarColorHelper.getWeatherTextColor(mContext);
        mWeatherIconColor = StatusBarColorHelper.getWeatherIconColor(mContext);
        if (animate) {
            mColorToChange = WEATHER_COLORS;
            mColorTransitionAnimator.start();
        } else {
            mWeatherLayout.setTextColor(mWeatherTextColor);
            mWeatherLayout.setIconColor(mWeatherIconColor);
            mWeatherTextColorOld = mWeatherTextColor;
            mWeatherIconColorOld = mWeatherIconColor;
            mWeatherTextColorTint = mWeatherTextColor;
            mWeatherIconColorTint = mWeatherIconColor;
        }
    }

    public void setShowNetworkTraffic(boolean show) {
        mNetworkTraffic.setShow(show);
    }

    public void setShowNetworkTrafficOnKeyguard(boolean show) {
        mNetworkTrafficKeyguard.setShow(show);
    }

    public void setNetworkTrafficActivity(int activity) {
        mNetworkTraffic.setActivity(activity);
        mNetworkTrafficKeyguard.setActivity(activity);
    }

    public void setNetworkTrafficType(int type) {
        mNetworkTraffic.setType(type);
        mNetworkTrafficKeyguard.setType(type);
    }

    public void setNetworkTrafficIsBit(boolean isBit) {
        mNetworkTraffic.setIsBit(isBit);
        mNetworkTrafficKeyguard.setIsBit(isBit);
    }

    public void setNetworkTrafficHideTraffic(boolean hide, int threshold, boolean iconAsIndicator) {
        mNetworkTraffic.setHide(hide, threshold, iconAsIndicator);
        mNetworkTrafficKeyguard.setHide(hide, threshold, iconAsIndicator);
    }

    public void updateNetworkTrafficColors(boolean animate) {
        mNetworkTrafficTextColor = StatusBarColorHelper.getNetworkTrafficTextColor(mContext);
        mNetworkTrafficIconColor = StatusBarColorHelper.getNetworkTrafficIconColor(mContext);
        if (animate && mNetworkTraffic.getVisibility() == View.VISIBLE) {
            mColorToChange = NETWORK_TRAFFIC_COLORS;
            mColorTransitionAnimator.start();
        } else {
            mNetworkTraffic.setTextColor(mNetworkTrafficTextColor);
            mNetworkTraffic.setIconColor(mNetworkTrafficIconColor);
            mNetworkTrafficTextColorOld = mNetworkTrafficTextColor;
            mNetworkTrafficIconColorOld = mNetworkTrafficIconColor;
            mNetworkTrafficTextColorTint = mNetworkTrafficTextColor;
            mNetworkTrafficIconColorTint = mNetworkTrafficIconColor;
        }
        mNetworkTrafficKeyguard.setTextColor(mNetworkTrafficTextColor);
        mNetworkTrafficKeyguard.setIconColor(mNetworkTrafficIconColor);
    }

    public void updateStatusNetworkIconColors(boolean animate) {
        mStatusIconColor = StatusBarColorHelper.getStatusIconColor(mContext);
        mNetworkSignalColor = StatusBarColorHelper.getNetworkSignalColor(mContext);
        mNoSimColor = StatusBarColorHelper.getNoSimColor(mContext);
        mAirplaneModeColor = StatusBarColorHelper.getAirplaneModeColor(mContext);
        if (animate) {
            mColorToChange = STATUS_NETWORK_ICON_COLORS;
            mColorTransitionAnimator.start();
        } else {
            mSignalCluster.setIconTint(
                    mNetworkSignalColor, mNoSimColor, mAirplaneModeColor);
            mStatusIconColorOld = mStatusIconColor;
            mNetworkSignalColorOld = mNetworkSignalColor;
            mNoSimColorOld = mNoSimColor;
            mAirplaneModeColorOld = mAirplaneModeColor;
            mStatusIconColorTint = mStatusIconColor;
            mNetworkSignalColorTint = mNetworkSignalColor;
            mNoSimColorTint = mNoSimColor;
            mAirplaneModeColorTint = mAirplaneModeColor;
        }
        mSignalClusterKeyguard.setIconTint(
                mNetworkSignalColor, mNoSimColor, mAirplaneModeColor);
        updateStatusIconKeyguardColor();
    }

    private void updateStatusIconKeyguardColor() {
        if (mStatusIconsKeyguard.getChildCount() > 0) {
            for (int index = 0; index < mStatusIconsKeyguard.getChildCount(); index++) {
                StatusBarIconView v = (StatusBarIconView) mStatusIconsKeyguard.getChildAt(index);
                v.setColorFilter(mStatusIconColor, Mode.MULTIPLY);
            }
        }
    }

    public void updateShowTicker(boolean show) {
        mShowTicker = show;
        if (mShowTicker && (mTicker == null || mTickerView == null)) {
            inflateTickerView();
        }
    }

    public void updateNotificationIconColor() {
        mNotificationIconColor = StatusBarColorHelper.getNotificationIconColor(mContext);
        mNotificationIconColorTint = mNotificationIconColor;
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mNotificationIcons.getChildAt(i);
            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
            boolean colorize = !isPreL || isGrayscale(v);
            if (colorize) {
                v.setImageTintList(ColorStateList.valueOf(mNotificationIconColor));
            }
        }
        mMoreIcon.setColorFilter(mNotificationIconColor, Mode.MULTIPLY);
        updateTickerIconColor(mNotificationIconColor);
    }

    private void updateTickerIconColor(int color) {
        if (mTicker != null && mTickerView != null) {
            mTicker.setIconColorTint(ColorStateList.valueOf(color));
        }
    }

    public void updateTickerTextColor() {
        mTickerTextColor = StatusBarColorHelper.getTickerTextColor(mContext);
        mTickerTextColorTint = mTickerTextColor;
        if (mTicker != null && mTickerView != null) {
            mTicker.setTextColor(mTickerTextColor);
        }
    }

    private void inflateTickerView() {
        final ViewStub tickerStub = (ViewStub) mStatusBar.findViewById(R.id.ticker_stub);
        if (tickerStub != null) {
            mTickerView = tickerStub.inflate();
            mTicker = new MyTicker(mContext, mStatusBar);

            TickerView tickerView = (TickerView) mStatusBar.findViewById(R.id.tickerText);
            tickerView.mTicker = mTicker;
            updateTickerIconColor(mNotificationIconColor);
            updateTickerTextColor();
        } else {
            mShowTicker = false;
        }
    }

    public void addTickerEntry(StatusBarNotification n) {
        mTicker.addEntry(n);
    }

    public void removeTickerEntry(StatusBarNotification n) {
        mTicker.removeEntry(n);
    }

    public void haltTicker() {
        if (mTicking) {
            mTicker.halt();
        }
    }

    private class MyTicker extends Ticker {
        MyTicker(Context context, View sb) {
            super(context, sb);
        }

        @Override
        public void tickerStarting() {
            if (!mShowTicker) return;
            mTicking = true;
            mStatusBarContents.setVisibility(View.GONE);
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                mCenterClockLayout.setVisibility(View.GONE);
            }
            if (mShowBatteryBar) {
                mBatteryBar.setVisibility(View.GONE);
            }
            mTickerView.setVisibility(View.VISIBLE);
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_up_in, null));
            mStatusBarContents.startAnimation(loadAnim(com.android.internal.R.anim.push_up_out, null));
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                mCenterClockLayout.startAnimation(loadAnim(com.android.internal.R.anim.push_up_out, null));
            }
            if (mShowBatteryBar) {
                mBatteryBar.startAnimation(loadAnim(com.android.internal.R.anim.push_up_out, null));
            }
        }

        @Override
        public void tickerDone() {
            if (!mShowTicker) return;

            mStatusBarContents.setVisibility(View.VISIBLE);
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                mCenterClockLayout.setVisibility(View.VISIBLE);
            }
            if (mShowBatteryBar) {
                mBatteryBar.setVisibility(View.VISIBLE);
            }
            mTickerView.setVisibility(View.GONE);
            mStatusBarContents.startAnimation(loadAnim(com.android.internal.R.anim.push_down_in, null));
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                mCenterClockLayout.startAnimation(loadAnim(com.android.internal.R.anim.push_down_in, null));
            }
            if (mShowBatteryBar) {
                mBatteryBar.startAnimation(loadAnim(com.android.internal.R.anim.push_down_in, null));
            }
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_down_out,
                        mTickingDoneListener));
        }

        public void tickerHalting() {
            if (!mShowTicker) return;

            if (mStatusBarContents.getVisibility() != View.VISIBLE) {
                mStatusBarContents.setVisibility(View.VISIBLE);
                mStatusBarContents
                        .startAnimation(loadAnim(com.android.internal.R.anim.fade_in, null));
            }
            if (mClockStyle == CLOCK_STYLE_CENTERED) {
                if (mCenterClockLayout.getVisibility() != View.VISIBLE) {
                    mCenterClockLayout.setVisibility(View.VISIBLE);
                    mStatusBarContents
                            .startAnimation(loadAnim(com.android.internal.R.anim.fade_in, null));
                }
            }
            if (mShowBatteryBar) {
                if (mBatteryBar.getVisibility() != View.VISIBLE) {
                    mBatteryBar.setVisibility(View.VISIBLE);
                    mBatteryBar
                            .startAnimation(loadAnim(com.android.internal.R.anim.fade_in, null));
                }
            }

            mTickerView.setVisibility(View.GONE);
            // we do not animate the ticker away at this point, just get rid of it (b/6992707)

        }
    }

    Animation.AnimationListener mTickingDoneListener = new Animation.AnimationListener() {;
        public void onAnimationEnd(Animation animation) {
            mTicking = false;
        }
        public void onAnimationRepeat(Animation animation) {
        }
        public void onAnimationStart(Animation animation) {
        }
    };

    private Animation loadAnim(int id, Animation.AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }
}
