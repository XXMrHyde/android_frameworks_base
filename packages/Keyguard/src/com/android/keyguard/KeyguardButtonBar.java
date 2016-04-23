/*
 * Copyright (C) 2014 Slimroms
 *
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
package com.android.keyguard;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import com.android.internal.util.darkkat.ImageHelper;
import com.android.internal.util.darkkat.LockScreenButtonBarHelper;
import com.android.internal.util.darkkat.LockScreenColorHelper;
import com.android.internal.util.slim.AppHelper;
import com.android.internal.util.slim.ActionConfig;
import com.android.internal.util.slim.Action;
import com.android.internal.widget.LockPatternUtils;

import com.android.keyguard.R;

import java.util.ArrayList;

public class KeyguardButtonBar extends LinearLayout {

    private Handler mHandler = new Handler();
    private LockPatternUtils mLockPatternUtils;
    private SettingsObserver mSettingsObserver;
    private PackageManager mPackageManager;
    private Context mContext;
    private boolean mEnabled = false;
    private boolean mHideBar;
    private boolean mForceHideBar = false;
    private boolean mIsOwnerInfoVisible = false;
    private boolean mDozing = false;

    public KeyguardButtonBar(Context context) {
        this(context, null);
    }

    public KeyguardButtonBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mSettingsObserver = new SettingsObserver(mHandler);
        mHideBar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTON_BAR_HIDE_BAR, 1) == 1;
        createBarButtons();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public void onAttachedToWindow() {
        mSettingsObserver.observe();

    }

    @Override
    public void onDetachedFromWindow() {
        mSettingsObserver.unobserve();
    }

    private void createBarButtons() {
        ArrayList<ActionConfig> actionConfigs =
                LockScreenButtonBarHelper.getButtonBarConfig(mContext);
        if (actionConfigs.size() == 0) {
            mEnabled = false;
            updateVisibility();
            return;
        }

        mEnabled = true;
        updateVisibility();

        ContentResolver resolver = mContext.getContentResolver();

        int launchType = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_BUTTON_BAR_LAUNCH_TYPE, 2);

        ActionConfig actionConfig;
        for (int i = 0; i < actionConfigs.size(); i++) {
            actionConfig = actionConfigs.get(i);

            final String action = actionConfig.getClickAction();
            ImageView iv = new ImageView(mContext);
            int dimens = Math.round(mContext.getResources().getDimensionPixelSize(
                    com.android.internal.R.dimen.app_icon_size));
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(dimens, dimens);
            iv.setLayoutParams(lp);

            Drawable d = LockScreenButtonBarHelper.getButtonBarIconImage(mContext, action, actionConfig.getIcon());
            iv.setImageDrawable(d);
            setRippleColor(iv, d);
            iv.setContentDescription(AppHelper.getFriendlyNameForUri(
                    mContext, mPackageManager, actionConfig.getClickAction()));
            iv.setClickable(true);

            if (launchType == 0) {
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                        Action.processAction(mContext, action, false);
                    }
                });
            } else if (launchType == 1) {
                final GestureDetector gestureDetector = new GestureDetector(mContext,
                        new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                        Action.processAction(mContext, action, false);
                        return true;
                    }
                });
                iv.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    }
                });
            } else {
                iv.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                        Action.processAction(mContext, action, true);
                        return true;
                    }
                });
            }

            addView(iv);
            if (i+1 < actionConfigs.size()) {
                addView(new Space(mContext), new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
        }
    }

    private void updateVisibility() {
        if (!mEnabled || (mHideBar && mForceHideBar)) {
            setVisibility(View.GONE);
        } else {
            setVisibility(mDozing && !showOnAmbientDisplay() ? View.GONE : View.VISIBLE);
        }
    }

    private void setRippleColor(ImageView iv, Drawable d) {
        RippleDrawable rd = (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable).mutate();
        // the default alpha for ripple is almost invisible when it is not translucent white,
        // so use an alpha value of 128 instead
        final int rippleColor = LockScreenColorHelper.getRippleColor(mContext, 128);
        final boolean colorizeRipple = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTON_BAR_COLORIZE_RIPPLE, 0) == 1;
        int colorToUse = rippleColor;

        if (!colorizeRipple) {
            if (d != null) {
                Palette palette = Palette.generate(ImageHelper.drawableToBitmap(d));
                final int paletteColor = palette.getDarkVibrantColor(0x80ffffff);
                colorToUse = (128 << 24) | (paletteColor & 0x00ffffff);
            }
        }

        rd.setColor(ColorStateList.valueOf(colorToUse));
        iv.setBackground(rd);
    }

    private void setBackgroundColor() {
        final int backgroundColor = LockScreenColorHelper.getBackgroundColor(mContext);
        final boolean showBackground = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTON_BAR_SHOW_BACKGROUND, 1) == 1;
        ((Drawable) getBackground()).setTint(backgroundColor);
        ((Drawable) getBackground()).setAlpha(showBackground && !mDozing ? 255 : 0);
    }

    public void setHideBar() {
        mHideBar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_BUTTON_BAR_HIDE_BAR, 1) == 1;
        updateVisibility();
    }

    public void setNotificationCountAndOverflowVisibility(int maxNotificationCount,
            int currentNotificationCount, boolean isOverflowVisible) {
        if (mIsOwnerInfoVisible) {
            mForceHideBar = currentNotificationCount >= maxNotificationCount;
        } else {
            mForceHideBar = isOverflowVisible;
        }
        updateVisibility();
    }

    public void setOwnerInfoVisibility(boolean visible) {
        mIsOwnerInfoVisible = visible;
        updateVisibility();
    }

    public void setDozing(boolean dozing) {
        mDozing = dozing;
        setBackgroundColor();
        updateVisibility();
    }

    private boolean showOnAmbientDisplay() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AMBIENT_DISPLAY_SHOW_BUTTON_BAR, 0) == 1;
    }

    public void doHapticKeyClick(int type) {
        if (mLockPatternUtils.isTactileFeedbackEnabled()) {
            performHapticFeedback(type,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_BUTTONS),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_LAUNCH_TYPE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_SHOW_BACKGROUND),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_HIDE_BAR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_COLORIZE_RIPPLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BACKGROUND_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_RIPPLE_COLOR),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_SHOW_BACKGROUND))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BACKGROUND_COLOR))) {
                setBackgroundColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_HIDE_BAR))) {
                setHideBar();
            } else {
                removeAllViews();
                createBarButtons();
            }

        }

        public void update() {
            removeAllViews();
            createBarButtons();
            setBackgroundColor();
        }
    }
}
