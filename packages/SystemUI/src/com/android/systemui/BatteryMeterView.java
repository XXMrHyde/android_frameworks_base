/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryMeterView extends View implements DemoMode,
        BatteryController.BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";
    public static final String SHOW_PERCENT_SETTING = "status_bar_show_battery_percent";

    private static final int FULL = 96;
    private static final boolean SINGLE_DIGIT_PERCENT = false;
    private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction

    public static enum BatteryMeterMode {
        BATTERY_METER_ICON_PORTRAIT,
        BATTERY_METER_ICON_LANDSCAPE,
        BATTERY_METER_CIRCLE,
        BATTERY_METER_GONE
    }

    protected BatteryMeterMode mBatteryMeterMode;
    private BatteryMeterDrawable mBatteryMeterDrawable;
    private final Object mLock = new Object();

    private final int mLowLevel;
    private final int mCriticalLevel;
    private final String mWarningString;

    private boolean mIgnoreSystemUITuner = false;
    private boolean mShowPercent;
    private boolean mCutOutText = true;

    private boolean mIsCircleDotted = false;
    private int mDotLength = 0;
    private int mDotInterval = 0;

    private boolean mShowChargeAnimation = false;
    private boolean mIsAnimating = false;
    private int mAnimationLevel;

    private int mFrameColor;
    private int mFillColor = Color.WHITE;
    private int mTint = mFillColor;
    private int mTextColor = Color.WHITE;
    private final int mLowLevelColor = 0xfff4511e; // deep orange 600

    private float mButtonHeightFraction;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;

    private int mHeight;
    private int mWidth;

    private boolean mDemoMode;
    private boolean mPowerSaveEnabled;

    private final BatteryTracker mTracker;
    private final BatteryTracker mDemoTracker;
    private final SettingObserver mSettingObserver;
    private final Handler mHandler;

    private BatteryController mBatteryController;

    protected boolean mAttached;

    private final class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        boolean present = true;
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                setContentDescription(
                        context.getString(R.string.accessibility_battery_level, level));
                synchronized (mLock) {
                    if (mBatteryMeterDrawable != null) {
                        setVisibility(View.VISIBLE);
                        invalidateIfVisible();
                    }
                }
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC
                                    : 0);
                            dummy.putExtra("testmode", true);
                        }
                        getContext().sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        postDelayed(this, 200);
                    }
                });
            }
        }

        protected boolean shouldIndicateCharging() {
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                return true;
            }
            if (plugged) {
                return status == BatteryManager.BATTERY_STATUS_FULL;
            }
            return false;
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (!mIgnoreSystemUITuner) {
                updateShowPercent();
                if (!mIsAnimating) {
                    postInvalidate();
                }
            }
        }
    }

    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            invalidateIfVisible();
        }
    };

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources res = context.getResources();
        mTracker = new BatteryTracker();
        mDemoTracker = new BatteryTracker();
        mSettingObserver = new SettingObserver();
        mHandler = new Handler();

        mFrameColor = (77 << 24) | (mFillColor & 0x00ffffff);
        mLowLevel = res.getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = res.getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        mBatteryMeterMode = BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT;
        mButtonHeightFraction = res.getFraction(
                R.fraction.battery_button_height_fraction, 1, 1);
        mSubpixelSmoothingLeft = res.getFraction(
                R.fraction.battery_subpixel_smoothing_left, 1, 1);
        mSubpixelSmoothingRight = res.getFraction(
                R.fraction.battery_subpixel_smoothing_right, 1, 1);

        updateShowPercent();
        mBatteryMeterDrawable = createBatteryMeterDrawable(mBatteryMeterMode);
    }

    protected BatteryMeterDrawable createBatteryMeterDrawable(BatteryMeterMode mode) {
        Resources res = mContext.getResources();
        switch (mode) {
            case BATTERY_METER_ICON_LANDSCAPE:
                return new NormalBatteryMeterDrawable(res, true);
            case BATTERY_METER_CIRCLE:
                return new CircleBatteryMeterDrawable(res);
            case BATTERY_METER_GONE:
                return null;
            default:
                return new NormalBatteryMeterDrawable(res, false);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_LEVEL_TEST);
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            // preload the battery level
            mTracker.onReceive(getContext(), sticky);
        }
        mBatteryController.addStateChangedCallback(this);
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SHOW_PERCENT_SETTING), false, mSettingObserver);
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached = false;
        getContext().unregisterReceiver(mTracker);
        mBatteryController.removeStateChangedCallback(this);
        getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mBatteryMeterMode.compareTo(BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE) == 0) {
            width = (int)(height * 1.2f);
        } else if (mBatteryMeterMode == BatteryMeterMode.BATTERY_METER_CIRCLE) {
            height += (CircleBatteryMeterDrawable.STROKE_WITH / 3);
            width = height;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                mBatteryMeterDrawable.onSizeChanged(w, h, oldw, oldh);
            }
        }
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        // TODO: Use this callback instead of own broadcast receiver.
    }

    @Override
    public void onPowerSaveChanged() {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        if (!mIsAnimating) {
            invalidate();
        }
    }

    public void updateBatteryIndicator(int indicator) {
        BatteryMeterMode meterMode = BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT;
        switch (indicator) {
            case 1:
                meterMode = BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE;
                break;
            case 2:
                meterMode = BatteryMeterMode.BATTERY_METER_CIRCLE;
                break;
            case 3:
                meterMode = BatteryMeterMode.BATTERY_METER_GONE;
                break;
            default:
                break;
        }
        setBatteryMeterMode(meterMode);
        invalidateIfVisible();
    }

    private void setBatteryMeterMode(BatteryMeterMode mode) {
        if (mBatteryMeterMode == mode) {
            return;
        }

        mBatteryMeterMode = mode;
        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        if (mode == BatteryMeterMode.BATTERY_METER_GONE) {
            setVisibility(View.GONE);
            synchronized (mLock) {
                mBatteryMeterDrawable = null;
            }
        } else {
            synchronized (mLock) {
                if (mBatteryMeterDrawable != null) {
                    mBatteryMeterDrawable.onDispose();
                }
                mBatteryMeterDrawable = createBatteryMeterDrawable(mode);
            }
            if (mBatteryMeterMode == BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT ||
                    mBatteryMeterMode == BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE) {
                ((NormalBatteryMeterDrawable)mBatteryMeterDrawable).loadBoltPoints(
                        mContext.getResources());
            }
            if (tracker.present) {
                setVisibility(View.VISIBLE);
                requestLayout();
                invalidateIfVisible();
            } else {
                setVisibility(View.GONE);
            }
        }
    }

    private void updateShowPercent() {
        if (!mIgnoreSystemUITuner) {
            mShowPercent = 0 != Settings.System.getInt(getContext().getContentResolver(),
                    SHOW_PERCENT_SETTING, 0);
        }
    }

    public void setTextVisibility(boolean show) {
        if (!mIgnoreSystemUITuner) {
            mIgnoreSystemUITuner = true;
        }
        mShowPercent = show;
        if (!mIsAnimating) {
            invalidateIfVisible();
        }
    }

    public void updateCircleDots(int interval, int length) {
        mDotInterval = interval;
        if (mDotInterval == 0) {
            mDotLength = 0;
            mIsCircleDotted = false;
        } else {
            mDotLength = length;
            mIsCircleDotted = true;
        }
        if (!mIsAnimating) {
            invalidateIfVisible();
        }
    }

    public void setShowChargeAnimation(boolean showChargeAnimation) {
        if (mShowChargeAnimation != showChargeAnimation) {
            mShowChargeAnimation = showChargeAnimation;
            if (mShowChargeAnimation) {
                invalidateIfVisible();
            }
        }
    }

    public void setCutOutText(boolean cutOutText) {
        mCutOutText = cutOutText;
        if (!mIsAnimating) {
            invalidateIfVisible();
        }
    }

    public void setBatteryColors(int tint) {
        mTint = tint;
        if (!mIsAnimating) {
            invalidateIfVisible();
        }
    }

    public void setTextColor(int color) {
        mTextColor = color;
        if (!mIsAnimating) {
            invalidateIfVisible();
        };
    }

    private int getTintForLevel(int percent) {
        if (percent <= mLowLevel && !mPowerSaveEnabled) {
            return mLowLevelColor;
        } else {
            return mTint;
        }
    }

    private int getTextColorForLevel(int percent) {
        if (percent <= mLowLevel && !mPowerSaveEnabled) {
            return mLowLevelColor;
        } else {
            return mTextColor;
        }
    }

    protected void invalidateIfVisible() {
        if (getVisibility() == View.VISIBLE) {
            if (mAttached) {
                postInvalidate();
            } else {
                invalidate();
            }
        }
    }

    @Override
    public void draw(Canvas c) {
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
                mBatteryMeterDrawable.onDraw(c, tracker);
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (getVisibility() == View.VISIBLE) {
            if (!mDemoMode && command.equals(COMMAND_ENTER)) {
                mDemoMode = true;
                mDemoTracker.level = mTracker.level;
                mDemoTracker.plugged = mTracker.plugged;
            } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
                mDemoMode = false;
                postInvalidate();
            } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
               String level = args.getString("level");
               String plugged = args.getString("plugged");
               if (level != null) {
                   mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
               }
               if (plugged != null) {
                   mDemoTracker.plugged = Boolean.parseBoolean(plugged);
               }
               postInvalidate();
            }
        }
    }

    protected interface BatteryMeterDrawable {
        void onDraw(Canvas c, BatteryTracker tracker);
        void onSizeChanged(int w, int h, int oldw, int oldh);
        void onDispose();
    }

    protected class NormalBatteryMeterDrawable implements BatteryMeterDrawable {
        private static final boolean SINGLE_DIGIT_PERCENT = false;
        private static final boolean SHOW_100_PERCENT = false;

        private boolean mDisposed;

        protected final boolean mHorizontal;

        private final Paint mFramePaint, mFillPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
        private float mTextHeight, mWarningTextHeight;

        private final Path mShapePath = new Path();
        private final Path mClipPath = new Path();
        private final Path mTextPath = new Path();

        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mBoltFrame = new RectF();

        public NormalBatteryMeterDrawable(Resources res, boolean horizontal) {
            super();
            mHorizontal = horizontal;
            mDisposed = false;

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mFramePaint.setColor(mFrameColor);

            mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFillPaint.setDither(true);
            mFillPaint.setStrokeWidth(0);
            mFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mFillPaint.setColor(mFillColor);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);
            mWarningTextPaint.setColor(mLowLevelColor);

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBoltPoints = loadBoltPoints(res);
        }

        @Override
        public void onDraw(Canvas c, BatteryTracker tracker) {
            if (mDisposed) return;

            final int level = tracker.level;

            if (level == BatteryTracker.UNKNOWN_LEVEL) return;

            final int pt = getPaddingTop() + (mHorizontal ? (int)(mHeight * 0.12f) : 0);
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            final int pb = getPaddingBottom() + (mHorizontal ? (int)(mHeight * 0.08f) : 0);
            final int height = mHeight - pt - pb;
            final int width = mWidth - pl - pr;

            final int buttonHeight = (int) ((mHorizontal ? width : height) * mButtonHeightFraction);

            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

            if (mHorizontal) {
                mButtonFrame.set(
                        /*cover frame border of intersecting area*/
                        width - buttonHeight - mFrame.left,
                        mFrame.top + Math.round(height * 0.25f),
                        mFrame.right,
                        mFrame.bottom - Math.round(height * 0.25f));

                mButtonFrame.top += mSubpixelSmoothingLeft;
                mButtonFrame.bottom -= mSubpixelSmoothingRight;
                mButtonFrame.right -= mSubpixelSmoothingRight;
            } else {
                // button-frame: area above the battery body
                mButtonFrame.set(
                        mFrame.left + Math.round(width * 0.25f),
                        mFrame.top,
                        mFrame.right - Math.round(width * 0.25f),
                        mFrame.top + buttonHeight);

                mButtonFrame.top += mSubpixelSmoothingLeft;
                mButtonFrame.left += mSubpixelSmoothingLeft;
                mButtonFrame.right -= mSubpixelSmoothingRight;
            }

            // frame: battery body area

            if (mHorizontal) {
                mFrame.right -= buttonHeight;
            } else {
                mFrame.top += buttonHeight;
            }
            mFrame.left += mSubpixelSmoothingLeft;
            mFrame.top += mSubpixelSmoothingLeft;
            mFrame.right -= mSubpixelSmoothingRight;
            mFrame.bottom -= mSubpixelSmoothingRight;

            float drawFrac;

            if (mIsAnimating) {
                if (mAnimationLevel >= FULL) {
                    drawFrac = 1f;
                } else if (mAnimationLevel <= mCriticalLevel) {
                    drawFrac = 0f;
                } else {
                    drawFrac = (float) mAnimationLevel / 100f;
                }
            } else {
                if (level >= FULL) {
                    drawFrac = 1f;
                } else if (level <= mCriticalLevel) {
                    drawFrac = 0f;
                } else {
                    drawFrac = (float) level / 100f;
                }
            }

            final float levelTop;

            if (drawFrac == 1f) {
                if (mHorizontal) {
                    levelTop = mButtonFrame.right;
                } else {
                    levelTop = mButtonFrame.top;
                }
            } else {
                if (mHorizontal) {
                    levelTop = (mFrame.right - (mFrame.width() * (1f - drawFrac)));
                } else {
                    levelTop = (mFrame.top + (mFrame.height() * (1f - drawFrac)));
                }
            }

            // define the battery shape
            mShapePath.reset();
            mShapePath.moveTo(mButtonFrame.left, mButtonFrame.top);
            if (mHorizontal) {
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.bottom);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.bottom);
                mShapePath.lineTo(mFrame.right, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);
            } else {
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
                mShapePath.lineTo(mButtonFrame.right, mFrame.top);
                mShapePath.lineTo(mFrame.right, mFrame.top);
                mShapePath.lineTo(mFrame.right, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);
            }

            boolean boltOpaque = true;
            boolean pctOpaque = true;
            float pctX = 0, pctY = 0;
            String pctText = null;
            if (tracker.shouldIndicateCharging()
                        && (!mShowPercent || !mShowChargeAnimation)) {
                // define the bolt shape
                final float bl = mFrame.left + mFrame.width() / (mHorizontal ? 9f : 4.5f);
                final float bt = mFrame.top + mFrame.height() / (mHorizontal ? 4.5f : 6f);
                final float br = mFrame.right - mFrame.width() / (mHorizontal ? 6f : 7f);
                final float bb = mFrame.bottom - mFrame.height() / (mHorizontal ? 7f : 10f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }

                if (mCutOutText) {
                    float boltPct = mHorizontal ?
                            (mBoltFrame.left - levelTop) / (mBoltFrame.left - mBoltFrame.right) :
                            (mBoltFrame.bottom - levelTop) / (mBoltFrame.bottom - mBoltFrame.top);
                    boltPct = Math.min(Math.max(boltPct, 0), 1);
                    boltOpaque = boltPct <= BOLT_LEVEL_THRESHOLD;
                }
                if (!boltOpaque) {
                    mShapePath.op(mBoltPath, Path.Op.DIFFERENCE);
                }
            } else if (mShowPercent && level > mCriticalLevel) {
                // compute percentage text
                final float full = mHorizontal ? 0.60f : 0.45f;
                final float nofull = mHorizontal ? 0.75f : 0.6f;
                final float single = mHorizontal ? 0.86f : 0.75f;
                mTextPaint.setTextSize(height *
                        (SINGLE_DIGIT_PERCENT ? single
                                : (tracker.level == 100 ? full : nofull)));
                mTextHeight = -mTextPaint.getFontMetrics().ascent;
                pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
                pctX = mWidth * 0.5f;
                pctY = (mHeight + mTextHeight) * 0.47f;
                if (mCutOutText) {
                    if (mHorizontal) {
                        pctOpaque = pctX > levelTop;
                    } else {
                        pctOpaque = levelTop > pctY;
                    }
                } 
                if (!pctOpaque) {
                    mTextPath.reset();
                    mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                    // cut the percentage text out of the overall shape
                    mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
                }
            }

            // apply battery tint
            final PorterDuffColorFilter cff = new PorterDuffColorFilter(getTintForLevel(50), Mode.MULTIPLY);
            final PorterDuffColorFilter cfb = new PorterDuffColorFilter((tracker.plugged
                    ? getTintForLevel(50) : getTintForLevel(level)), Mode.MULTIPLY);
            mFramePaint.setColorFilter(cff);
            mFillPaint.setColorFilter(cfb);

            // update text and bolt color
            mTextPaint.setColor(getTextColorForLevel(level));
            mBoltPaint.setColor(getTextColorForLevel(50));

            // draw the battery shape background
            c.drawPath(mShapePath, mFramePaint);

            // draw the battery shape, clipped to charging level
            if (mHorizontal) {
                mFrame.right = levelTop;
            } else {
                mFrame.top = levelTop;
            }
            mClipPath.reset();
            mClipPath.addRect(mFrame,  Path.Direction.CCW);
            mShapePath.op(mClipPath, Path.Op.INTERSECT);
            c.drawPath(mShapePath, mFillPaint);


            if (tracker.shouldIndicateCharging()
                        && (!mShowPercent || !mShowChargeAnimation)) {
                if (boltOpaque) {
                    // draw the bolt
                    c.drawPath(mBoltPath, mBoltPaint);
                }
            } else if (mShowPercent) {
                if (level <= mCriticalLevel) {
                    // draw the warning text
                    final float x = mWidth * 0.5f;
                    final float y = (mHeight + mWarningTextHeight) * 0.48f;
                    c.drawText(mWarningString, x, y, mWarningTextPaint);
                } else if (pctOpaque) {
                    // draw the percentage text
                    c.drawText(pctText, pctX, pctY, mTextPaint);
                }
            }

            if (mIsAnimating) {
                updateChargeAnim(tracker);
            } else {
                startChargeAnim(tracker);
            }
        }

        @Override
        public void onDispose() {
            mHandler.removeCallbacks(mInvalidate);
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mHeight = h;
            mWidth = w;
            mWarningTextPaint.setTextSize(h * 0.75f);
            mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
        }

        private float[] loadBoltPoints(Resources res) {
            final int[] pts = res.getIntArray((mHorizontal
                    ? R.array.batterymeter_inverted_bolt_points
                    : R.array.batterymeter_bolt_points));
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }
    }

    protected class CircleBatteryMeterDrawable implements BatteryMeterDrawable {
        private static final boolean SINGLE_DIGIT_PERCENT = false;
        private static final boolean SHOW_100_PERCENT = false;

        public static final float STROKE_WITH = 6.5f;

        private boolean mDisposed;

        private Paint mFramePaint, mFillPaint, mWarningTextPaint, mTextPaint, mBoltPaint;

        private RectF mRectLeft;
        private final RectF mBoltFrame = new RectF();

        private int mCircleSize;
        private float mTextX, mTextY;

        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        public CircleBatteryMeterDrawable(Resources res) {
            super();
            mDisposed = false;

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setStrokeCap(Paint.Cap.BUTT);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.STROKE);
            mFramePaint.setColor(mFrameColor);

            mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFillPaint.setStrokeCap(Paint.Cap.BUTT);
            mFillPaint.setDither(true);
            mFillPaint.setStrokeWidth(0);
            mFillPaint.setStyle(Paint.Style.STROKE);
            mFillPaint.setColor(mFillColor);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);
            mWarningTextPaint.setColor(mLowLevelColor);

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBoltPoints = loadBoltPoints(res);
        }

        @Override
        public void onDraw(Canvas c, BatteryTracker tracker) {
            if (mDisposed) return;

            if (mRectLeft == null) {
                initSizeBasedStuff();
            }

            drawCircle(c, tracker, mTextX, mRectLeft);

            if (mIsAnimating) {
                updateChargeAnim(tracker);
            } else {
                startChargeAnim(tracker);
            }
        }

        private void drawCircle(Canvas canvas, BatteryTracker tracker,
                float textX, RectF drawRect) {
            boolean unknownStatus = tracker.status == BatteryManager.BATTERY_STATUS_UNKNOWN;
            int level = tracker.level;

            // apply battery tint
            final PorterDuffColorFilter cff = new PorterDuffColorFilter(getTintForLevel(50), Mode.MULTIPLY);
            final PorterDuffColorFilter cfb = new PorterDuffColorFilter((tracker.plugged
                    ? getTintForLevel(50) : getTintForLevel(level)), Mode.MULTIPLY);
            mFramePaint.setColorFilter(cff);
            mFillPaint.setColorFilter(cfb);

            // update text and bolt color
            mTextPaint.setColor(getTextColorForLevel(level));
            mBoltPaint.setColor(getTextColorForLevel(50));

            if (mIsCircleDotted) {
                // change mPaintStatus from solid to dashed
                mFillPaint.setPathEffect(
                        new DashPathEffect(new float[]{mDotLength,mDotInterval},0));
            } else {
                mFillPaint.setPathEffect(null);
            }

            Paint paint;

            if (unknownStatus) {
                paint = mFramePaint;
                level = 100; // Draw all the circle;
            } else {
                paint = mFillPaint;
                if (tracker.status == BatteryManager.BATTERY_STATUS_FULL) {
                    level = 100;
                }
            }

            // draw thin gray ring first
            canvas.drawArc(drawRect, 270, 360, false, mFramePaint);
            // draw colored arc representing charge level
            canvas.drawArc(drawRect, 270, (mIsAnimating ? mAnimationLevel : level) * 3.6f, false, paint);

            // if chosen by options, draw percentage text in the middle
            // always skip percentage when 100, so layout doesnt break
            if (unknownStatus) {
                canvas.drawText("?", textX, mTextY, mTextPaint);
            } else if (tracker.shouldIndicateCharging()
                        && (!mShowPercent || !mShowChargeAnimation)) {
                // draw the bolt
                final float bl = (int)(drawRect.left + drawRect.width() / 3.2f);
                final float bt = (int)(drawRect.top + drawRect.height() / 4f);
                final float br = (int)(drawRect.right - drawRect.width() / 5.2f);
                final float bb = (int)(drawRect.bottom - drawRect.height() / 8f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }
                canvas.drawPath(mBoltPath, mBoltPaint);
            } else if (mShowPercent) {
                if (level <= mCriticalLevel) {
                    // draw the warning text
                    canvas.drawText(mWarningString, textX, mTextY, mWarningTextPaint);
                } else {
                    // draw the percentage text
                    String pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
                    canvas.drawText(pctText, textX, mTextY, mTextPaint);
                }
            }
        }

        /**
         * initializes all size dependent variables
         * sets stroke width and text size of all involved paints
         * YES! i think the method name is appropriate
         */
        private void initSizeBasedStuff() {
            mCircleSize = Math.min(getMeasuredWidth(), getMeasuredHeight());
            mWarningTextPaint.setTextSize(mCircleSize / 2f);
            mTextPaint.setTextSize(mCircleSize / 2f);

            float strokeWidth = mCircleSize / STROKE_WITH;
            mFramePaint.setStrokeWidth(strokeWidth);
            mFillPaint.setStrokeWidth(strokeWidth);

            // calculate rectangle for drawArc calls
            int pLeft = getPaddingLeft();
            mRectLeft = new RectF(pLeft + strokeWidth / 2.0f, 0 + strokeWidth / 2.0f, mCircleSize
                    - strokeWidth / 2.0f + pLeft, mCircleSize - strokeWidth / 2.0f);

            // calculate Y position for text
            Rect bounds = new Rect();
            mTextPaint.getTextBounds("99", 0, "99".length(), bounds);
            mTextX = mCircleSize / 2.0f + getPaddingLeft();
            // the +1dp at end of formula balances out rounding issues.works out on all resolutions
            mTextY = mCircleSize / 2.0f + (bounds.bottom - bounds.top) / 2.0f
                    - strokeWidth / 2.0f + getResources().getDisplayMetrics().density;
        }

        @Override
        public void onDispose() {
            mHandler.removeCallbacks(mInvalidate);
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            initSizeBasedStuff();
        }

        private float[] loadBoltPoints(Resources res) {
            final int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }
    }

    private void startChargeAnim(BatteryTracker tracker) {
        if (!tracker.shouldIndicateCharging()
                || tracker.status == BatteryManager.BATTERY_STATUS_FULL
                || !mShowChargeAnimation || mIsAnimating) {
            return;
        }
        mIsAnimating = true;
        mAnimationLevel = tracker.level;
        updateChargeAnim(tracker);
    }

    /**
     * updates the animation counter
     * cares for timed callbacks to continue animation cycles
     * uses mInvalidate for delayed invalidate() callbacks
     */
    private void updateChargeAnim(BatteryTracker tracker) {
        // Stop animation: 
        // when after unplugging/after disabling charge animation,
        // the meter animated back to the current level, or
        // when the battery is full, and the meter animated back to full
        if ((!tracker.shouldIndicateCharging() && mAnimationLevel == tracker.level)
                || (!mShowChargeAnimation && mAnimationLevel == tracker.level)
                || (tracker.status == BatteryManager.BATTERY_STATUS_FULL
                && mAnimationLevel >= FULL)) {
            mIsAnimating = false;
            mAnimationLevel = tracker.level;
            return;
        }

        if (mAnimationLevel > 100) {
            mAnimationLevel = 0;
        } else {
            mAnimationLevel += 1;
        }

        postInvalidateDelayed(50);
    }
}
