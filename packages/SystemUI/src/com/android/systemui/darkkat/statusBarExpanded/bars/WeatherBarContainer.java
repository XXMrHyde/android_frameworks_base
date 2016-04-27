/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.darkkat.statusBarExpanded.bars;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.SystemUIDialog;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.cm.WeatherController.DayForecast;
import com.android.internal.util.darkkat.DeviceUtils;
import com.android.internal.util.darkkat.SBEPanelColorHelper;
import com.android.internal.util.darkkat.WeatherHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherBarContainer extends FrameLayout implements
        WeatherController.Callback {

    private final Context mContext;
    private PhoneStatusBar mStatusBar;
    private WeatherController mWeatherController;

    private LinearLayout mWeatherBar;
    private RelativeLayout mNoWeatherLayout;

    private ImageView mLockClockIcon;
    private TextView mNoWeatherPrimaryText;
    private TextView mNoWeatherSecondaryText;
    private ImageView mWeatherSettingsButton;
    private ImageView mWeatherSettingsIcon;

    private boolean mWeatherAvailable = false;
    private boolean mListening = false;

    public WeatherBarContainer(Context context) {
        this(context, null);
    }

    public WeatherBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWeatherBar = 
                (LinearLayout) findViewById(R.id.status_bar_expanded_weather_bar);
        mNoWeatherLayout = 
                (RelativeLayout) findViewById(R.id.weather_bar_no_weather_info_layout);
        mLockClockIcon = (ImageView) findViewById(R.id.weather_lock_clock_icon);
        mNoWeatherPrimaryText = (TextView) findViewById(R.id.no_weather_info_primary_text);
        mNoWeatherSecondaryText = (TextView) findViewById(R.id.no_weather_info_secondary_text);
        mWeatherSettingsButton = (ImageView) findViewById(R.id.weather_settings_button);
        mWeatherSettingsIcon = (ImageView) findViewById(R.id.weather_settings_icon);
    }

    public void setUp(PhoneStatusBar statusBar, WeatherController weather) {
        mStatusBar = statusBar;
        mWeatherController = weather;
        updateWeatherLayouts();
    }

    public void setListening(boolean listening) {
        updateWeatherLayouts();
        if (!mStatusBar.isLockClockInstalledAndEnabled() || mWeatherController == null) {
            return;
        }
        if (listening && !mListening) {
            mListening = true;
            mWeatherController.addCallback(this);
        }
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            mWeatherAvailable = true;
        } else {
            mWeatherAvailable = false;
        }
        updateWeatherLayouts();
        if (mWeatherAvailable) {
            createItems(info);
        }
    }

    private void updateWeatherLayouts() {
        if (WeatherHelper.getLockClockAvailability(mContext)
                == WeatherHelper.LOCK_CLOCK_MISSING) {
            mNoWeatherLayout.setVisibility(View.VISIBLE);
            LinearLayout noWeatherTextLayout = 
                    (LinearLayout) findViewById(R.id.no_weather_info_text_layout);
            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) noWeatherTextLayout.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.removeRule(RelativeLayout.START_OF);
            mNoWeatherPrimaryText.setText(R.string.weather_lock_clock_missing_title);
            mNoWeatherSecondaryText.setText(R.string.weather_lock_clock_missing_summary);
            mWeatherSettingsButton.setVisibility(View.INVISIBLE);
            mWeatherSettingsIcon.setVisibility(View.INVISIBLE);
            mWeatherSettingsButton.setOnClickListener(null);
            mWeatherBar.setOnLongClickListener(null);
            mWeatherBar.setVisibility(View.GONE);
        } else if (WeatherHelper.getLockClockAvailability(mContext)
                == WeatherHelper.LOCK_CLOCK_DISABLED) {
            mNoWeatherLayout.setVisibility(View.VISIBLE);
            LinearLayout noWeatherTextLayout = 
                    (LinearLayout) findViewById(R.id.no_weather_info_text_layout);
            FrameLayout noWeatherSettingsButtonLayout = 
                    (FrameLayout) findViewById(R.id.no_weather_settings_button_layout);
            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) noWeatherTextLayout.getLayoutParams();
            int rule = noWeatherSettingsButtonLayout.getId();
            lp.addRule(RelativeLayout.START_OF, rule);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
            noWeatherTextLayout.setLayoutParams(lp);
            mNoWeatherPrimaryText.setText(R.string.weather_lock_clock_disabled_title);
            mNoWeatherSecondaryText.setText(R.string.weather_lock_clock_disabled_summary);
            mWeatherSettingsButton.setVisibility(View.VISIBLE);
            mWeatherSettingsButton.setImageResource(R.drawable.ic_lock_clock);
            mWeatherSettingsIcon.setVisibility(View.VISIBLE);
            mWeatherSettingsIcon.setImageResource(R.drawable.ic_settings_applications);
            mWeatherSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEnableLockClockDialog();
                }
            });
            mWeatherBar.setOnLongClickListener(null);
            mWeatherBar.setVisibility(View.GONE);
        } else if (!mWeatherAvailable) {
            mNoWeatherLayout.setVisibility(View.VISIBLE);
            LinearLayout noWeatherTextLayout = 
                    (LinearLayout) findViewById(R.id.no_weather_info_text_layout);
            FrameLayout noWeatherSettingsButtonLayout = 
                    (FrameLayout) findViewById(R.id.no_weather_settings_button_layout);
            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) noWeatherTextLayout.getLayoutParams();
            int rule = noWeatherSettingsButtonLayout.getId();
            lp.addRule(RelativeLayout.START_OF, rule);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
            noWeatherTextLayout.setLayoutParams(lp);
            mNoWeatherPrimaryText.setText(R.string.weather_info_not_available_title);
            mNoWeatherSecondaryText.setText(R.string.weather_info_not_available_summary);
            mWeatherSettingsButton.setVisibility(View.VISIBLE);
            mWeatherSettingsButton.setImageResource(R.drawable.ic_weather_settings);
            mWeatherSettingsIcon.setVisibility(View.VISIBLE);
            mWeatherSettingsIcon.setImageResource(R.drawable.tuner);
            mWeatherSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mStatusBar.startActivity(WeatherHelper.getWeatherSettingsIntent(), true);
                }
            });
            mWeatherBar.setOnLongClickListener(null);
            mWeatherBar.setVisibility(View.GONE);
        } else {
            mNoWeatherLayout.setVisibility(View.GONE);
            mWeatherSettingsButton.setOnClickListener(null);
            mWeatherBar.setVisibility(View.VISIBLE);
            mWeatherBar.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                    mStatusBar.startForecastActivity();
                    return true;
                }
            });
        }
        setNoWeatherColors();
    }

    private void createItems(WeatherController.WeatherInfo info) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TimeZone myTimezone = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(myTimezone);
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mWeatherBar.removeAllViews();

        boolean isToday = false;
        if (WeatherHelper.showCurrent(mContext)) {
            View currentItem = inflater.inflate(R.layout.weather_bar_current_item, null);

            TextView updateTime = (TextView) currentItem.findViewById(R.id.weather_update_time);
            updateTime.setText(getUpdateTime(info));
            updateTime.setTextColor(textColorPrimary);
            calendar.roll(Calendar.DAY_OF_WEEK, true);

            ImageView currentImage = (ImageView) currentItem.findViewById(R.id.weather_image);
            Drawable icon = WeatherHelper.getCurrentConditionDrawable(mContext, info)
                    .getConstantState().newDrawable();
            currentImage.setImageDrawable(icon);
            if (WeatherHelper.getIconType(mContext) == 0) {
                currentImage.setColorFilter(iconColor, Mode.MULTIPLY);
            }
            TextView temp = (TextView) currentItem.findViewById(R.id.weather_temp);
            temp.setText(info.temp);
            temp.setTextColor(textColorPrimary);

            mWeatherBar.addView(currentItem,
                  new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            isToday = true;
        }

        ArrayList<DayForecast> forecasts = info.forecasts;

        for (DayForecast d : forecasts) {
            if (!isToday) {
                View forecastItem = inflater.inflate(R.layout.weather_bar_forecast_item, null);

                TextView day = (TextView) forecastItem.findViewById(R.id.forecast_day);
                day.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
                day.setTextColor(textColorPrimary);
                calendar.roll(Calendar.DAY_OF_WEEK, true);

                ImageView image = (ImageView) forecastItem.findViewById(R.id.weather_image);
                Drawable icon = WeatherHelper.getForcastConditionDrawable(mContext, d)
                        .getConstantState().newDrawable();
                image.setImageDrawable(icon);
                if (WeatherHelper.getIconType(mContext) == 0) {
                    image.setColorFilter(iconColor, Mode.MULTIPLY);
                }
                TextView temps = (TextView) forecastItem.findViewById(R.id.forecast_temps);
                temps.setText(isToday ? info.temp : d.low + " | " + d.high);
                temps.setTextColor(isToday ? textColorPrimary : textColorSecondary);

                mWeatherBar.addView(forecastItem,
                      new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            } else {
                isToday = false;
            }
        }
    }

    public void updateItems() {
        if (mWeatherAvailable && mWeatherController != null) {
            createItems(mWeatherController.getWeatherInfo());
        }
        setNoWeatherColors();
    }

    private void setNoWeatherColors() {
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        int lockClockIconColor =  iconColor;
        final int iconColorSecondary = (77 << 24) | (iconColor & 0x00ffffff);
        if (WeatherHelper.getLockClockAvailability(mContext)
                == WeatherHelper.LOCK_CLOCK_MISSING) {
            lockClockIconColor = 0xffff0000;
        } else if (WeatherHelper.getLockClockAvailability(mContext)
                == WeatherHelper.LOCK_CLOCK_DISABLED) {
            lockClockIconColor = 0x77ff0000;
        }
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mLockClockIcon.setColorFilter(lockClockIconColor, Mode.MULTIPLY);
        mNoWeatherPrimaryText.setTextColor(textColorPrimary);
        mNoWeatherSecondaryText.setTextColor(textColorSecondary);
        mWeatherSettingsButton.setColorFilter(iconColor, Mode.MULTIPLY);
        mWeatherSettingsIcon.setColorFilter(iconColorSecondary, Mode.SRC_IN);
    }

    public void setRippleColor() {
        RippleDrawable background =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_rectangle).mutate();
        final ColorStateList color =
                ColorStateList.valueOf(SBEPanelColorHelper.getRippleColor(mContext));
        background.setColor(color);
        mWeatherBar.setBackground(background);
        ((RippleDrawable) mWeatherSettingsButton.getBackground()).setColor(color);


    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private String getUpdateTime(WeatherController.WeatherInfo info) {
        if (info.timeStamp != null) {
            Date lastUpdate = new Date(info.timeStamp);
            StringBuilder sb = new StringBuilder();
            sb.append(DateFormat.getTimeFormat(mContext).format(lastUpdate));
            return sb.toString();
        } else {
            String empty = "";
            return empty;
        }
    }

    private void showEnableLockClockDialog() {
        final CharSequence message = mContext.getString(DeviceUtils.isPhone(mContext)
                ? R.string.enable_lock_clock_dialog_message
                : R.string.enable_lock_clock_dialog_tablet_message);
        final SystemUIDialog d = new SystemUIDialog(mContext, AlertDialog.THEME_MATERIAL_DARK);
        d.setTitle(mContext.getString(R.string.enable_lock_clock_dialog_title));
        d.setMessage(message);
        d.setPositiveButton(R.string.enable_lock_clock_dialog_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStatusBar.startActivity(WeatherHelper.getLockClockAppDetailSettingsIntent(), true);
            }
        });
        d.setNegativeButton(R.string.enable_lock_clock_dialog_cancel, null);
        d.show();
    }
}
