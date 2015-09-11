/*
 * Copyright (C) 2015 DarkKat
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.internal.util.darkkat.ColorHelper;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryBarLayout extends RelativeLayout implements
        BatteryController.BatteryStateChangeCallback {

    private static final int INDICATOR_HIDDEN = 0;
    private static final int INDICATOR_FILL_ONLY = 1;

    private View mBatteryBarFrame;
    private View mBatteryBar;

    private int mNewColor;
    private int mOldColor;
    private int mNewFrameColor;
    private int mOldFrameColor;
    private final int mWarningColor = 0xfff4511e;

    private int mIndicator;
    private boolean mShowBar;
    private boolean mShowFrame;
    private int mThickness;

    private int mBatteryLevel = 0;
    private int mAnimLevel = 0;
    private boolean mIsCharging;
    private boolean mIsAnimating = false;
    private int mAnimationSpeed;
    private boolean mAnimationDisabled;
    private boolean mPowerSaveEnabled;

    private ContentResolver mResolver;
    private BatteryController mBatteryController;
    private DisplayMetrics mMetrics;
    private Animator mColorTransitionAnimator;
    private Handler mHandler;

    public BatteryBarLayout(Context context) {
        this(context, null, 0);
    }

    public BatteryBarLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryBarLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mResolver = context.getContentResolver();
        mMetrics = context.getResources().getDisplayMetrics();
        mHandler = new Handler();
        updateSettings();
    }

    private final Runnable mChargeAnim = new Runnable() {
        public void run() {
            updateChargeAnim();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController.addStateChangedCallback(this);
        if (mBatteryBar == null) {
            addBars();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBatteryController.removeStateChangedCallback(this);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        if (mPowerSaveEnabled != mBatteryController.isPowerSave()) {
            mPowerSaveEnabled = mBatteryController.isPowerSave();
            if (mBatteryBar != null) {
                updateColor(true);
            }
        }
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryLevel = level;
        if (mBatteryBar != null) {
            setLevel(mBatteryLevel);
            mIsCharging = charging;
            if (shouldIndicateCharging()) {
                startChargeAnim();
            }
            updateColor(true);
        }
    }

    @Override
    public void onPowerSaveChanged() {
        if (mPowerSaveEnabled != mBatteryController.isPowerSave()) {
            mPowerSaveEnabled = mBatteryController.isPowerSave();
            if (mBatteryBar != null) {
                updateColor(true);
            }
        }
    }

    private void addBars() {
        int w = (int) (((getWidth() / 100.0) * mBatteryLevel) + 0.5);
        int h = (int) ((mMetrics.density * mThickness) + 0.5);

        mBatteryBarFrame = new View(mContext);
        mBatteryBar = new View(mContext);
        RelativeLayout.LayoutParams paramsFrame = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams paramsBar = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        paramsBar.width = w;
        paramsFrame.height = h;
        paramsBar.height = h;

        addView(mBatteryBarFrame, paramsFrame);
        addView(mBatteryBar, paramsBar);

        mOldColor = mNewColor;
        mOldFrameColor = mNewFrameColor;
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);

        updateVisibility(false, false);
        updateThickness(false);
        updateColor(false);

    }

    private void setLevel(int level) {
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) mBatteryBar.getLayoutParams();
        int w = (int) (((getWidth() / 100.0) * level) + 0.5);
        params.width = w;
        mBatteryBar.setLayoutParams(params);
    }

    public void updateSettings() {
        mIndicator = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_INDICATOR, 0);
        mThickness = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_THICKNESS, 1);
        int animationSpeed = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED, 0);
        mNewColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_COLOR, 0xffffffff);
        mNewFrameColor = (102 << 24) | (mNewColor & 0x00ffffff);

        mShowBar = false;
        mShowFrame = false;
        if (mIndicator == INDICATOR_FILL_ONLY) {
            mShowBar = true;
        } else if (mIndicator != INDICATOR_HIDDEN) {
            mShowBar = true;
            mShowFrame = true;
        }
        if (animationSpeed == 0) {
            mAnimationDisabled = true;
        } else {
            mAnimationDisabled = false;
            mAnimationSpeed = animationSpeed;
        }
    }

    public void updateVisibility(boolean forceUpdate, boolean forceHide) {
        if (forceUpdate) {
            updateSettings();
        }
        if (mBatteryBarFrame != null) {
            if (mShowFrame) {
                mBatteryBarFrame.setVisibility(View.VISIBLE);
            } else {
                mBatteryBarFrame.setVisibility(View.GONE);
            }
        }
        if (mShowBar) {
            setVisibility(forceHide ? View.GONE : View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    public void updateThickness(boolean forceUpdate) {
        if (forceUpdate) {
            updateSettings();
        }
        RelativeLayout.LayoutParams paramsFrame =
                (RelativeLayout.LayoutParams) mBatteryBarFrame.getLayoutParams();
        RelativeLayout.LayoutParams paramsBar =
                (RelativeLayout.LayoutParams) mBatteryBar.getLayoutParams();
        int h = (int) ((mMetrics.density * mThickness) + 0.5);
        paramsFrame.height = h;
        paramsBar.height = h;

        mBatteryBarFrame.setLayoutParams(paramsFrame);
        mBatteryBar.setLayoutParams(paramsBar);
    }

    public void updateAnimationSpeed() {
        int animationSpeed = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED, 0);
        if (animationSpeed == 0) {
            mAnimationDisabled = true;
        } else {
            mAnimationDisabled = false;
            mAnimationSpeed = animationSpeed;
        }
        if (shouldIndicateCharging()) {
            startChargeAnim();
        }
        updateColor(true);
    }

    public void updateColor(boolean forceUpdate) {
        if (forceUpdate) {
            updateSettings();
            if (useWarningColor()) {
                mNewColor = mWarningColor;
            }
            if (mOldColor != mNewColor
                    || mOldFrameColor != mNewFrameColor) {
                mColorTransitionAnimator.start();
            }
        } else {
            if (useWarningColor()) {
                mNewColor = mWarningColor;
                mOldColor = mNewColor;
            }
            mBatteryBarFrame.setBackgroundColor(mNewFrameColor);
            mBatteryBar.setBackgroundColor(mNewColor);
        }
    }

    private boolean useWarningColor() {
        return mBatteryLevel <= 15 && !(mPowerSaveEnabled || mIsCharging);
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mShowFrame) {
                    int blendedFrame = ColorHelper.getBlendColor(mOldFrameColor, mNewFrameColor, position);
                    mBatteryBarFrame.setBackgroundColor(blendedFrame);
                }
                int blendedBar = ColorHelper.getBlendColor(mOldColor, mNewColor, position);
                mBatteryBar.setBackgroundColor(blendedBar);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOldFrameColor = mNewFrameColor;
                mOldColor = mNewColor;
                if (!mShowFrame) {
                    mBatteryBarFrame.setBackgroundColor(mNewFrameColor);
                }
            }
        });
        return animator;
    }

    private boolean shouldIndicateCharging() {
        return !mAnimationDisabled && (mIsCharging && !mIsAnimating);
    }

    private void startChargeAnim() {
        mIsAnimating = true;
        mAnimLevel = mBatteryLevel;

        updateChargeAnim();
    }

    private void updateChargeAnim() {
        if ((!mIsCharging && mAnimLevel == 0)
                || (mAnimationSpeed == 0 && mAnimLevel == 0)) {
            stopChargeAnim();
            return;
        }

        if (mAnimLevel > 100) {
            mAnimLevel = 0;
        } else {
            mAnimLevel += mAnimationSpeed;
        }
        setLevel(mAnimLevel);

        mHandler.removeCallbacks(mChargeAnim);
        mHandler.postDelayed(mChargeAnim, 50);
    }

    private void stopChargeAnim() {
        mIsAnimating = false;
        mAnimLevel = 0;
        mHandler.removeCallbacks(mChargeAnim);
        setLevel(mBatteryLevel);
    }
}
