/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.systemui.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.internal.util.darkkat.SBEPanelColorHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

public class ToggleSlider extends RelativeLayout {
    public interface Listener {
        public void onInit(ToggleSlider v);
        public void onChanged(ToggleSlider v, boolean tracking, boolean checked, int value,
                boolean stopTracking);
    }

    private Listener mListener;
    private boolean mTracking;

    private CompoundButton mToggle;
    private ToggleSeekBar mSlider;
    private TextView mLabel;

    private ToggleSlider mMirror;
    private BrightnessMirrorController mMirrorController;

    public ToggleSlider(Context context) {
        this(context, null);
    }

    public ToggleSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        View.inflate(context, R.layout.status_bar_toggle_slider, this);

        final Resources res = context.getResources();
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ToggleSlider, defStyle, 0);

        final boolean isOnExpandedBar = a.getBoolean(R.styleable.ToggleSlider_isOnExpandedBar, false);

        mToggle = (CompoundButton) findViewById(R.id.toggle);
        mToggle.setOnCheckedChangeListener(mCheckListener);

        mSlider = (ToggleSeekBar) findViewById(R.id.slider);
        mSlider.setOnSeekBarChangeListener(mSeekListener);

        if (isOnExpandedBar) {
            final int leftPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.brightness_slider_bar_toggle_slider_left_padding);
            mSlider.setPadding(leftPadding, 0, 0, 0);
        }

        mLabel = (TextView) findViewById(R.id.label);
        mLabel.setText(a.getString(R.styleable.ToggleSlider_text));

        mSlider.setAccessibilityLabel(getContentDescription().toString());

        a.recycle();
    }

    public void setMirror(ToggleSlider toggleSlider) {
        mMirror = toggleSlider;
        if (mMirror != null) {
            mMirror.setChecked(mToggle.isChecked());
            mMirror.setMax(mSlider.getMax());
            mMirror.setValue(mSlider.getProgress());
        }
    }

    public void setMirrorController(BrightnessMirrorController c) {
        mMirrorController = c;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mListener != null) {
            mListener.onInit(this);
        }
    }

    public void setOnChangedListener(Listener l) {
        mListener = l;
    }

    public void setChecked(boolean checked) {
        mToggle.setChecked(checked);
    }

    public boolean isChecked() {
        return mToggle.isChecked();
    }

    public void setMax(int max) {
        mSlider.setMax(max);
        if (mMirror != null) {
            mMirror.setMax(max);
        }
    }

    public void setValue(int value) {
        mSlider.setProgress(value);
        if (mMirror != null) {
            mMirror.setValue(value);
        }
    }

    public void setIconColor() {
        final int iconColor = SBEPanelColorHelper.getIconColor(mContext);
        final int progressBarColor = 0xff009688; // Material deep teal 500
        final int progressBarBgColor = (179 << 24) | (iconColor & 0x00ffffff); // Icon color with a transparency of 70%
        mSlider.setThumbTintMode(Mode.MULTIPLY);
        mSlider.setThumbTintList(ColorStateList.valueOf(iconColor));
        mSlider.setProgressTintList(ColorStateList.valueOf(progressBarColor));
        mSlider.setProgressBackgroundTintList(
                ColorStateList.valueOf(progressBarBgColor));
    }

    public void setRippleColor(int color) {
        RippleDrawable rd =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_borderless).mutate();
        rd.setColor(ColorStateList.valueOf(color));
        mSlider.setBackground(rd);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mMirror != null) {
            MotionEvent copy = ev.copy();
            mMirror.dispatchTouchEvent(copy);
            copy.recycle();
        }
        return super.dispatchTouchEvent(ev);
    }

    private final OnCheckedChangeListener mCheckListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton toggle, boolean checked) {
            mSlider.setEnabled(!checked);

            if (mListener != null) {
                mListener.onChanged(
                        ToggleSlider.this, mTracking, checked, mSlider.getProgress(), false);
            }

            if (mMirror != null) {
                mMirror.mToggle.setChecked(checked);
            }
        }
    };

    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mListener != null) {
                mListener.onChanged(
                        ToggleSlider.this, mTracking, mToggle.isChecked(), progress, false);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mTracking = true;

            if (mListener != null) {
                mListener.onChanged(ToggleSlider.this, mTracking, mToggle.isChecked(),
                        mSlider.getProgress(), false);
            }

            mToggle.setChecked(false);

            if (mMirrorController != null) {
                mMirrorController.showMirror();
                mMirrorController.setLocation((View) getParent());
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mTracking = false;

            if (mListener != null) {
                mListener.onChanged(ToggleSlider.this, mTracking, mToggle.isChecked(),
                        mSlider.getProgress(), true);
            }

            if (mMirrorController != null) {
                mMirrorController.hideMirror();
            }
        }
    };
}

