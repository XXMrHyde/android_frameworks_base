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

package com.android.systemui.darkkat.QuickAccess;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.darkkat.QABConstants;
import com.android.internal.util.darkkat.QABHelper;
import com.android.internal.util.darkkat.SBEPanelColorHelper;
import com.android.internal.util.slim.ActionConfig;

import com.android.systemui.R;
import com.android.systemui.darkkat.QuickAccess.buttons.AirplaneModeButton;
import com.android.systemui.darkkat.QuickAccess.buttons.BluetoothButton;
import com.android.systemui.darkkat.QuickAccess.buttons.ColorInversionButton;
import com.android.systemui.darkkat.QuickAccess.buttons.DataButton;
import com.android.systemui.darkkat.QuickAccess.buttons.HotspotButton;
import com.android.systemui.darkkat.QuickAccess.buttons.LocationButton;
import com.android.systemui.darkkat.QuickAccess.buttons.NfcButton;
import com.android.systemui.darkkat.QuickAccess.buttons.QabButton;
import com.android.systemui.darkkat.QuickAccess.buttons.RotationLockButton;
import com.android.systemui.darkkat.QuickAccess.buttons.TorchButton;
import com.android.systemui.darkkat.QuickAccess.buttons.WifiButton;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.LocationController;

import java.util.ArrayList;

public class QuickAccessBar extends LinearLayout {

    private final Context mContext;
    private final Handler mHandler;
    private final SettingsObserver mSettingsObserver;
    private PhoneStatusBar mStatusBar;
    private BluetoothController mBluetoothController;
    private NetworkController mNetworkController;
    private RotationLockController mRotationLockController;
    private LocationController mLocationController;
    private HotspotController mHotspotController;
    private FlashlightController mFlashlightController;
    private ArrayList<QabButton> mButtons;
    private boolean mListening = false;

    public QuickAccessBar(Context context) {
        this(context, null);
    }

    public QuickAccessBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        mButtons = new ArrayList<QabButton>();

        boolean showBar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_SHOW_QAB, 1) == 1;
        setVisibility(showBar ? View.INVISIBLE : View.GONE);
    }

    public void setUp(PhoneStatusBar statusBar, BluetoothController bluetooth, NetworkController network,
            RotationLockController rotationLock, LocationController location, HotspotController hotspot,
             FlashlightController flashlight) {
        mStatusBar = statusBar;
        mBluetoothController = bluetooth;
        mNetworkController = network;
        mRotationLockController = rotationLock;
        mLocationController = location;
        mHotspotController = hotspot;
        mFlashlightController = flashlight;
        createBarButtons();
        mSettingsObserver.observe();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    private void createBarButtons() {
        ArrayList<ActionConfig> actionConfigs =
                QABHelper.getQABarConfig(mContext);
        ActionConfig actionConfig;

        if (mButtons.size() != 0) {
            mButtons.clear();
        }

        for (int i = 0; i < actionConfigs.size(); i++) {
            actionConfig = actionConfigs.get(i);
            final String button = actionConfig.getClickAction();
            QabButton qabButton = createButton(button);
            setIconColor(qabButton);
            setRippleColor(qabButton);
            addView(qabButton);
            mButtons.add(qabButton);
        }
    }

    public void setListening(boolean listening) {
        if (mButtons.size() == 0 || mListening == listening) return;
        mListening = listening;
        for (int i = 0; i < mButtons.size(); i++) {
            QabButton button = mButtons.get(i);
            button.setListening(mListening);
        }
    }

    private QabButton createButton(String action) {
        QabButton button = null;

        if (action.equals(QABConstants.BUTTON_AIRPLANE)) {
            button = new AirplaneModeButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_airplane),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_airplane_off));
        } else if (action.equals(QABConstants.BUTTON_BLUETOOTH)) {
            button = new BluetoothButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_bt),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_bt_off));
        } else if (action.equals(QABConstants.BUTTON_DATA)) {
            button = new DataButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_data),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_data_off));
        } else if (action.equals(QABConstants.BUTTON_FLASHLIGHT)) {
            button = new TorchButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_torch),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_torch_off));
        } else if (action.equals(QABConstants.BUTTON_HOTSPOT)) {
            button = new HotspotButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_hotspot),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_hotspot_off));
        } else if (action.equals(QABConstants.BUTTON_INVERSION)) {
            button = new ColorInversionButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_inversion),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_inversion_off));
        } else if (action.equals(QABConstants.BUTTON_LOCATION)) {
            button = new LocationButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_location),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_location_off));
        } else if (action.equals(QABConstants.BUTTON_NFC)) {
            button = new NfcButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_nfc),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_nfc_off));
        } else if (action.equals(QABConstants.BUTTON_ROTATION)) {
            button = new RotationLockButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_rotation),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_rotation_off));
        } else if (action.equals(QABConstants.BUTTON_WIFI)) {
            button = new WifiButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_wifi),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_wifi_off));
        }

        int dimens = mContext.getResources().getDimensionPixelSize(R.dimen.qab_button_size);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(dimens, dimens);
        button.setLayoutParams(lp);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setClickable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                ((QabButton) v).handleClick();
            }
        });
        if (!(button instanceof TorchButton)) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                    ((QabButton) v).handleLongClick();
                    return true;
                }
            });
        }
        return button;
    }

    public BluetoothController getBluetoothController() {
        return mBluetoothController;
    }

    public FlashlightController getFlashlightController() {
        return mFlashlightController;
    }

    public HotspotController getHotspotController() {
        return mHotspotController;
    }

    public LocationController getLocationController() {
        return mLocationController;
    }

    public NetworkController getNetworkController() {
        return mNetworkController;
    }

    public RotationLockController getRotationLockController() {
        return mRotationLockController;
    }

    public void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    public void setColors() {
        removeAllViews();
        createBarButtons();
    }

    private void setIconColor(ImageView iv) {
        iv.setColorFilter(SBEPanelColorHelper.getIconColor(mContext), Mode.MULTIPLY);
    }

    private void setRippleColor(ImageView iv) {
        RippleDrawable rd = (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_oval).mutate();

        rd.setColor(ColorStateList.valueOf(SBEPanelColorHelper.getRippleColor(mContext)));
        iv.setBackground(rd);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_EXPANDED_QAB_BUTTONS),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void update() {
            removeAllViews();
            createBarButtons();
        }
    }

    public void startSettingsActivity(final Intent intent) {
        mStatusBar.postStartActivityDismissingKeyguard(intent, 0);
    }
}
