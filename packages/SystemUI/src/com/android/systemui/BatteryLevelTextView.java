/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.statusbar.policy.BatteryController;

import java.text.NumberFormat;

public class BatteryLevelTextView extends TextView implements
        BatteryController.BatteryStateChangeCallback{

    private static final String STATUS_BAR_BATTERY_STATUS_STYLE =
            "status_bar_battery_status_style";
    private static final String STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE =
            "status_bar_battery_status_percent_style";

    private int mWidth;
    private int mHeight;

    private String mPercentage;
    private int mBatteryLevel = 0;
    private boolean mShow;

    private boolean mPowerSaveEnabled = false;

    private Paint mTextFramePaint;
    private Paint mTextPaint;
    private float mTextHeight;
    private int mTextColor;
    private int mOldTextColor;

    private boolean mBatteryCharging;
    private boolean mIsAnimating;
    private int mChargeAnimSpeed;
    private float mChargingTextHeight;
    private int mCurrentTextYTop;
    private boolean mChargeAnimDisabled;

    private Animator mColorTransitionAnimator;
    private float mColorAnimatimationPosition;
    private boolean mAnimateColorTransition = false;

    private boolean mAttached;
    private boolean mIsHeader = false;

    private BatteryController mBatteryController;
    private ContentResolver mResolver;
    private Handler mHandler;

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED))) {
                setChargeAnimSpeed();
            } else {
                updateBatteryText();
            }
        }
    };

    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            invalidateIfVisible();
        }
    };

    public BatteryLevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();
        mHandler = new Handler();

        Typeface font = Typeface.create("sans-serif", Typeface.NORMAL);
        mTextFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextFramePaint.setTypeface(font);
        mTextFramePaint.setTextAlign(Paint.Align.CENTER);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTypeface(font);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mTextColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR, 0xffffffff);
        mOldTextColor = mTextColor;
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);

        updateBatteryText();
        setChargeAnimSpeed();
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        if (mAttached) {
            mBatteryController.addStateChangedCallback(this);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Respect font size setting.
        int textSize = getResources().getDimensionPixelSize(R.dimen.battery_level_text_size);
        mTextFramePaint.setTextSize(textSize);
        mTextPaint.setTextSize(textSize);
     }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryLevel = level;
        mPercentage = NumberFormat.getPercentInstance().format((double) mBatteryLevel / 100.0);
        setText(mPercentage);
        mBatteryCharging = charging;
        invalidateIfVisible();
    }

    @Override
    public void onPowerSaveChanged() {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidateIfVisible();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBatteryController != null) {
            mBatteryController.addStateChangedCallback(this);
        }
        mResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE),
                false, mObserver);
        mResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE),
                false, mObserver);
        mResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED),
                false, mObserver);
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        mResolver.unregisterContentObserver(mObserver);

        if (mBatteryController != null) {
            mBatteryController.removeStateChangedCallback(this);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
    }

    @Override
    public void draw(Canvas c) {
        final float x = mWidth * 0.5f;
        final float y = (mHeight + mTextHeight) * 0.47f;
        mTextHeight = -mTextPaint.getFontMetrics().ascent;
        mChargingTextHeight = (mTextPaint.descent() - mTextPaint.ascent())  + 0.5f;

        mTextFramePaint.setColor(getColorForLevel(mIsHeader));
        mTextFramePaint.setAlpha(102);
        mTextPaint.setColor(getColorForLevel(mIsHeader));

        if (mIsAnimating) {
            float drawFrac = (float) mCurrentTextYTop / 100f;
            c.drawText(mPercentage, x, y, mTextFramePaint);

            c.clipRect(new RectF(0.0f, (mChargingTextHeight * (1f - drawFrac)),
                    (float) mWidth, mChargingTextHeight), Region.Op.REPLACE);
            c.drawText(mPercentage, x, y, mTextPaint);

            updateChargeAnim();
        } else {
            c.drawText(mPercentage, x, y, mTextPaint);
            startChargeAnim();
        }
    }

    private void invalidateIfVisible() {
        if (getVisibility() == View.VISIBLE && mAttached) {
            if (mAttached) {
                postInvalidate();
            } else {
                invalidate();
            }
        }
    }

    private void updateVisibility() {
        if (mShow) {
            super.setVisibility(View.VISIBLE);
        } else {
            super.setVisibility(View.GONE);
        }
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                mColorAnimatimationPosition = position;
                postInvalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOldTextColor = mTextColor;
                mAnimateColorTransition = false;
            }
        });
        return animator;
    }

    private void updateBatteryText() {
        int currentUserId = ActivityManager.getCurrentUser();
        int mode = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE, 2, currentUserId);

        boolean showNextPercent = mode == 1;
        int batteryStyle = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, 0, currentUserId);

        switch (batteryStyle) {
            case 3: //BATTERY_METER_TEXT
                showNextPercent = true;
                break;
            case 4: //BATTERY_METER_GONE
                showNextPercent = false;
                break;
            default:
                break;
        }

        mShow = showNextPercent;
        updateVisibility();
        invalidateIfVisible();
    }

    public void setChargeAnimSpeed() {
        int currentUserId = ActivityManager.getCurrentUser();
        int chargeAnimSpeed = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_CHARGING_ANIMATION_SPEED, 0,
                currentUserId);

        if (chargeAnimSpeed == 0) {
            mChargeAnimDisabled = true;
        } else {
            mChargeAnimDisabled = false;
            mChargeAnimSpeed = chargeAnimSpeed;
        }

        invalidateIfVisible();
    }

    public void setTextColor(boolean isHeader) {
        mIsHeader = isHeader;
        if (!mIsAnimating && mBatteryLevel > 15 && !isHeader) {
            mAnimateColorTransition = true;
            mColorTransitionAnimator.start();
        } else {
            invalidateIfVisible();
        }

    }

    public int getColorForLevel(boolean isHeader) {
        int headerTextColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR, 0xffffffff);
        mTextColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR, 0xff000000);

        if (isHeader) {
            return headerTextColor;
        } else if (mAnimateColorTransition) {
            int blendedTextColor = ColorHelper.getBlendColor(
                    mOldTextColor, mTextColor, mColorAnimatimationPosition);
            return blendedTextColor;
        } else if (mBatteryLevel <= 15 && !mPowerSaveEnabled) {
            return 0xfff4511e;
        } else {
            return mTextColor;
        }
    }

    private void startChargeAnim() {
        if (!mBatteryCharging || mChargeAnimDisabled) {
            return;
        }
        mIsAnimating = true;
        mCurrentTextYTop = 0;
        updateChargeAnim();
    }

    private void updateChargeAnim() {
        // Stop animation when battery is full or
        // after the meter animated back to 0 after unplugging or
        // after the meter animated back to 0 after disabling charge animation
        if ((!mBatteryCharging && mCurrentTextYTop == 0)
                || (mChargeAnimDisabled && mCurrentTextYTop == 0)) {
            stopChargeAnim();
            return;
        }
        if (mCurrentTextYTop > 100) {
            mCurrentTextYTop = 0;
        } else {
            mCurrentTextYTop += mChargeAnimSpeed;
        }

        mHandler.removeCallbacks(mInvalidate);
        mHandler.postDelayed(mInvalidate, 50);
    }

    private void stopChargeAnim() {
        mIsAnimating = false;
        mCurrentTextYTop = 0;
        mHandler.removeCallbacks(mInvalidate);
        invalidateIfVisible();
    }
}
