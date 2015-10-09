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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
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

import com.android.internal.util.darkkat.QSColorHelper;
import com.android.internal.util.darkkat.SBEHeaderColorHelper;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.BatteryLevelTextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.phone.StatusBarHeaderExpandedPanel;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.UserInfoController;

import java.text.NumberFormat;

/**
 * The view to manage the header area in the expanded status bar.
 */
public class StatusBarHeaderView extends RelativeLayout implements View.OnClickListener,
        NextAlarmController.NextAlarmChangeCallback {

    private boolean mExpanded;
    private boolean mListening;

    private StatusBarHeaderExpandedPanel mExpandedPanel;
    private View mDateGroup;
    private TextView mTime;
    private TextView mAmPm;
    private MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private TextView mDateCollapsed;
    private TextView mDateExpanded;
    private View mSwapPanelsButton;
    private View mHeaderSettingsButton;
    private View mSettingsButton;
    private View mCollapsedPanelLayout;
    private View mQsDetailHeader;
    private TextView mQsDetailHeaderTitle;
    private Switch mQsDetailHeaderSwitch;
    private ImageView mQsDetailHeaderProgress;
    private TextView mAlarmStatus;

    private boolean mAlarmShowing;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private int mCollapsedHeight;
    private int mExpandedHeight;

    private ActivityStarter mActivityStarter;
    private NextAlarmController mNextAlarmController;
    private QSPanel mQSPanel;

    private final Rect mClipBounds = new Rect();

    private boolean mCaptureValues;
    private final LayoutValues mCollapsedValues = new LayoutValues();
    private final LayoutValues mExpandedValues = new LayoutValues();
    private final LayoutValues mCurrentValues = new LayoutValues();

    private float mCurrentT;
    private boolean mShowingDetail;
    private float mElevation;
    private boolean mCollapsedPanelBgVisible = true;

    public StatusBarHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private SettingsObserver mSettingsObserver;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCollapsedPanelLayout = findViewById(R.id.collapsed_panel_layout);
        mDateGroup = findViewById(R.id.date_group);
        mTime = (TextView) findViewById(R.id.time_view);
        mAmPm = (TextView) findViewById(R.id.am_pm_view);
        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
        mDateExpanded = (TextView) findViewById(R.id.date_expanded);
        mSwapPanelsButton = findViewById(R.id.header_swap_panels_button);
        mSwapPanelsButton.setOnClickListener(this);
        mHeaderSettingsButton = findViewById(R.id.header_settings_button);
        mHeaderSettingsButton.setOnClickListener(this);

        mHeaderSettingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startQsSettingsActivity();
                return true;
            }
        });

        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mQsDetailHeader = findViewById(R.id.qs_detail_header);
        mQsDetailHeader.setAlpha(0);
        mQsDetailHeaderTitle = (TextView) mQsDetailHeader.findViewById(android.R.id.title);
        mQsDetailHeaderSwitch = (Switch) mQsDetailHeader.findViewById(android.R.id.toggle);
        mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        mAlarmStatus.setOnClickListener(this);
        mExpandedPanel = (StatusBarHeaderExpandedPanel) findViewById(R.id.header_expanded_panel);
        mSettingsObserver = new SettingsObserver(new Handler());

        mCollapsedHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height);
        mExpandedHeight = getResources().getDimensionPixelSize(
                R.dimen.status_bar_header_height_expanded);
        mElevation = getResources().getDimension(R.dimen.header_elevation);

        updateVisibilities();
        updateBackgroundColor();
        updateTextColorSettings();
        updateIconColorSettings();
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
        mAlarmStatus.setX(findViewById(R.id.clock_date_view).getLeft() + mDateGroup.getLeft() + mDateCollapsed.getRight());
        mAlarmStatus.setY(findViewById(R.id.clock_date_view).getTop() + mDateGroup.getTop() - mAlarmStatus.getPaddingTop());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mDateCollapsed, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mDateExpanded, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(this, android.R.id.toggle, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(mAmPm, R.dimen.qs_time_collapsed_size);
    }

    private void requestCaptureValues() {
        mCaptureValues = true;
        requestLayout();
    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
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
        requestCaptureValues();
        updateBackgroundColor();
        updateTextColorSettings();
        updateIconColorSettings();
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
        mMultiUserSwitch.setVisibility(mExpanded && getQSType() == 0 ? View.VISIBLE : View.INVISIBLE);
        mMultiUserAvatar.setVisibility(mExpanded && getQSType() == 0 ? View.VISIBLE : View.INVISIBLE);
        mQsDetailHeader.setVisibility(mExpanded && mShowingDetail? View.VISIBLE : View.INVISIBLE);
    }

    private void updateListeners() {
        if (mListening) {
            mSettingsObserver.observe();
            mNextAlarmController.addStateChangedCallback(this);
            mExpandedPanel.setListening(true);
        } else {
            mNextAlarmController.removeStateChangedCallback(this);
            mExpandedPanel.setListening(false);
            mSettingsObserver.unobserve();
        }
    }

    private void updateAmPmTranslation() {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        mAmPm.setTranslationX((rtl ? 1 : -1) * mTime.getWidth() * (1 - mTime.getScaleX()));
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
        mMultiUserSwitch.setClickable(mExpanded && getQSType() == 0);
        mMultiUserSwitch.setFocusable(mExpanded && getQSType() == 0);
        mAlarmStatus.setClickable(mNextAlarm != null && mNextAlarm.getShowIntent() != null);
//        mExpandedPanel.updateClickTargets(mExpanded);
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

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == mSettingsButton) {
            startSettingsActivity();
        } else if (v == mHeaderSettingsButton) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings$StatusBarExpandedHeaderSettingsActivity"));
                mActivityStarter.startActivity(intent, true /* dismissShade */);
        } else if (v == mSwapPanelsButton) {
            mExpandedPanel.swapPanels(true /* with animation */);
        } else if (v == mAlarmStatus && mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null && showIntent.isActivity()) {
                mActivityStarter.startActivity(showIntent.getIntent(), true /* dismissShade */);
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

    private void startQsSettingsActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$StatusBarExpandedQsSettingsActivity"));
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    public void setQSPanel(QSPanel qsp) {
        mQSPanel = qsp;
        if (mQSPanel != null) {
            mQSPanel.setCallback(mQsPanelCallback);
        }
        mMultiUserSwitch.setQsPanel(qsp);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // We don't want that everything lights up when we click on the header, so block the request
        // here.
    }

    private void captureLayoutValues(LayoutValues target) {

        target.settingsX = mExpanded && getQSType() == 0
                ? mSettingsButton.getLeft() - mSettingsButton.getWidth()
                : mSettingsButton.getLeft();
        target.settingsRotation = !mExpanded ? 180f : 0f;

        target.expandedPanelY = mExpandedPanel.getTop();
        target.collapsedPanelElevation = mExpanded ? 0 : mElevation;

        target.dateCollapsedAlpha = getAlphaForVisibility(mDateCollapsed);
        target.dateExpandedAlpha = getAlphaForVisibility(mDateExpanded);
        target.alarmStatusAlpha = getAlphaForVisibility(mAlarmStatus);
        target.multiUserSwitchAlpha = getAlphaForVisibility(mMultiUserSwitch);
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
        float height = mCollapsedHeight + mCurrentT * (mExpandedHeight - mCollapsedHeight);
        if (height < mCollapsedHeight) {
            height = mCollapsedHeight;
        }
        if (height > mExpandedHeight) {
            height = mExpandedHeight;
        }
        if (getQSType() == 0) {
            mSettingsButton.setX(values.settingsX);
            mSettingsButton.setRotation(values.settingsRotation);
            mHeaderSettingsButton.setX(values.settingsX - mHeaderSettingsButton.getWidth());
            mSwapPanelsButton.setX(values.settingsX - mHeaderSettingsButton.getWidth() * 2);
        }
        mExpandedPanel.setY(height - mExpandedPanel.getHeight());
        if (!mShowingDetail) {
            // Otherwise it needs to stay invisible
            applyAlpha(mAlarmStatus, values.alarmStatusAlpha);
        }
        mCollapsedPanelLayout.setElevation(values.collapsedPanelElevation);
        if (mCurrentT == 1f) {
            mCollapsedPanelBgVisible = false;
            mCollapsedPanelLayout.setBackground(null);
            mCollapsedPanelLayout.setClickable(false);
            mCollapsedPanelLayout.setFocusable(false);
        } else if (!mCollapsedPanelBgVisible) {
            mCollapsedPanelBgVisible = true;
            mCollapsedPanelLayout.setBackground(getColoredBackgroundDrawable(
                    mContext.getDrawable(R.drawable.notification_header_bg), true));
            mCollapsedPanelLayout.setClickable(true);
            mCollapsedPanelLayout.setFocusable(true);
        }
        applyAlpha(mDateCollapsed, values.dateCollapsedAlpha);
        applyAlpha(mDateExpanded, values.dateExpandedAlpha);
        applyAlpha(mMultiUserSwitch, values.multiUserSwitchAlpha);
    }

    /**
     * Captures all layout values (position, visibility) for a certain state. This is used for
     * animations.
     */
    private static final class LayoutValues {

        float settingsX;
        float settingsRotation;
        float expandedPanelY;
        float collapsedPanelElevation;
        float dateExpandedAlpha;
        float dateCollapsedAlpha;
        float alarmStatusAlpha;
        float multiUserSwitchAlpha;

        public void interpoloate(LayoutValues v1, LayoutValues v2, float t) {
            settingsX = v1.settingsX * (1 - t) + v2.settingsX * t;
            settingsRotation = v1.settingsRotation * (1 - t) + v2.settingsRotation * t;
            expandedPanelY = v1.expandedPanelY * (1 - t) + v2.expandedPanelY * t;

            float t3 = Math.max(0, t - 0.7f) / 0.3f;
            collapsedPanelElevation = v1.collapsedPanelElevation * (1 - t3) + v2.collapsedPanelElevation * t3;
            dateExpandedAlpha = v1.dateExpandedAlpha * (1 - t3) + v2.dateExpandedAlpha * t3;
            dateCollapsedAlpha = v1.dateCollapsedAlpha * (1 - t3) + v2.dateCollapsedAlpha * t3;
            alarmStatusAlpha = v1.alarmStatusAlpha * (1 - t3) + v2.alarmStatusAlpha * t3;
            multiUserSwitchAlpha = v1.multiUserSwitchAlpha * (1 - t3) + v2.multiUserSwitchAlpha * t3;
        }
    }

    private final QSPanel.Callback mQsPanelCallback = new QSPanel.Callback() {
        private boolean mScanState;

        @Override
        public void onToggleStateChanged(final boolean state) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleToggleStateChanged(state);
                }
            });
        }

        @Override
        public void onShowingDetail(final QSTile.DetailAdapter detail) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleShowingDetail(detail);
                }
            });
        }

        @Override
        public void onScanStateChanged(final boolean state) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleScanStateChanged(state);
                }
            });
        }

        private void handleToggleStateChanged(boolean state) {
            mQsDetailHeaderSwitch.setChecked(state);
        }

        private void handleScanStateChanged(boolean state) {
            if (mScanState == state) return;
            mScanState = state;
            final Animatable anim = (Animatable) mQsDetailHeaderProgress.getDrawable();
            if (state) {
                mQsDetailHeaderProgress.animate().alpha(1f);
                anim.start();
            } else {
                mQsDetailHeaderProgress.animate().alpha(0f);
                anim.stop();
            }
        }

        private void handleShowingDetail(final QSTile.DetailAdapter detail) {
            final boolean showingDetail = detail != null;
            transition(mExpandedPanel, !showingDetail);
            transition(mQsDetailHeader, showingDetail);
            mShowingDetail = showingDetail;
            if (showingDetail) {
                mQsDetailHeaderTitle.setText(detail.getTitle());
                mQsDetailHeaderTitle.setTextColor(QSColorHelper.getTextColor(mContext));
                final Boolean toggleState = detail.getToggleState();
                if (toggleState == null) {
                    mQsDetailHeaderSwitch.setVisibility(INVISIBLE);
                    mQsDetailHeader.setClickable(false);
                } else {
                    mQsDetailHeaderSwitch.setVisibility(VISIBLE);
                    mQsDetailHeaderSwitch.setChecked(toggleState);
                    mQsDetailHeader.setClickable(true);
                    mQsDetailHeader.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            detail.setToggleState(!mQsDetailHeaderSwitch.isChecked());
                        }
                    });
                }
            } else {
                mQsDetailHeader.setClickable(false);
            }
        }

        private void transition(final View v, final boolean in) {
            if (in) {
                v.bringToFront();
                v.setVisibility(VISIBLE);
            }
            if (v.hasOverlappingRendering()) {
                v.animate().withLayer();
            }
            v.animate()
                    .alpha(in ? 1 : 0)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (!in) {
                                v.setVisibility(INVISIBLE);
                            }
                        }
                    })
                    .start();
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_RIPPLE_COLOR),
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
                updateTextColorSettings();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR))) {
                updateIconColorSettings();
            }
        }

        public void updateSettings() {
            updateBackgroundColor();
            updateTextColorSettings();
            updateIconColorSettings();
        }
    }

    private void updateTextColorSettings() {
        mTime.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255));
        mAmPm.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255));
        mDateCollapsed.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 178));
        mDateExpanded.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 178));
        mAlarmStatus.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 100));
        mExpandedPanel.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 255), true);
        mExpandedPanel.setTextColor(
                SBEHeaderColorHelper.getTextColor(mContext, 100), false);
    }

    private void updateIconColorSettings() {
        final int iconColor = SBEHeaderColorHelper.getIconColor(mContext);

        ((ImageView) mSettingsButton).setColorFilter(
                SBEHeaderColorHelper.getIconColor(mContext), Mode.MULTIPLY);
        ((ImageView) mHeaderSettingsButton).setColorFilter(
                SBEHeaderColorHelper.getIconColor(mContext), Mode.MULTIPLY);
        ((ImageView) mSwapPanelsButton).setColorFilter(
                SBEHeaderColorHelper.getIconColor(mContext), Mode.MULTIPLY);
        Drawable alarmIcon = getResources().getDrawable(R.drawable.ic_access_alarms_small);
        alarmIcon.setColorFilter(
                SBEHeaderColorHelper.getIconColor(mContext), Mode.MULTIPLY);
        mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmIcon, null, null, null);
        mExpandedPanel.setIconColor(
                SBEHeaderColorHelper.getIconColor(mContext));
    }

    private void updateBackgroundColor() {
        setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.notification_header_bg), true));
        mCollapsedPanelLayout.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.notification_header_bg), true));
        mMultiUserSwitch.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mSettingsButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mHeaderSettingsButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mSwapPanelsButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mAlarmStatus.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_rectangle), false));
    }

    private RippleDrawable getColoredBackgroundDrawable(Drawable rd, boolean applyBackgroundColor) {
        RippleDrawable background = (RippleDrawable) rd;
        int states[][] = new int[][] {
            new int[] {
                com.android.internal.R.attr.state_enabled
            }
        };
        int colors[] = new int[] {
            SBEHeaderColorHelper.getRippleColor(mContext)
        };
        ColorStateList color = new ColorStateList(states, colors);

        background.setColor(color);
        if (applyBackgroundColor) {
            background.setColorFilter(
                    SBEHeaderColorHelper.getBackgroundColor(mContext), Mode.MULTIPLY);
        }
        return background;
    }

    private int getQSType() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_TYPE, 0);
    }

    public View getCollapsedPanelLayout() {
        return mCollapsedPanelLayout;
    }
}
