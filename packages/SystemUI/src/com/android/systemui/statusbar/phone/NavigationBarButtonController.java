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

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class NavigationBarButtonController {
    private final Context mContext;
    private final NavigationBarView mNavigationBarView;
    private BackButtonDrawable mBackDrawable, mBackLandDrawable;
    private Drawable mRecentIcon, mRecentLandIcon;
    private Animator mColorTransitionAnimator;
    private int mIconColorOld;

    public NavigationBarButtonController(Context context, NavigationBarView navigationBarView) {
        mContext = context;
        mNavigationBarView = navigationBarView;
        mColorTransitionAnimator = createColorTransitionAnimator(0f, 1f);
        mIconColorOld = getIconColor();
        updateIcons(true);
    }

    public void updateIcons(boolean colorizeBackButton) {
        mRecentIcon = mContext.getResources().getDrawable(R.drawable.ic_sysbar_recent);
        mRecentLandIcon = mRecentIcon;
        if (colorizeBackButton) {
            setBackButtonIconColor(getIconColor());
        }
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                final int blended = ColorHelper.getBlendColor(
                        mIconColorOld, getIconColor(), position);
                setBackButtonIconColor(blended);
                setButtonIconColor((ImageView) mNavigationBarView.getHomeButton(), blended);
                setButtonIconColor((ImageView) mNavigationBarView.getRecentsButton(), blended);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIconColorOld = getIconColor();
            }
        });
        return animator;
    }

    public void updateColors(boolean animate) {
        updateIconColor(animate);
        updateRippleColor();
    }

    private void updateIconColor(boolean animate) {
        if (animate) {
            mColorTransitionAnimator.start();
            setButtonIconColor((ImageView) mNavigationBarView.getMenuButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButton(), getIconColor());
        } else {
            setBackButtonIconColor(getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getHomeButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getRecentsButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getMenuButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButton(), getIconColor());
            mIconColorOld = getIconColor();
        }

        LinearLayout lightsOut =
                (LinearLayout) mNavigationBarView.getCurrentView().findViewById(R.id.lights_out);
        for (int index = 0; index < lightsOut.getChildCount(); index++) {
            if (lightsOut.getChildAt(index) instanceof ImageView) {
                setButtonIconColor((ImageView) lightsOut.getChildAt(index), getIconColor());
            }
        }
    }

    private void updateRippleColor() {
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getBackButton(), getRippleColor());
        setButtonRippleColor(mNavigationBarView.getHomeButton(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getRecentsButton(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getMenuButton(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getImeSwitchButton(), getRippleColor());

    }

    private void setButtonIconColor(ImageView button, int iconColor) {
        button.setColorFilter(iconColor, Mode.MULTIPLY);
    }

    private void setBackButtonIconColor(int color) {
        final Drawable backIcon =
                mContext.getResources().getDrawable(R.drawable.ic_sysbar_back).mutate();
        final Drawable backLandIcon =
                mContext.getResources().getDrawable(R.drawable.ic_sysbar_back).mutate();
        backIcon.setColorFilter(color, Mode.MULTIPLY);
        backLandIcon.setColorFilter(color, Mode.MULTIPLY);
        mBackDrawable = new BackButtonDrawable(backIcon);
        mBackLandDrawable = new BackButtonDrawable(backLandIcon);
        if (mNavigationBarView.getCurrentView() != null) {
            ((ImageView) mNavigationBarView.getBackButton()).setImageDrawable(
                    mNavigationBarView.isVertical() ? mBackLandDrawable : mBackDrawable);
        }
    }

    private void setButtonRippleColor(KeyButtonView button, int rippleColor) {
        button.setRippleColor(rippleColor);
    }

    private int getIconColor() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_ICON_COLOR, 0xffffffff);
    }

    private int getRippleColor() {
        return Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.NAVIGATION_BAR_BUTTON_RIPPLE_COLOR, 0xffffffff);
    }

    public void setImeVisible(boolean visible) {
        mBackDrawable.setImeVisible(visible);
        mBackLandDrawable.setImeVisible(visible);
    }
}
