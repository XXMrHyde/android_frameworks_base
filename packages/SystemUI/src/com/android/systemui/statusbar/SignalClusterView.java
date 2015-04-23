/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;

import java.util.ArrayList;
import java.util.List;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkControllerImpl.SignalCluster,
        SecurityController.SecurityControllerCallback {

    static final String TAG = "SignalClusterView";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int DEFAULT_COLOR = 0xffffffff;
    private static final int DEFAULT_ACTIVITY_COLOR = 0xff000000;

    NetworkControllerImpl mNC;
    SecurityController mSC;
    private SettingsObserver mObserver;

    Handler mHandler;

    private boolean mNoSimsVisible = false;
    private boolean mVpnVisible = false;
    private boolean mWifiVisible = false;
    private int mInetCondition = 0;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private int mAirplaneContentDescription;
    private String mWifiDescription;
    private ArrayList<PhoneState> mPhoneStates = new ArrayList<PhoneState>();

    ViewGroup mWifiGroup;
    ImageView mVpn, mWifi, mWifiActivity, mAirplane, mNoSims;
    View mWifiAirplaneSpacer;
    View mWifiSignalSpacer;
    LinearLayout mMobileSignalGroup;

    private int mWideTypeIconStartPadding;
    private int mSecondaryTelephonyPadding;
    private int mEndPadding;
    private int mEndPaddingNothingVisible;

    private int mOldNetworkColor;
    private int mNetworkColor;
    private int mOldAirplaneModeColor;
    private int mAirplaneModeColor;
    private Animator mColorTransitionAnimator;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_ICONS_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_ICONS_FULLY_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_ACTIVITY_ICONS_NORMAL_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_ACTIVITY_ICONS_FULLY_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_AIRPLANE_MODE_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateColors(true);
        }
    }


    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ContentResolver resolver = mContext.getContentResolver();

        mHandler = new Handler();
        mObserver = new SettingsObserver(mHandler);
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
        mOldNetworkColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ICONS_NORMAL_COLOR,
                DEFAULT_COLOR, UserHandle.USER_CURRENT);
        mAirplaneModeColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_AIRPLANE_MODE_ICON_COLOR,
                DEFAULT_COLOR, UserHandle.USER_CURRENT);
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    public void setSecurityController(SecurityController sc) {
        if (DEBUG) Log.d(TAG, "SecurityController=" + sc);
        mSC = sc;
        mSC.addCallback(this);
        mVpnVisible = mSC.isVpnEnabled();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWideTypeIconStartPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.wide_type_icon_start_padding);
        mSecondaryTelephonyPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.secondary_telephony_padding);
        mEndPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.signal_cluster_battery_padding);
        mEndPaddingNothingVisible = getContext().getResources().getDimensionPixelSize(
                R.dimen.no_signal_cluster_battery_padding);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mVpn            = (ImageView) findViewById(R.id.vpn);
        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        mNoSims         = (ImageView) findViewById(R.id.no_sims);
        mWifiAirplaneSpacer =         findViewById(R.id.wifi_airplane_spacer);
        mWifiSignalSpacer =           findViewById(R.id.wifi_signal_spacer);
        mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);
        for (PhoneState state : mPhoneStates) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }

        mObserver.observe();
        updateColors(false);
        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mVpn            = null;
        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mAirplane       = null;
        mMobileSignalGroup.removeAllViews();
        mMobileSignalGroup = null;

        super.onDetachedFromWindow();
    }

    // From SecurityController.
    @Override
    public void onStateChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                mVpnVisible = mSC.isVpnEnabled();
                apply();
            }
        });
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int inetCondition,
            int activityIcon, String contentDescription) {
        boolean updateColors = false;
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        if (mInetCondition != inetCondition) {
            mInetCondition = inetCondition;
            updateColors = true;
        }
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;
        if (updateColors) {
            updateColors(false);
        }
        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon, int inetCondition,
            int activityIcon, int typeIcon, String contentDescription, String typeContentDescription,
            boolean isTypeIconWide, int subId) {
        boolean updateColors = false;
        PhoneState state = getOrInflateState(subId);
        state.mMobileVisible = visible;
        state.mMobileStrengthId = strengthIcon;
        if (mInetCondition != inetCondition) {
            mInetCondition = inetCondition;
            updateColors = true;
        }
        state.mMobileActivityId = activityIcon;
        state.mMobileTypeId = typeIcon;
        state.mMobileDescription = contentDescription;
        state.mMobileTypeDescription = typeContentDescription;
        state.mIsMobileTypeIconWide = isTypeIconWide;
        if (updateColors) {
            updateColors(false);
        }
        apply();
    }

    @Override
    public void setNoSims(boolean show) {
        mNoSimsVisible = show;
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        // Clear out all old subIds.
        mPhoneStates.clear();
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.removeAllViews();
        }
        final int n = subs.size();
        for (int i = 0; i < n; i++) {
            inflatePhoneState(subs.get(i).getSubscriptionId());
        }
    }

    private PhoneState getOrInflateState(int subId) {
        for (PhoneState state : mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        return inflatePhoneState(subId);
    }

    private PhoneState inflatePhoneState(int subId) {
        PhoneState state = new PhoneState(subId, mContext);
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }
        mPhoneStates.add(state);
        return state;
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId, int contentDescription) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        mAirplaneContentDescription = contentDescription;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        for (PhoneState state : mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
        }
        if (mWifiActivity != null) {
            mWifiActivity.setImageDrawable(null);
        }

        for (PhoneState state : mPhoneStates) {
            if (state.mMobile != null) {
                state.mMobile.setImageDrawable(null);
            }
            if (state.mMobileActivity != null) {
                state.mMobileActivity.setImageDrawable(null);
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
            }
        }

        if(mAirplane != null) {
            mAirplane.setImageDrawable(null);
        }

        apply();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        mVpn.setVisibility(mVpnVisible ? View.VISIBLE : View.GONE);
        if (DEBUG) Log.d(TAG, String.format("vpn: %s", mVpnVisible ? "VISIBLE" : "GONE"));
        if (mWifiVisible) {
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        boolean anyMobileVisible = false;
        int firstMobileTypeId = 0;
        for (PhoneState state : mPhoneStates) {
            if (state.apply(anyMobileVisible)) {
                if (!anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setContentDescription(mAirplaneContentDescription != 0 ?
                    mContext.getString(mAirplaneContentDescription) : null);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode && mWifiVisible) {
            mWifiAirplaneSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiAirplaneSpacer.setVisibility(View.GONE);
        }

        if (((anyMobileVisible && firstMobileTypeId != 0) || mNoSimsVisible) && mWifiVisible) {
            mWifiSignalSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiSignalSpacer.setVisibility(View.GONE);
        }

        mNoSims.setVisibility(mNoSimsVisible ? View.VISIBLE : View.GONE);

        boolean anythingVisible = mNoSimsVisible || mWifiVisible || mIsAirplaneMode
                || anyMobileVisible || mVpnVisible;
        setPaddingRelative(0, 0, anythingVisible ? mEndPadding : mEndPaddingNothingVisible, 0);
    }

    private void updateColors(boolean forceAnimation) {
        ContentResolver resolver = mContext.getContentResolver();

        int networkNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ICONS_NORMAL_COLOR,
                DEFAULT_COLOR, UserHandle.USER_CURRENT);
        int networkFullyColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ICONS_FULLY_COLOR,
                networkNormalColor, UserHandle.USER_CURRENT);
        int networkActivityNormalColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ACTIVITY_ICONS_NORMAL_COLOR,
                DEFAULT_ACTIVITY_COLOR, UserHandle.USER_CURRENT);
        int networkActivityFullyColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_ACTIVITY_ICONS_FULLY_COLOR,
                networkActivityNormalColor, UserHandle.USER_CURRENT);
        mAirplaneModeColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_AIRPLANE_MODE_ICON_COLOR,
                networkNormalColor, UserHandle.USER_CURRENT);

        mNetworkColor =
                mInetCondition == 0 ? networkNormalColor : networkFullyColor;
        int networkActivityColor =
                mInetCondition == 0 ? networkActivityNormalColor : networkActivityFullyColor;

        if (forceAnimation) {
            mColorTransitionAnimator.start();
        } else {
            if (mWifi != null) {
                mWifi.setColorFilter(mNetworkColor, Mode.MULTIPLY);
            }
            for (PhoneState state : mPhoneStates) {
                if (state.mMobile != null) {
                    state.mMobile.setColorFilter(mNetworkColor, Mode.MULTIPLY);
                }
                if (state.mMobileType != null) {
                    state.mMobileType.setColorFilter(mNetworkColor, Mode.MULTIPLY);
                }
            }
            if(mAirplane != null) {
                mAirplane.setColorFilter(mAirplaneModeColor, Mode.MULTIPLY);
            }
            if(mNoSims != null) {
                mNoSims.setColorFilter(mNetworkColor, Mode.MULTIPLY);
            }
            mOldNetworkColor = mNetworkColor;
            mOldAirplaneModeColor = mAirplaneModeColor;
        }
        if (mWifiActivity != null) {
            mWifiActivity.setColorFilter(networkActivityColor, Mode.MULTIPLY);
        }
        for (PhoneState state : mPhoneStates) {
            if (state.mMobileActivity != null) {
                state.mMobileActivity.setColorFilter(networkActivityColor, Mode.MULTIPLY);
            }
        }
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                int networkColorblended = ColorHelper.getBlendColor(
                        mOldNetworkColor, mNetworkColor, position);
                int airplaneModeColorblended = ColorHelper.getBlendColor(
                        mOldAirplaneModeColor, mAirplaneModeColor, position);

                if (mWifi != null) {
                    mWifi.setColorFilter(networkColorblended, Mode.MULTIPLY);
                }
                for (PhoneState state : mPhoneStates) {
                    if (state.mMobile != null) {
                        state.mMobile.setColorFilter(networkColorblended, Mode.MULTIPLY);
                    }
                    if (state.mMobileType != null) {
                        state.mMobileType.setColorFilter(networkColorblended, Mode.MULTIPLY);
                    }
                }
                if(mAirplane != null) {
                    mAirplane.setColorFilter(airplaneModeColorblended, Mode.MULTIPLY);
                }
                if(mNoSims != null) {
                    mNoSims.setColorFilter(networkColorblended, Mode.MULTIPLY);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOldNetworkColor = mNetworkColor;
                mOldAirplaneModeColor = mAirplaneModeColor;
            }
        });
        return animator;
    }

    private class PhoneState {
        private final int mSubId;
        private boolean mMobileVisible = false;
        private int mMobileStrengthId = 0, mMobileActivityId = 0, mMobileTypeId = 0;
        private boolean mIsMobileTypeIconWide;
        private String mMobileDescription, mMobileTypeDescription;

        private ViewGroup mMobileGroup;
        private ImageView mMobile, mMobileActivity, mMobileType;

        public PhoneState(int subId, Context context) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.mobile_signal_group, null);
            setViews(root);
            mSubId = subId;
        }

        public void setViews(ViewGroup root) {
            mMobileGroup    = root;
            mMobile         = (ImageView) root.findViewById(R.id.mobile_signal);
            mMobileActivity = (ImageView) root.findViewById(R.id.mobile_inout);
            mMobileType     = (ImageView) root.findViewById(R.id.mobile_type);
        }

        public boolean apply(boolean isSecondaryIcon) {
            if (mMobileVisible && !mIsAirplaneMode) {
                mMobile.setImageResource(mMobileStrengthId);
                mMobileActivity.setImageResource(mMobileActivityId);
                mMobileType.setImageResource(mMobileTypeId);
                mMobileGroup.setContentDescription(mMobileTypeDescription
                        + " " + mMobileDescription);
                mMobileGroup.setVisibility(View.VISIBLE);
            } else {
                mMobileGroup.setVisibility(View.GONE);
            }

            // When this isn't next to wifi, give it some extra padding between the signals.
            mMobileGroup.setPaddingRelative(isSecondaryIcon ? mSecondaryTelephonyPadding : 0,
                    0, 0, 0);
            mMobile.setPaddingRelative(mIsMobileTypeIconWide ? mWideTypeIconStartPadding : 0,
                    0, 0, 0);

            if (DEBUG) Log.d(TAG, String.format("mobile: %s sig=%d act=%d typ=%d",
                        (mMobileVisible ? "VISIBLE" : "GONE"), mMobileStrengthId,
                        mMobileActivityId, mMobileTypeId));

            mMobileType.setVisibility(mMobileTypeId != 0 ? View.VISIBLE : View.GONE);
            mMobileActivity.setVisibility(mMobileTypeId != 0 ? View.VISIBLE : View.GONE);

            return mMobileVisible;
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (mMobileVisible && mMobileGroup != null
                    && mMobileGroup.getContentDescription() != null) {
                event.getText().add(mMobileGroup.getContentDescription());
            }
        }
    }
}

