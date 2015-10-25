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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.android.systemui.R;

public class NetworkTraffic extends TextView {
    private static final int TRAFFIC_DOWN           = 0;
    private static final int TRAFFIC_UP             = 1;
    private static final int TRAFFIC_UP_DOWN        = 2;
    private static final int TRAFFIC_NO_ACTIVITY    = 3;
    private static final int TRAFFIC_TYPE_TEXT      = 0;
    private static final int TRAFFIC_TYPE_ICON      = 1;
    private static final int TRAFFIC_TYPE_TEXT_ICON = 2;

    private boolean mEnabled;
    private boolean mShowDl;
    private boolean mShowUl;
    private boolean mShowText;
    private boolean mShowIcon;
    private boolean mIsBit;
    private boolean mIconShowing = false;
    private boolean mHide;
    private int mIconColor;

    private boolean mAttached = false;
    private boolean mReceiverRegistered= false;
    private boolean mIsUpdating = false;

    private final Resources mResources;
    private final ContentResolver mResolver;

    private final int mTxtSizeSingle;
    private final int mTxtSizeDual;
    private final int mPaddingEnd;
    private boolean mPaddingEndApplied = false;
    private final NumberFormat mDecimalFormat;
    private final NumberFormat mIntegerFormat;

    private long mTotalRxBytes;
    private long mTotalTxBytes;
    private long mLastUpdateTime;

    private SettingsObserver mSettingsObserver;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ACTIVITY),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TYPE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_BIT_BYTE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_HIDE_TRAFFIC),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);

            updateSettings();
        }

        void unObserve() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ACTIVITY))) {
                updateTrafficActivity();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TYPE))) {
                updateType();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_BIT_BYTE))) {
                updateBitByte();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_HIDE_TRAFFIC))) {
                updateHideTraffic();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TEXT_COLOR))) {
                updateTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ICON_COLOR))) {
                updateIconColor();
            }
        }

    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (mEnabled && getConnectAvailable()) {
                    if (mAttached) {
                        startTrafficUpdates();
                    }
                } else {
                    stopTrafficUpdates();
                }
            }
        }
    };

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            updateTraffic();
        }
    };

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
        mDecimalFormat = new DecimalFormat("##0.0");
        mIntegerFormat = NumberFormat.getIntegerInstance();
        mTxtSizeSingle = mResources.getDimensionPixelSize(R.dimen.network_traffic_single_text_size);
        mTxtSizeDual = mResources.getDimensionPixelSize(R.dimen.network_traffic_dual_text_size);
        mPaddingEnd = mResources.getDimensionPixelSize(R.dimen.network_traffic_text_end_padding);

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttached = true;
        updateReceiverState();
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(getHandler());
        }
        mSettingsObserver.observe();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached = false;
        updateReceiverState();
        if (mSettingsObserver == null) {
            mSettingsObserver.unObserve();
        }
    }

    private void updateReceiverState() {
        boolean shouldBeRegistered = mEnabled && mAttached && !mReceiverRegistered;
        if (shouldBeRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    getHandler());
            mReceiverRegistered = true;
        } else if ((!mEnabled || !mAttached) && mReceiverRegistered) {
            getContext().unregisterReceiver(mIntentReceiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        if (screenState == SCREEN_STATE_OFF) {
            stopTrafficUpdates();
        } else {
            if (mEnabled && getConnectAvailable()) {
                if (mAttached) {
                    startTrafficUpdates();
                }
            }
        }
        super.onScreenStateChanged(screenState);
    }

    public void startTrafficUpdates() {
        if (!mIsUpdating) {
            mTotalRxBytes = TrafficStats.getTotalRxBytes();
            mTotalTxBytes = TrafficStats.getTotalTxBytes();
            mLastUpdateTime = SystemClock.elapsedRealtime();

            if (getHandler() != null) {
                getHandler().removeCallbacks(mRunnable);
                getHandler().post(mRunnable);
                mIsUpdating = true;
            }
        }
    }

    private void updateTraffic() {
        long td = SystemClock.elapsedRealtime() - mLastUpdateTime;

        if (td == 0 || !mEnabled) {
            // we just updated the view, nothing further to do
            return;
        }

        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long newRxBytes = currentRxBytes - mTotalRxBytes;
        long newTxBytes = currentTxBytes - mTotalTxBytes;

        String output = "";
        String outputUp = "";
        String outputDown = "";
        int textSize = mTxtSizeSingle;
        int state = TRAFFIC_NO_ACTIVITY;

        if (mShowUl && newTxBytes != 0) {
            state = TRAFFIC_UP;
        }
        if (mShowDl && newRxBytes != 0 ) {
            if (state == TRAFFIC_UP) {
                state = TRAFFIC_UP_DOWN;
            } else {
                state = TRAFFIC_DOWN;
            }
        }
        if (mShowText) {
            if (mShowUl) {
                outputUp = formatTraffic(mIsBit ? newTxBytes * 8000 / td : newTxBytes * 1000 / td);
            }
            if (mShowDl) {
                outputDown = formatTraffic(mIsBit ? newRxBytes * 8000 / td : newRxBytes * 1000 / td);
            }

        }

        if (mHide) {
            // Hide if there is no traffic
            if (state == TRAFFIC_UP) {
                output = outputUp;
            } else if (state == TRAFFIC_DOWN) {
                output = outputDown;
            } else if (state == TRAFFIC_UP_DOWN) {
                output = outputUp + "\n" + outputDown;
                textSize = mTxtSizeDual;
            }
            if (state == TRAFFIC_NO_ACTIVITY) {
                if (getVisibility() != View.GONE) {
                    setVisibility(View.GONE);
                }
            } else {
                if (getVisibility() != View.VISIBLE) {
                    setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
            output = outputUp + "\n" + outputDown;
            textSize = mTxtSizeDual;
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)textSize);
        setText(output);

        if (mShowIcon) {
            updateDrawable(mIconColor, state);
        } else if (!mShowIcon && mIconShowing) {
            removeDrawable();
        }

        mTotalRxBytes = currentRxBytes;
        mTotalTxBytes = currentTxBytes;
        mLastUpdateTime = SystemClock.elapsedRealtime();
        if (getHandler() != null) {
            getHandler().postDelayed(mRunnable, 500);
        }
    }

    private void stopTrafficUpdates() {
        if (getHandler() != null) {
            getHandler().removeCallbacks(mRunnable);
            mIsUpdating = false;
        }
        setText("");
        removeDrawable();
        if (getVisibility() != View.GONE) {
            setVisibility(View.GONE);
        }
    }

    private void updateSettings() {
        updateTrafficActivity();
        updateType();
        updateBitByte();
        updateHideTraffic();
        updateTextColor();
        updateIconColor();
    }

    private void updateTrafficActivity() {
        final int activity = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ACTIVITY,
                TRAFFIC_NO_ACTIVITY, UserHandle.USER_CURRENT);
        mEnabled = activity != TRAFFIC_NO_ACTIVITY;
        mShowDl = activity == TRAFFIC_DOWN || activity == TRAFFIC_UP_DOWN;
        mShowUl = activity == TRAFFIC_UP || activity == TRAFFIC_UP_DOWN;

        if (mEnabled && getConnectAvailable()) {
            if (mAttached) {
                startTrafficUpdates();
            }
        } else {
            stopTrafficUpdates();
        }
        updateReceiverState();
    }

    private void updateType() {
        final int type = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TYPE, TRAFFIC_TYPE_TEXT_ICON,
                UserHandle.USER_CURRENT);
        mShowText = type == TRAFFIC_TYPE_TEXT || type == TRAFFIC_TYPE_TEXT_ICON;
        mShowIcon = type == TRAFFIC_TYPE_ICON || type == TRAFFIC_TYPE_TEXT_ICON;

        if (!mShowIcon && mIconShowing) {
            removeDrawable();
        }
    }

    private void updateBitByte() {
        mIsBit = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_BIT_BYTE, 0,
                UserHandle.USER_CURRENT) == 1;
    }

    private void updateHideTraffic() {
        mHide = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_HIDE_TRAFFIC, 1,
                UserHandle.USER_CURRENT) == 1;
    }

    private void updateTextColor() {
        int textColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_TEXT_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);

        setTextColor(textColor);
    }

    private void updateIconColor() {
        mIconColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_ICON_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
    }

    private boolean getConnectAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            return connectivityManager.getActiveNetworkInfo().isConnected();
        } catch (Exception ignored) {
        }
        return false;
    }

    private String formatTraffic(long trafffic) {
        if (trafffic > 10485760) { // 1024 * 1024 * 10
            return mIntegerFormat.format(trafffic / 1048576)
                    + (mIsBit ? " Mbit/s" : " MB/s");
        } else if (trafffic > 1048576) { // 1024 * 1024
            return mDecimalFormat.format(((float) trafffic) / 1048576f)
                    + (mIsBit ? " Mbit/s" : " MB/s");
        } else if (trafffic > 10240) { // 1024 * 10
            return mIntegerFormat.format(trafffic / 1024)
                    + (mIsBit ? " Kbit/s" : " KB/s");
        } else if (trafffic > 1024) { // 1024
            return mDecimalFormat.format(((float) trafffic) / 1024f)
                    + (mIsBit ? " Kbit/s" :  " KB/s");
        } else {
            return mIntegerFormat.format(trafffic)
                    + (mIsBit ? " bit/s" : " B/s");
        }
    }

    private void updateDrawable(int color, int state) {
        Drawable drawable = null;
        if (mShowIcon) {
            if (state == TRAFFIC_UP) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_out);
            } else if (state == TRAFFIC_DOWN) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_in);
            } else if (state == TRAFFIC_UP_DOWN) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_signal_inout);
            }
            if (drawable != null) {
                drawable.setColorFilter(color, Mode.MULTIPLY);
            }
        }
        if (drawable == null && !mPaddingEndApplied
                || drawable != null && mPaddingEndApplied) {
            setPaddingRelative(0, 0, drawable == null ? mPaddingEnd : 0, 0);
            mPaddingEndApplied = drawable == null;
        }
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        mIconShowing = true;
    }

    private void removeDrawable() {
        if (!mPaddingEndApplied) {
            setPaddingRelative(0, 0, mPaddingEnd, 0);
            mPaddingEndApplied = true;
        }
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        mIconShowing = false;
    }
}
