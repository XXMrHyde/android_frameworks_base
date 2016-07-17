/*
 * Copyright (C) 2016 DarkKat
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

package com.android.systemui.darkkat.statusBarExpanded.bars.quickAccessButtons;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.systemui.darkkat.statusBarExpanded.bars.QuickAccessBar;
import com.android.systemui.R;

public class SyncButton extends QabButton {
    private static final Intent SYNC_SETTINGS = new Intent(Settings.ACTION_SYNC_SETTINGS);

    public SyncButton(Context context, QuickAccessBar bar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, bar, iconEnabled, iconDisabled);

        updateState(isSyncEnabled());
    }

    @Override
    public void setListening(boolean listening) {
        updateState(isSyncEnabled());
    }

    @Override
    public void handleClick() {
        updateState(!isSyncEnabled());
        ContentResolver.setMasterSyncAutomatically(!isSyncEnabled());

    }

    @Override
    public void handleLongClick() {
        mBar.startSettingsActivity(SYNC_SETTINGS);
    }

    private boolean isSyncEnabled() {
        return ContentResolver.getMasterSyncAutomatically();
    }
}
