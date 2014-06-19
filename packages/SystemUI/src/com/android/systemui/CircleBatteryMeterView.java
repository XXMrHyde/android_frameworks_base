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

package com.android.systemui;

import android.view.ViewGroup.LayoutParams;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
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

import com.android.systemui.BatteryMeterView;

/**
 * Note about CircleBattery Implementation:
 *
 * Unfortunately, we cannot use BatteryController or DockBatteryController here,
 * since communication between controller and this view is not possible without
 * huge changes. As a result, this Class is doing everything by itself,
 * monitoring battery level and battery settings.
 */

public class CircleBatteryMeterView extends ImageView {
    private Handler mHandler;
    private Context mContext;
    private BatteryReceiver mBatteryReceiver = null;

    public static final int BATTERY_STYLE_CIRCLE = 2;

    // state variables
    private boolean mAttached;         // whether or not attached to a window
    private boolean mActivated;        // whether or not activated due to system settings
    private boolean mShowText = false; // whether or not to show percentage number
    private int     mBatteryStatus;    // current battery status
    private int     mLevel;            // current battery level
    private int     mWarningLevel;     // battery level under which circle should become red
    private int     mAnimOffset;       // current level of charging animation
    private boolean mIsAnimating;      // stores charge-animation status to reliably remove callbacks
    private int mDockLevel;            // current dock battery level
    private boolean mDockIsCharging;   // whether or not dock battery is currently charging
    private boolean mIsDocked = false; // whether or not dock battery is connected

    private int     mCircleSize;       // draw size of circle. read rather complicated from
                                       // another status bar icon, so it fits the icon size
                                       // no matter the dps and resolution
    private RectF mRectLeft;           // contains the precalculated rect used in drawArc(), derived from mCircleSize
    private RectF mRectRight;          // contains the precalculated rect used in drawArc() for dock battery
    private Float mTextLeftX;          // precalculated x position for drawText() to appear centered
    private Float mTextY;              // precalculated y position for drawText() to appear vertical-centered
    private Float mTextRightX;         // precalculated x position for dock battery drawText()

    // quite a lot of paint variables. helps to move cpu-usage from actual drawing to initialization
    private Paint   mPaintFont;
    private Paint   mPaintThinRing;
    private Paint   mPaintStatus;

    private boolean mIsCharging = false;
    private boolean mIsCircleDotted = false;
    private int mDotLength;
    private int mDotInterval;
    private int mDotOffset;
    private boolean mCustomThinRingColor;
    private boolean mCustomHightColor;
    private int mThinRingColor;
    private int mCircleColor;
    private int mCircleTextColor;
    private int mCircleTextHightColor;
    private int mCircleTextChargingColor;
    private int mCircleAnimSpeed;

    private String mCircleBatteryView;

    // runnable to invalidate view via mHandler.postDelayed() call
    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            if (mActivated && mAttached) {
                invalidate();
            }
        }
    };

    // keeps track of current battery level and charger-plugged-state
    class BatteryReceiver extends BroadcastReceiver {
        private boolean mIsRegistered = false;

        public BatteryReceiver(Context context) {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mIsCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

                if (mActivated && mAttached) {
                    LayoutParams l = getLayoutParams();
                    l.width = mCircleSize + getPaddingLeft()
                            + (mIsDocked ? mCircleSize + getPaddingLeft() : 0);
                    setLayoutParams(l);

                    updateSettings();
                }
            }
        }

        private void registerSelf() {
            if (!mIsRegistered) {
                mIsRegistered = true;

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                mContext.registerReceiver(mBatteryReceiver, filter);
            }
        }

        private void unregisterSelf() {
            if (mIsRegistered) {
                mIsRegistered = false;
                mContext.unregisterReceiver(this);
            }
        }

        private void updateRegistration() {
            if (mActivated && mAttached) {
                registerSelf();
            } else {
                unregisterSelf();
            }
        }
    }

    /***
     * Start of CircleBattery implementation
     */
    public CircleBatteryMeterView(Context context) {
        this(context, null);
    }

    public CircleBatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleBatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray circleBatteryType = context.obtainStyledAttributes(attrs,
            com.android.systemui.R.styleable.BatteryIcon, 0, 0);

        mCircleBatteryView = circleBatteryType.getString(
                com.android.systemui.R.styleable.BatteryIcon_batteryView);

        if (mCircleBatteryView == null) {
            mCircleBatteryView = "statusbar";
        }

        mContext = context;
        mHandler = new Handler();
        mBatteryReceiver = new BatteryReceiver(mContext);
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            mBatteryReceiver.updateRegistration();
            updateSettings();
            mHandler.postDelayed(mInvalidate, 250);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mBatteryReceiver.updateRegistration();
            mRectLeft = null; // makes sure, size based variables get
                              // recalculated on next attach
            mCircleSize = 0;  // makes sure, mCircleSize is reread from icons on
                              // next attach
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCircleSize == 0) {
            initSizeMeasureIconHeight();
        }

        setMeasuredDimension(mCircleSize + getPaddingLeft(), mCircleSize);
    }

    protected void drawCircle(Canvas canvas, int level, int animOffset, float textX, RectF drawRect) {

        if (mIsCircleDotted) {
            // change mPaintStatus from solid to dashed
            mPaintStatus.setPathEffect(new DashPathEffect(new float[]{mDotLength,mDotInterval},mDotOffset));
        }else {
            mPaintStatus.setPathEffect(null);
        }

        // draw thin ring first
        canvas.drawArc(drawRect, 270, 360, false, mPaintThinRing);
        // draw colored arc representing charge level
        canvas.drawArc(drawRect, 270 + animOffset, 3.6f * level, false, mPaintStatus);
        // if chosen by options, draw percentage text in the middle
        // always skip percentage when 100, so layout doesnt break
        if (level < 100 && mShowText) {
            canvas.drawText(Integer.toString(level), textX, mTextY, mPaintFont);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRectLeft == null) {
            initSizeBasedStuff();
        }

        updateChargeAnim();

        if (mIsDocked) {
            drawCircle(canvas, mDockLevel, (mDockIsCharging ? mAnimOffset : 0), mTextLeftX, mRectLeft);
            drawCircle(canvas, mLevel, (mIsCharging ? mAnimOffset : 0), mTextRightX, mRectRight);
        } else {
            drawCircle(canvas, mLevel, (mIsCharging ? mAnimOffset : 0), mTextLeftX, mRectLeft);
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        boolean showBatteryStatus = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS, 1,
                UserHandle.USER_CURRENT) == 1;
        int batteryStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, 2,
                UserHandle.USER_CURRENT);
        mShowText = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT, 1,
                UserHandle.USER_CURRENT) == 1;
        mCustomThinRingColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_CUSTOM_FRAME_COLOR, 0,
                UserHandle.USER_CURRENT) == 1;
        mIsCircleDotted = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOTTED, 0,
                UserHandle.USER_CURRENT) == 1;
        mDotLength = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_LENGTH, 3,
                UserHandle.USER_CURRENT);
        mDotInterval = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_INTERVAL, 2,
                UserHandle.USER_CURRENT);
        mDotOffset = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CIRCLE_DOT_OFFSET, 0,
                UserHandle.USER_CURRENT);
        mCustomHightColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_CUSTOM_TEXT_HIGHT_COLOR, 1,
                UserHandle.USER_CURRENT) == 1;

        int defaultThinRingColor = mContext.getResources().getColor(
                com.android.systemui.R.color.batterymeter_frame_color);
        int defaultColor = mContext.getResources().getColor(
                com.android.systemui.R.color.batterymeter_charge_color);
        int defaultTextChargingColor = Color.GREEN;

        mThinRingColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_FRAME_COLOR,
                defaultThinRingColor, UserHandle.USER_CURRENT);
        mCircleColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_COLOR,
                defaultColor, UserHandle.USER_CURRENT);
        mCircleTextColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR,
                defaultColor, UserHandle.USER_CURRENT);
        mCircleTextHightColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_HIGHT_COLOR,
                defaultTextChargingColor, UserHandle.USER_CURRENT);
        mCircleTextChargingColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR,
                defaultTextChargingColor, UserHandle.USER_CURRENT);
        mCircleAnimSpeed = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3,
                UserHandle.USER_CURRENT);

        mWarningLevel = mContext.getResources().getInteger(
                R.integer.config_lowBatteryWarningLevel);
        mActivated = batteryStyle == BATTERY_STYLE_CIRCLE;
 
        setVisibility(showBatteryStatus && mActivated ? View.VISIBLE : View.GONE);

        /*
         * initialize vars and force redraw
         */
        initializeCircleVars();
        mRectLeft = null;
        mCircleSize = 0;

        if (mBatteryReceiver != null) {
            mBatteryReceiver.updateRegistration();
        }

        if (showBatteryStatus && mActivated && mAttached) {
            invalidate();
        }
    }

    private void initializeCircleVars() {

        // initialize and setup all paint variables
        // stroke width is later set in initSizeBasedStuff()
        Resources res = getResources();

        mPaintFont = new Paint();
        mPaintFont.setAntiAlias(true);
        mPaintFont.setDither(true);
        mPaintFont.setStyle(Paint.Style.STROKE);

        mPaintThinRing = new Paint(mPaintFont);
        mPaintStatus = new Paint(mPaintFont);

        mPaintThinRing.setStrokeCap(Paint.Cap.BUTT);
        mPaintStatus.setStrokeCap(Paint.Cap.BUTT);

        // font needs some extra settings
        mPaintFont.setTextAlign(Align.CENTER);
        mPaintFont.setFakeBoldText(true);

        updateColors();
    }

    private void updateColors() {

        int thinRingColor = mThinRingColor;
        int statusColor = mCircleColor;
        int textColor = mCircleTextColor;

        if (mLevel <= mWarningLevel && !mIsCharging) {
            thinRingColor = Color.RED;
            statusColor = Color.RED;
            textColor = Color.RED;
        } else if (mLevel >= 90 && mCustomHightColor && !mIsCharging) {
            textColor = mCircleTextHightColor;
        } else if (mIsCharging) {
            textColor = mCircleTextChargingColor;
        } else if (!mCustomThinRingColor) {
            thinRingColor = statusColor;
        }

        mPaintThinRing.setColor(thinRingColor);
        mPaintThinRing.setAlpha(51);
        mPaintStatus.setColor(statusColor);
        mPaintFont.setColor(textColor);
    }

    /**
     * updates the animation counter
     * cares for timed callbacks to continue animation cycles
     * uses mInvalidate for delayed invalidate() callbacks
     */
    private void updateChargeAnim() {
        if (!(mIsCharging || mDockIsCharging) || (mLevel >= 97 && mDockLevel >= 97)) {
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

        float strokeWidth = mCircleSize / 7f;
        mPaintStatus.setStrokeWidth(strokeWidth);
        mPaintThinRing.setStrokeWidth(strokeWidth);

        // calculate rectangle for drawArc calls
        int pLeft = getPaddingLeft();
        mRectLeft = new RectF(pLeft + strokeWidth / 2.0f, 0 + strokeWidth / 2.0f, mCircleSize
                - strokeWidth / 2.0f + pLeft, mCircleSize - strokeWidth / 2.0f);
        int off = pLeft + mCircleSize;
        mRectRight = new RectF(mRectLeft.left + off, mRectLeft.top, mRectLeft.right + off,
                mRectLeft.bottom);

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
        Bitmap measure = null;
        if (mCircleBatteryView.equals("quicksettings")) {
            measure = BitmapFactory.decodeResource(getResources(),
                    com.android.systemui.R.drawable.ic_qs_wifi_full_4);
        } else if (mCircleBatteryView.equals("statusbar")) {
            measure = BitmapFactory.decodeResource(getResources(),
                    com.android.systemui.R.drawable.stat_sys_wifi_signal_4_fully);
        }
        if (measure == null) {
            return;
        }
        final int x = measure.getWidth() / 2;

        mCircleSize = measure.getHeight();
    }

    public boolean hasPercent() {
        return mShowText;
    }
}
