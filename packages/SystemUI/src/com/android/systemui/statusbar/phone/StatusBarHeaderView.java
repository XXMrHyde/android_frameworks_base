/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.darkkat.SBEHeaderColorHelper;

import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.EmergencyListener;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.tuner.TunerService;

import java.text.NumberFormat;

/**
 * The view to manage the header area in the expanded status bar.
 */
public class StatusBarHeaderView extends RelativeLayout implements View.OnClickListener,
        BatteryController.BatteryStateChangeCallback, NextAlarmController.NextAlarmChangeCallback,
        EmergencyListener {

    private boolean mExpanded;
    private boolean mListening;

    private ViewGroup mSystemIconsContainer;
    private View mSystemIconsSuperContainer;
    private BatteryMeterView mBatteryMeterView;
    private View mDateGroup;
    private View mClock;
    private TextView mTime;
    private TextView mAmPm;
    private TextView mDateCollapsed;
    private TextView mDateExpanded;
    private LinearLayout mSystemIcons;
    private SignalClusterView mSignalCluster;
    private SettingsButton mSettingsButton;
    private ImageView mTunerIcon;
    private View mSettingsContainer;
    private View mQsDetailHeader;
    private TextView mQsDetailHeaderTitle;
    private Switch mQsDetailHeaderSwitch;
    private ImageView mQsDetailHeaderProgress;
    private TextView mEmergencyCallsOnly;
    private TextView mBatteryLevel;
    private TextView mAlarmStatus;

    private boolean mShowEmergencyCallsOnly;
    private boolean mAlarmShowing;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private int mCollapsedHeight;
    private int mExpandedHeight;

    private int mClockMarginBottomExpanded;
    private int mClockMarginBottomCollapsed;

    private int mClockCollapsedSize;
    private int mClockExpandedSize;

    /**
     * In collapsed QS, the clock is scaled down a bit post-layout to allow for a nice
     * transition. These values determine that factor.
     */
    private float mClockCollapsedScaleFactor;

    private ActivityStarter mActivityStarter;
    private BatteryController mBatteryController;
    private NextAlarmController mNextAlarmController;

    private final Rect mClipBounds = new Rect();

    private boolean mCaptureValues;
    private boolean mSignalClusterDetached;
    private final LayoutValues mCollapsedValues = new LayoutValues();
    private final LayoutValues mExpandedValues = new LayoutValues();
    private final LayoutValues mCurrentValues = new LayoutValues();

    private float mCurrentT;
    private boolean mShowingDetail;
    private boolean mDetailTransitioning;

    private SettingsObserver mSettingsObserver;

    public StatusBarHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        mSystemIconsContainer = (ViewGroup) findViewById(R.id.system_icons_container);
        mSystemIconsSuperContainer.setOnClickListener(this);
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.battery);
        mDateGroup = findViewById(R.id.date_group);
        mClock = findViewById(R.id.clock);
        mTime = (TextView) findViewById(R.id.time_view);
        mAmPm = (TextView) findViewById(R.id.am_pm_view);
        mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
        mDateExpanded = (TextView) findViewById(R.id.date_expanded);
        mSettingsContainer = findViewById(R.id.settings_button_container);
        mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mTunerIcon = (ImageView) findViewById(R.id.tuner_icon);
        mQsDetailHeader = findViewById(R.id.qs_detail_header);
        mQsDetailHeader.setAlpha(0);
        mQsDetailHeaderTitle = (TextView) mQsDetailHeader.findViewById(android.R.id.title);
        mQsDetailHeaderSwitch = (Switch) mQsDetailHeader.findViewById(android.R.id.toggle);
        mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        mEmergencyCallsOnly = (TextView) findViewById(R.id.header_emergency_calls_only);
        mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        mAlarmStatus.setOnClickListener(this);
        mSignalCluster = (SignalClusterView) findViewById(R.id.signal_cluster);
        mSystemIcons = (LinearLayout) findViewById(R.id.system_icons);
        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();
        loadDimens();
        updateVisibilities();
        updateClockScale();
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) != (oldRight - oldLeft)) {
                    // width changed, update clipping
                    setClipping(getHeight());
                }
                boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                mTime.setPivotX(rtl ? mTime.getWidth() : 0);
                mTime.setPivotY(mTime.getBaseline());
                updateAmPmTranslation();
            }
        });
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(mClipBounds);
            }
        });
        requestCaptureValues();

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
        ((RippleDrawable) getBackground()).setForceSoftware(true);
        ((RippleDrawable) mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) mSystemIconsSuperContainer.getBackground()).setForceSoftware(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mCaptureValues) {
            if (mExpanded) {
                captureLayoutValues(mExpandedValues);
            } else {
                captureLayoutValues(mCollapsedValues);
            }
            mCaptureValues = false;
            updateLayoutValues(mCurrentT);
        }
        mAlarmStatus.setX(mDateGroup.getLeft() + mDateCollapsed.getRight());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mBatteryLevel, R.dimen.battery_level_text_size);
        FontSizeUtils.updateFontSize(mEmergencyCallsOnly,
                R.dimen.qs_emergency_calls_only_text_size);
        FontSizeUtils.updateFontSize(mDateCollapsed, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mDateExpanded, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(this, android.R.id.toggle, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(mAmPm, R.dimen.qs_time_collapsed_size);
        FontSizeUtils.updateFontSize(this, R.id.empty_time_view, R.dimen.qs_time_expanded_size);

        mEmergencyCallsOnly.setText(com.android.internal.R.string.emergency_calls_only);

        mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        mClockCollapsedScaleFactor = (float) mClockCollapsedSize / (float) mClockExpandedSize;

        updateClockScale();
        updateClockCollapsedMargin();
    }

    private void updateClockCollapsedMargin() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.clock_collapsed_bottom_margin);
        int largePadding = res.getDimensionPixelSize(
                R.dimen.clock_collapsed_bottom_margin_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f,
                FontSizeUtils.LARGE_TEXT_SCALE) - 1f) / (FontSizeUtils.LARGE_TEXT_SCALE - 1f);
        mClockMarginBottomCollapsed = Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        requestLayout();
    }

    private void requestCaptureValues() {
        mCaptureValues = true;
        requestLayout();
    }

    private void loadDimens() {
        mCollapsedHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height);
        mExpandedHeight = getResources().getDimensionPixelSize(
                R.dimen.status_bar_header_height_expanded);
        mClockMarginBottomExpanded =
                getResources().getDimensionPixelSize(R.dimen.clock_expanded_bottom_margin);
        updateClockCollapsedMargin();
        mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        mClockCollapsedScaleFactor = (float) mClockCollapsedSize / (float) mClockExpandedSize;

    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        if (mBatteryMeterView != null) {
            mBatteryMeterView.setBatteryController(batteryController);
        }
    }

    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        mNextAlarmController = nextAlarmController;
    }

    public int getCollapsedHeight() {
        return mCollapsedHeight;
    }

    public int getExpandedHeight() {
        return mExpandedHeight;
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;
        updateListeners();
    }

    public void setExpanded(boolean expanded) {
        boolean changed = expanded != mExpanded;
        mExpanded = expanded;
        if (changed) {
            updateEverything();
        }
    }

    public void updateEverything() {
        updateHeights();
        updateVisibilities();
        updateClickTargets();
        updateClockScale();
        updateClockLp();
        requestCaptureValues();
    }

    private void updateHeights() {
        int height = mExpanded ? mExpandedHeight : mCollapsedHeight;
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp.height != height) {
            lp.height = height;
            setLayoutParams(lp);
        }
    }

    private void updateVisibilities() {
        mDateCollapsed.setVisibility(mExpanded && mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
        mDateExpanded.setVisibility(mExpanded && mAlarmShowing ? View.INVISIBLE : View.VISIBLE);
        mAlarmStatus.setVisibility(mExpanded && mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
        mQsDetailHeader.setVisibility(mExpanded && mShowingDetail? View.VISIBLE : View.INVISIBLE);
        if (mSignalCluster != null) {
            updateSignalClusterDetachment();
        }
        mEmergencyCallsOnly.setVisibility(mExpanded && mShowEmergencyCallsOnly ? VISIBLE : GONE);
        mBatteryLevel.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
    }

    private void updateSignalClusterDetachment() {
        boolean detached = mExpanded;
        if (detached != mSignalClusterDetached) {
            if (detached) {
                getOverlay().add(mSignalCluster);
            } else {
                reattachSignalCluster();
            }
        }
        mSignalClusterDetached = detached;
    }

    private void reattachSignalCluster() {
        getOverlay().remove(mSignalCluster);
        mSystemIcons.addView(mSignalCluster, 1);
    }

    private void updateListeners() {
        if (mListening) {
            mBatteryController.addStateChangedCallback(this);
            mNextAlarmController.addStateChangedCallback(this);
        } else {
            mBatteryController.removeStateChangedCallback(this);
            mNextAlarmController.removeStateChangedCallback(this);
        }
    }

    private void updateClockScale() {
        mTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, mExpanded
                ? mClockExpandedSize
                : mClockCollapsedSize);
        mTime.setScaleX(1f);
        mTime.setScaleY(1f);
        updateAmPmTranslation();
    }

    private void updateAmPmTranslation() {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        mAmPm.setTranslationX((rtl ? 1 : -1) * mTime.getWidth() * (1 - mTime.getScaleX()));
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        mBatteryLevel.setText(percentage);
    }

    @Override
    public void onPowerSaveChanged() {
        // could not care less
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            mAlarmStatus.setText(KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm));
        }
        mAlarmShowing = nextAlarm != null;
        updateEverything();
        requestCaptureValues();
    }

    private void updateClickTargets() {
        mSystemIconsSuperContainer.setClickable(mExpanded);
        mSystemIconsSuperContainer.setFocusable(mExpanded);
        mAlarmStatus.setClickable(mNextAlarm != null && mNextAlarm.getShowIntent() != null);
    }

    private void updateClockLp() {
        int marginBottom = mExpanded
                ? mClockMarginBottomExpanded
                : mClockMarginBottomCollapsed;
        LayoutParams lp = (LayoutParams) mDateGroup.getLayoutParams();
        if (marginBottom != lp.bottomMargin) {
            lp.bottomMargin = marginBottom;
            mDateGroup.setLayoutParams(lp);
        }
    }

    public void setExpansion(float t) {
        if (!mExpanded) {
            t = 0f;
        }
        mCurrentT = t;
        float height = mCollapsedHeight + t * (mExpandedHeight - mCollapsedHeight);
        if (height < mCollapsedHeight) {
            height = mCollapsedHeight;
        }
        if (height > mExpandedHeight) {
            height = mExpandedHeight;
        }
        setClipping(height);
        updateLayoutValues(t);
    }

    private void updateLayoutValues(float t) {
        if (mCaptureValues) {
            return;
        }
        mCurrentValues.interpoloate(mCollapsedValues, mExpandedValues, t);
        applyLayoutValues(mCurrentValues);
    }

    private void setClipping(float height) {
        mClipBounds.set(getPaddingLeft(), 0, getWidth() - getPaddingRight(), (int) height);
        setClipBounds(mClipBounds);
        invalidateOutline();
    }

    @Override
    public void onClick(View v) {
        if (v == mSettingsButton) {
            startSettingsActivity();
        } else if (v == mSystemIconsSuperContainer) {
            startBatteryActivity();
        } else if (v == mAlarmStatus && mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null) {
                mActivityStarter.startPendingIntentDismissingKeyguard(showIntent);
            }
        }
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    private void startBatteryActivity() {
        mActivityStarter.startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                true /* dismissShade */);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
                requestCaptureValues();
            }
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // We don't want that everything lights up when we click on the header, so block the request
        // here.
    }

    private void captureLayoutValues(LayoutValues target) {
        target.timeScale = mExpanded ? 1f : mClockCollapsedScaleFactor;
        target.clockY = mClock.getBottom();
        target.dateY = mDateGroup.getTop();
        target.emergencyCallsOnlyAlpha = getAlphaForVisibility(mEmergencyCallsOnly);
        target.alarmStatusAlpha = getAlphaForVisibility(mAlarmStatus);
        target.dateCollapsedAlpha = getAlphaForVisibility(mDateCollapsed);
        target.dateExpandedAlpha = getAlphaForVisibility(mDateExpanded);
        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            target.batteryX = mSystemIconsSuperContainer.getLeft()
                    + mSystemIconsContainer.getRight();
        } else {
            target.batteryX = mSystemIconsSuperContainer.getLeft()
                    + mSystemIconsContainer.getLeft();
        }
        target.batteryY = mSystemIconsSuperContainer.getTop() + mSystemIconsContainer.getTop();
        target.batteryLevelAlpha = getAlphaForVisibility(mBatteryLevel);
        target.signalClusterAlpha = mSignalClusterDetached ? 0f : 1f;
    }

    private float getAlphaForVisibility(View v) {
        return v == null || v.getVisibility() == View.VISIBLE ? 1f : 0f;
    }

    private void applyAlpha(View v, float alpha) {
        if (v == null || v.getVisibility() == View.GONE) {
            return;
        }
        if (alpha == 0f) {
            v.setVisibility(View.INVISIBLE);
        } else {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(alpha);
        }
    }

    private void applyLayoutValues(LayoutValues values) {
        mTime.setScaleX(values.timeScale);
        mTime.setScaleY(values.timeScale);
        mClock.setY(values.clockY - mClock.getHeight());
        mDateGroup.setY(values.dateY);
        mAlarmStatus.setY(values.dateY - mAlarmStatus.getPaddingTop());
        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            mSystemIconsSuperContainer.setX(values.batteryX - mSystemIconsContainer.getRight());
        } else {
            mSystemIconsSuperContainer.setX(values.batteryX - mSystemIconsContainer.getLeft());
        }
        mSystemIconsSuperContainer.setY(values.batteryY - mSystemIconsContainer.getTop());
        if (mSignalCluster != null && mExpanded) {
            if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
                mSignalCluster.setX(mSystemIconsSuperContainer.getX()
                        - mSignalCluster.getWidth());
            } else {
                mSignalCluster.setX(mSystemIconsSuperContainer.getX()
                        + mSystemIconsSuperContainer.getWidth());
            }
            mSignalCluster.setY(
                    mSystemIconsSuperContainer.getY() + mSystemIconsSuperContainer.getHeight()/2
                            - mSignalCluster.getHeight()/2);
        } else if (mSignalCluster != null) {
            mSignalCluster.setTranslationX(0f);
            mSignalCluster.setTranslationY(0f);
        }
        if (!mSettingsButton.isAnimating()) {
            mSettingsContainer.setTranslationY(mSystemIconsSuperContainer.getTranslationY());
        }
        applyAlpha(mEmergencyCallsOnly, values.emergencyCallsOnlyAlpha);
        if (!mShowingDetail && !mDetailTransitioning) {
            // Otherwise it needs to stay invisible
            applyAlpha(mAlarmStatus, values.alarmStatusAlpha);
        }
        applyAlpha(mDateCollapsed, values.dateCollapsedAlpha);
        applyAlpha(mDateExpanded, values.dateExpandedAlpha);
        applyAlpha(mBatteryLevel, values.batteryLevelAlpha);
        applyAlpha(mSignalCluster, values.signalClusterAlpha);
        if (!mExpanded) {
            mTime.setScaleX(1f);
            mTime.setScaleY(1f);
        }
        updateAmPmTranslation();
    }

    /**
     * Captures all layout values (position, visibility) for a certain state. This is used for
     * animations.
     */
    private static final class LayoutValues {

        float dateExpandedAlpha;
        float dateCollapsedAlpha;
        float emergencyCallsOnlyAlpha;
        float alarmStatusAlpha;
        float timeScale = 1f;
        float clockY;
        float dateY;
        float batteryX;
        float batteryY;
        float batteryLevelAlpha;
        float signalClusterAlpha;

        public void interpoloate(LayoutValues v1, LayoutValues v2, float t) {
            timeScale = v1.timeScale * (1 - t) + v2.timeScale * t;
            clockY = v1.clockY * (1 - t) + v2.clockY * t;
            dateY = v1.dateY * (1 - t) + v2.dateY * t;
            batteryX = v1.batteryX * (1 - t) + v2.batteryX * t;
            batteryY = v1.batteryY * (1 - t) + v2.batteryY * t;

            float t1 = Math.max(0, t - 0.5f) * 2;
            emergencyCallsOnlyAlpha =
                    v1.emergencyCallsOnlyAlpha * (1 - t1) + v2.emergencyCallsOnlyAlpha * t1;

            float t2 = Math.min(1, 2 * t);
            signalClusterAlpha = v1.signalClusterAlpha * (1 - t2) + v2.signalClusterAlpha * t2;

            float t3 = Math.max(0, t - 0.7f) / 0.3f;
            batteryLevelAlpha = v1.batteryLevelAlpha * (1 - t3) + v2.batteryLevelAlpha * t3;
            dateExpandedAlpha = v1.dateExpandedAlpha * (1 - t3) + v2.dateExpandedAlpha * t3;
            dateCollapsedAlpha = v1.dateCollapsedAlpha * (1 - t3) + v2.dateCollapsedAlpha * t3;
            alarmStatusAlpha = v1.alarmStatusAlpha * (1 - t3) + v2.alarmStatusAlpha * t3;
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_BG_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_RIPPLE_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_ICON_INDICATOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_TEXT),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_INTERVAL),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_LENGTH),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_CHARGE_ANIMATION),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CUT_OUT_TEXT),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_TEXT_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_NO_SIM_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_AIRPLANE_MODE_COLOR),
                    false, this);
            updateSettings();
        }

        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_BG_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_RIPPLE_COLOR))) {
                updateBackgroundColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR))) {
                updateTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_NO_SIM_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_NETWORK_ICONS_AIRPLANE_MODE_COLOR))) {
                updateIconColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_ICON_INDICATOR))) {
                updateBatteryIndicator();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_TEXT))) {
                updateBatteryTextVisibility();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_INTERVAL))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_LENGTH))) {
                updateBatteryCircleDots();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_CHARGE_ANIMATION))) {
                updateShowChargeAnimation();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_CUT_OUT_TEXT))) {
                updateCutOutBatteryText();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_BATTERY_TEXT_COLOR))) {
                updateBatteryTextColor();
            }

        }

        void updateSettings() {
            updateBackgroundColor();
            updateTextColor();
            updateIconColor();
            updateBatteryIndicator();
            updateBatteryTextVisibility();
            updateBatteryCircleDots();
            updateShowChargeAnimation();
            updateCutOutBatteryText();
            updateCutOutBatteryText();
            updateBatteryTextColor();
        }
    }

    private void updateBackgroundColor() {
        setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.notification_header_bg), true));
        mSettingsButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable), false));
        mSystemIconsSuperContainer.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable), false));
        mAlarmStatus.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable), false));
    }

    private void updateTextColor() {
        mBatteryLevel.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255));
        mTime.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255));
        mAmPm.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255));
        mDateCollapsed.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 178));
        mDateExpanded.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 178));
        mEmergencyCallsOnly.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 102));
        mAlarmStatus.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 100));
    }

    private void updateIconColor() {
        final int iconColor = SBEHeaderColorHelper.getIconColor(mContext);
        final int noSimIconColor = SBEHeaderColorHelper.getNoSimIconColor(mContext);
        final int airplaneModeIconColor = SBEHeaderColorHelper.getAirplaneModeIconColor(mContext);
        final int tunerIconColor = (77 << 24) | (iconColor & 0x00ffffff);

        mSignalCluster.setIconTint(
                iconColor, noSimIconColor, airplaneModeIconColor);
        mBatteryMeterView.setBatteryColors(iconColor);
        ((ImageView) mSettingsButton).setImageTintList(ColorStateList.valueOf(iconColor));
        mTunerIcon.setImageTintList(
                ColorStateList.valueOf(tunerIconColor));
        Drawable alarmIcon =
                getResources().getDrawable(R.drawable.ic_access_alarms_small).mutate();
        alarmIcon.setTintList(ColorStateList.valueOf(iconColor));
        mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmIcon, null, null, null);
    }

    private void updateBatteryIndicator() {
        final int indicator = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_ICON_INDICATOR, 0);

        mBatteryMeterView.updateBatteryIndicator(indicator);
    }

    private void updateBatteryTextVisibility() {
        final boolean show = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_TEXT, 0) == 1;

        mBatteryMeterView.setTextVisibility(show);
    }

    private void updateBatteryCircleDots() {
        final int interval = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_INTERVAL, 0);
        final int length = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_CIRCLE_DOT_LENGTH, 0);

        mBatteryMeterView.updateCircleDots(interval, length);
    }
    private void updateShowChargeAnimation() {
        final boolean show = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_SHOW_CHARGE_ANIMATION, 0) == 1;

        mBatteryMeterView.setShowChargeAnimation(show);
    }

    private void updateCutOutBatteryText() {
        final boolean cutOut = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_CUT_OUT_TEXT, 1) == 1;

        mBatteryMeterView.setCutOutText(cutOut);
    }

    private void updateBatteryTextColor() {
        final int textColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_BATTERY_TEXT_COLOR, 0xffffffff);

        mBatteryMeterView.setTextColor(textColor);
    }

    private RippleDrawable getColoredBackgroundDrawable(Drawable rd, boolean applyBackgroundColor) {
        RippleDrawable background = (RippleDrawable) rd.mutate();

        background.setColor(ColorStateList.valueOf(
                SBEHeaderColorHelper.getRippleColor(mContext)));
        if (applyBackgroundColor) {
            background.setTintList(ColorStateList.valueOf(
                    SBEHeaderColorHelper.getBackgroundColor(mContext)));
        }
        return background;
    }
}
