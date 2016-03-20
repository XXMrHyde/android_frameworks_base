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

package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.darkkat.NetworkTrafficController;
import com.android.systemui.darkkat.NetworkTrafficController.Traffic;
import com.android.systemui.R;

public class NetworkTraffic extends LinearLayout implements
        NetworkTrafficController.Callback {

    private static final int TRAFFIC_IN     = 0;
    private static final int TRAFFIC_OUT    = 1;
    private static final int TRAFFIC_IN_OUT = 2;

    private static final int TRAFFIC_TYPE_TEXT      = 0;
    private static final int TRAFFIC_TYPE_ICON      = 1;
    private static final int TRAFFIC_TYPE_TEXT_ICON = 2;

    private final Resources mResources;
    private final ContentResolver mResolver;
    private final boolean mIsOnKeyguard;

    private NetworkTrafficController mNetworkTrafficController;

    private TextView mTextView;
    private ImageView mIconView;

    private boolean mAttached = false;
    private boolean mListening = false;

    private boolean mShowTraffic;
    private boolean mShowActivityIn;
    private boolean mShowActivityOut;
    private boolean mShowText;
    private boolean mShowIcon;
    private boolean mIsBit;

    private boolean mHide;
    private int mThreshold;
    private boolean mIconAsIndicator;
    private long mOutSpeed = 0;
    private long mInSpeed = 0;

    private final int mTxtSizeSingle;
    private final int mTxtSizeDual;

    public NetworkTraffic(Context context) {
        this(context, null);
    }

    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mResources = getResources();
        mResolver = context.getContentResolver();

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.NetworkTraffic,
                defStyle, 0);
        mIsOnKeyguard = atts.getBoolean(R.styleable.NetworkTraffic_isOnKeyguard, false);
        atts.recycle();

        mTxtSizeSingle = mResources.getDimensionPixelSize(R.dimen.network_traffic_single_text_size);
        mTxtSizeDual = mResources.getDimensionPixelSize(R.dimen.network_traffic_dual_text_size);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTextView = (TextView) findViewById(R.id.network_traffic_text);
        mIconView = (ImageView) findViewById(R.id.network_traffic_icon);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttached = true;
        if (!mIsOnKeyguard) {
            setListening(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached = false;
        if (!mIsOnKeyguard) {
            setListening(false);
        }
    }

    public void setListening(boolean listening) {
        mListening = listening;

        if (isTrafficEnabled()) {
            mNetworkTrafficController.addCallback(this);
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        } else if (mNetworkTrafficController != null) {
            mNetworkTrafficController.removeCallback(this);
        }
    }

    public void setNetworkTrafficController(NetworkTrafficController ntc) {
        mNetworkTrafficController = ntc;

        if (isTrafficEnabled()) {
            mNetworkTrafficController.addCallback(this);
        }
    }

    @Override
    public void onNetworkTrafficChanged(Traffic traffic) {
        updateTraffic(traffic);
    }

    public void setShow(boolean show) {
        mShowTraffic = show;

        if (mShowTraffic && getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        } else if (!mShowTraffic && getVisibility() != View.GONE) {
            setVisibility(View.GONE);
        }
        if (isTrafficEnabled()) {
            mNetworkTrafficController.addCallback(this);
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        } else if (mNetworkTrafficController != null) {
            mNetworkTrafficController.removeCallback(this);
        }
    }

    public void setActivity(int activity) {
        mShowActivityIn = activity == TRAFFIC_IN || activity == TRAFFIC_IN_OUT;
        mShowActivityOut = activity == TRAFFIC_OUT || activity == TRAFFIC_IN_OUT;

        if (isTrafficEnabled()) {
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        }
    }

    public void setType(int type) {
        mShowText = type == TRAFFIC_TYPE_TEXT || type == TRAFFIC_TYPE_TEXT_ICON;
        mShowIcon = type == TRAFFIC_TYPE_ICON || type == TRAFFIC_TYPE_TEXT_ICON;

        if (isTrafficEnabled()) {
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        }
    }

    public void setIsBit(boolean isBit) {
        mIsBit = isBit;

        if (isTrafficEnabled()) {
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        }
    }

    public void setHide(boolean hide, int threshold, boolean iconAsIndicator) {
        mHide = hide;
        if (threshold > 0) {
            mThreshold = threshold * 1024;
        } else {
            mThreshold = threshold;
        }
        mIconAsIndicator = iconAsIndicator;

        if (isTrafficEnabled()) {
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        }
    }

    private boolean shouldHide(long speed) {
        return speed <= mThreshold;
    }

    public void setTextColor(int color) {
        if (mTextView != null) {
            mTextView.setTextColor(color);
        }
    }

    public void setIconColor(int color) {
        if (mIconView != null) {
            mIconView.setImageTintList(ColorStateList.valueOf(color));
        }

    }

    private void updateTraffic(Traffic traffic) {
        if (mTextView == null || mIconView == null) {
            return;
        }

        mOutSpeed = traffic.outSpeed;
        mInSpeed = traffic.inSpeed;
        String speed = "";
        String speedOut = "";
        String speedIn = "";
        String blankSpace = " ";
        int textSize = mTxtSizeSingle;
        boolean visible = true;

        if (mShowText) {
            speedOut = mIsBit ? (traffic.outSpeedInBits + blankSpace + traffic.outUnitAsBits)
                    : (traffic.outSpeedInBytes + blankSpace + traffic.outUnitAsBytes);
            speedIn = mIsBit ? (traffic.inSpeedInBits + blankSpace + traffic.inUnitAsBits)
                    : (traffic.inSpeedInBytes + blankSpace + traffic.inUnitAsBytes);

            if (mHide) {
                if (mShowActivityOut && !shouldHide(mOutSpeed) && mShowActivityIn && !shouldHide(mInSpeed)) {
                    speed = speedOut + "\n" + speedIn;
                    textSize = mTxtSizeDual;
                } else if (mShowActivityOut && !shouldHide(mOutSpeed)) {
                    speed = speedOut;
                } else if (mShowActivityIn && !shouldHide(mInSpeed)) {
                    speed = speedIn;
                } else {
                    visible = false;
                }
            } else {
                if (mShowActivityOut && mShowActivityIn) {
                    speed = speedOut + "\n" + speedIn;
                    textSize = mTxtSizeDual;
                } else if (mShowActivityOut) {
                    speed = speedOut;
                } else if (mShowActivityIn) {
                    speed = speedIn;
                } else {
                    visible = false;
                }
            }
        } else {
            visible = false;
        }

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) textSize);
        mTextView.setText(speed);
        if (visible && mTextView.getVisibility() != View.VISIBLE) {
            mTextView.setVisibility(View.VISIBLE);
        } else if (!visible && mTextView.getVisibility() != View.GONE) {
            mTextView.setVisibility(View.GONE);
        }
        if (mHide) {
            updateDrawable(mIconAsIndicator ? traffic.activityOut : !shouldHide(mOutSpeed),
                    mIconAsIndicator ? traffic.activityIn : !shouldHide(mInSpeed));
        } else {
            updateDrawable(true, true);
        }
    }

    private void updateDrawable(boolean outIconVisible, boolean inIconVisible) {
        Drawable drawable = null;
        boolean iconVisible = false;

        if (mShowIcon) {
            if (mShowActivityOut && outIconVisible) {
                iconVisible = true;
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_out);
            }
            if (mShowActivityIn && inIconVisible) {
                drawable = iconVisible
                        ? mResources.getDrawable(R.drawable.stat_sys_signal_inout)
                        : mResources.getDrawable(R.drawable.stat_sys_signal_in);
                if (!iconVisible) {
                    iconVisible = true;
                }
            }
            if (!iconVisible && !mHide) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_inout);
            }
            if (iconVisible && mIconView.getVisibility() != View.VISIBLE) {
                mIconView.setVisibility(View.VISIBLE);
            } else if (!iconVisible && mIconView.getVisibility() != View.INVISIBLE) {
                mIconView.setVisibility(View.INVISIBLE);
            }
            mIconView.setImageDrawable(drawable);
        } else {
            if (mIconView.getVisibility() != View.GONE) {
                mIconView.setVisibility(View.GONE);
            }
            mIconView.setImageDrawable(drawable);
        }
    }

    private boolean isTrafficEnabled() {
        return mNetworkTrafficController != null
                && mShowTraffic
                && mListening
                && mAttached;
    }
}
