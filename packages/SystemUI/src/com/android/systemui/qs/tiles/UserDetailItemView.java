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

package com.android.systemui.qs.tiles;

import com.android.internal.util.ArrayUtils;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.darkkat.util.QSColorHelper;
import com.android.systemui.statusbar.phone.UserAvatarView;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Displays one user in the {@link UserDetailView} view.
 */
public class UserDetailItemView extends LinearLayout {

    private UserAvatarView mAvatar;
    private TextView mName;
    private Typeface mRegularTypeface;
    private Typeface mActivatedTypeface;

    public UserDetailItemView(Context context) {
        this(context, null);
    }

    public UserDetailItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserDetailItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public UserDetailItemView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.UserDetailItemView, defStyleAttr, defStyleRes);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.UserDetailItemView_regularFontFamily:
                    mRegularTypeface = Typeface.create(a.getString(attr), 0 /* style */);
                    break;
                case R.styleable.UserDetailItemView_activatedFontFamily:
                    mActivatedTypeface = Typeface.create(a.getString(attr), 0 /* style */);
                    break;
            }
        }
        a.recycle();
    }

    public static UserDetailItemView convertOrInflate(Context context, View convertView,
            ViewGroup root) {
        if (!(convertView instanceof UserDetailItemView)) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.qs_user_detail_item, root, false);
        }
        return (UserDetailItemView) convertView;
    }

    public void bind(String name, Bitmap picture) {
        mName.setText(name);
        mAvatar.setBitmap(picture);
    }

    public void bind(String name, Bitmap picture, boolean isCurrent) {
        mName.setText(name);
        if (!isCurrent) {
            mName.setTextColor((mName.getCurrentTextColor() & 0xff000000)
                    | (QSColorHelper.getTextColor(mContext) & 0x00ffffff));
        } else {
            mName.setTextColor(QSColorHelper.getAccentColor(mContext));
        }
        mAvatar.setBitmap(picture);
        mAvatar.setActiveFrameColor(QSColorHelper.getAccentColor(mContext));
    }

    public void bind(String name, Drawable picture) {
        mName.setText(name);
        mAvatar.setDrawable(picture);
    }

    public void bind(String name, Drawable picture, boolean isCurrent, boolean colorize) {
        int color = colorize ? QSColorHelper.getIconColor(mContext) : 0;
        mName.setText(name);
        if (!isCurrent) {
            mName.setTextColor((mName.getCurrentTextColor() & 0xff000000)
                    | (QSColorHelper.getTextColor(mContext) & 0x00ffffff));
        } else {
            mName.setTextColor(QSColorHelper.getAccentColor(mContext));
        }
        mAvatar.setDrawable(picture, color);
        mAvatar.setActiveFrameColor(QSColorHelper.getAccentColor(mContext));
    }

    @Override
    protected void onFinishInflate() {
        mAvatar = (UserAvatarView) findViewById(R.id.user_picture);
        mName = (TextView) findViewById(R.id.user_name);
        if (mRegularTypeface == null) {
            mRegularTypeface = mName.getTypeface();
        }
        if (mActivatedTypeface == null) {
            mActivatedTypeface = mName.getTypeface();
        }
        updateTypeface();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mName, R.dimen.qs_detail_item_secondary_text_size);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTypeface();
    }

    private void updateTypeface() {
        boolean activated = ArrayUtils.contains(getDrawableState(), android.R.attr.state_activated);
        mName.setTypeface(activated ? mActivatedTypeface : mRegularTypeface);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
