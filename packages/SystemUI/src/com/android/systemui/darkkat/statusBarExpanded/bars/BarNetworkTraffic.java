/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.darkkat.statusBarExpanded.bars;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.darkkat.NetworkTrafficController;
import com.android.systemui.R;

public class BarNetworkTraffic extends LinearLayout implements
        NetworkTrafficController.Callback {

    private NetworkTrafficController mNetworkTrafficController;

    private ImageView mOutIconView;
    private ImageView mInIconView;

    private TextView mOutValueTextView;
    private TextView mOutUnitTextView;
    private TextView mInValueTextView;
    private TextView mInUnitTextView;

    private final int mTrafficInBytes = 0;
    private int mBitByte;

    private boolean mListening = false;

    public BarNetworkTraffic(Context context) {
        this(context, null);
    }

    public BarNetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarNetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        mOutIconView = (ImageView) findViewById(R.id.bars_network_traffic_out_icon);
        mInIconView = (ImageView) findViewById(R.id.bars_network_traffic_in_icon);

        mOutValueTextView = (TextView) findViewById(R.id.bars_network_traffic_out_value_text);
        mOutUnitTextView = (TextView) findViewById(R.id.bars_network_traffic_out_unit_text);
        mInValueTextView = (TextView) findViewById(R.id.bars_network_traffic_in_value_text);
        mInUnitTextView = (TextView) findViewById(R.id.bars_network_traffic_in_unit_text);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mOutUnitTextView.setY(mOutValueTextView.getBottom() - mOutUnitTextView.getHeight());
        mInUnitTextView.setY(mInValueTextView.getBottom() - mInUnitTextView.getHeight());
    }

    public void setNetworkTrafficController(NetworkTrafficController ntc) {
        mNetworkTrafficController = ntc;
    }

    public void setListening(boolean listening) {
        setVisibility(listening ? View.VISIBLE : View.GONE);
        if (mNetworkTrafficController == null || mListening == listening) {
            return;
        }
        mListening = listening;
        if (mListening) {
            mNetworkTrafficController.addCallback(this);
        } else {
            mNetworkTrafficController.removeCallback(this);
        }
    }

    @Override
    public void onNetworkTrafficChanged(NetworkTrafficController.Traffic traffic) {
        mOutIconView.setVisibility(traffic.activityOut ? View.VISIBLE : View.INVISIBLE);
        mInIconView.setVisibility(traffic.activityIn ? View.VISIBLE : View.INVISIBLE);
        mOutValueTextView.setText(mBitByte == mTrafficInBytes ?
                traffic.outSpeedInBytes : traffic.outSpeedInBits);
        mOutUnitTextView .setText(mBitByte == mTrafficInBytes ?
                traffic.outUnitAsBytes : traffic.outUnitAsBits);
        mInValueTextView.setText(mBitByte == mTrafficInBytes ?
                traffic.inSpeedInBytes : traffic.inSpeedInBits);
        mInUnitTextView.setText(mBitByte == mTrafficInBytes ?
                traffic.inUnitAsBytes : traffic.inUnitAsBits);

    }

    public void setBitByte(int bitByte) {
        mBitByte = bitByte;
    }

    public void setIconColor(int color) {
        mOutIconView.setImageTintList(ColorStateList.valueOf(color));
        mInIconView.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setTextColors(int primaryColor, int secondaryColor) {
        mOutValueTextView.setTextColor(primaryColor);
        mInValueTextView.setTextColor(primaryColor);
        mOutUnitTextView.setTextColor(secondaryColor);
        mInUnitTextView.setTextColor(secondaryColor);
    }
}
