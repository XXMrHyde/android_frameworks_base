/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.UserHandle;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.util.darkkat.StatusBarColorHelper;

import com.android.keyguard.CarrierText;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.darkkat.NetworkTrafficController;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.NetworkTraffic;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;

import java.text.NumberFormat;

/**
 * The header group on Keyguard.
 */
public class KeyguardStatusBarView extends RelativeLayout
        implements BatteryController.BatteryStateChangeCallback {

    private boolean mBatteryCharging;
    private boolean mKeyguardUserSwitcherShowing;
    private boolean mListening;

    private View mSystemIconsSuperContainer;
    private NetworkTraffic mNetworkTraffic;
    private MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private TextView mBatteryLevel;

    private BatteryController mBatteryController;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;

    private int mSystemIconsSwitcherHiddenExpandedMargin;
    private Interpolator mFastOutSlowInInterpolator;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        mNetworkTraffic = (NetworkTraffic) findViewById(R.id.keyguard_network_traffic_layout);
        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        loadDimens();
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in);
        updateUserSwitcher();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Respect font size setting.
        ((CarrierText) findViewById(R.id.keyguard_carrier_text)).setTextSize(
                TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(
                        com.android.internal.R.dimen.text_size_small_material));
        mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
    }

    private void loadDimens() {
        mSystemIconsSwitcherHiddenExpandedMargin = getResources().getDimensionPixelSize(
                R.dimen.system_icons_switcher_hidden_expanded_margin);
    }

    private void updateVisibilities() {
        if (mMultiUserSwitch.getParent() != this && !mKeyguardUserSwitcherShowing) {
            if (mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(mMultiUserSwitch);
            }
            addView(mMultiUserSwitch, 0);
        } else if (mMultiUserSwitch.getParent() == this && mKeyguardUserSwitcherShowing) {
            removeView(mMultiUserSwitch);
        }
        updateBatteryLevelVisibility();
    }

    private void updateSystemIconsLayoutParams() {
        RelativeLayout.LayoutParams lp =
                (LayoutParams) mSystemIconsSuperContainer.getLayoutParams();
        int marginEnd = mKeyguardUserSwitcherShowing ? mSystemIconsSwitcherHiddenExpandedMargin : 0;
        if (marginEnd != lp.getMarginEnd()) {
            lp.setMarginEnd(marginEnd);
            mSystemIconsSuperContainer.setLayoutParams(lp);
        }
    }

    public void setNetworkTrafficController(NetworkTrafficController ntc) {
        mNetworkTraffic.setNetworkTrafficController(ntc);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;
        if (mListening) {
            mBatteryController.addStateChangedCallback(this);
        } else {
            mBatteryController.removeStateChangedCallback(this);
        }
        mNetworkTraffic.setListening(mListening);
    }

    private void updateUserSwitcher() {
        boolean keyguardSwitcherAvailable = mKeyguardUserSwitcher != null;
        mMultiUserSwitch.setClickable(keyguardSwitcherAvailable);
        mMultiUserSwitch.setFocusable(keyguardSwitcherAvailable);
        mMultiUserSwitch.setKeyguardMode(keyguardSwitcherAvailable);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        ((BatteryMeterView) findViewById(R.id.battery)).setBatteryController(batteryController);
        ((BatteryBar) findViewById(R.id.battery_bar)).setBatteryController(batteryController);
    }

    public void setUserSwitcherController(UserSwitcherController controller) {
        mMultiUserSwitch.setUserSwitcherController(controller);
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                mMultiUserAvatar.setColorFilter(null);
                mMultiUserAvatar.setImageDrawable(picture);
                UserInfo userInfo = null;
                try {
                    userInfo = ActivityManagerNative.getDefault().getCurrentUser();
                } catch (RemoteException e) {
                    Log.e("KeyguardStatusBarView", "Couldn't get user info", e);
                }
                if (userInfo != null && userInfo.isGuest()) {
                    mMultiUserAvatar.setColorFilter(
                            StatusBarColorHelper.getUserIconColor(getContext()), Mode.SRC_IN);
                }
            }
        });
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        mBatteryLevel.setText(percentage);
        boolean changed = mBatteryCharging != charging;
        mBatteryCharging = charging;
        if (changed) {
            updateVisibilities();
        }
    }

    @Override
    public void onPowerSaveChanged() {
        // could not care less
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        mKeyguardUserSwitcher = keyguardUserSwitcher;
        mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean showing, boolean animate) {
        mKeyguardUserSwitcherShowing = showing;
        if (animate) {
            animateNextLayoutChange();
        }
        updateVisibilities();
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        final int systemIconsCurrentX = mSystemIconsSuperContainer.getLeft();
        final boolean userSwitcherVisible = mMultiUserSwitch.getParent() == this;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                boolean userSwitcherHiding = userSwitcherVisible
                        && mMultiUserSwitch.getParent() != KeyguardStatusBarView.this;
                mSystemIconsSuperContainer.setX(systemIconsCurrentX);
                mSystemIconsSuperContainer.animate()
                        .translationX(0)
                        .setDuration(400)
                        .setStartDelay(userSwitcherHiding ? 300 : 0)
                        .setInterpolator(mFastOutSlowInInterpolator)
                        .start();
                if (userSwitcherHiding) {
                    getOverlay().add(mMultiUserSwitch);
                    mMultiUserSwitch.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setStartDelay(0)
                            .setInterpolator(PhoneStatusBar.ALPHA_OUT)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    mMultiUserSwitch.setAlpha(1f);
                                    getOverlay().remove(mMultiUserSwitch);
                                }
                            })
                            .start();

                } else {
                    mMultiUserSwitch.setAlpha(0f);
                    mMultiUserSwitch.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .setStartDelay(200)
                            .setInterpolator(PhoneStatusBar.ALPHA_IN);
                }
                return true;
            }
        });

    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            mSystemIconsSuperContainer.animate().cancel();
            mMultiUserSwitch.animate().cancel();
            mMultiUserSwitch.setAlpha(1f);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void updateBatteryLevelVisibility() {
        mBatteryLevel.setVisibility(showBattery() && mBatteryCharging && !showBatteryText() ? View.VISIBLE : View.GONE);
    }

    private boolean showBattery() {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_ICON_INDICATOR, 0,
                UserHandle.USER_CURRENT) != 3;
    }

    private boolean showBatteryText() {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_SHOW_TEXT, 0,
                UserHandle.USER_CURRENT) == 1;
    }
}
