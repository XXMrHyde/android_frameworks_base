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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.android.internal.R;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryBar extends ProgressBar implements
        BatteryController.BatteryStateChangeCallback {

    private static final int INDICATOR_HIDDEN     = 0;
    private static final int INDICATOR_FILL_ONLY  = 1;

    private int mColor = Color.WHITE;
    private final int mLowLevelColor = 0xfff4511e; // deep orange 600

    private final int mLowLevel;
    private int mBatteryLevel = 0;
    private int mAnimationLevel = 0;

    private boolean mShowChargeAnimation = false;
    private boolean mIsCharging = false;
    private boolean mIsAnimating = false;
    private boolean mPowerSaveEnabled = false;

    private Handler mHandler;
    private BatteryController mBatteryController;

    private final Runnable mChargeAnim = new Runnable() {
        public void run() {
            updateChargeAnim();
        }
    };

    public BatteryBar(Context context) {
        this(context, null, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();
        mLowLevel = mContext.getResources().getInteger(
                R.integer.config_lowBatteryWarningLevel);
        final int backgroundColor = (77 << 24) | (mColor & 0x00ffffff);
        setProgressBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        setProgressTintList(ColorStateList.valueOf(mColor));
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
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryLevel = level;
        setProgress(mBatteryLevel);
        mIsCharging = charging;
        setProgressTintList(ColorStateList.valueOf(getColorForLevel(mBatteryLevel)));
        startChargeAnim();
    }

    @Override
    public void onPowerSaveChanged() {
        if (mPowerSaveEnabled != mBatteryController.isPowerSave()) {
            mPowerSaveEnabled = mBatteryController.isPowerSave();
            setProgressTintList(ColorStateList.valueOf(getColorForLevel(mBatteryLevel)));
        }
    }

    public void setShowChargeAnimation(boolean showChargeAnimation) {
        mShowChargeAnimation = showChargeAnimation;
        startChargeAnim();
    }

    public void setColor(int color) {
        mColor = color;
        final int backgroundColor = (77 << 24) | (mColor & 0x00ffffff);
        setProgressBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        setProgressTintList(ColorStateList.valueOf(getColorForLevel(mBatteryLevel)));
    }

    private int getColorForLevel(int percent) {
        if (percent <= mLowLevel && !mPowerSaveEnabled) {
            return mLowLevelColor;
        } else {
            return mColor;
        }
    }

    private void startChargeAnim() {
        if (!mShowChargeAnimation || !mIsCharging || mIsAnimating) {
            return;
        }
        mIsAnimating = true;
        mAnimationLevel = mBatteryLevel;

        updateChargeAnim();
    }

    private void updateChargeAnim() {
        if ((!mIsCharging && mAnimationLevel == mBatteryLevel)
                || (!mShowChargeAnimation && mAnimationLevel == mBatteryLevel)) {
            mIsAnimating = false;
            return;
        }

        if (mAnimationLevel > 100) {
            mAnimationLevel = 0;
        } else {
            mAnimationLevel += 1;
        }
        setProgress(mAnimationLevel);

        mHandler.removeCallbacks(mChargeAnim);
        mHandler.postDelayed(mChargeAnim, 50);
    }
}
