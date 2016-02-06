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

    private static final int TRAFFIC_DOWN        = 0;
    private static final int TRAFFIC_UP          = 1;
    private static final int TRAFFIC_UP_DOWN     = 2;

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
    private boolean mShowDl;
    private boolean mShowUl;
    private boolean mShowText;
    private boolean mShowIcon;
    private boolean mIsBit;
    private boolean mHide;

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
        updateDrawable(traffic.activityUp, traffic.activityDown);
        updateText(traffic);
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

    public void setActivityDirection(int activity) {
        mShowDl = activity == TRAFFIC_DOWN || activity == TRAFFIC_UP_DOWN;
        mShowUl = activity == TRAFFIC_UP || activity == TRAFFIC_UP_DOWN;

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

    public void setHide(boolean hide) {
        mHide = hide;

        if (isTrafficEnabled()) {
            onNetworkTrafficChanged(mNetworkTrafficController.getTraffic());
        }
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

    private void updateText(Traffic traffic) {
        if (mTextView == null) {
            return;
        }

        String output = "";
        String outputUp = "";
        String outputDown = "";
        String blankSpace = " ";
        int textSize = mTxtSizeSingle;
        boolean visible = true;

        if (mShowText) {
            outputUp = mIsBit ? (traffic.upBitsValue + blankSpace + traffic.upBitsUnit)
                    : (traffic.upBytesValue + blankSpace + traffic.upBytesUnit);
            outputDown = mIsBit ? (traffic.downBitsValue + blankSpace + traffic.downBitsUnit)
                    : (traffic.downBytesValue + blankSpace + traffic.downBytesUnit);

            if (mHide) {
                if (mShowUl && traffic.activityUp && mShowDl && traffic.activityDown) {
                    output = outputUp + "\n" + outputDown;
                    textSize = mTxtSizeDual;
                } else if (mShowUl && traffic.activityUp) {
                    output = outputUp;
                } else if (mShowDl && traffic.activityDown) {
                    output = outputDown;
                } else {
                    visible = false;
                }
            } else {
                if (mShowUl && mShowDl) {
                    output = outputUp + "\n" + outputDown;
                    textSize = mTxtSizeDual;
                } else if (mShowUl) {
                    output = outputUp;
                } else if (mShowDl) {
                    output = outputDown;
                } else {
                    visible = false;
                }
            }
        } else {
            visible = false;
        }

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)textSize);
        mTextView.setText(output);
        if (visible && mTextView.getVisibility() != View.VISIBLE) {
            mTextView.setVisibility(View.VISIBLE);
        } else if (!visible && mTextView.getVisibility() != View.GONE) {
            mTextView.setVisibility(View.GONE);
        }
    }

    private void updateDrawable(boolean showUp, boolean showDown) {
        if (mIconView == null) {
            return;
        }

        Drawable drawable = null;
        boolean iconVisible = false;

        if (mShowIcon) {
            if (mShowUl && showUp) {
                iconVisible = true;
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_out);
            }
            if (mShowDl && showDown) {
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
