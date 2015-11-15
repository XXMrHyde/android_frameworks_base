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
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
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
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.BatteryBar;
import com.android.systemui.statusbar.policy.NetworkTraffic;
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

    private static final int CARRIER_LABEL_COLOR         = 0;
    private static final int BATTERY_COLORS              = 1;
    private static final int CLOCK_COLOR                 = 2;
    private static final int NETWORK_TRAFFIC_COLORS      = 3;
    private static final int STATUS_NETWORK_ICON_COLORS = 4;

    private Context mContext;
    private PhoneStatusBar mPhoneStatusBar;
    private KeyguardStatusBarView mKeyguardStatusBarView;
    private Interpolator mLinearOutSlowIn;
    private Interpolator mFastOutSlowIn;
    private DemoStatusIcons mDemoStatusIcons;
    private NotificationColorUtil mNotificationColorUtil;

    private LinearLayout mSystemIconArea;
    private LinearLayout mStatusIcons;
    private LinearLayout mStatusIconsKeyguard;
    private SignalClusterView mSignalCluster;
    private SignalClusterView mSignalClusterKeyguard;
    private CarrierText mCarrierLabel;
    private CarrierText mCarrierLabelKeyguard;
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
    private NetworkTraffic mNetworkTraffic;
    private int mCarrierLabelColor;
    private int mCarrierLabelColorOld;
    private int mCarrierLabelColorTint;
    private int mBatteryFrameColor;
    private int mBatteryFrameColorOld;
    private int mBatteryFrameColorTint;
    private int mBatteryColor;
    private int mBatteryColorOld;
    private int mBatteryColorTint;
    private int mBatteryTextColor;
    private int mBatteryTextColorOld;
    private int mBatteryTextColorTint;
    private int mClockColor;
    private int mClockColorOld;
    private int mClockColorTint;
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
        mPhoneStatusBar = phoneStatusBar;
        mKeyguardStatusBarView = keyguardStatusBar;
        mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        mSystemIconArea = (LinearLayout) statusBar.findViewById(R.id.system_icon_area);
        mStatusIcons = (LinearLayout) statusBar.findViewById(R.id.statusIcons);
        mSignalCluster = (SignalClusterView) statusBar.findViewById(R.id.signal_cluster);
        mSignalClusterKeyguard = (SignalClusterView) keyguardStatusBar.findViewById(R.id.signal_cluster);
        mNotificationIconArea = statusBar.findViewById(R.id.notification_icon_area_inner);
        mCarrierLabel = (CarrierText) statusBar.findViewById(R.id.status_bar_carrier_text);
        mCarrierLabelKeyguard = (CarrierText) keyguardStatusBar.findViewById(R.id.keyguard_carrier_text);
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
        mNetworkTraffic = (NetworkTraffic) statusBar.findViewById(R.id.network_traffic_layout);
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
        mCarrierLabelColor = StatusBarColorHelper.getCarrierLabelColor(mContext);
        mCarrierLabelColorOld = mCarrierLabelColor;
        mCarrierLabelColorTint = mCarrierLabelColor;
        mBatteryFrameColor = StatusBarColorHelper.getBatteryFrameColor(mContext);
        mBatteryFrameColorOld = mBatteryFrameColor;
        mBatteryFrameColorTint = mBatteryFrameColor;
        mBatteryColor = StatusBarColorHelper.getBatteryColor(mContext);
        mBatteryColorOld = mBatteryColor;
        mBatteryColorTint = mBatteryColor;
        mBatteryTextColor = StatusBarColorHelper.getBatteryTextColor(mContext);
        mBatteryTextColorOld = mBatteryTextColor;
        mBatteryTextColorTint = mBatteryTextColor;
        mClockColor = StatusBarColorHelper.getClockColor(mContext);
        mClockColorOld = mClockColor;
        mClockColorTint = mClockColor;
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

    public void setIconsDark(boolean dark) {
        if (mTransitionPending) {
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
            mCarrierLabelColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                    mCarrierLabelColor, StatusBarColorHelper.getCarrierLabelColorDark(mContext));
        }
        mBatteryFrameColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mBatteryFrameColor,  StatusBarColorHelper.getBatteryFrameColorDark(mContext));
        mBatteryColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mBatteryColor, StatusBarColorHelper.getBatteryColorDark(mContext));
        mBatteryTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mBatteryTextColor, StatusBarColorHelper.getBatteryTextColorDark(mContext));
        mClockColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mClockColor, StatusBarColorHelper.getClockColorDark(mContext));
        mNetworkTrafficTextColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNetworkTrafficTextColor, StatusBarColorHelper.getNetworkTrafficTextColorDark(mContext));
        mNetworkTrafficIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(mDarkIntensity,
                mNetworkTrafficIconColor, StatusBarColorHelper.getNetworkTrafficIconColorDark(mContext));
        mNetworkSignalColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mNetworkSignalColor, StatusBarColorHelper.getNetworkSignalColorDark(mContext));
        mNoSimColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mNoSimColor, StatusBarColorHelper.getNoSimColorDark(mContext));
        mAirplaneModeColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mAirplaneModeColor, StatusBarColorHelper.getAirplaneModeColorDark(mContext));
        mStatusIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mStatusIconColor, StatusBarColorHelper.getStatusIconColorDark(mContext));
        mNotificationIconColorTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mNotificationIconColor, StatusBarColorHelper.getNotificationIconColorDark(mContext));

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
            mCarrierLabel.setTextColor(mCarrierLabelColorTint);
        }
        mBatteryMeterView.setBatteryColors(mBatteryFrameColorTint, mBatteryColorTint);
        mBatteryMeterView.setTextColor(mBatteryTextColorTint);
        mBatteryBar.setColor(mBatteryColorTint);
        if (mClockStyle == CLOCK_STYLE_DEFAULT) {
            mClockDefault.setTextColor(mClockColorTint);
        }
        if (mClockStyle == CLOCK_STYLE_CENTERED) {
            mClockCentered.setTextColor(mClockColorTint);
        }
        mNetworkTraffic.setTextColor(mNetworkTrafficTextColorTint);
        mNetworkTraffic.setIconColor(mNetworkTrafficIconColorTint);
        mSignalCluster.setIconTint(
                mNetworkSignalColorTint, mNoSimColorTint, mAirplaneModeColorTint, mDarkIntensity);
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mStatusIcons.getChildAt(i);
            v.setImageTintList(ColorStateList.valueOf(mStatusIconColorTint));
        }
        mMoreIcon.setImageTintList(ColorStateList.valueOf(mNotificationIconColorTint));
        applyNotificationIconsTint();
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
                if (mColorToChange == CARRIER_LABEL_COLOR) {
                    final int blended = ColorHelper.getBlendColor(
                            mCarrierLabelColorOld, mCarrierLabelColor, position);
                    mCarrierLabel.setTextColor(blended);
                } else if (mColorToChange == BATTERY_COLORS) {
                    final int blendedFrame = ColorHelper.getBlendColor(
                            mBatteryFrameColorOld, mBatteryFrameColor, position);
                    final int blended = ColorHelper.getBlendColor(
                            mBatteryColorOld, mBatteryColor, position);
                    final int blendedText = ColorHelper.getBlendColor(
                            mBatteryTextColorOld, mBatteryTextColor, position);
                    mBatteryMeterView.setBatteryColors(blendedFrame, blended);
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
                        v.setImageTintList(ColorStateList.valueOf(blendedStatus));
                    }
                    mSignalCluster.setIconTint(
                            blendedSignal, blendedNoSim, blendedAirPlaneMode, mDarkIntensity);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mColorToChange == CARRIER_LABEL_COLOR) {
                    mCarrierLabelColorOld = mCarrierLabelColor;
                    mCarrierLabelColorTint = mCarrierLabelColor;
                } else if (mColorToChange == BATTERY_COLORS) {
                    mBatteryFrameColorOld = mBatteryFrameColor;
                    mBatteryColorOld = mBatteryColor;
                    mBatteryTextColorOld = mBatteryTextColor;
                    mBatteryFrameColorTint = mBatteryFrameColor;
                    mBatteryColorTint = mBatteryColor;
                    mBatteryTextColorTint = mBatteryTextColor;
                } else if (mColorToChange == CLOCK_COLOR) {
                    mClockColorOld = mClockColor;
                    mClockColorTint = mClockColor;
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

    public void updateCarrierLabelVisibility(boolean show, boolean forceHide, int maxAllowedIcons) {
        boolean forceHideByNumberOfIcons = false;
        if (forceHide && mNotificationIcons.getChildCount() >= maxAllowedIcons) {
            forceHideByNumberOfIcons = true;
        }
        mCarrierLabel.setVisibility(show && !forceHideByNumberOfIcons ? View.VISIBLE : View.GONE);
    }

    public void updateCarrierLabelKeyguardVisibility(boolean show) {
        mCarrierLabelKeyguard.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void updateCarrierLabelSettings() {
        mCarrierLabel.updateCarrierLabelSettings();
        mCarrierLabelKeyguard.updateCarrierLabelSettings();
        mCarrierLabel.updateCarrierText();
        mCarrierLabelKeyguard.updateCarrierText();
    }

    public void updateCarrierLabelColor(boolean animate) {
        mCarrierLabelColor = StatusBarColorHelper.getCarrierLabelColor(mContext);
        if (animate) {
            mColorToChange = CARRIER_LABEL_COLOR;
            mColorTransitionAnimator.start();
        } else {
            mCarrierLabel.setTextColor(mCarrierLabelColor);
            mCarrierLabelColorOld = mCarrierLabelColor;
            mCarrierLabelColorTint = mCarrierLabelColor;
        }
        mCarrierLabelKeyguard.setTextColor(mCarrierLabelColor);
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
        mBatteryFrameColor = StatusBarColorHelper.getBatteryFrameColor(mContext);
        mBatteryColor = StatusBarColorHelper.getBatteryColor(mContext);
        mBatteryTextColor = StatusBarColorHelper.getBatteryTextColor(mContext);
        if (animate) {
            mColorToChange = BATTERY_COLORS;
            mColorTransitionAnimator.start();
        } else {
            mBatteryMeterView.setBatteryColors(mBatteryFrameColor, mBatteryColor);
            mBatteryMeterView.setTextColor(mBatteryTextColor);
            mBatteryBar.setColor(mBatteryColor);
            mBatteryFrameColorOld = mBatteryFrameColor;
            mBatteryColorOld = mBatteryColor;
            mBatteryTextColorOld = mBatteryTextColor;
            mBatteryFrameColorTint = mBatteryFrameColor;
            mBatteryColorTint = mBatteryColor;
            mBatteryTextColorTint = mBatteryTextColor;
        }
        mBatteryMeterViewKeyguard.setBatteryColors(mBatteryFrameColor, mBatteryColor);
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

    public void updateNetworkTrafficColors(boolean animate) {
        mNetworkTrafficTextColor = StatusBarColorHelper.getNetworkTrafficTextColor(mContext);
        mNetworkTrafficIconColor = StatusBarColorHelper.getNetworkTrafficIconColor(mContext);
        if (animate && mNetworkTraffic.isUpdating()) {
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
            mSignalCluster.setIgnoreSystemUITuner(true);
            mSignalCluster.setIconTint(
                    mNetworkSignalColor, mNoSimColor, mAirplaneModeColor, mDarkIntensity);
            mStatusIconColorOld = mStatusIconColor;
            mNetworkSignalColorOld = mNetworkSignalColor;
            mNoSimColorOld = mNoSimColor;
            mAirplaneModeColorOld = mAirplaneModeColor;
            mStatusIconColorTint = mStatusIconColor;
            mNetworkSignalColorTint = mNetworkSignalColor;
            mNoSimColorTint = mNoSimColor;
            mAirplaneModeColorTint = mAirplaneModeColor;
        }
        mSignalClusterKeyguard.setIgnoreSystemUITuner(true);
        mSignalClusterKeyguard.setIconTint(
                mNetworkSignalColor, mNoSimColor, mAirplaneModeColor, 0f);
        updateStatusIconKeyguardColor();
    }

    private void updateStatusIconKeyguardColor() {
        if (mStatusIconsKeyguard.getChildCount() > 0) {
            for (int index = 0; index < mStatusIconsKeyguard.getChildCount(); index++) {
                StatusBarIconView v = (StatusBarIconView) mStatusIconsKeyguard.getChildAt(index);
                v.setImageTintList(ColorStateList.valueOf(mStatusIconColor));
            }
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
        mMoreIcon.setImageTintList(ColorStateList.valueOf(mNotificationIconColor));
    }
}
