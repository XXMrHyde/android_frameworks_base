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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.systemui.R;

public class BatteryMeterView extends View implements DemoMode {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    public static final boolean ENABLE_PERCENT = true;
    public static final boolean SINGLE_DIGIT_PERCENT = false;
    public boolean SHOW_100_PERCENT = false;

    public static final int BATTERY_STYLE_NORMAL                = 0;
    public static final int BATTERY_STYLE_PERCENT               = 1;

    public static final int FULL = 96;
    public static final int EMPTY = 4;

    public static final float SUBPIXEL = 0.4f;  // inset rects for softer edges

    int[] mColors;

    private Handler mHandler;

    private boolean mAttached;
    private boolean mActivated;
    private boolean mShowIcon = true;
    private boolean mShowMiniIcon;
    public boolean mShowPercent = true;
    private boolean mShowPercentSign = true;
    Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextFramePaint, mTextPaint, mBoltPaint;
    private int mButtonHeight;
    private int mCurrentBatteryYTop;
    private float mTextHeight, mWarningTextHeight;
    private float mChargingTextHeight;
    private int mCurrentTextYTop;

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private final float[] mBoltPoints;
    private final Path mBoltPath = new Path();

    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mClipFrame = new RectF();
    private final RectF mBoltFrame = new RectF();

    private int mBatteryStyle;
    private int mAnimSpeed;
    private int mBatteryColor;
    private boolean mCustomFrameColor;
    private boolean mCustomHightColor;
    private int mFrameColor;
    private int mBatteryTextColor;
    private int mBatteryTextHightColor;
    private int mBatteryTextChargingColor;
    private boolean mTextOnly = false;
    private String mBatteryTypeView;
    private int mWarningLevel;
    private boolean mIsCharging;

    private class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
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
                mIsCharging = plugged;
                setContentDescription(
                        context.getString(R.string.accessibility_battery_level, level));
                updateSettings();

                if (mActivated && mAttached) {
                    postInvalidate();

                    if (mIsCharging) {
                        startChargingAnimation();
                    } else {
                        stopChargingAnimation();
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
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC : 0);
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
    }

    BatteryTracker mTracker = new BatteryTracker();

    // runnable to invalidate view via mHandler.postDelayed() call
    private final Runnable mPostInvalidate = new Runnable() {
        public void run() {
            if(mActivated && mAttached) {
                postInvalidate();
            }
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
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        TypedArray batteryType = context.obtainStyledAttributes(attrs,
            com.android.systemui.R.styleable.BatteryIcon, 0, 0);
        mBatteryTypeView = batteryType.getString(
            com.android.systemui.R.styleable.BatteryIcon_batteryView);

        if (mBatteryTypeView == null) {
            mBatteryTypeView = "statusbar";
        }

        mHandler = new Handler();

        final int N = levels.length();
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = levels.getInt(i, 0);
            mColors[2*i+1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();

        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setDither(true);
        mFramePaint.setStrokeWidth(0);
        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStrokeWidth(0);
        mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mTextFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextFramePaint.setTextAlign(Paint.Align.CENTER);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWarningTextPaint.setColor(mColors[1]);
        Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
        mWarningTextPaint.setTypeface(font);
        mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

        mBoltPaint = new Paint();
        mBoltPaint.setAntiAlias(true);
        mBoltPoints = loadBoltPoints(res);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        updateSettings();
    }

    private static float[] loadBoltPoints(Resources res) {
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

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(ACTION_LEVEL_TEST);
            final Intent sticky = getContext().registerReceiver(mTracker, filter);
            if (sticky != null) {
                // preload the battery level
                mTracker.onReceive(getContext(), sticky);
            }
            mHandler.postDelayed(mPostInvalidate, 250);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            stopChargingAnimation();
            mAttached = false;
            getContext().unregisterReceiver(mTracker);
        }
    }

    private void updateChargeAnim() {
        if (!mTextOnly) {
            mCurrentBatteryYTop += mAnimSpeed;
            if (mCurrentBatteryYTop > 100) {
                mCurrentBatteryYTop = 0;
            }
            mCurrentTextYTop = 0;
        } else {
            mCurrentTextYTop += mAnimSpeed;
            if (mCurrentTextYTop > 100) {
                mCurrentTextYTop = 0;
            }
            mCurrentBatteryYTop = 0;
        }
        mHandler.removeCallbacks(mPostInvalidate);
        mHandler.postDelayed(mPostInvalidate, 50);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        mWarningTextPaint.setTextSize(h * 0.75f);
        mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
    }

    private int getColorForLevel(int percent) {
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) return color;
        }
        return color;
    }

    @Override
    public void draw(Canvas c) {
        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        final int level = tracker.level;

        if (level == BatteryTracker.UNKNOWN_LEVEL) return;

        final int pt = getPaddingTop();
        final int pl = getPaddingLeft();
        final int pr = getPaddingRight();
        final int pb = getPaddingBottom();
        int height = mHeight - pt - pb;
        int width = mWidth - pl - pr;

        mButtonHeight = (int) (height * 0.12f);
        if (mShowMiniIcon && mShowPercent) {
            mFrame.set(0, 0, (width / 3), height);
            mFrame.offset(pl + ((width / 3) * 2), pt);
        } else {
            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);
        }
        mButtonFrame.set(
                mFrame.left + width * 0.25f,
                mFrame.top,
                mFrame.right - width * 0.25f,
                mFrame.top + mButtonHeight + 5 /*cover frame border of intersecting area*/);

        mButtonFrame.top += SUBPIXEL;
        mButtonFrame.left += SUBPIXEL;
        mButtonFrame.right -= SUBPIXEL;

        mFrame.top += mButtonHeight;
        mFrame.left += SUBPIXEL;
        mFrame.top += SUBPIXEL;
        mFrame.right -= SUBPIXEL;
        mFrame.bottom -= SUBPIXEL;

        if (mShowIcon) {
            // first, draw the battery shape
            c.drawRect(mFrame, mFramePaint);
            float drawFrac;
            if (mIsCharging && mAnimSpeed != 0) {
                drawFrac = (float) mCurrentBatteryYTop / 100f;
                boolean batteryFull;

                if (mCurrentBatteryYTop >= FULL) {
                    batteryFull = true;
                } else {
                    batteryFull = false;
                }
                c.drawRect(mButtonFrame, batteryFull ? mBatteryPaint : mFramePaint);
            } else {
                mCurrentBatteryYTop = 0;
                drawFrac = (float) level / 100f;

                if (level >= FULL) {
                    drawFrac = 1f;
                } else if (level <= EMPTY) {
                    drawFrac = 0f;
                }
                c.drawRect(mButtonFrame, drawFrac == 1f ? mBatteryPaint : mFramePaint);
            }

            mClipFrame.set(mFrame);
            mClipFrame.top += (mFrame.height() * (1f - drawFrac));

            c.save(Canvas.CLIP_SAVE_FLAG);
            c.clipRect(mClipFrame);
            c.drawRect(mFrame, mBatteryPaint);
            c.restore();
            if (mIsCharging && mAnimSpeed != 0) {
                updateChargeAnim();
            }
        }

        if (tracker.plugged && (mAnimSpeed == 0 || mShowMiniIcon) && !mTextOnly) {
            // draw the bolt
            final float bl = mFrame.left + mFrame.width() / 4.5f;
            final float bt = mFrame.top + mFrame.height() / 6f;
            final float br = mFrame.right - mFrame.width() / 7f;
            final float bb = mFrame.bottom - mFrame.height() / 10f;
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
            c.drawPath(mBoltPath, mBoltPaint);
        } else if (level <= EMPTY && mBatteryStyle == BATTERY_STYLE_NORMAL) {
            final float x = mWidth * 0.5f;
            final float y = (mHeight + mWarningTextHeight) * 0.48f;
            c.drawText(mWarningString, x, y, mWarningTextPaint);
        }
        if (mShowPercent && !(tracker.plugged && mAnimSpeed == 0 && !mShowMiniIcon)) {
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            if (mBatteryStyle == BATTERY_STYLE_NORMAL) {
                if(mShowMiniIcon) {
                    mTextFramePaint.setTextSize(height *
                            (SINGLE_DIGIT_PERCENT ? 0.95f
                                    : (tracker.level == 100 ? 0.58f : 0.7f)));
                    mTextPaint.setTextSize(height *
                            (SINGLE_DIGIT_PERCENT ? 0.95f
                                    : (tracker.level == 100 ? 0.58f : 0.7f)));
                } else {
                    mTextPaint.setTextSize(height *
                            (SINGLE_DIGIT_PERCENT ? 0.75f
                                    : (tracker.level == 100 ? 0.38f : 0.5f)));
                }
            } else if (mTextOnly) {
                if (mBatteryTypeView.equals("statusbar")) {
                    mTextFramePaint.setTextSize((int) (metrics.density * 16f));
                    mTextPaint.setTextSize((int) (metrics.density * 16f));
                } else if (mBatteryTypeView.equals("quicksettings")) {
                    mTextFramePaint.setTextSize((int) (metrics.density * 22f + 0.5f));
                    mTextPaint.setTextSize((int) (metrics.density * 22f + 0.5f));
                }
            }
            mTextHeight = -mTextPaint.getFontMetrics().ascent;
            String str;
            if ((mTextOnly && mShowPercentSign) || (mTextOnly && mBatteryTypeView.equals("quicksettings"))) {
                str = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level) + "%";
            } else {
                str = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
            }
            final float x;
            if (mShowMiniIcon && !mTextOnly && mShowPercent) {
              if (mBatteryTypeView.equals("statusbar")) {
                x = (mWidth * 0.5f) - (mWidth * 0.25f);
              } else if (mBatteryTypeView.equals("quicksettings")) {
                x = (mWidth * 0.5f) - (mWidth * 0.2f);
              } else {
                x = mWidth * 0.5f;
              }
            } else {
              x = mWidth * 0.5f;
            }
            final float y = (mHeight + mTextHeight) * 0.47f;
            mChargingTextHeight = (mTextPaint.descent() - mTextPaint.ascent())  + 0.5f;
            if (mIsCharging && mTextOnly && mAnimSpeed != 0) {
                float drawFrac = (float) mCurrentTextYTop / 100f;
                c.drawText(str, x, y, mTextFramePaint);

                c.clipRect(new RectF(0.0f, (mChargingTextHeight * (1f - drawFrac)),
                        (float) mWidth, mChargingTextHeight), Region.Op.REPLACE);
                c.drawText(str, x, y, mTextPaint);

                updateChargeAnim();
            } else {
                mCurrentTextYTop = 0;
                c.drawText(str, x, y, mTextPaint);
            }
        }
    }

    private boolean mDemoMode;
    private BatteryTracker mDemoTracker = new BatteryTracker();

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
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

    public void updateSettings() {
        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        final int level = tracker.level;
        ContentResolver resolver = mContext.getContentResolver();

        boolean showBatteryStatus = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_STATUS, 1,
                UserHandle.USER_CURRENT) == 1;
        mBatteryStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, 2,
                UserHandle.USER_CURRENT);
        mShowMiniIcon = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_MINI_ICON, 0,
                UserHandle.USER_CURRENT) == 1;
        mShowPercent = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT, 1,
                UserHandle.USER_CURRENT) == 1;
        mShowPercentSign = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_SHOW_PERCENTAGE_SIGN, 1,
                UserHandle.USER_CURRENT) == 1;
        mAnimSpeed = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_ANIMATION_SPEED, 3,
                UserHandle.USER_CURRENT);
        mCustomFrameColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_CUSTOM_FRAME_COLOR, 0,
                UserHandle.USER_CURRENT) == 1;
        mCustomHightColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_CUSTOM_TEXT_HIGHT_COLOR, 1,
                UserHandle.USER_CURRENT) == 1;

        int defaultFrameColor = mContext.getResources().getColor(
                R.color.batterymeter_frame_color);
        int defaultTextColor = mContext.getResources().getColor(
                R.color.batterymeter_charge_color);
        int defaultChargingColor = Color.GREEN;

        mFrameColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_FRAME_COLOR,
                defaultFrameColor, UserHandle.USER_CURRENT);
        mBatteryColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_COLOR,
                defaultTextColor, UserHandle.USER_CURRENT);
        mBatteryTextColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR,
                defaultTextColor, UserHandle.USER_CURRENT);
        mBatteryTextHightColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_HIGHT_COLOR,
                defaultChargingColor, UserHandle.USER_CURRENT);
        mBatteryTextChargingColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR,
                defaultChargingColor, UserHandle.USER_CURRENT);

        mWarningLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);

        mActivated = (mBatteryStyle == BATTERY_STYLE_NORMAL ||
                      mBatteryStyle == BATTERY_STYLE_PERCENT);

        setVisibility(showBatteryStatus && mActivated ? View.VISIBLE : View.GONE);

        if (mActivated) {
            LinearLayout.LayoutParams lp = null;
            float width = 0f;
            float height = 0f;
            Resources res = mContext.getResources();
            DisplayMetrics metrics = res.getDisplayMetrics();
            if (mBatteryTypeView.equals("statusbar")) {
                height = metrics.density * 16f + 0.5f;
                if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                    if (level < 10) {
                        width = metrics.density * (mShowPercentSign ? 22f : 10f)
                                + 0.5f;
                    } else if (level >= 10 && level <= 99) {
                        width = metrics.density * (mShowPercentSign ? 30f : 18f)
                                + 0.5f;
                    } else {
                        width = metrics.density * (mShowPercentSign ? 38f : 26f)
                                + 0.5f;
                    }
                } else {
                    if (mShowMiniIcon) {
                        width = metrics.density * (mShowPercent ? 22f : 7f) + 0.5f;
                    } else {
                        width = metrics.density * 10.5f + 0.5f;
                    }
                }
                lp = new LinearLayout.LayoutParams((int) width, (int) height);
                lp.setMarginStart((int) (metrics.density * 6f + 0.5f));
                lp.setMargins(0, 0, 0, (int) (metrics.density * 0.5f + 0.5f));
                setLayoutParams(lp);
            } else if (mBatteryTypeView.equals("quicksettings")) {
                height = metrics.density * 32f + 0.5f;
                if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                    width = metrics.density * 52f + 0.5f;
                } else {
                    if (mShowMiniIcon) {
                        width = metrics.density * (mShowPercent ? 37f : 16f) + 0.5f;
                    } else {
                        width = metrics.density * 22f + 0.5f;
                    }
                }
                lp = new LinearLayout.LayoutParams((int) width, (int) height);
                lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                setLayoutParams(lp);
            }

            updateBattery();
        }
    }

    private void updateBattery() {
        mTextOnly = false;
        SHOW_100_PERCENT = false;
        Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
        if (mBatteryStyle == BATTERY_STYLE_NORMAL) {
            mShowIcon = true;
            SHOW_100_PERCENT = mShowPercent ? true : false;
            if (mShowMiniIcon) {
                font = Typeface.create("sans-serif", Typeface.NORMAL);
            }
        } else if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
            mShowIcon = false;
            mShowPercent = true;
            mTextOnly = true;
            SHOW_100_PERCENT = true;
            font = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        mTextFramePaint.setTypeface(font);
        mTextPaint.setTypeface(font);

        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;

        if (tracker.level <= mWarningLevel && !tracker.plugged) {
            mBatteryPaint.setColor(Color.RED);
            mFramePaint.setColor(Color.RED);
            mFramePaint.setAlpha(51);
            mTextPaint.setColor(Color.RED);
        } else {
            mBatteryPaint.setColor(mBatteryColor);
            if (!mCustomFrameColor) {
                mFramePaint.setColor(mBatteryPaint.getColor());
                mFramePaint.setAlpha(51);
            } else {
                mFramePaint.setColor(mFrameColor);
            }

            if (tracker.level >= 90) {
                if (mCustomHightColor) {
                    mTextPaint.setColor(mBatteryTextHightColor);
                } else {
                    mTextPaint.setColor(Color.GREEN);
                }
            } else {
                mTextPaint.setColor(mBatteryTextColor);
            }
            if (tracker.plugged) {
                mBoltPaint.setColor(mBatteryTextChargingColor);
                mTextPaint.setColor(mBatteryTextChargingColor);
                mTextFramePaint.setColor(mBatteryTextChargingColor);
                mTextFramePaint.setAlpha(51);
            }
        }

        postInvalidate();
    }

    private void startChargingAnimation() {
        if (!mIsCharging) {
            mIsCharging = true;
            mHandler.removeCallbacks(mPostInvalidate);
            mCurrentBatteryYTop = 0;
            mCurrentTextYTop = 0;
            updateChargeAnim();
        }
    }

    private void stopChargingAnimation() {
        if (mIsCharging) {
            mIsCharging = false;
            mHandler.removeCallbacks(mPostInvalidate);
            mCurrentBatteryYTop = 0;
            mCurrentTextYTop = 0;
            mHandler.post(mPostInvalidate);
        }
    }

    public boolean hasPercent() {
        return mShowPercent;
    }
}
