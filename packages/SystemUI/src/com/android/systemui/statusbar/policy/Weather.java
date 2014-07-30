
package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.darkkat.ButtonsConstants;
import com.android.internal.util.darkkat.DkActions;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class Weather extends LinearLayout {

    private static final String TAG = "Weather";

    private boolean mAttached;

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_HIGH = "todays_high";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LAST_UPDATE = "timestamp";
    public static final String EXTRA_CONDITION_CODE = "condition_code";

    public static final int STYLE_PANEL = 0;
    public static final int STYLE_BAR = 1;
    public static final int ICON_STYLE_MONOCHROME = 0;
    public static final int ICON_STYLE_COLOR = 1;

    private TextView mWeatherBarText;
    private LinearLayout mWeatherPanelTopBar;
    private LinearLayout mWeatherPanelLeft;
    private LinearLayout mWeatherPanelRight;
    private LinearLayout mWeatherPanel;
    private LinearLayout mLayoutConditionImage;
    private TextView mCity;
    private ImageView mRefreshButton;
    private ImageView mCollapseButton;
    private ImageView mSettingsButton;
    private ImageView mWeatherDivider;
    private TextView mCurrentTemp;
    private TextView mTemp;
    private TextView mCondition;
    private ImageView mConditionImage;
    private TextView mHumidity;
    private TextView mWinds;
    private TextView mTimestamp;
    private String mCondition_code = "";

    private int mWeatherStyle;
    private int mIconType;
    private boolean mShowLocation;
    private boolean mShowWinds;
    private boolean mShowHumidity;
    private boolean mShowTimestamp;

    private String mClickSettings;
    private String mClickLeftPanel;
    private String mLongClickLeftPanel;
    private String mClickImage;
    private String mLongClickImage;
    private String mClickRightPanel;
    private String mLongClickRightPanel;

    private int mBgColor;
    private int mBgPressedColor;
    private int mIconColor;
    private int mButtonIconColor;
    private int mTextColor;

    private Intent mIntent = null;
    private SettingsObserver mSettingsObserver;
    private ContentResolver mResolver;

    BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIntent = intent;
            updateWeather(mIntent);
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_WIND),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_HUMIDITY),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_TIMESTAMP),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_TOP_BAR_SETTINGS),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_LEFT_PANEL),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_LEFT_PANEL),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_IMAGE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_IMAGE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_RIGHT_PANEL),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_RIGHT_PANEL),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_BUTTON_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_PRESSED_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_WEATHER_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private View.OnClickListener mWeatherOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.button_refresh) {
                refreshWeather(v);
            } else if (v.getId() == R.id.button_collapse) {
                updateWeatherStyle(STYLE_BAR);
                Settings.System.putInt(mResolver,
                        Settings.System.STATUS_BAR_EXPANDED_WEATHER_STYLE, STYLE_BAR);
            } else if (v.getId() == R.id.button_settings) {
                vibrate();
                DkActions.processAction(mContext, mClickSettings, false);
            } else if (v.getId() == R.id.weather_bar_text) {
                updateWeatherStyle(STYLE_PANEL);
                Settings.System.putInt(mResolver,
                        Settings.System.STATUS_BAR_EXPANDED_WEATHER_STYLE, STYLE_PANEL);
            } else if (v.getId() == R.id.weather_panel_left) {
                if (mClickLeftPanel.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                } else if (mClickLeftPanel.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mClickLeftPanel, false);
                }
            } else if (v.getId() == R.id.layout_condition_image) {
                if (mClickImage.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                } else if (mClickImage.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mClickImage, false);
                }
            } else if (v.getId() == R.id.weather_panel_right) {
                if (mClickRightPanel.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                } else if (mClickRightPanel.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mClickRightPanel, false);
                }
            }
        }
    };

    private View.OnLongClickListener mWeatherLongListener =
            new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (v.getId() == R.id.weather_panel_left) {
                if (mLongClickLeftPanel.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                    return true;
                } else if (mLongClickLeftPanel.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mLongClickLeftPanel, false);
                }
            } else if (v.getId() == R.id.layout_condition_image) {
                if (mLongClickImage.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                return true;
                } else if (mLongClickImage.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mLongClickImage, false);
                }
            } else if (v.getId() == R.id.weather_panel_right) {
                if (mLongClickRightPanel.equals(ButtonsConstants.ACTION_NULL)) {
                    // do nothing
                    return true;
                } else if (mLongClickRightPanel.equals(ButtonsConstants.ACTION_REFRESH_WEATHER)) {
                    refreshWeather(v);
                } else {
                    DkActions.processAction(mContext, mLongClickRightPanel, false);
                }
            }
            return true;
        }
    };

    public Weather(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = mContext.getContentResolver();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWeatherBarText = (TextView) this.findViewById(R.id.weather_bar_text);
        mWeatherPanelTopBar = (LinearLayout) this.findViewById(R.id.weather_panel_top_bar);
        mWeatherPanel = (LinearLayout) this.findViewById(R.id.weather_panel);
        mWeatherPanelLeft = (LinearLayout) this.findViewById(R.id.weather_panel_left);
        mWeatherPanelRight = (LinearLayout) this.findViewById(R.id.weather_panel_right);
        mLayoutConditionImage = (LinearLayout) this.findViewById(R.id.layout_condition_image);
        mRefreshButton = (ImageView) this.findViewById(R.id.button_refresh);
        mCollapseButton = (ImageView) this.findViewById(R.id.button_collapse);
        mSettingsButton = (ImageView) this.findViewById(R.id.button_settings);
        mWeatherDivider = (ImageView) this.findViewById(R.id.weather_divider);
        mCity = (TextView) this.findViewById(R.id.city);
        mCurrentTemp = (TextView) this.findViewById(R.id.current_temp);
        mTemp = (TextView) this.findViewById(R.id.high_low_temp);
        mCondition = (TextView) this.findViewById(R.id.condition);
        mConditionImage = (ImageView) this.findViewById(R.id.condition_image);
        mHumidity = (TextView) this.findViewById(R.id.humidity);
        mWinds = (TextView) this.findViewById(R.id.winds);
        mTimestamp = (TextView) this.findViewById(R.id.timestamp);

        if (!mAttached) {
            mAttached = true;
            mWeatherBarText.setOnClickListener(mWeatherOnClickListener);
            mRefreshButton.setOnClickListener(mWeatherOnClickListener);
            mCollapseButton.setOnClickListener(mWeatherOnClickListener);
            mSettingsButton.setOnClickListener(mWeatherOnClickListener);
            mWeatherPanelLeft.setOnClickListener(mWeatherOnClickListener);
            mWeatherPanelRight.setOnClickListener(mWeatherOnClickListener);
            mWeatherPanelLeft.setOnLongClickListener(mWeatherLongListener);
            mWeatherPanelRight.setOnLongClickListener(mWeatherLongListener);
            mLayoutConditionImage.setOnClickListener(mWeatherOnClickListener);
            mLayoutConditionImage.setOnLongClickListener(mWeatherLongListener);
            IntentFilter filter = new IntentFilter("com.android.settings.INTENT_WEATHER_UPDATE");
            mContext.registerReceiver(weatherReceiver, filter, null, getHandler());
            if (mSettingsObserver == null) {
                mSettingsObserver = new SettingsObserver(new Handler());
            }
            mSettingsObserver.observe();
            updateSettings();
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mResolver.unregisterContentObserver(mSettingsObserver);
            mContext.unregisterReceiver(weatherReceiver);
            mAttached = false;
        }
    }

    private void refreshWeather(View v) {
        vibrate();
        Intent weatherintent = new Intent("com.android.settings.INTENT_WEATHER_REQUEST");
        weatherintent.putExtra("com.android.settings.INTENT_EXTRA_TYPE", "updateweather");
        weatherintent.putExtra("com.android.settings.INTENT_EXTRA_ISMANUAL", true);
        v.getContext().sendBroadcast(weatherintent);
    }

    public void updateWeather(Intent intent) {
        mCondition_code = (String) intent.getCharSequenceExtra(EXTRA_CONDITION_CODE);

        mWeatherBarText.setText(setWeatherBarText(intent));
        mCity.setText(intent.getCharSequenceExtra(EXTRA_CITY));
        mCurrentTemp.setText(intent.getCharSequenceExtra(EXTRA_TEMP));
        mTemp.setText(setTemp(intent));
        mCondition.setText(intent.getCharSequenceExtra(EXTRA_CONDITION));
        int level = 100;
        try {
            level = Integer.parseInt(mCondition_code);
        } catch (Exception e) {
        }
        mConditionImage.setImageLevel(level);
        mHumidity.setText(intent.getCharSequenceExtra(EXTRA_HUMIDITY));
        mWinds.setText(intent.getCharSequenceExtra(EXTRA_WIND));
        mTimestamp.setText(intent.getCharSequenceExtra(EXTRA_LAST_UPDATE));
    }

    private CharSequence setWeatherBarText(Intent intent) {
        String text = ((mShowLocation ? (String) intent.getCharSequenceExtra(EXTRA_CITY) + ", " : "")
                + (String) intent.getCharSequenceExtra(EXTRA_TEMP) + ", " 
                + (String) intent.getCharSequenceExtra(EXTRA_CONDITION));
        return text;
    }

    private CharSequence setTemp(Intent intent) {
        String text =
                (String) intent.getCharSequenceExtra(EXTRA_HIGH) + "/"
                + (String) intent.getCharSequenceExtra(EXTRA_LOW);
        return text;
    }

    private void updateSettings() {
        mWeatherStyle = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_STYLE, 0,
                UserHandle.USER_CURRENT);
        mIconType = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON, 0,
                UserHandle.USER_CURRENT);
        mShowLocation = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_LOCATION, 0,
                UserHandle.USER_CURRENT) == 1;
        mShowHumidity = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_HUMIDITY, 0,
                UserHandle.USER_CURRENT) == 1;
        mShowWinds = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_WIND, 0,
                UserHandle.USER_CURRENT) == 1;
        mShowTimestamp = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_SHOW_TIMESTAMP, 0,
                UserHandle.USER_CURRENT) == 1;
        mClickSettings = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_TOP_BAR_SETTINGS,
                UserHandle.USER_CURRENT);
        mClickLeftPanel = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_LEFT_PANEL,
                UserHandle.USER_CURRENT);
        mLongClickLeftPanel = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_LEFT_PANEL,
                UserHandle.USER_CURRENT);
        mClickImage = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_IMAGE,
                UserHandle.USER_CURRENT);
        mLongClickImage = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_IMAGE,
                UserHandle.USER_CURRENT);
        mClickRightPanel = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_CLICK_RIGHT_PANEL,
                UserHandle.USER_CURRENT);
        mLongClickRightPanel = Settings.System.getStringForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_LONG_CLICK_RIGHT_PANEL,
                UserHandle.USER_CURRENT);
        mBgColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mBgPressedColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_BACKGROUND_PRESSED_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mIconColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_ICON_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mButtonIconColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_BUTTON_ICON_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mTextColor = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_WEATHER_TEXT_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);

        if (mClickSettings == null || mClickSettings == "") {
            mClickSettings = ButtonsConstants.ACTION_WEATHER_SETTINGS;
        }
        if (mClickLeftPanel == null || mClickLeftPanel == "") {
            mClickLeftPanel = ButtonsConstants.ACTION_NULL;
        }
        if (mLongClickLeftPanel == null || mLongClickLeftPanel == "") {
            mLongClickLeftPanel = ButtonsConstants.ACTION_NULL;
        }
        if (mClickImage == null || mClickImage == "") {
            mClickImage = ButtonsConstants.ACTION_NULL;
        }
        if (mLongClickImage == null || mLongClickImage == "") {
            mLongClickImage = ButtonsConstants.ACTION_NULL;
        }
        if (mClickRightPanel == null || mClickRightPanel == "") {
            mClickRightPanel = ButtonsConstants.ACTION_NULL;
        }
        if (mLongClickRightPanel == null || mLongClickRightPanel == "") {
            mLongClickRightPanel = ButtonsConstants.ACTION_NULL;
        }

        updateWeatherStyle(mWeatherStyle);
        updateConditionImage(mIconType);
        updateAdditionalInfoVisibility();
        updateBackground();
        updateButtonColors();
        setTextColor(mTextColor);
        if (mIntent != null) {
            updateWeather(mIntent);
        }
    }

    private void updateWeatherStyle(int style) {
        if (style == STYLE_PANEL) {
            mWeatherBarText.setVisibility(View.GONE);
            mWeatherBarText.setClickable(false);
            mWeatherPanelTopBar.setVisibility(View.VISIBLE);
            mWeatherPanelTopBar.setClickable(true);
            mWeatherPanel.setVisibility(View.VISIBLE);
            mWeatherPanel.setClickable(true);
            mLayoutConditionImage.setClickable(true);
            mWeatherDivider.setVisibility(View.VISIBLE);
        } else {
            mWeatherDivider.setVisibility(View.GONE);
            mLayoutConditionImage.setClickable(false);
            mWeatherPanel.setVisibility(View.GONE);
            mWeatherPanel.setClickable(false);
            mWeatherPanelTopBar.setVisibility(View.GONE);
            mWeatherPanelTopBar.setClickable(false);
            mWeatherBarText.setVisibility(View.VISIBLE);
            mWeatherBarText.setClickable(true);
        }
    }

    private void updateConditionImage(int iconStyle) {
        if (mIconType == ICON_STYLE_MONOCHROME) {
            mConditionImage.setImageResource(R.drawable.weather_condition);
            mConditionImage.setColorFilter(mIconColor, Mode.MULTIPLY);
        } else if (mIconType == ICON_STYLE_COLOR) {
            mConditionImage.setImageResource(R.drawable.weather_condition_color);
            mConditionImage.setColorFilter(null);
        } else {
            mConditionImage.setImageResource(R.drawable.weather_condition_vclouds);
            mConditionImage.setColorFilter(null);
        }
    }

    private void updateAdditionalInfoVisibility() {
        mCity.setVisibility(mShowLocation ? View.VISIBLE : View.GONE);
        mHumidity.setVisibility(mShowHumidity ? View.VISIBLE : View.GONE);
        mWinds.setVisibility(mShowWinds ? View.VISIBLE : View.GONE);
        mTimestamp.setVisibility(mShowTimestamp ? View.VISIBLE : View.GONE);
    }

    private void updateBackground() {
        ColorDrawable weatherBgDrawable = new ColorDrawable(mBgColor);

        setBackground(weatherBgDrawable);
        mWeatherBarText.getBackground().setColorFilter(mBgPressedColor, Mode.MULTIPLY);
        mWeatherPanelLeft.getBackground().setColorFilter(mBgPressedColor, Mode.MULTIPLY);
        mWeatherPanelRight.getBackground().setColorFilter(mBgPressedColor, Mode.MULTIPLY);
        mLayoutConditionImage.getBackground().setColorFilter(mBgPressedColor, Mode.MULTIPLY);
    }

    private void updateButtonColors() {
        setButtonColors(mRefreshButton, mButtonIconColor);
        setButtonColors(mCollapseButton, mButtonIconColor);
        setButtonColors(mSettingsButton, mButtonIconColor);
    }

    private void setButtonColors(ImageView iv, int buttonIconColor) {
        iv.getBackground().setColorFilter(null);
        iv.setColorFilter(null);
        iv.getBackground().setColorFilter(buttonIconColor, Mode.MULTIPLY);
        iv.setColorFilter(buttonIconColor, Mode.MULTIPLY);
    }

    private void setTextColor(int color) {
        mWeatherBarText.setTextColor(color);
        mCity.setTextColor(color);
        mCurrentTemp.setTextColor(color);
        mTemp.setTextColor(color);
        mCondition.setTextColor(color);
        mHumidity.setTextColor(color);
        mWinds.setTextColor(color);
        mTimestamp.setTextColor(color);
    }

    private void vibrate() {
        android.os.Vibrator vib = (android.os.Vibrator)mContext.getSystemService(
                Context.VIBRATOR_SERVICE);
        vib.vibrate(50);
    }
}
