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

        final String noTraffic =
                mResources.getString(R.string.network_traffic_no_traffic);
        final String defaultUnitAsBits =
                mResources.getString(R.string.network_traffic_unit_bits_per_second);
        final String defaultUnitAsBytes =
                mResources.getString(R.string.network_traffic_unit_bytes_per_second);

        long trafficOutSpeed = 0;
        long trafficInSpeed = 0;
        String formattedOutSpeedInBits = noTraffic;
        String formattedOutSpeedInBytes = noTraffic;
        String formattedInSpeedInBits = noTraffic;
        String formattedInSpeedInBytes = noTraffic;
        String formattedOutUnitAsBits = defaultUnitAsBits;
        String formattedOutUnitAsBytes = defaultUnitAsBytes;
        String formattedInUnitAsBits = defaultUnitAsBits;
        String formattedInUnitAsBytes = defaultUnitAsBytes;
        boolean trafficActivityOut = false;
        boolean trafficActivityIn = false;

        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long newRxBytes = currentRxBytes - mTotalRxBytes;
        long newTxBytes = currentTxBytes - mTotalTxBytes;

        if (newTxBytes != 0) {
            trafficOutSpeed = newTxBytes * 1000 / td;
            formattedOutSpeedInBits = formatTraffic(newTxBytes * 8000 / td);
            formattedOutSpeedInBytes = formatTraffic(newTxBytes * 1000 / td);
            formattedOutUnitAsBits = getUnit(newTxBytes * 8000 / td, true);
            formattedOutUnitAsBytes = getUnit(newTxBytes * 1000 / td, false);
            trafficActivityOut = true;
        }
        if (newRxBytes != 0 ) {
            trafficInSpeed = newRxBytes * 1000 / td;
            formattedInSpeedInBits = formatTraffic(newRxBytes * 8000 / td);
            formattedInSpeedInBytes = formatTraffic(newRxBytes * 1000 / td);
            formattedInUnitAsBits = getUnit(newRxBytes * 8000 / td, true);
            formattedInUnitAsBytes = getUnit(newRxBytes * 1000 / td, false);
            trafficActivityIn = true;
        }

        mTraffic.outSpeed = trafficOutSpeed;
        mTraffic.inSpeed = trafficInSpeed;
        mTraffic.outSpeedInBits = formattedOutSpeedInBits;
        mTraffic.outSpeedInBytes = formattedOutSpeedInBytes;
        mTraffic.inSpeedInBits = formattedInSpeedInBits;
        mTraffic.inSpeedInBytes = formattedInSpeedInBytes;
        mTraffic.outUnitAsBits = formattedOutUnitAsBits;
        mTraffic.outUnitAsBytes = formattedOutUnitAsBytes;
        mTraffic.inUnitAsBits = formattedInUnitAsBits;
        mTraffic.inUnitAsBytes = formattedInUnitAsBytes;
        mTraffic.activityOut = trafficActivityOut;
        mTraffic.activityIn = trafficActivityIn;

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

        final String noTraffic =
                mResources.getString(R.string.network_traffic_no_traffic);
        final String defaultUnitAsBits =
                mResources.getString(R.string.network_traffic_unit_bits_per_second);
        final String defaultUnitAsBytes =
                mResources.getString(R.string.network_traffic_unit_bytes_per_second);

        traffic.outSpeed = 0;
        traffic.inSpeed = 0;
        traffic.outSpeedInBits = noTraffic;
        traffic.outSpeedInBytes = noTraffic;
        traffic.inSpeedInBits = noTraffic;
        traffic.inSpeedInBytes = noTraffic;
        traffic.outUnitAsBits = defaultUnitAsBits;
        traffic.outUnitAsBytes = defaultUnitAsBytes;
        traffic.inUnitAsBits = defaultUnitAsBits;
        traffic.inUnitAsBytes = defaultUnitAsBytes;
        traffic.activityOut = false;
        traffic.activityIn = false;

        return traffic;
    }
}
