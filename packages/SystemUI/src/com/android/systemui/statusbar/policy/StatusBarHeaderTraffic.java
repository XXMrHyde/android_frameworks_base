package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.android.systemui.R;

public class StatusBarHeaderTraffic extends LinearLayout {
    private static final int TRAFFIC_UP_DOWN = 0;
    private static final int TRAFFIC_DOWN    = 1;
    private static final int TRAFFIC_UP      = 2;
    private static final int NO_TRAFFIC      = 3;

    private ImageView mUpIconView;
    private ImageView mDownIconView;

    private TextView mUpValueTextView;
    private TextView mUpUnitTextView;
    private TextView mDownValueTextView;
    private TextView mDownUnitTextView;

    private boolean mIsShowing = false;
    private boolean mHide;
    private boolean mIsBit;
    private int mSummaryTime;

    private long mTotalRxBytes;
    private long mTotalTxBytes;
    private long mLastUpdateTime;
    private long mTrafficBurstStartTime;
    private long mTrafficBurstStartRxBytes;
    private long mTrafficBurstStartTxBytes;
    private long mKeepOnUntil = Long.MIN_VALUE;
    private NumberFormat mDecimalFormat = new DecimalFormat("##0.0");
    private NumberFormat mIntegerFormat = NumberFormat.getIntegerInstance();

    private boolean mAttached;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (!mIsShowing && getConnectAvailable()) {
                    startTrafficUpdates();
                } else if (mIsShowing && !getConnectAvailable()) {
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

    public StatusBarHeaderTraffic(Context context) {
        this(context, null);
    }

    public StatusBarHeaderTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarHeaderTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateSettings();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        mUpIconView = (ImageView) findViewById(R.id.expanded_panel_traffic_up_icon);
        mDownIconView = (ImageView) findViewById(R.id.expanded_panel_traffic_down_icon);

        mUpValueTextView = (TextView) findViewById(R.id.expanded_panel_traffic_up_value_text);
        mUpUnitTextView = (TextView) findViewById(R.id.expanded_panel_traffic_up_unit_text);
        mDownValueTextView = (TextView) findViewById(R.id.expanded_panel_traffic_down_value_text);
        mDownUnitTextView = (TextView) findViewById(R.id.expanded_panel_traffic_down_unit_text);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mUpUnitTextView.setY(mUpValueTextView.getBottom() - mUpUnitTextView.getHeight());
        mDownUnitTextView.setY(mDownValueTextView.getBottom() - mDownUnitTextView.getHeight());
    }

    public void setListening(boolean listening) {
        if (listening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    getHandler());
            startTrafficUpdates();
        } else {
            getContext().unregisterReceiver(mIntentReceiver);
            stopTrafficUpdates();
        }
    }

    public void startTrafficUpdates() {
        if (getConnectAvailable()) {
            mTotalRxBytes = TrafficStats.getTotalRxBytes();
            mTotalTxBytes = TrafficStats.getTotalTxBytes();
            mLastUpdateTime = SystemClock.elapsedRealtime();
            mTrafficBurstStartTime = Long.MIN_VALUE;

            getHandler().removeCallbacks(mRunnable);
            getHandler().post(mRunnable);
            mIsShowing = true;
        }
    }

    private void updateTraffic() {
        long td = SystemClock.elapsedRealtime() - mLastUpdateTime;

        if (td == 0) {
            // we just updated the view, nothing further to do
            return;
        }

        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long newRxBytes = currentRxBytes - mTotalRxBytes;
        long newTxBytes = currentTxBytes - mTotalTxBytes;

        String upValue = "";
        String upUnit = "";
        String downValue = "";
        String downUnit = "";

        int state = NO_TRAFFIC;

        if (mHide && newTxBytes == 0) {
            long trafficBurstTxBytes = currentTxBytes - mTrafficBurstStartTxBytes;

            if (trafficBurstTxBytes != 0 && mSummaryTime != 0) {
                upValue = formatTraffic(trafficBurstTxBytes, false);
                upUnit = formatUnit(trafficBurstTxBytes, false);
                state = TRAFFIC_UP;

                mKeepOnUntil = SystemClock.elapsedRealtime() + mSummaryTime;
                mTrafficBurstStartTime = Long.MIN_VALUE;
                mTrafficBurstStartTxBytes = currentTxBytes;
            }
        } else {
            if (mHide && mTrafficBurstStartTime == Long.MIN_VALUE) {
                mTrafficBurstStartTime = mLastUpdateTime;
                mTrafficBurstStartTxBytes = mTotalTxBytes;
            }
            upValue = formatTraffic(mIsBit ? newTxBytes * 8000 / td : newTxBytes * 1000 / td, true);
            upUnit = formatUnit(mIsBit ? newTxBytes * 8000 / td : newTxBytes * 1000 / td, true);
            state = TRAFFIC_UP;
        }

        if (mHide && newRxBytes == 0) {
            long trafficBurstRxBytes = currentRxBytes - mTrafficBurstStartRxBytes;

            if (trafficBurstRxBytes != 0 && mSummaryTime != 0) {
                if (upValue != "") {
                    state = TRAFFIC_UP_DOWN;
                } else {
                    state = TRAFFIC_DOWN;
                }
                downValue = formatTraffic(trafficBurstRxBytes, false);
                downUnit = formatUnit(trafficBurstRxBytes, false);

                mKeepOnUntil = SystemClock.elapsedRealtime() + mSummaryTime;
                mTrafficBurstStartTime = Long.MIN_VALUE;
                mTrafficBurstStartRxBytes = currentRxBytes;
            }
        } else {
            if (mHide && mTrafficBurstStartTime == Long.MIN_VALUE) {
                mTrafficBurstStartTime = mLastUpdateTime;
                mTrafficBurstStartRxBytes = mTotalRxBytes;
            }
            if (upValue != "") {
                state = TRAFFIC_UP_DOWN;
            } else {
                state = TRAFFIC_DOWN;
            }
            downValue = formatTraffic(mIsBit ? newRxBytes * 8000 / td : newRxBytes * 1000 / td, true);
            downUnit = formatUnit(mIsBit ? newRxBytes * 8000 / td : newRxBytes * 1000 / td, true);
        }

        mUpValueTextView.setText(upValue);
        mUpUnitTextView.setText(upUnit);
        mDownValueTextView.setText(downValue);
        mDownUnitTextView.setText(downUnit);

        // Hide if there is no traffic
        if (mHide && newRxBytes == 0 && newTxBytes == 0) {
            if (mKeepOnUntil < SystemClock.elapsedRealtime()) {
                mUpValueTextView.setText("");
                mUpUnitTextView.setText("");
                mDownValueTextView.setText("");
                mDownUnitTextView.setText("");
                mUpIconView.setVisibility(View.GONE);
                mDownIconView.setVisibility(View.GONE);
                mUpValueTextView.setVisibility(View.GONE);
                mUpUnitTextView.setVisibility(View.GONE);
                mDownValueTextView.setVisibility(View.GONE);
                mDownUnitTextView.setVisibility(View.GONE);
                state = NO_TRAFFIC;
            }
        } else {
            mUpValueTextView.setVisibility(View.VISIBLE);
            mUpUnitTextView.setVisibility(View.VISIBLE);
            mDownValueTextView.setVisibility(View.VISIBLE);
            mDownUnitTextView.setVisibility(View.VISIBLE);
        }
        boolean showUpIcon = state == TRAFFIC_UP ||  state == TRAFFIC_UP_DOWN;
        boolean showDownIcon = state == TRAFFIC_DOWN ||  state == TRAFFIC_UP_DOWN;
        updateDrawable(showUpIcon, showDownIcon);

        mTotalRxBytes = currentRxBytes;
        mTotalTxBytes = currentTxBytes;
        mLastUpdateTime = SystemClock.elapsedRealtime();
        if (getHandler() != null) {
            getHandler().postDelayed(mRunnable, 500);
        }
    }

    private void stopTrafficUpdates() {
        getHandler().removeCallbacks(mRunnable);
        mIsShowing = false;
        mUpIconView.setVisibility(View.GONE);
        mDownIconView.setVisibility(View.GONE);
        mUpValueTextView.setText("");
        mUpUnitTextView.setText("");
        mDownValueTextView.setText("");
        mDownUnitTextView.setText("");
    }

    private String formatTraffic(long trafffic, boolean speed) {
        if (trafffic > 10485760) { // 1024 * 1024 * 10
            return mIntegerFormat.format(trafffic / 1048576);
        } else if (trafffic > 1048576) { // 1024 * 1024
            return mDecimalFormat.format(((float) trafffic) / 1048576f);
        } else if (trafffic > 10240) { // 1024 * 10
            return mIntegerFormat.format(trafffic / 1024);
        } else if (trafffic > 1024) { // 1024
            return mDecimalFormat.format(((float) trafffic) / 1024f);
        } else {
            return mIntegerFormat.format(trafffic);
        }
    }

    private String formatUnit(long trafffic, boolean speed) {
        if (trafffic > 10485760) { // 1024 * 1024 * 10
            return (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB");
        } else if (trafffic > 1048576) { // 1024 * 1024
            return (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB");
        } else if (trafffic > 10240) { // 1024 * 10
            return (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB");
        } else if (trafffic > 1024) { // 1024
            return (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB");
        } else {
            return (speed ? (mIsBit ? "bit/s" : "B/s") : "B");
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

//        mHide = Settings.System.getIntForUser(resolver,
//                Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC, 1,
//                UserHandle.USER_CURRENT) == 1;
//        mSummaryTime = Settings.System.getIntForUser(resolver,
//                Settings.System.STATUS_BAR_TRAFFIC_SUMMARY, 3000,
//                UserHandle.USER_CURRENT);
//        mIsBit = Settings.System.getIntForUser(resolver,
//                Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE, 0,
//                UserHandle.USER_CURRENT) == 1;

        mHide = true;
        mSummaryTime = 3000;
        mIsBit = true;
    }

    public void updateDrawable(boolean showUpIcon, boolean showDownIcon) {
        mUpIconView.setVisibility(showUpIcon ? View.VISIBLE : View.GONE);
        mDownIconView.setVisibility(showDownIcon ? View.VISIBLE : View.GONE);
    }

    public void updateTextColor(int color, boolean isOpaque) {
        if (isOpaque) {
            mUpValueTextView.setTextColor(color);
            mDownValueTextView.setTextColor(color);
        } else {
            mUpUnitTextView.setTextColor(color);
            mDownUnitTextView.setTextColor(color);
        }
    }

    public void updateIconColor(int color) {
        mUpIconView.setColorFilter(color, Mode.MULTIPLY);
        mDownIconView.setColorFilter(color, Mode.MULTIPLY);
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
}
