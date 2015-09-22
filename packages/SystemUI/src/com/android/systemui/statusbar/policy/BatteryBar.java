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
import android.content.res.ColorStateList;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ProgressBar;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryBar extends ProgressBar implements
        BatteryController.BatteryStateChangeCallback {

    private static final int INDICATOR_HIDDEN     = 0;
    private static final int INDICATOR_FILL_ONLY  = 1;

    private int mNewColor;
    private int mOldColor;
    private int mNewFrameColor;
    private int mOldFrameColor;
    private final int mWarningColor = 0xfff4511e;

    private int mIndicator;
    private boolean mShowBar;
    private boolean mShowFrame;

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

    public BatteryBar(Context context) {
        this(context, null, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mResolver = context.getContentResolver();
        mMetrics = context.getResources().getDisplayMetrics();
        mHandler = new Handler();
        updateSettings();
        mOldColor = mNewColor;
        mOldFrameColor = mNewFrameColor;
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController.addStateChangedCallback(this);
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
            updateColor(true);
        }
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryLevel = level;
        setLevel(mBatteryLevel);
        mIsCharging = charging;
        if (shouldIndicateCharging()) {
            startChargeAnim();
        }
        updateColor(true);
    }

    @Override
    public void onPowerSaveChanged() {
        if (mPowerSaveEnabled != mBatteryController.isPowerSave()) {
            mPowerSaveEnabled = mBatteryController.isPowerSave();
            updateColor(true);
        }
    }

    private void setLevel(int level) {
        setProgress(level);
    }

    public void updateSettings() {
        mIndicator = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_INDICATOR, 0);
        int animationSpeed = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED, 0);
        mNewColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_COLOR, 0xffffffff);
        mNewFrameColor = (77 << 24) | (mNewColor & 0x00ffffff);

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

    public void updateVisibility(boolean forceHide) {
        updateSettings();
        if (!mShowBar) {
            setVisibility(View.GONE);
        } else {
            setVisibility(forceHide ? View.GONE : View.VISIBLE);
        }
        setProgressBackgroundTintList(mShowFrame
                ? ColorStateList.valueOf(mNewFrameColor)
                : ColorStateList.valueOf(0x00000000));
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
            setProgressTintList(ColorStateList.valueOf(mNewColor));
            setProgressBackgroundTintList(mShowFrame
                    ? ColorStateList.valueOf(mNewFrameColor)
                    : ColorStateList.valueOf(0x00000000));
        }
    }

    private boolean useWarningColor() {
        return mBatteryLevel <= 15 && !(mPowerSaveEnabled || mIsCharging);
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

    private final Runnable mChargeAnim = new Runnable() {
        public void run() {
            updateChargeAnim();
        }
    };

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mShowFrame) {
                    int blendedFrame = ColorHelper.getBlendColor(mOldFrameColor, mNewFrameColor, position);
                    setProgressBackgroundTintList(ColorStateList.valueOf(blendedFrame));
                }
                int blendedProgress = ColorHelper.getBlendColor(mOldColor, mNewColor, position);
                setProgressTintList(ColorStateList.valueOf(blendedProgress));
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOldFrameColor = mNewFrameColor;
                mOldColor = mNewColor;
            }
        });
        return animator;
    }

    public boolean isBatteryBarEnabled() {
        updateSettings();
        return mShowBar;
    }

    public int getThickness() {
        int thicknessDp =  Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_BAR_THICKNESS, 1);

        return (int) ((mMetrics.density * thicknessDp) + 0.5);
    }
}
