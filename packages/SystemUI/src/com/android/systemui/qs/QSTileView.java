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
 * limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.darkkat.util.QSColorHelper;
import com.android.systemui.qs.QSTile.AnimationIcon;
import com.android.systemui.qs.QSTile.State;

import java.util.Objects;

/** View that represents a standard quick settings tile. **/
public class QSTileView extends ViewGroup {
    private static final Typeface CONDENSED = Typeface.create("sans-serif-condensed",
            Typeface.NORMAL);

    protected final Context mContext;
    private final int mIconSizePx;
    private final int mTileSpacingPx;
    private int mTilePaddingTopPx;
    private final int mTilePaddingBelowIconPx;

    private final View mIcon;
    private QSDualTileLabel mLabel;
    private Drawable mTileBackground;

    private boolean mShowDetailOnClick;
    private OnClickListener mClickPrimary;
    private OnLongClickListener mLongClick;

    private final H mHandler = new H();

    public QSTileView(Context context) {
        super(context);

        mContext = context;
        final Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        mTileSpacingPx = res.getDimensionPixelSize(R.dimen.qs_tile_spacing);
        mTilePaddingBelowIconPx =  res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        setClipChildren(false);

        mIcon = createIcon();
        mLabel = createLabel();
        mTileBackground = createTileBackground();
        addView(mIcon);
        addView(mLabel);
        setBackground(mTileBackground);

        setClickable(true);
        updateTopPadding();
        setId(View.generateViewId());
    }

    private void updateTopPadding() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top);
        int largePadding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale,
                1.0f, FontSizeUtils.LARGE_TEXT_SCALE) - 1f) / (FontSizeUtils.LARGE_TEXT_SCALE - 1f);
        mTilePaddingTopPx = Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        requestLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size));
    }

    protected View createIcon() {
        final ImageView icon = new ImageView(mContext);
        icon.setId(android.R.id.icon);
        icon.setScaleType(ScaleType.CENTER_INSIDE);
        return icon;
    }

    private QSDualTileLabel createLabel() {
        final Resources res = mContext.getResources();
        final int horizontalPaddingPx = res.getDimensionPixelSize(
                R.dimen.qs_tile_label_padding_horizontal);


        QSDualTileLabel label = new QSDualTileLabel(mContext);
        label.setTextColor();
        label.setPadding(horizontalPaddingPx, 0, horizontalPaddingPx, 0);
        label.setTypeface(CONDENSED);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
        label.setClickable(false);
        return label;
    }

    private Drawable createTileBackground() {
        final int[] attrs = new int[] { android.R.attr.selectableItemBackgroundBorderless };
        final TypedArray ta = mContext.obtainStyledAttributes(attrs);
        final Drawable d = ta.getDrawable(0);
        ta.recycle();
        if (d instanceof RippleDrawable) {
            d.mutate();
            ((RippleDrawable) d).setColor(QSColorHelper.getRippleColorStateList(mContext));
        }
        return d;
    }

    public void init(OnClickListener clickPrimary, OnLongClickListener longClick) {
        mClickPrimary = clickPrimary;
        mLongClick = longClick;

        setOnClickListener(mClickPrimary);
        setOnLongClickListener(mLongClick);
        setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int h = MeasureSpec.getSize(heightMeasureSpec);
        final int iconSpec = exactly(mIconSizePx);
        mIcon.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST), iconSpec);
        mLabel.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
        setMeasuredDimension(w, h);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getMeasuredWidth();
        final int h = getMeasuredHeight();

        int top = 0;
        top += mTileSpacingPx;
        top += mTilePaddingTopPx;
        final int iconLeft = (w - mIcon.getMeasuredWidth()) / 2;
        layout(mIcon, iconLeft, top);
        if (mTileBackground instanceof RippleDrawable) {
            updateRippleSize(w, h);

        }
        top = mIcon.getBottom();
        top += mTilePaddingBelowIconPx;
        layout(mLabel, 0, top);
    }

    private void updateRippleSize(int width, int height) {
        // center the touch feedback on the center of the icon, and dial it down a bit
        final int cx = width / 2;
        final int cy = height / 2;
        final int rad = (int)(mIcon.getHeight() * 1.25f);
        mTileBackground.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }

    protected void handleStateChanged(QSTile.State state) {
        if (mIcon instanceof ImageView) {
            setIcon((ImageView) mIcon, state);
        }
        mLabel.setText(state.label);
        setContentDescription(state.contentDescription);
    }

    protected void setIcon(ImageView iv, QSTile.State state) {
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            Drawable d = state.icon != null ? state.icon.getDrawable(mContext) : null;
            if (d != null && state.autoMirrorDrawable) {
                d.setAutoMirrored(true);
            }
            iv.setImageDrawable(d);
            if (d != null) {
                d.setTint(QSColorHelper.getQSIconColor(mContext));
            }
            iv.setTag(R.id.qs_icon_tag, state.icon);
            if (d instanceof Animatable) {
                Animatable a = (Animatable) d;
                if (state.icon instanceof AnimationIcon && !iv.isShown()) {
                    a.stop(); // skip directly to end state
                }
            }
        }
    }

    public void onStateChanged(QSTile.State state) {
        mHandler.obtainMessage(H.STATE_CHANGED, state).sendToTarget();
    }

    /**
     * Update the accessibility order for this view.
     *
     * @param previousView the view which should be before this one
     * @return the last view in this view which is accessible
     */
    public View updateAccessibilityOrder(View previousView) {
        View firstView;
        View lastView;
        firstView = this;
        lastView = this;
        firstView.setAccessibilityTraversalAfter(previousView.getId());
        return lastView;
    }

    public void setTextColor() {
        mLabel.setTextColor();
    }

    public void setIconColor() {
        if (mIcon instanceof ImageView) {
            if (((ImageView) mIcon).getDrawable() != null) {
                ((ImageView) mIcon).getDrawable().setTint(QSColorHelper.getQSIconColor(mContext));
            }
        } else if (mIcon instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) mIcon).getChildCount(); i++) {
                if (((ViewGroup) mIcon).getChildAt(i) instanceof ImageView) {
                    ImageView iv = (ImageView) ((ViewGroup) mIcon).getChildAt(i);
                    if (iv.getDrawable() != null) {
                        iv.getDrawable().setTint(QSColorHelper.getQSIconColor(mContext));
                    }
                }
            }
        }
        mLabel.setIconColor();
    }

    public void setRippleColor() {
        if (getBackground() instanceof RippleDrawable) {
            ((RippleDrawable) getBackground()).setColor(
                    QSColorHelper.getRippleColorStateList(mContext));
        }
    }

    public void setShowDetailOnClick(boolean show) {
        final boolean changed = show != mShowDetailOnClick;
        mShowDetailOnClick = show;
        if (changed) {
            if (mShowDetailOnClick) {
                mLabel.setFirstLineCaret(mContext.getDrawable(R.drawable.ic_qs_show_detail_on_click));
            } else {
                mLabel.setFirstLineCaret(null);
            }
        }
    }

    private class H extends Handler {
        private static final int STATE_CHANGED = 1;
        public H() {
            super(Looper.getMainLooper());
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STATE_CHANGED) {
                handleStateChanged((State) msg.obj);
            }
        }
    }
}
