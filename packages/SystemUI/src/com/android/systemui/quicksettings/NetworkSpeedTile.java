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

public class NetworkSpeedTile extends QuickSettingsTile {
    private boolean mEnabled = false;

    public NetworkSpeedTile(Context context, QuickSettingsController qsc) {
        super(context, qsc);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change the system setting
                Settings.System.putIntForUser(mContext.getContentResolver(),
                        Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR,
                        mEnabled ? 0 : 1, UserHandle.USER_CURRENT);
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
                Settings.System.STATUS_BAR_NETWORK_SPEED_INDICATOR), this);
    }

    @Override
    void onPostCreate() {
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        mEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR, 0,
            UserHandle.USER_CURRENT) == 1;
        int state = Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.STATUS_BAR_NETWORK_SPEED_INDICATOR, 2,
            UserHandle.USER_CURRENT);

        if (mEnabled) {
            mDrawableColor = mDrawableEnabledColor;
            mLabel = mContext.getString(R.string.quick_settings_network_speed);
            if (state == 0) {
                mDrawable = R.drawable.ic_qs_network_traffic_down;
            } else if (state == 1) {
                mDrawable = R.drawable.ic_qs_network_traffic_up;
            } else {
                mDrawable = R.drawable.ic_qs_network_traffic_updown;
            }
        } else {
            mDrawable = R.drawable.ic_qs_network_traffic_updown;
            mDrawableColor = mDrawableDisabledColor;
            mLabel = mContext.getString(R.string.quick_settings_network_speed_off);
        }
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateResources();
    }
}
