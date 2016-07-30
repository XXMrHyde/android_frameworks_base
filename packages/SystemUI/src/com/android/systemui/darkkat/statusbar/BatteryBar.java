/*
 * Copyright (C) 2016 DarkKat
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

package com.android.systemui.darkkat.statusbar;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryBar extends ProgressBar implements
        BatteryController.BatteryStateChangeCallback {

    private final int[] mColors;
    private int mIconTint = Color.WHITE;
    private int mFrameColor;
    private int mChargeColor;

    private int mBatteryLevel = 0;
    private int mAnimationLevel = 0;

    private boolean mShowChargeAnimation = false;
    private boolean mIsCharging = false;
    private boolean mIsAnimating = false;
    private boolean mPowerSaveEnabled = false;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

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

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final Resources res = context.getResources();

        mFrameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.batterymeter_frame_color));
        mChargeColor = context.getColor(R.color.batterymeter_charge_color);
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        final int N = levels.length();
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = levels.getInt(i, 0);
            mColors[2*i+1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        atts.recycle();

        mDarkModeBackgroundColor =
                context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        mLightModeBackgroundColor =
                context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);

        setProgressBackgroundTintList(ColorStateList.valueOf(mLightModeBackgroundColor));
        setProgressTintList(ColorStateList.valueOf(mLightModeFillColor));

        mHandler = new Handler();
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
        setProgressTintList(ColorStateList.valueOf(getColorForLevel(mIsCharging
                ? 50 : mBatteryLevel)));
        startChargeAnim();
    }

    @Override
    public void onPowerSaveChanged() {
        if (mPowerSaveEnabled != mBatteryController.isPowerSave()) {
            mPowerSaveEnabled = mBatteryController.isPowerSave();
            setProgressTintList(ColorStateList.valueOf(getColorForLevel(mIsCharging
                    ? 50 : mBatteryLevel)));
        }
    }

    public void setShowChargeAnimation(boolean showChargeAnimation) {
        mShowChargeAnimation = showChargeAnimation;
        startChargeAnim();
    }

    private int getColorForLevel(int percent) {

        // If we are in power save mode, always use the normal color.
        if (mPowerSaveEnabled) {
            return mIconTint;
        }
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) {

                // Respect tinting for "normal" level
                if (i == mColors.length-2) {
                    return mIconTint;
                } else {
                    return color;
                }
            }
        }
        return color;
    }

    public void setIconColor(int fillColor) {
        mFrameColor = (mFrameColor & 0xff000000) | (fillColor & 0x00ffffff);
        mIconTint = fillColor;
        mChargeColor = fillColor;
        setProgressBackgroundTintList(ColorStateList.valueOf(mFrameColor));
        setProgressTintList(ColorStateList.valueOf(
                getColorForLevel(mIsCharging ? 50 : mBatteryLevel)));

    }

    public void setDarkIntensity(float darkIntensity, int fillColor, int fillColorDark) {
        int backgroundTint = getBackgroundColor(darkIntensity, fillColor, fillColorDark);
        int fillTint = getFillColor(darkIntensity, fillColor, fillColorDark);
        mFrameColor = backgroundTint;
        mIconTint = fillTint;
        mChargeColor = fillTint;
        setProgressBackgroundTintList(ColorStateList.valueOf(mFrameColor));
        setProgressTintList(ColorStateList.valueOf(
                getColorForLevel(mIsCharging ? 50 : mBatteryLevel)));
    }

    private int getBackgroundColor(float darkIntensity, int lightColor, int darkColor) {
        int lightModeColor = (mLightModeBackgroundColor & 0xff000000) | (lightColor & 0x00ffffff);
        int darkModeColor = (mDarkModeBackgroundColor & 0xff000000) | (darkColor & 0x00ffffff);
        return getColorForDarkIntensity(darkIntensity, lightModeColor, darkModeColor);
    }

    private int getFillColor(float darkIntensity, int lightColor, int darkColor) {
        int lightModeColor = (mLightModeFillColor & 0xff000000) | (lightColor & 0x00ffffff);
        int darkModeColor = (mDarkModeFillColor & 0xff000000) | (darkColor & 0x00ffffff);
        return getColorForDarkIntensity(darkIntensity, lightModeColor, darkModeColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
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
