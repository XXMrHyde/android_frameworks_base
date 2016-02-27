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
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class NavigationBarButtonController {
    private static final int MENU_BUTTON_VISIBILITY_ON_REQUEST = 0;
    private static final int MENU_BUTTON_VISIBILITY_VISIBLE    = 1;
    private static final int MENU_BUTTON_VISIBILITY_HIDDEN     = 2;

    private static final int MENU_IME_BUTTON_POSITION_RIGHT = 0;
    private static final int MENU_IME_BUTTON_POSITION_LEFT  = 1;

    private final Context mContext;
    private final NavigationBarView mNavigationBarView;
    private BackButtonDrawable mBackDrawable, mBackLandDrawable;
    private Drawable mRecentIcon, mRecentLandIcon;
    private Animator mColorTransitionAnimator;

    private int mMenuButtonVisibility;
    private int mMenuButtonPosition;
    private int mImeButtonPosition;

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
                if (animateMenuLeftColorTransition()) {
                    setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonLeft(), blended);
                }
                setBackButtonIconColor(blended);
                setButtonIconColor((ImageView) mNavigationBarView.getHomeButton(), blended);
                setButtonIconColor((ImageView) mNavigationBarView.getRecentsButton(), blended);
                if (animateMenuRightColorTransition()) {
                    setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonRight(), blended);
                }
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

    public void updateMenuButtonVisibility() {
        updateMenuButtonVisibility(false, false);
    }

    public void updateMenuButtonVisibility(boolean isRequestShow, boolean hide) {
        mMenuButtonVisibility = getMenuButtonVisibility();

        if (hide && mMenuButtonPosition == mImeButtonPosition) {
            if (mNavigationBarView.getMenuButtonLeft().getVisibility() != View.INVISIBLE) {
                mNavigationBarView.getMenuButtonLeft().setVisibility(View.INVISIBLE);
            }
            if (mNavigationBarView.getMenuButtonRight().getVisibility() != View.INVISIBLE) {
                mNavigationBarView.getMenuButtonRight().setVisibility(View.INVISIBLE);
            }
        } else {
            if (mMenuButtonVisibility == MENU_BUTTON_VISIBILITY_ON_REQUEST && isRequestShow) {
                mNavigationBarView.getMenuButtonLeft().setVisibility(mMenuButtonPosition
                        == MENU_IME_BUTTON_POSITION_LEFT ? View.VISIBLE : View.INVISIBLE);
                mNavigationBarView.getMenuButtonRight().setVisibility(mMenuButtonPosition
                        == MENU_IME_BUTTON_POSITION_RIGHT ? View.VISIBLE : View.INVISIBLE);
            } else if (mMenuButtonVisibility == MENU_BUTTON_VISIBILITY_VISIBLE) {
                mNavigationBarView.getMenuButtonLeft().setVisibility(mMenuButtonPosition
                        == MENU_IME_BUTTON_POSITION_LEFT ? View.VISIBLE : View.INVISIBLE);
                mNavigationBarView.getMenuButtonRight().setVisibility(mMenuButtonPosition
                        == MENU_IME_BUTTON_POSITION_RIGHT ? View.VISIBLE : View.INVISIBLE);
            } else {
                if (mNavigationBarView.getMenuButtonLeft().getVisibility() != View.INVISIBLE) {
                    mNavigationBarView.getMenuButtonLeft().setVisibility(View.INVISIBLE);
                }
                if (mNavigationBarView.getMenuButtonRight().getVisibility() != View.INVISIBLE) {
                    mNavigationBarView.getMenuButtonRight().setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public void updateMenuButtonPosition() {
        mMenuButtonPosition = getMenuButtonPosition();
        updateMenuButtonVisibility();
    }

    public void updateImeButtonPosition() {
        mImeButtonPosition = getImeButtonPosition();
    }

    public void setShowImeButton(boolean show) {
        if (mImeButtonPosition == MENU_IME_BUTTON_POSITION_LEFT) {
            mNavigationBarView.getImeSwitchButtonLeft().setVisibility(
                    show ? View.VISIBLE : View.INVISIBLE);
            mNavigationBarView.getImeSwitchButtonRight().setVisibility(View.INVISIBLE);
        } else {
            mNavigationBarView.getImeSwitchButtonRight().setVisibility(
                    show ? View.VISIBLE : View.INVISIBLE);
            mNavigationBarView.getImeSwitchButtonLeft().setVisibility(View.INVISIBLE);
        }
    }

    public void updateColors(boolean animate) {
        updateIconColor(animate);
        updateRippleColor();
    }

    public void setImeVisible(boolean visible) {
        mBackDrawable.setImeVisible(visible);
        mBackLandDrawable.setImeVisible(visible);
    }

    private void updateIconColor(boolean animate) {
        if (animate) {
            mColorTransitionAnimator.start();
            if (!animateMenuLeftColorTransition()) {
                setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonLeft(), getIconColor());
            }
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButtonLeft(), getIconColor());
            if (!animateMenuRightColorTransition()) {
                setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonRight(), getIconColor());
            }
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButtonRight(), getIconColor());
        } else {
            setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonLeft(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButtonLeft(), getIconColor());
            setBackButtonIconColor(getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getHomeButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getRecentsButton(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getMenuButtonRight(), getIconColor());
            setButtonIconColor((ImageView) mNavigationBarView.getImeSwitchButtonRight(), getIconColor());
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
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getMenuButtonLeft(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getImeSwitchButtonLeft(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getBackButton(), getRippleColor());
        setButtonRippleColor(mNavigationBarView.getHomeButton(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getRecentsButton(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getMenuButtonRight(), getRippleColor());
        setButtonRippleColor((KeyButtonView) mNavigationBarView.getImeSwitchButtonRight(), getRippleColor());

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

    private int getMenuButtonVisibility() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_MENU_BUTTON_VISIBILITY,
                MENU_BUTTON_VISIBILITY_ON_REQUEST);
    }

    private int getMenuButtonPosition() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_MENU_BUTTON_POSITION,
                MENU_IME_BUTTON_POSITION_RIGHT);
    }

    private int getImeButtonPosition() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_IME_BUTTON_POSITION,
                MENU_IME_BUTTON_POSITION_RIGHT);
    }

    private int getIconColor() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_ICON_COLOR, 0xffffffff);
    }

    private int getRippleColor() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_RIPPLE_COLOR, 0xffffffff);
    }

    private boolean animateMenuLeftColorTransition() {
        return mMenuButtonVisibility == MENU_BUTTON_VISIBILITY_VISIBLE
                && mMenuButtonPosition == MENU_IME_BUTTON_POSITION_LEFT;
    }

    private boolean animateMenuRightColorTransition() {
        return mMenuButtonVisibility == MENU_BUTTON_VISIBILITY_VISIBLE
                && mMenuButtonPosition == MENU_IME_BUTTON_POSITION_RIGHT;
    }
}
