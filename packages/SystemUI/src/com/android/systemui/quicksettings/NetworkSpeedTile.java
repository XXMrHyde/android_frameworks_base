/*
 * Copyright (C) 2014 DarkKat
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

package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.policy.TrafficDl;
import com.android.systemui.statusbar.policy.TrafficUl;

public class NetworkSpeedTile extends QuickSettingsTile {
    private boolean mEnabled = false;

    public NetworkSpeedTile(Context context, QuickSettingsController qsc) {
        super(context, qsc, R.layout.quick_settings_tile_network_speed);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change the system setting
                Settings.System.putIntForUser(mContext.getContentResolver(),
                        Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR, mEnabled ? 0 : 1,
                        UserHandle.USER_CURRENT);
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings",
                    "com.android.settings.Settings$NetworkSpeedIndicatorSettingsActivity");
                startSettingsActivity(intent);
                return true;
            }
        };

        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_UPLOAD), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_DOWNLOAD), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_TRAFFIC_SUMMARY), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_UPLOAD_COLOR), this);
        qsc.registerObservedContent(Settings.System.getUriFor(
                Settings.System.STATUS_BAR_NETWORK_SPEED_DOWNLOAD_COLOR), this);
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        TrafficUl trafficUp = (TrafficUl) mTile.findViewById(R.id.text_ul);
        TrafficDl trafficDown = (TrafficDl) mTile.findViewById(R.id.text_dl);

        if (trafficUp != null) {
            trafficUp.updateSettings();
        }
        if (trafficDown != null) {
            trafficDown.updateSettings();
        }
        updateTile();
    }

    @Override
    void onPostCreate() {
        updateTile();
        super.onPostCreate();
    }

    private synchronized void updateTile() {
        mEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR, 0,
            UserHandle.USER_CURRENT) == 1;
        if (mEnabled) {
            mLabel = mContext.getString(R.string.quick_settings_network_speed);
        } else {
            mLabel = mContext.getString(R.string.quick_settings_network_speed_off);
        }
        updateQuickSettings();
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        if (tv != null) {
            tv.setText(mLabel);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
            tv.setPadding(0, mTileTextPadding, 0, 0);
            tv.setTextColor(mTileTextColor);
        }
    }

}
