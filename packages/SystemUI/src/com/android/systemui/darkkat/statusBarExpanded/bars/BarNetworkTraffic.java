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

    private ImageView mUpIconView;
    private ImageView mDownIconView;

    private TextView mUpValueTextView;
    private TextView mUpUnitTextView;
    private TextView mDownValueTextView;
    private TextView mDownUnitTextView;

    private final int trafficInBytes = 0;
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
        mUpIconView = (ImageView) findViewById(R.id.bars_network_traffic_up_icon);
        mDownIconView = (ImageView) findViewById(R.id.bars_network_traffic_down_icon);

        mUpValueTextView = (TextView) findViewById(R.id.bars_network_traffic_up_value_text);
        mUpUnitTextView = (TextView) findViewById(R.id.bars_network_traffic_up_unit_text);
        mDownValueTextView = (TextView) findViewById(R.id.bars_network_traffic_down_value_text);
        mDownUnitTextView = (TextView) findViewById(R.id.bars_network_traffic_down_unit_text);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mUpUnitTextView.setY(mUpValueTextView.getBottom() - mUpUnitTextView.getHeight());
        mDownUnitTextView.setY(mDownValueTextView.getBottom() - mDownUnitTextView.getHeight());
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
        mUpIconView.setVisibility(traffic.activityUp ? View.VISIBLE : View.INVISIBLE);
        mDownIconView.setVisibility(traffic.activityDown ? View.VISIBLE : View.INVISIBLE);
        mUpValueTextView.setText(mBitByte == trafficInBytes ?
                traffic.upBytesValue : traffic.upBitsValue);
        mUpUnitTextView .setText(mBitByte == trafficInBytes ?
                traffic.upBytesUnit : traffic.upBitsUnit);
        mDownValueTextView.setText(mBitByte == trafficInBytes ?
                traffic.downBytesValue : traffic.downBitsValue);
        mDownUnitTextView.setText(mBitByte == trafficInBytes ?
                traffic.downBytesUnit : traffic.downBitsUnit);

    }

    public void setBitByte(int bitByte) {
        mBitByte = bitByte;
    }

    public void setIconColor(int color) {
        mUpIconView.setImageTintList(ColorStateList.valueOf(color));
        mDownIconView.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setTextColors(int primaryColor, int secondaryColor) {
        mUpValueTextView.setTextColor(primaryColor);
        mDownValueTextView.setTextColor(primaryColor);
        mUpUnitTextView.setTextColor(secondaryColor);
        mDownUnitTextView.setTextColor(secondaryColor);
    }
}
