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

package com.android.systemui.darkkat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.SystemClock;

import com.android.systemui.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class NetworkTrafficControllerImpl implements NetworkTrafficController {

    private final Context mContext;
    private final Resources mResources;
    private final Handler mHandler = new Handler();

    private final NumberFormat mDecimalFormat;
    private final NumberFormat mIntegerFormat;

    private long mTotalRxBytes;
    private long mTotalTxBytes;
    private long mLastUpdateTime;

    private final ArrayList<Callback> mCallbacks;
    private Traffic mTraffic;

    private boolean mScreenStateOn = false;
    private boolean mIsUpdating = false;
    private boolean mReceiverRegistered = false;
    private boolean mHasCallbacks = false;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (shouldStartTrafficUpdates()) {
                    startTrafficUpdates();
                } else if (shouldStopTrafficUpdates()) {
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

    public NetworkTrafficControllerImpl(Context context) {
        mContext = context;
        mResources = mContext.getResources();

        mDecimalFormat = new DecimalFormat("##0.0");
        mIntegerFormat = NumberFormat.getIntegerInstance();

        mCallbacks = new ArrayList<Callback>();
        mTraffic = getDefaultTraffic();
    }

    public void setScreenState(boolean on) {
        mScreenStateOn = on;
        updateReceiverState();

        if (shouldStartTrafficUpdates()) {
            startTrafficUpdates();
        } else if (shouldStopTrafficUpdates()) {
            stopTrafficUpdates();
        }
    }

    private void updateReceiverState() {
        if (mScreenStateOn && !mReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);
            mReceiverRegistered = true;
        } else if (!mScreenStateOn && mReceiverRegistered) {
            mContext.unregisterReceiver(mIntentReceiver);
            mReceiverRegistered = false;
        }
    }

    public void addCallback(Callback callback) {
        if (callback == null || mCallbacks.contains(callback)) {
            return;
        }
        mCallbacks.add(callback);
        mHasCallbacks = true;

        if (shouldStartTrafficUpdates()) {
            startTrafficUpdates();
        }
    }

    public void removeCallback(Callback callback) {
        if (callback == null || !mCallbacks.contains(callback)) {
            return;
        }
        mCallbacks.remove(callback);
        if (mCallbacks.isEmpty()) {
            mHasCallbacks = false;
        }

        if (shouldStopTrafficUpdates()) {
            stopTrafficUpdates();
        }
    }

    @Override
    public Traffic getTraffic() {
        return mTraffic;
    }

    private boolean shouldStartTrafficUpdates() {
        return !mIsUpdating && mScreenStateOn && mReceiverRegistered && mHasCallbacks
                && getConnectAvailable();
    }

    private void startTrafficUpdates() {
        mTotalRxBytes = TrafficStats.getTotalRxBytes();
        mTotalTxBytes = TrafficStats.getTotalTxBytes();
        mLastUpdateTime = SystemClock.elapsedRealtime();

        mHandler.removeCallbacks(mRunnable);
        mHandler.post(mRunnable);
        mIsUpdating = true;
    }

    private void updateTraffic() {
        long td = SystemClock.elapsedRealtime() - mLastUpdateTime;

        final String defaultValue =
                mResources.getString(R.string.network_traffic_no_traffic);
        final String defaultBitsUnit =
                mResources.getString(R.string.network_traffic_unit_bits_per_second);
        final String defaultBytesUnit =
                mResources.getString(R.string.network_traffic_unit_bytes_per_second);

        String trafficUpBitsValue = defaultValue;
        String trafficUpBytesValue = defaultValue;
        String trafficDownBitsValue = defaultValue;
        String trafficDownBytesValue = defaultValue;
        String trafficUpBitsUnit = defaultBitsUnit;
        String trafficUpBytesUnit = defaultBytesUnit;
        String trafficDownBitsUnit = defaultBitsUnit;
        String trafficDownBytesUnit = defaultBytesUnit;
        boolean trafficActivityUp = false;
        boolean trafficActivityDown = false;

        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long newRxBytes = currentRxBytes - mTotalRxBytes;
        long newTxBytes = currentTxBytes - mTotalTxBytes;

        if (newTxBytes != 0) {
            trafficUpBitsValue = formatTraffic(newTxBytes * 8000 / td);
            trafficUpBytesValue = formatTraffic(newTxBytes * 1000 / td);
            trafficUpBitsUnit = getUnit(newTxBytes * 8000 / td, true);
            trafficUpBytesUnit = getUnit(newTxBytes * 1000 / td, false);
            trafficActivityUp = true;
        }
        if (newRxBytes != 0 ) {
            trafficDownBitsValue = formatTraffic(newRxBytes * 8000 / td);
            trafficDownBytesValue = formatTraffic(newRxBytes * 1000 / td);
            trafficDownBitsUnit = getUnit(newRxBytes * 8000 / td, true);
            trafficDownBytesUnit = getUnit(newRxBytes * 1000 / td, false);
            trafficActivityDown = true;
        }

        mTraffic.upBitsValue = trafficUpBitsValue;
        mTraffic.upBytesValue = trafficUpBytesValue;
        mTraffic.downBitsValue = trafficDownBitsValue;
        mTraffic.downBytesValue = trafficDownBytesValue;
        mTraffic.upBitsUnit = trafficUpBitsUnit;
        mTraffic.upBytesUnit = trafficUpBytesUnit;
        mTraffic.downBitsUnit = trafficDownBitsUnit;
        mTraffic.downBytesUnit = trafficDownBytesUnit;
        mTraffic.activityUp = trafficActivityUp;
        mTraffic.activityDown = trafficActivityDown;

        for (Callback callback : mCallbacks) {
            callback.onNetworkTrafficChanged(mTraffic);
        }

        mTotalRxBytes = currentRxBytes;
        mTotalTxBytes = currentTxBytes;
        mLastUpdateTime = SystemClock.elapsedRealtime();

        mHandler.postDelayed(mRunnable, 500);
    }

    private boolean shouldStopTrafficUpdates() {
        return mIsUpdating && (!mScreenStateOn || !mReceiverRegistered || !mHasCallbacks
                || !getConnectAvailable());
    }

    private void stopTrafficUpdates() {
        mHandler.removeCallbacks(mRunnable);
        mIsUpdating = false;
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
        if (trafffic > TrafficStats.MB_IN_BYTES * 10) {
            return mIntegerFormat.format(trafffic / TrafficStats.MB_IN_BYTES);
        } else if (trafffic > TrafficStats.MB_IN_BYTES) {
            return mDecimalFormat.format(((float) trafffic) / 1048576f);
        } else if (trafffic > TrafficStats.KB_IN_BYTES * 10) {
            return mIntegerFormat.format(trafffic / TrafficStats.KB_IN_BYTES);
        } else if (trafffic > TrafficStats.KB_IN_BYTES) {
            return mDecimalFormat.format(((float) trafffic) / 1024f);
        } else {
            return mIntegerFormat.format(trafffic);
        }
    }

    private String getUnit(long trafffic, boolean isBits) {
        int resId = 0;
        final String emptyString = "";
        if (trafffic > TrafficStats.MB_IN_BYTES) {
            resId = isBits ? R.string.network_traffic_unit_mega_bits_per_second
                    : R.string.network_traffic_unit_mega_bytes_per_second;
        } else if (trafffic > TrafficStats.KB_IN_BYTES) {
            resId =  isBits ? R.string.network_traffic_unit_kilo_bits_per_second
                    : R.string.network_traffic_unit_kilo_bytes_per_second;
        } else {
            resId =  isBits ? R.string.network_traffic_unit_bits_per_second
                    : R.string.network_traffic_unit_bytes_per_second;
        }
        return resId > 0 ? mResources.getString(resId) : emptyString;
    }

    private Traffic getDefaultTraffic() {
        Traffic traffic = new Traffic();

        final String defaultValue =
                mResources.getString(R.string.network_traffic_no_traffic);
        final String defaultBitsUnit =
                mResources.getString(R.string.network_traffic_unit_bits_per_second);
        final String defaultBytesUnit =
                mResources.getString(R.string.network_traffic_unit_bytes_per_second);

        traffic.upBitsValue = defaultValue;
        traffic.upBytesValue = defaultValue;
        traffic.downBitsValue = defaultValue;
        traffic.downBytesValue = defaultValue;
        traffic.upBitsUnit = defaultBitsUnit;
        traffic.upBytesUnit = defaultBytesUnit;
        traffic.downBitsUnit = defaultBitsUnit;
        traffic.downBytesUnit = defaultBytesUnit;
        traffic.activityUp = false;
        traffic.activityDown = false;

        return traffic;
    }
}
