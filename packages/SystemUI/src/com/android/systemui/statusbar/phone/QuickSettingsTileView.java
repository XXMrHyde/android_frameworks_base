/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import com.android.systemui.R;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 *
 */
public class QuickSettingsTileView extends FrameLayout {

    private int mColSpan;
    private final int mRowSpan;

    public QuickSettingsTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColSpan = 1;
        mRowSpan = 1;
        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
        setTilebackground();
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    public void setContent(int layoutId, LayoutInflater inflater) {
        inflater.inflate(layoutId, this);
    }

    @Override
    public void setVisibility(int vis) {
        if (QuickSettings.DEBUG_GONE_TILES) {
            if (vis == View.GONE) {
                vis = View.VISIBLE;
                setAlpha(0.25f);
                setEnabled(false);
            } else {
                setAlpha(1f);
                setEnabled(true);
            }
        }
        super.setVisibility(vis);
    }

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                   Settings.System.getUriFor(Settings.System.QAS_ENABLE_THEME_DEFAULT), false, this);
            resolver.registerContentObserver(
                   Settings.System.getUriFor(Settings.System.QAS_TILE_BACKGROUND_COLOR), false, this);
            resolver.registerContentObserver(
                   Settings.System.getUriFor(Settings.System.QAS_TILE_BACKGROUND_ALPHA), false, this);
            setTilebackground();
        }

        @Override
        public void onChange(boolean selfChange) {
            setTilebackground();
        }
    }

    public void setTilebackground() {
        boolean themeDefault = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QAS_ENABLE_THEME_DEFAULT, 0) == 0;
        boolean systemDefault = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QAS_ENABLE_THEME_DEFAULT, 0) == 1;
        boolean customColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QAS_ENABLE_THEME_DEFAULT, 0) == 2;
        int color = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QAS_TILE_BACKGROUND_COLOR, 0xff202020);
        float alpha = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.QAS_TILE_BACKGROUND_ALPHA, 0.0f);

        setBackgroundResource(0);
        setBackgroundResource(R.drawable.qs_tile_background_system);

        if (themeDefault) {
            setBackgroundResource(0);
            setBackgroundResource(R.drawable.qs_tile_background);
        } else if (systemDefault) {
            setBackgroundResource(0);
            setBackgroundResource(R.drawable.qs_tile_background_system);
        } else if (customColor) {
            setBackgroundResource(0);
            setBackgroundResource(R.drawable.qs_tile_background);
            Drawable background = getBackground();
            background.setColorFilter(color, Mode.SRC_ATOP);
            setBackground(background);
        }
        Drawable background = getBackground();
        background.setAlpha(0);
        background.setAlpha((int) ((1-alpha) * 255));
        setBackground(background);
    }
}
