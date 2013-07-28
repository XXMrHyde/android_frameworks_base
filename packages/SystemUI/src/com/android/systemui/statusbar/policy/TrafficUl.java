package com.android.systemui.statusbar.policy;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class TrafficUl extends TextView {
    private boolean mAttached;
    TrafficStats mTrafficStats;
    Handler mHandler;
    Handler mTrafficHandler;
    boolean mIsTrafficEnabled;
    boolean mShowUl;
    boolean mTrafficHide;
    float mSpeedUl;
    float mTotalTxBytes;
    boolean mIsBit;
    int mTrafficUlColor;

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
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_NETWORK_SPEED_UPLOAD_COLOR), false, this);
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
        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        mTrafficStats = new TrafficStats();
        settingsObserver.observe();
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
        }
        updateSettings();
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

    public void updateTraffic() {
        mTrafficHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ContentResolver resolver = mContext.getContentResolver();
                mSpeedUl = (mTrafficStats.getTotalTxBytes() - mTotalTxBytes) / 1024 / 3;
                mTotalTxBytes = mTrafficStats.getTotalTxBytes();
                DecimalFormat DecimalFormatfnum = new DecimalFormat("###0");

                if (mIsBit) {
                    if (mSpeedUl / 1024 >= 1) {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl / 128) + "Mb/s");
                    } else if (mSpeedUl <= 0.01) {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl * 8192) + "b/s");
                    } else {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl * 8) + "Kb/s");
                    }
                } else {
                    if (mSpeedUl / 1024 >= 1) {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl / 1024) + "MB/s");
                    } else if (mSpeedUl <= 0.01) {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl * 1024) + "B/s");
                    } else {
                        setText("U: " + DecimalFormatfnum.format(mSpeedUl) + "KB/s");
                    }
                }
                if (mIsTrafficEnabled && mShowUl && getConnectAvailable()) {
                    if ((mTrafficHide) && (mSpeedUl == 0)) {
                        setVisibility(View.GONE);
                    } else {
                        setVisibility(View.VISIBLE);
                    }
                } else {
                     setVisibility(View.GONE);
                }
                setTextColor(mTrafficUlColor);
                update();
                super.handleMessage(msg);
            }
        };
        mTotalTxBytes = mTrafficStats.getTotalTxBytes(); 
        mTrafficHandler.sendEmptyMessage(0);
    }

    private boolean getConnectAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager.getActiveNetworkInfo().isConnected())
                return true;
            else
                return false;
        } catch (Exception ex) {
        }
        return false;
    }

    public void update() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.postDelayed(mRunnable, 2000);
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
        }
    };

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsTrafficEnabled = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR, 0) == 1);
        mTrafficHide = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC, 1) == 1);
        mShowUl = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_UPLOAD, 1) == 1);
        mIsBit = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE, 0) == 1);
        mTrafficUlColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_UPLOAD_COLOR, 0xff33b5e5);

        if (mAttached) {
            updateTraffic();
        }
    }
}
