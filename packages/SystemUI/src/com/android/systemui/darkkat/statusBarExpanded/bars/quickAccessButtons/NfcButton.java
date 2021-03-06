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

package com.android.systemui.darkkat.statusBarExpanded.bars.quickAccessButtons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;

public class NfcButton extends QabButton {
    private static final Intent NFC_SETTINGS = new Intent("android.settings.NFC_SETTINGS");

    private NfcAdapter mNfcAdapter;

    private boolean mEnabled;
    private boolean mReceiverRegistered = false;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mEnabled = isNfcEnabled();
            updateState(mEnabled);
        }
    };

    public NfcButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        try {
            mNfcAdapter = NfcAdapter.getNfcAdapter(mContext);
        } catch (UnsupportedOperationException e) {
            mNfcAdapter = null;
        }
        mEnabled = isNfcEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (mEnabled != isNfcEnabled()) {
            mEnabled = isNfcEnabled();
            updateState(mEnabled);
        }
        if (listening) {
            if (!mReceiverRegistered) {
                mContext.registerReceiver(mReceiver,
                        new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
                mReceiverRegistered = true;
            }
        } else {
            if (mReceiverRegistered) {
                mContext.unregisterReceiver(mReceiver);
                mReceiverRegistered = false;
            }
        }
    }

    @Override
    public void handleClick() {
        if (mNfcAdapter == null) {
            try {
                mNfcAdapter = NfcAdapter.getNfcAdapter(mContext);
            } catch (UnsupportedOperationException e) {
                mNfcAdapter = null;
            }
        }
        if (mNfcAdapter != null && mEnabled) {
            mNfcAdapter.disable();
        } else if (mNfcAdapter != null) {
            mNfcAdapter.enable();
        }
    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(NFC_SETTINGS);
    }

    private boolean isNfcEnabled() {
        if (mNfcAdapter == null) {
            try {
                mNfcAdapter = NfcAdapter.getNfcAdapter(mContext);
            } catch (UnsupportedOperationException e) {
                mNfcAdapter = null;
            }
        }
        if (mNfcAdapter == null) {
            return false;
        }
        int state = mNfcAdapter.getAdapterState();
        if (state == NfcAdapter.STATE_TURNING_ON
                || state == NfcAdapter.STATE_ON) {
            return true;
        } else {
            return false;
        }
    }
}
