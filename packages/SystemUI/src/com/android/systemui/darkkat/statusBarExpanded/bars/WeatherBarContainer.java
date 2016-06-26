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

import com.android.internal.util.darkkat.DeviceUtils;
import com.android.internal.util.darkkat.SBEPanelColorHelper;
import com.android.internal.util.darkkat.WeatherHelper;
import com.android.internal.util.darkkat.WeatherServiceController;
import com.android.internal.util.darkkat.WeatherServiceController.DayForecast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherBarContainer extends FrameLayout implements
        WeatherServiceController.Callback {

    private final Context mContext;
    private PhoneStatusBar mStatusBar;
    private WeatherServiceController mWeatherController;

    private LinearLayout mWeatherBar;
    private RelativeLayout mNoWeatherLayout;

    private ImageView mDKWeatherServiceIcon;
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
        mDKWeatherServiceIcon = (ImageView) findViewById(R.id.weather_dk_weather_service_icon);
        mNoWeatherPrimaryText = (TextView) findViewById(R.id.no_weather_info_primary_text);
        mNoWeatherSecondaryText = (TextView) findViewById(R.id.no_weather_info_secondary_text);
        mWeatherSettingsButton = (ImageView) findViewById(R.id.weather_settings_button);
        mWeatherSettingsIcon = (ImageView) findViewById(R.id.weather_settings_icon);
    }

    public void setUp(PhoneStatusBar statusBar, WeatherServiceController weather) {
        mStatusBar = statusBar;
        mWeatherController = weather;
        updateWeatherLayouts();
    }

    public void setListening(boolean listening) {
        updateWeatherLayouts();
        if (!WeatherHelper.isWeatherServiceAvailable(mContext) || mWeatherController == null) {
            return;
        }
        if (listening && !mListening) {
            mListening = true;
            mWeatherController.addCallback(this);
        }
    }

    @Override
    public void onWeatherChanged(WeatherServiceController.WeatherInfo info) {
        if (info.formattedTemperature != null && info.condition != null) {
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
        if (WeatherHelper.getWeatherServiceAvailability(mContext)
                == WeatherHelper.PACKAGE_MISSING) {
            mNoWeatherLayout.setVisibility(View.VISIBLE);
            LinearLayout noWeatherTextLayout = 
                    (LinearLayout) findViewById(R.id.no_weather_info_text_layout);
            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) noWeatherTextLayout.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.removeRule(RelativeLayout.START_OF);
            mNoWeatherPrimaryText.setText(R.string.weather_dk_weather_service_missing_title);
            mNoWeatherSecondaryText.setText(R.string.weather_dk_weather_service_missing_summary);
            mWeatherSettingsButton.setVisibility(View.INVISIBLE);
            mWeatherSettingsIcon.setVisibility(View.INVISIBLE);
            mWeatherSettingsButton.setOnClickListener(null);
            mWeatherBar.setVisibility(View.GONE);
        } else if (WeatherHelper.getWeatherServiceAvailability(mContext)
                == WeatherHelper.PACKAGE_DISABLED) {
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
            mNoWeatherPrimaryText.setText(R.string.weather_dk_weather_service_disabled_title);
            mNoWeatherSecondaryText.setText(R.string.weather_dk_weather_service_disabled_summary);
            mWeatherSettingsButton.setVisibility(View.VISIBLE);
            mWeatherSettingsButton.setImageResource(R.drawable.ic_no_weather_service);
            mWeatherSettingsIcon.setVisibility(View.VISIBLE);
            mWeatherSettingsIcon.setImageResource(R.drawable.ic_settings_applications);
            mWeatherSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEnableDKWeatherServiceDialog();
                }
            });
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
                    mStatusBar.startActivity(WeatherHelper.getWeatherServiceSettingsIntent(), true);
                }
            });
            mWeatherBar.setVisibility(View.GONE);
        } else {
            mNoWeatherLayout.setVisibility(View.GONE);
            mWeatherSettingsButton.setOnClickListener(null);
            mWeatherBar.setVisibility(View.VISIBLE);
        }
        setNoWeatherColors();
    }

    private void createItems(WeatherServiceController.WeatherInfo info) {
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
            setItemLongClickAction(currentItem, 0);

            TextView updateTime = (TextView) currentItem.findViewById(R.id.weather_update_time);
            updateTime.setText(getUpdateTime(info.timestamp));
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
            temp.setText(info.formattedTemperature);
            temp.setTextColor(textColorPrimary);

            mWeatherBar.addView(currentItem,
                  new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            isToday = true;
        }

        ArrayList<DayForecast> forecasts = (ArrayList) info.forecasts;
        for (int i = 0; i < forecasts.size(); i++) {
            if (!isToday) {
                DayForecast d = forecasts.get(i);

                View forecastItem = inflater.inflate(R.layout.weather_bar_forecast_item, null);
                setItemLongClickAction(forecastItem, i);

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
                temps.setText(d.temperatureLow + " | " + d.temperatureHigh);
                temps.setTextColor(textColorSecondary);

                mWeatherBar.addView(forecastItem,
                      new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            } else {
                isToday = false;
            }
        }
    }

    public void setItemLongClickAction(View item, final int dayIndex) {
        RippleDrawable background =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_borderless).mutate();
        final ColorStateList color =
                ColorStateList.valueOf(SBEPanelColorHelper.getRippleColor(mContext));
        background.setColor(color);
        item.setBackground(background);
        item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                mStatusBar.showDetailedWeather(dayIndex);
                return true;
            }
        });
    }

    public void updateItems() {
        if (mWeatherAvailable && mWeatherController != null) {
            createItems(mWeatherController.getWeatherInfo());
        }
        setNoWeatherColors();
    }

    private void setNoWeatherColors() {
        final int iconColor =  SBEPanelColorHelper.getIconColor(mContext);
        int dkWeatherServiceIconColor =  iconColor;
        final int iconColorSecondary = (77 << 24) | (iconColor & 0x00ffffff);
        if (WeatherHelper.getWeatherServiceAvailability(mContext)
                == WeatherHelper.PACKAGE_MISSING) {
            dkWeatherServiceIconColor = 0xffff0000;
        } else if (WeatherHelper.getWeatherServiceAvailability(mContext)
                == WeatherHelper.PACKAGE_DISABLED) {
            dkWeatherServiceIconColor = 0x77ff0000;
        }
        final int textColorPrimary = SBEPanelColorHelper.getTextColor(mContext);
        final int textColorSecondary = (179 << 24) | (textColorPrimary & 0x00ffffff);
        mDKWeatherServiceIcon.setColorFilter(dkWeatherServiceIconColor, Mode.MULTIPLY);
        mNoWeatherPrimaryText.setTextColor(textColorPrimary);
        mNoWeatherSecondaryText.setTextColor(textColorSecondary);
        mWeatherSettingsButton.setColorFilter(iconColor, Mode.MULTIPLY);
        mWeatherSettingsIcon.setColorFilter(iconColorSecondary, Mode.SRC_IN);
    }

    public void setRippleColor() {
        final ColorStateList color =
                ColorStateList.valueOf(SBEPanelColorHelper.getRippleColor(mContext));
        ((RippleDrawable) mWeatherSettingsButton.getBackground()).setColor(color);

    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private String getUpdateTime(String timestamp) {
        String updateTime = "";
        if (timestamp != null) {
            updateTime = timestamp;
            Date lastUpdate = new Date(updateTime);
            StringBuilder sb = new StringBuilder();
            sb.append(DateFormat.getTimeFormat(mContext).format(lastUpdate));
            return sb.toString();
        } else {
            return updateTime;
        }
    }

    private void showEnableDKWeatherServiceDialog() {
        final CharSequence message = mContext.getString(DeviceUtils.isPhone(mContext)
                ? R.string.enable_dk_weather_service_dialog_message
                : R.string.enable_dk_weather_service_dialog_tablet_message);
        final SystemUIDialog d = new SystemUIDialog(mContext, AlertDialog.THEME_MATERIAL_DARK);
        d.setTitle(mContext.getString(R.string.enable_dk_weather_service_dialog_title));
        d.setMessage(message);
        d.setPositiveButton(R.string.enable_dk_weather_service_dialog_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStatusBar.startActivity(WeatherHelper.getWeatherServiceAppDetailSettingsIntent(), true);
            }
        });
        d.setNegativeButton(R.string.enable_dk_weather_service_dialog_cancel, null);
        d.show();
    }
}
