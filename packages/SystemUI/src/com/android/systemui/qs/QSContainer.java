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
import com.android.systemui.R;

/**
 * Wrapper view with background which contains {@link QSPanel}
 */
public class QSContainer extends FrameLayout {

    private HorizontalScrollView mQABarContainer;
    private QuickAccessBar mQABar;
    private QSPanel mQSPanel;

    private int mHeightOverride = -1;
    private final int mPadding;

    private boolean mShowBrightnessSlider;
    private boolean mShowQABar;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ContentResolver resolver = context.getContentResolver();
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.qs_container_padding_top_bottom);
        mShowBrightnessSlider = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_BRIGHTNESS_SLIDER, 1,
                UserHandle.USER_CURRENT) == 1;
        mShowQABar = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB, 1, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQABarContainer =
                (HorizontalScrollView) findViewById(R.id.quick_access_bar_container);
        mQABar = (QuickAccessBar) findViewById(R.id.quick_access_bar);
        mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        mQSPanel.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        mQABarContainer.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        int height = 0;
        if (mShowBrightnessSlider) {
            height += mQSPanel.getMeasuredHeight();
        }
        if (mShowQABar) {
            height += mQABarContainer.getMeasuredHeight();
        }
        if (mShowBrightnessSlider || mShowQABar) {
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
        final int qsBarTop = mPadding + (mShowBrightnessSlider ? qsPanelheight : 0);
        mQSPanel.layout(0, mPadding, mQSPanel.getMeasuredWidth(),
                mPadding + mQSPanel.getMeasuredHeight());
        mQABarContainer.layout(0, qsBarTop, mQABarContainer.getMeasuredWidth(),
                qsBarTop + mQABarContainer.getMeasuredHeight());
        updateBottom();
    }

    public void setShowBrightnessSlider(boolean ShowBrightnessSlider) {
        mShowBrightnessSlider = ShowBrightnessSlider;
        setVisibility(mShowBrightnessSlider || mShowQABar
                ? View.INVISIBLE : View.GONE);
        requestLayout();
    }

    public void setShowQABar(boolean showShowQABar) {
        mShowQABar = showShowQABar;
        if (mShowQABar) {
            mQSPanel.setVisibility(mShowBrightnessSlider ? View.INVISIBLE : View.GONE);
            mQABarContainer.setVisibility(View.INVISIBLE);
            mQABar.setVisibility(View.INVISIBLE);
            setVisibility(View.INVISIBLE);
        } else {
            mQABarContainer.setVisibility(View.GONE);
            mQABar.setVisibility(View.GONE);
            mQSPanel.setVisibility(mShowBrightnessSlider ? View.INVISIBLE : View.GONE);
            setVisibility(mShowBrightnessSlider ? View.INVISIBLE : View.GONE);
        }
        requestLayout();
    }

    public void updateVisibility(boolean keyguardShowing, boolean visible) {
        if (!mShowQABar && !mShowBrightnessSlider) {
            return;
        }
        setVisibility(keyguardShowing && !visible ? View.INVISIBLE : View.VISIBLE);
        if (mShowQABar) {
            mQABarContainer.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
            mQABar.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
            mQSPanel.setVisibility(visible && mShowBrightnessSlider ? View.VISIBLE : View.INVISIBLE);
        } else {
            mQSPanel.setVisibility(mShowBrightnessSlider ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setListening(boolean listening) {
        mQSPanel.setListening(listening && mShowBrightnessSlider);
        mQABar.setListening(listening && mShowQABar);
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
        } else if (!mShowQABar && !mShowBrightnessSlider) {
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
