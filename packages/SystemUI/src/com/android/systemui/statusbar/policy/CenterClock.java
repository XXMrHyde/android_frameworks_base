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

import android.content.ContentResolver;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class CenterClock extends Clock {

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
    }
	
    public void updateClockVisibility() {
	ContentResolver resolver = mContext.getContentResolver();

    mShowClock = (Settings.System.getInt(resolver, Settings.System.STATUS_BAR_SHOW_CLOCK, 1) == 1);

    mCenterClock = (Settings.System.getInt(resolver, Settings.System.STATUS_BAR_CLOCK_POSITION, 0) == 1);

	if (mShowClock && mCenterClock)
	    setVisibility(View.VISIBLE);
	else
	    setVisibility(View.GONE);
	
    }
}
