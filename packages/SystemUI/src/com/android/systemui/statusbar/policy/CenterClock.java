/*
* Copyright (C) 2006 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.systemui.statusbar.policy;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

public class CenterClock extends Clock {

    private boolean mHidden;

    protected int mClockColor = com.android.internal.R.color.holo_blue_light;

    protected class SettingsObserver extends ContentObserver {
	    SettingsObserver(Handler handler) {
	        super(handler);
	    }

	    void observe() {
	        ContentResolver resolver = mContext.getContentResolver();
	        resolver.registerContentObserver(Settings.System.getUriFor(
	                Settings.System.STATUS_BAR_SHOW_CLOCK), false, this);
	        resolver.registerContentObserver(Settings.System.getUriFor(
	                Settings.System.STATUS_BAR_CLOCK_POSITION), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_COLOR), false, this);
	    }
        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
	
    public CenterClock(Context context) {
	    this(context, null);
    }
	
    public CenterClock(Context context, AttributeSet attrs) {
	    this(context, attrs, 0);
    }
	
    public CenterClock(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
        if (isClickable()) {
            setOnClickListener(this);
            setOnLongClickListener(this);
        }
        updateSettings();
    }

    public void setHidden(boolean hidden) {
        mHidden = hidden;
        updateVisibility();
    }

    public void updateSettings() {
	    ContentResolver resolver = mContext.getContentResolver();

        mShowClock = (Settings.System.getInt(resolver, Settings.System.STATUS_BAR_SHOW_CLOCK, 1) == 1);

        mCenterClock = (Settings.System.getInt(resolver, Settings.System.STATUS_BAR_CLOCK_POSITION, 0) == 1);

        mClockColor = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_CLOCK_COLOR, 0xff33b5e5);

        if (mClockColor == Integer.MIN_VALUE) {
            // flag to reset the color
            mClockColor = 0xff33b5e5;
        }
        setTextColor(mClockColor);
        updateVisibility();
    }

    public void updateVisibility() {

	    if (mShowClock && mCenterClock) {
	        setVisibility(View.VISIBLE);
	    } else {
	        setVisibility(View.GONE);
        }
    }

    private void collapseStartActivity(Intent what) {
        // collapse status bar
        StatusBarManager statusBarManager = (StatusBarManager) getContext().getSystemService(
                Context.STATUS_BAR_SERVICE);
        statusBarManager.collapsePanels();

        // dismiss keyguard in case it was active and no passcode set
        try {
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (Exception ex) {
            // no action needed here
        }

        // start activity
        what.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(what);
    }

    @Override
    public void onClick(View v) {
        // start com.android.deskclock/.DeskClock
        ComponentName clock = new ComponentName("com.android.deskclock",
                "com.android.deskclock.DeskClock");
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(clock);
        collapseStartActivity(intent);
    }

    @Override
    public boolean onLongClick(View v) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        collapseStartActivity(intent);

        // consume event
        return true;
    }
}
