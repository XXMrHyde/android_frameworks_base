/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.DemoMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import libcore.icu.LocaleData;

/**
 * Digital clock for the status bar.
 */
public class Clock extends TextView implements DemoMode {
    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    private int mAmPmStyle = AM_PM_STYLE_GONE;

    public static final int DATE_STYLE_REGULAR = 0;
    public static final int DATE_STYLE_LOWERCASE = 1;
    public static final int DATE_STYLE_UPPERCASE = 2;

    protected int mDateStyle = DATE_STYLE_UPPERCASE;

    private boolean mAttached;
    private Calendar mCalendar;
    private String mClockFormatString;
    private SimpleDateFormat mClockFormat;
    private boolean mIs24 = true;
    private Locale mLocale;

    protected boolean mShowClock;
    protected boolean mShowDate;
    protected boolean mCenterClock;
    protected int mClockColor = 0xffffffff;
    protected boolean mDateSizeSmall;

    private SettingsObserver mSettingsObserver;

    protected class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_CLOCK), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_DATE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_POSITION), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_DATE_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_AM_PM), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE_SIZE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE_STYLE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE_FORMAT), false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_USER_SWITCHED);

            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());

        // Make sure we update to the current time
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(new Handler());
        }
        mSettingsObserver.observe();
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            getContext().getContentResolver().unregisterContentObserver(mSettingsObserver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                if (mClockFormat != null) {
                    mClockFormat.setTimeZone(mCalendar.getTimeZone());
                }
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                final Locale newLocale = getResources().getConfiguration().locale;
                if (! newLocale.equals(mLocale)) {
                    mLocale = newLocale;
                    mClockFormatString = ""; // force refresh
                }
            }
            updateSettings();
        }
    };

    final void updateClock() {
        if (mDemoMode) return;
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
    }

    private final CharSequence getSmallTime() {
        Context context = getContext();
        LocaleData d = LocaleData.get(context.getResources().getConfiguration().locale);

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        SimpleDateFormat sdf;
        String format = mIs24 ? d.timeFormat24 : d.timeFormat12;
        if (!format.equals(mClockFormatString)) {
            /*
             * Search for an unquoted "a" in the format string, so we can
             * add dummy characters around it to let us find it again after
             * formatting and change its size.
             */
            if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
                int a = -1;
                boolean quoted = false;
                for (int i = 0; i < format.length(); i++) {
                    char c = format.charAt(i);

                    if (c == '\'') {
                        quoted = !quoted;
                    }
                    if (!quoted && c == 'a') {
                        a = i;
                        break;
                    }
                }

                if (a >= 0) {
                    // Move a back so any whitespace before AM/PM is also in the alternate size.
                    final int b = a;
                    while (a > 0 && Character.isWhitespace(format.charAt(a-1))) {
                        a--;
                    }
                    format = format.substring(0, a) + MAGIC1 + format.substring(a, b)
                        + "a" + MAGIC2 + format.substring(b + 1);
                }
            }
            mClockFormat = sdf = new SimpleDateFormat(format);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }

        CharSequence dateString = null;

        String result = sdf.format(mCalendar.getTime());

        if (mShowDate) {
            Date now = new Date();

            String dateFormat = Settings.System.getString(getContext().getContentResolver(),
                    Settings.System.STATUS_BAR_DATE_FORMAT);

            if (dateFormat == null || dateFormat.isEmpty()) {
                // Set dateString to short uppercase Weekday (Default for AOKP) if empty
                dateString = DateFormat.format("EEE", now) + " ";
            } else {
                dateString = DateFormat.format(dateFormat, now) + " ";
            }
            if (mDateStyle == DATE_STYLE_LOWERCASE) {
                // When Date style is small, convert date to uppercase
                result = dateString.toString().toLowerCase() + result;
            } else if (mDateStyle == DATE_STYLE_UPPERCASE) {
                result = dateString.toString().toUpperCase() + result;
            } else {
                result = dateString.toString() + result;
            }
        }

        SpannableStringBuilder formatted = new SpannableStringBuilder(result);

        if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
            int magic1 = result.indexOf(MAGIC1);
            int magic2 = result.indexOf(MAGIC2);
            if (magic1 >= 0 && magic2 > magic1) {
                if (mAmPmStyle == AM_PM_STYLE_GONE) {
                    formatted.delete(magic1, magic2+1);
                } else {
                    if (mAmPmStyle == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, magic1, magic2,
                                          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    formatted.delete(magic2, magic2 + 1);
                    formatted.delete(magic1, magic1 + 1);
                }
            }
        }

        if (mDateSizeSmall) {
            if (dateString != null) {
                int dateStringLen = dateString.length();
                if (!mShowDate) {
                    formatted.delete(0, dateStringLen);
                } else {
                    CharacterStyle style = new RelativeSizeSpan(0.7f);
                    formatted.setSpan(style, 0, dateStringLen,
                                      Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                }
            }
        }
        return formatted; 

    }

    private boolean mDemoMode;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            updateClock();
        } else if (mDemoMode && command.equals(COMMAND_CLOCK)) {
            String millis = args.getString("millis");
            String hhmm = args.getString("hhmm");
            if (millis != null) {
                mCalendar.setTimeInMillis(Long.parseLong(millis));
            } else if (hhmm != null && hhmm.length() == 4) {
                int hh = Integer.parseInt(hhmm.substring(0, 2));
                int mm = Integer.parseInt(hhmm.substring(2));
                mCalendar.set(Calendar.HOUR, hh);
                mCalendar.set(Calendar.MINUTE, mm);
            }
            setText(getSmallTime());
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        Context context = getContext();

        mIs24 = DateFormat.is24HourFormat(context);

        mShowClock = Settings.System.getIntForUser(resolver,
			    Settings.System.STATUS_BAR_SHOW_CLOCK, 1, UserHandle.USER_CURRENT) == 1;
        mShowDate = Settings.System.getIntForUser(resolver,
			    Settings.System.STATUS_BAR_SHOW_DATE, 0, UserHandle.USER_CURRENT) == 1;
        mCenterClock = Settings.System.getIntForUser(resolver,
			    Settings.System.STATUS_BAR_CLOCK_POSITION, 0, UserHandle.USER_CURRENT) == 1;
        mClockColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_COLOR, 0xffffffff, UserHandle.USER_CURRENT);
        int amPmStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_AM_PM, AM_PM_STYLE_GONE, UserHandle.USER_CURRENT);
        mDateStyle = Settings.System.getIntForUser(resolver,
			    Settings.System.STATUS_BAR_DATE_STYLE, DATE_STYLE_UPPERCASE, UserHandle.USER_CURRENT);
        mDateSizeSmall = Settings.System.getIntForUser(resolver,
			    Settings.System.STATUS_BAR_DATE_SIZE, 0, UserHandle.USER_CURRENT) == 1;

        if (mIs24) {
            mAmPmStyle = AM_PM_STYLE_GONE;
        } else {
            if (mAmPmStyle != amPmStyle) {
                mAmPmStyle = amPmStyle;
                mClockFormatString = "";
            }
        }

        if (mAttached) {
            setTextColor(mClockColor);
            updateClockVisibility(true);
            updateClock();
        }
    }

    public void updateClockVisibility(boolean show) {
        if (mShowClock && !mCenterClock) {
            setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            setVisibility(View.GONE);
        }
    }
}

