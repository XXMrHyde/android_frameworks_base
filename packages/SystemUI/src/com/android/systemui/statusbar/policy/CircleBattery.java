/*
 * Copyright (C) 2012 Sven Dawitz for the CyanogenMod Project
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

import android.view.ViewGroup.LayoutParams;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.internal.R;

public class CircleBattery extends ImageView implements BatteryController.BatteryStateChangeCallback {
    private Handler mHandler;
    private Context mContext;
    private SettingsObserver mObserver;

    // state variables
    private boolean mAttached;      // whether or not attached to a window
    private boolean mActivated;     // whether or not activated due to system settings
    private boolean mShowText;      // whether or not to show percentage number
    private int     mBatteryStatus; // current battery status
    private int     mLevel;         // current battery level
    private int     mWarningLevel;  // battery level under which circle should become red
    private int     mAnimOffset;    // current level of charging animation
    private boolean mIsAnimating;   // stores charge-animation status to reliably remove callbacks

    private int     mCircleSize;    // draw size of circle. read rather complicated from
                                    // another status bar icon, so it fits the icon size
                                    // no matter the dps and resolution
    private RectF   mRectLeft;      // contains the precalculated rect used in drawArc(), derived from mCircleSize
    private Float   mTextLeftX;     // precalculated x position for drawText() to appear centered
    private Float   mTextY;         // precalculated y position for drawText() to appear vertical-centered

    // quite a lot of paint variables. helps to move cpu-usage from actual drawing to initialization
    private Paint   mPaintFont;
    private Paint   mPaintGray;
    private Paint   mPaintThinRing;
    private Paint   mPaintSystem;
    private Paint   mPaintRed;

    private boolean mShowBatteryStatus = true;
    private boolean mIsCharging = false;
    private boolean mIsCircleDotted = false;
    private int mDotLength;
    private int mDotInterval;
    private int mDotOffset;
    private int mBatteryStyle;
    private boolean mShowIconText;
    private boolean mEnableThemeDefault;
    private int mDefaultThinRingColor;
    private int mThinRingColor;
    private int mCircleColor;
    private int mCircleTextColor;
    private int mCircleTextChargingColor;
    private int mCircleAnimSpeed;

    // runnable to invalidate view via mHandler.postDelayed() call
    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            if (mActivated && mAttached) {
                invalidate();
            }
        }
    };

    // observes changes in system battery settings and enables/disables view accordingly
    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_ENABLE_THEME_DEFAULT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CIRCLE_DOTTED), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CIRCLE_DOT_LENGTH), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CIRCLE_DOT_INTERVAL), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CIRCLE_DOT_OFFSET), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_EMPTY_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_FILL_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED), false, this);
            onChange(true);
        }

        public void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    /***
     * Start of CircleBattery implementation
     */
    public CircleBattery(Context context) {
        this(context, null);
    }

    public CircleBattery(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleBattery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        mHandler = new Handler();
        mObserver = new SettingsObserver(mHandler);
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            mObserver.observe();
            mHandler.postDelayed(mInvalidate, 250);
            updateSettings();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mObserver.unobserve();
            mRectLeft = null; // makes sure, size based variables get
                                // recalculated on next attach
            mCircleSize = 0;    // makes sure, mCircleSize is reread from icons on
                                // next attach
        }
    }

    @Override
    public void onBatteryLevelChanged(int level, int status) {
        mLevel = level;
        mBatteryStatus = status;
        updateVisibility();
    }

    protected void updateVisibility() {
        setVisibility(mShowBatteryStatus && mActivated && isBatteryPresent() ? View.VISIBLE : View.GONE);

        if (mActivated && mAttached) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCircleSize == 0) {
            initSizeMeasureIconHeight();
        }

        setMeasuredDimension(mCircleSize + getPaddingLeft(), mCircleSize);
    }

    protected int getBatteryLevel() {
        return mLevel;
    }

    protected int getBatteryStatus() {
        return mBatteryStatus;
    }

    protected boolean isBatteryPresent() {
        return true;
    }

    protected void drawCircle(Canvas canvas, int level, int animOffset, float textX, RectF drawRect) {
        Paint usePaint = mPaintSystem;
        boolean unknownStatus = getBatteryStatus() == BatteryManager.BATTERY_STATUS_UNKNOWN;

        if (unknownStatus) {
            usePaint = mPaintGray;
            level = 100; // Draw all the circle;
        } else if (level < mWarningLevel) {
            usePaint = mPaintRed;
        } else if (getBatteryStatus() == BatteryManager.BATTERY_STATUS_FULL) {
            level = 100;
        }

        usePaint.setAntiAlias(true);
        if (mIsCircleDotted) {
            // change usePaint from solid to dashed
            usePaint.setPathEffect(new DashPathEffect(new float[]{mDotLength,mDotInterval},mDotOffset));
        }else {
            usePaint.setPathEffect(null);
        }

        // draw thin ring first
        canvas.drawArc(drawRect, 270, 360, false, mPaintThinRing);
        // draw colored arc representing charge level
        canvas.drawArc(drawRect, 270 + animOffset, 3.6f * level, false, usePaint);
        // if chosen by options, draw percentage text in the middle
        // always skip percentage when 100, so layout doesnt break
        if (unknownStatus) {
            mPaintFont.setColor(usePaint.getColor());
            canvas.drawText("?", textX, mTextY, mPaintFont);
        } else if (level < 100 && mShowText) {
            if (level < mWarningLevel) {
                mPaintFont.setColor(usePaint.getColor());
            } else if (mIsCharging) {
                mPaintFont.setColor(mCircleTextChargingColor);
            } else {
                mPaintFont.setColor(mCircleTextColor);
            }
            canvas.drawText(Integer.toString(level), textX, mTextY, mPaintFont);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRectLeft == null) {
            initSizeBasedStuff();
        }

        updateChargeAnim();

        mIsCharging = getBatteryStatus() == BatteryManager.BATTERY_STATUS_CHARGING;
        int offset = mIsCharging ? mAnimOffset : 0;

        drawCircle(canvas, getBatteryLevel(), offset, mTextLeftX, mRectLeft);
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mShowBatteryStatus = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS, 1) == 1;
        mBatteryStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, 3, UserHandle.USER_CURRENT);
        mShowText = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT, 1) == 1;
        mIsCircleDotted = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOTTED, 0) == 1;
        mDotLength = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_LENGTH, 3);
        mDotInterval = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_INTERVAL, 2);
        mDotOffset = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_OFFSET, 0);
        boolean mEnableThemeDefault = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_ENABLE_THEME_DEFAULT, 1) == 1;
        mDefaultThinRingColor = mContext.getResources().getColor(
                com.android.systemui.R.color.battery_empty_color);
        int defaultCircleColor = mContext.getResources().getColor(R.color.holo_blue_dark);
        int defaultTextColor = mContext.getResources().getColor(R.color.white);
        int defaultTextChargingColor = Color.GREEN;
        int thinRingColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_EMPTY_COLOR, mDefaultThinRingColor);
        int circleColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_FILL_COLOR, defaultCircleColor);
        int circleTextColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, defaultTextColor);
        int circleTextChargingColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, defaultTextChargingColor);
        mCircleAnimSpeed = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3);

        mThinRingColor = mEnableThemeDefault ? mDefaultThinRingColor : thinRingColor;
        mCircleColor = mEnableThemeDefault ? defaultCircleColor : circleColor;
        mCircleTextColor = mEnableThemeDefault ? defaultTextColor : circleTextColor;
        mCircleTextChargingColor = mEnableThemeDefault ? defaultTextChargingColor : circleTextChargingColor;

        mWarningLevel = mContext.getResources().getInteger(R.integer.config_lowBatteryWarningLevel);


        /*
         * initialize vars and force redraw
         */
        initializeCircleVars();
        mRectLeft = null;
        mCircleSize = 0;

        mActivated = (mBatteryStyle == BatteryController.BATTERY_STYLE_CIRCLE);
 
        updateVisibility();
    }

    private void initializeCircleVars() {

        // initialize and setup all paint variables
        // stroke width is later set in initSizeBasedStuff()
        Resources res = getResources();

        mPaintFont = new Paint();
        mPaintFont.setAntiAlias(true);
        mPaintFont.setDither(true);
        mPaintFont.setStyle(Paint.Style.STROKE);

        mPaintGray = new Paint(mPaintFont);
        mPaintThinRing = new Paint(mPaintFont);
        mPaintSystem = new Paint(mPaintFont);
        mPaintRed = new Paint(mPaintFont);

        mPaintGray.setStrokeCap(Paint.Cap.BUTT);
        mPaintThinRing.setStrokeCap(Paint.Cap.BUTT);
        mPaintSystem.setStrokeCap(Paint.Cap.BUTT);
        mPaintRed.setStrokeCap(Paint.Cap.BUTT);

        mPaintFont.setColor(mCircleTextColor);
        mPaintGray.setColor(mDefaultThinRingColor);
        mPaintThinRing.setColor(mThinRingColor);
        mPaintSystem.setColor(mCircleColor);
        mPaintRed.setColor(res.getColor(R.color.holo_red_light));

        // font needs some extra settings
        mPaintFont.setTextAlign(Align.CENTER);
        mPaintFont.setFakeBoldText(true);
    }

    /**
     * updates the animation counter
     * cares for timed callbacks to continue animation cycles
     * uses mInvalidate for delayed invalidate() callbacks
     */
    private void updateChargeAnim() {
        if (!mIsCharging || mLevel >= 97) {
            if (mIsAnimating) {
                mIsAnimating = false;
                mAnimOffset = 0;
                mHandler.removeCallbacks(mInvalidate);
            }
            return;
        }

        mIsAnimating = true;

        if (mAnimOffset > 360) {
            mAnimOffset = 0;
        } else {
            mAnimOffset += mCircleAnimSpeed;
        }

        mHandler.removeCallbacks(mInvalidate);
        mHandler.postDelayed(mInvalidate, 50);
    }

    /**
     * initializes all size dependent variables
     * sets stroke width and text size of all involved paints
     * YES! i think the method name is appropriate
     */
    private void initSizeBasedStuff() {
        if (mCircleSize == 0) {
            initSizeMeasureIconHeight();
        }

        mPaintFont.setTextSize(mCircleSize / 2f);

        float strokeWidth = mCircleSize / 6.5f;
        mPaintRed.setStrokeWidth(strokeWidth);
        mPaintSystem.setStrokeWidth(strokeWidth);
        mPaintThinRing.setStrokeWidth(strokeWidth / 3.5f);
        mPaintGray.setStrokeWidth(strokeWidth / 3.5f);

        // calculate rectangle for drawArc calls
        int pLeft = getPaddingLeft();
        mRectLeft = new RectF(pLeft + strokeWidth / 2.0f, 0 + strokeWidth / 2.0f, mCircleSize
                - strokeWidth / 2.0f + pLeft, mCircleSize - strokeWidth / 2.0f);

        // calculate Y position for text
        Rect bounds = new Rect();
        mPaintFont.getTextBounds("99", 0, "99".length(), bounds);
        mTextLeftX = mCircleSize / 2.0f + getPaddingLeft();
        // the +1 at end of formular balances out rounding issues. works out on all resolutions
        mTextY = mCircleSize / 2.0f + (bounds.bottom - bounds.top) / 2.0f - strokeWidth / 2.0f + 1;

        // force new measurement for wrap-content xml tag
        onMeasure(0, 0);
    }

    /**
     * we need to measure the size of the circle battery by checking another
     * resource. unfortunately, those resources have transparent/empty borders
     * so we have to count the used pixel manually and deduct the size from
     * it. Quite complicated, but the only way to fit properly into the
     * statusbar for all resolutions
     */
    private void initSizeMeasureIconHeight() {
        final Bitmap measure = BitmapFactory.decodeResource(getResources(),
                com.android.systemui.R.drawable.stat_sys_wifi_signal_4_fully);
        final int x = measure.getWidth() / 2;

        mCircleSize = 0;
        for (int y = 0; y < measure.getHeight(); y++) {
            int alpha = Color.alpha(measure.getPixel(x, y));
            if (alpha > 5) {
                mCircleSize++;
            }
        }
    }
}
