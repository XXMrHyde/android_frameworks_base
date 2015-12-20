/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.qs;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import com.android.systemui.darkkat.QuickAccess.QuickAccessBar;
import com.android.systemui.darkkat.weather.WeatherBarContainer;
import com.android.systemui.R;

/**
 * Wrapper view with background which contains {@link QSPanel}
 */
public class QSContainer extends FrameLayout {

    private QSPanel mQSPanel;
    private HorizontalScrollView mQABarContainer;
    private QuickAccessBar mQABar;
    private WeatherBarContainer mWeatherBarContainer;

    private int mHeightOverride = -1;
    private final int mPadding;

    private boolean mShowBrightnessSlider;
    private boolean mShowQABar;
    private boolean mShowWeatherBar;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ContentResolver resolver = context.getContentResolver();
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.qs_container_padding_top_bottom);
        mShowBrightnessSlider = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER, 1,
                UserHandle.USER_CURRENT) == 1;
        mShowQABar = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB, 1, UserHandle.USER_CURRENT) == 1;
        mShowWeatherBar = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_WEATHER, 0, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        mQABarContainer =
                (HorizontalScrollView) findViewById(R.id.quick_access_bar_container);
        mQABar = (QuickAccessBar) findViewById(R.id.quick_access_bar);
        mWeatherBarContainer = (WeatherBarContainer) findViewById(R.id.status_bar_expanded_weather_bar_container);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        mQSPanel.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        mQABarContainer.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        mWeatherBarContainer.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        int height = 0;
        if (mShowBrightnessSlider) {
            height += mQSPanel.getMeasuredHeight();
        }
        if (mShowQABar) {
            height += mQABarContainer.getMeasuredHeight();
        }
        if (mShowWeatherBar) {
            height += mWeatherBarContainer.getMeasuredHeight();
        }
        if (mShowBrightnessSlider || mShowQABar || mShowWeatherBar) {
            height += mPadding * 2;
        }
        setMeasuredDimension(width, height);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int qsPanelheight = mQSPanel.getMeasuredHeight();
        final int qaBarheight = mQABarContainer.getMeasuredHeight();
        final int WeatherBarContainerHeight = mWeatherBarContainer.getMeasuredHeight();
        final int qsPanelTop = mPadding;
        final int qaBarTop = qsPanelTop + (mShowBrightnessSlider ? qsPanelheight : 0);
        final int WeatherBarContainerTop = qaBarTop + (mShowQABar ? qaBarheight : 0);
        mQSPanel.layout(0, qsPanelTop, mQSPanel.getMeasuredWidth(),
                qsPanelTop + qsPanelheight);
        mQABarContainer.layout(0, qaBarTop, mQABarContainer.getMeasuredWidth(),
                qaBarTop + qaBarheight);
        mWeatherBarContainer.layout(0, WeatherBarContainerTop, mWeatherBarContainer.getMeasuredWidth(),
                WeatherBarContainerTop + WeatherBarContainerHeight);
        updateBottom();
    }

    public void setShowBrightnessSlider(boolean ShowBrightnessSlider) {
        mShowBrightnessSlider = ShowBrightnessSlider;
        setVisibility(mShowBrightnessSlider || mShowQABar || mShowWeatherBar
                ? View.INVISIBLE : View.GONE);
        mQSPanel.setVisibility(mShowBrightnessSlider ? View.INVISIBLE : View.GONE);
        requestLayout();
    }

    public void setShowQABar(boolean showQABar) {
        mShowQABar = showQABar;
        if (mShowQABar) {
            mQABarContainer.setVisibility(View.INVISIBLE);
            mQABar.setVisibility(View.INVISIBLE);
            setVisibility(View.INVISIBLE);
        } else {
            mQABarContainer.setVisibility(View.GONE);
            mQABar.setVisibility(View.GONE);
            setVisibility(mShowBrightnessSlider || mShowWeatherBar ? View.INVISIBLE : View.GONE);
        }
        requestLayout();
    }

    public void setShowWeatherBar(boolean showWeatherBar) {
        mShowWeatherBar = showWeatherBar;
        if (mShowWeatherBar) {
            mWeatherBarContainer.setVisibility(View.INVISIBLE);
            setVisibility(View.INVISIBLE);
        } else {
            mWeatherBarContainer.setVisibility(View.GONE);
            setVisibility(mShowBrightnessSlider || mShowQABar ? View.INVISIBLE : View.GONE);
        }
        requestLayout();
    }

    public void updateVisibility(boolean keyguardShowing, boolean visible) {
        if (!mShowQABar && !mShowBrightnessSlider && !mShowWeatherBar) {
            return;
        }
        setVisibility(keyguardShowing && !visible ? View.INVISIBLE : View.VISIBLE);
        if (mShowBrightnessSlider) {
            mQSPanel.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (mShowQABar) {
            mQABarContainer.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
            mQABar.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
        }
        if (mShowWeatherBar) {
            mWeatherBarContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setListening(boolean listening) {
        mQSPanel.setListening(listening && mShowBrightnessSlider);
        mQABar.setListening(listening && mShowQABar);
        mWeatherBarContainer.setListening(listening && mShowWeatherBar);
    }

    /**
     * Overrides the height of this view (post-layout), so that the content is clipped to that
     * height and the background is set to that height.
     *
     * @param heightOverride the overridden height
     */
    public void setHeightOverride(int heightOverride) {
        mHeightOverride = heightOverride;
        updateBottom();
    }

    /**
     * The height this view wants to be. This is different from {@link #getMeasuredHeight} such that
     * during closing the detail panel, this already returns the smaller height.
     */
    public int getDesiredHeight() {
        if (mQSPanel.isClosingDetail()) {
            return mQSPanel.getGridHeight() + getPaddingTop() + getPaddingBottom();
        } else if (!mShowQABar && !mShowBrightnessSlider && !mShowWeatherBar) {
            return 0;
        } else {
            return getMeasuredHeight();
        }
    }

    private void updateBottom() {
        int height = mHeightOverride != -1 ? mHeightOverride : getMeasuredHeight();
        setBottom(getTop() + height);
    }
}
