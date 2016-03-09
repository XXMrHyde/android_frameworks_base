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

package com.android.systemui.darkkat.statusbar;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.cm.WeatherController;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class StatusBarWeather extends LinearLayout implements
        WeatherController.Callback {

    private static final int WEATHER_TYPE_TEXT      = 0;
    private static final int WEATHER_TYPE_ICON      = 1;
    private static final int WEATHER_TYPE_TEXT_ICON = 2;

    private PhoneStatusBar mStatusBar;
    private WeatherController mWeatherController;

    private TextView mTextView;
    private ImageView mIconView;

    private boolean mShow = false;

    public StatusBarWeather(Context context) {
        this(context, null);
    }

    public StatusBarWeather(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarWeather(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTextView = (TextView) findViewById(R.id.status_bar_weather_text);
        mIconView = (ImageView) findViewById(R.id.status_bar_weather_icon);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mWeatherController != null) {
            mWeatherController.addCallback(this);
        }
    }

    public void setUp(PhoneStatusBar statusBar, WeatherController weather) {
        mStatusBar = statusBar;
        mWeatherController = weather;
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            if (mShow) {
                setVisibility(View.VISIBLE);
            }
            mTextView.setText(info.temp);
            mIconView.setImageDrawable(info.conditionDrawableMonochrome);
        } else {
            setVisibility(View.GONE);
            mTextView.setText("");
            mIconView.setImageDrawable(null);
        }

    }

    public void setShow(boolean show) {
        mShow = show;
        if (mShow && getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
            if (mWeatherController != null) {
                mWeatherController.addCallback(this);
            }
        } else if (!mShow && getVisibility() != View.GONE) {
            setVisibility(View.GONE);
        }
    }

    public void setType(int type) {
        final boolean showText = mShow && type == WEATHER_TYPE_TEXT || type == WEATHER_TYPE_TEXT_ICON;
        final boolean showIcon = mShow && type == WEATHER_TYPE_ICON || type == WEATHER_TYPE_TEXT_ICON;

        mTextView.setVisibility(showText ? View.VISIBLE : View.GONE);
        mIconView.setVisibility(showIcon ? View.VISIBLE : View.GONE);
    }

    public void setTextColor(int color) {
        if (mTextView != null) {
            mTextView.setTextColor(color);
        }
    }

    public void setIconColor(int color) {
        if (mIconView != null) {
            mIconView.setColorFilter(color, Mode.MULTIPLY);
        }

    }
}
