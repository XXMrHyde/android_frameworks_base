package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TrafficUl extends TextView {
    public static final String TAG = "TrafficUl";
    private boolean mAttached;
    boolean mTrafficMeterEnable;
    boolean mTrafficMeterHide;
    boolean mShowUl;
    boolean mIsBit;
    int mTrafficMeterSummaryTime;
    long totalTxBytes;
    long lastUpdateTime;
    long trafficBurstStartTime;
    long trafficBurstStartBytes;
    long keepOnUntil = Long.MIN_VALUE;
    NumberFormat decimalFormat = new DecimalFormat("##0.0");
    NumberFormat integerFormat = NumberFormat.getIntegerInstance();

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_UPLOAD), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_TRAFFIC_SUMMARY), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_ENABLE_THEME_DEFAULT), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_UPLOAD_COLOR), false, this);

            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }

    }

    public TrafficUl(Context context) {
        this(context, null);
    }

    public TrafficUl(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrafficUl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    getHandler());

            SettingsObserver settingsObserver = new SettingsObserver(getHandler());
            settingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateSettings();
            }
        }
    };

    @Override
    public void onScreenStateChanged(int screenState) {
        if (screenState == SCREEN_STATE_OFF) {
            stopTrafficUpdates();
        } else {
            startTrafficUpdates();
        }
        super.onScreenStateChanged(screenState);
    }

    private void stopTrafficUpdates() {
        getHandler().removeCallbacks(mRunnable);
        setText("");
    }

    public void startTrafficUpdates() {

        if (getConnectAvailable()) {
            totalTxBytes = TrafficStats.getTotalTxBytes();
            lastUpdateTime = SystemClock.elapsedRealtime();
            trafficBurstStartTime = Long.MIN_VALUE;

            getHandler().removeCallbacks(mRunnable);
            getHandler().post(mRunnable);
        }
    }

    private String formatTraffic(long trafffic, boolean speed) {
        if (trafffic > 10485760) { // 1024 * 1024 * 10
            return ("U: ")
                    + (speed ? "" : "(")
                    + integerFormat.format(trafffic / 1048576)
                    + (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB)");
        } else if (trafffic > 1048576) { // 1024 * 1024
            return ("U: ")
                    + (speed ? "" : "(")
                    + decimalFormat.format(((float) trafffic) / 1048576f)
                    + (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB)");
        } else if (trafffic > 10240) { // 1024 * 10
            return ("U: ")
                    + (speed ? "" : "(")
                    + integerFormat.format(trafffic / 1024)
                    + (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB)");
        } else if (trafffic > 1024) { // 1024
            return ("U: ")
                    + (speed ? "" : "(")
                    + decimalFormat.format(((float) trafffic) / 1024f)
                    + (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB)");
        } else {
            return ("U: ")
                    + (speed ? "" : "(")
                    + integerFormat.format(trafffic)
                    + (speed ? (mIsBit ? "bit/s" : "B/s") : "B)");
        }
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

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long td = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (td == 0 || !mTrafficMeterEnable) {
                // we just updated the view, nothing further to do
                return;
            }

            long currentTxBytes = TrafficStats.getTotalTxBytes();
            long newBytes = currentTxBytes - totalTxBytes;

            if (mTrafficMeterHide && newBytes == 0) {
                long trafficBurstBytes = currentTxBytes - trafficBurstStartBytes;

                if (trafficBurstBytes != 0 && mTrafficMeterSummaryTime != 0) {
                    setText(formatTraffic(trafficBurstBytes, false));

                    Log.i(TAG,
                            "Traffic burst ended: " + trafficBurstBytes + "B in "
                                    + (SystemClock.elapsedRealtime() - trafficBurstStartTime)
                                    / 1000 + "s");
                    keepOnUntil = SystemClock.elapsedRealtime() + mTrafficMeterSummaryTime;
                    trafficBurstStartTime = Long.MIN_VALUE;
                    trafficBurstStartBytes = currentTxBytes;
                }
            } else {
                if (mTrafficMeterHide && trafficBurstStartTime == Long.MIN_VALUE) {
                    trafficBurstStartTime = lastUpdateTime;
                    trafficBurstStartBytes = totalTxBytes;
                }
                setText(formatTraffic(mIsBit ? newBytes * 8000 / td : newBytes * 1000 / td, true));
            }

            // Hide if there is no traffic
            if (mShowUl) {
                if (mTrafficMeterHide && newBytes == 0) {
                    if (getVisibility() != GONE
                            && keepOnUntil < SystemClock.elapsedRealtime()) {
                        setText("");
                        setVisibility(View.GONE);
                    }
                } else {
                    if (getVisibility() != VISIBLE) {
                        setVisibility(View.VISIBLE);
                    }
                }
            } else {
                setVisibility(View.GONE);
            }

            totalTxBytes = currentTxBytes;
            lastUpdateTime = SystemClock.elapsedRealtime();
            getHandler().postDelayed(mRunnable, 500);
        }
    };

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mTrafficMeterEnable = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR, 0) == 1);
        mTrafficMeterHide = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC, 1) == 1);
        mTrafficMeterSummaryTime = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TRAFFIC_SUMMARY, 3000);
        mShowUl = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_UPLOAD, 1) == 1;
        mIsBit = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE, 0) == 1;
        boolean enableThemeDefault = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_ENABLE_THEME_DEFAULT, 1) == 1;
        int defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_light);

        int trafficULColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_UPLOAD_COLOR, defaultColor);


        if (mTrafficMeterEnable && mShowUl && getConnectAvailable()) {
            setVisibility(View.VISIBLE);
            if (mAttached) {
                startTrafficUpdates();
            }
        } else {
            setVisibility(View.GONE);
            setText("");
        }

        setTextColor(enableThemeDefault ? defaultColor : trafficULColor);
    }
}
