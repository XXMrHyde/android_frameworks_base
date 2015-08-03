/*
* Copyright (C) 2013 SlimRoms Project
*
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

package com.android.internal.util.darkkat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.util.slim.ActionConfig;
import com.android.internal.util.slim.ConfigSplitHelper;

import java.util.ArrayList;

public class PowerMenuHelper {

    // get and set the power menu configs from provider and return propper arraylist objects
    // @ActionConfig
    public static ArrayList<ActionConfig> getPowerMenuConfig(Context context) {
        return (ConfigSplitHelper.getActionConfigValues(context,
            getPowerMenuProvider(context), null, null, true));
    }

    // get @ActionConfig with description if needed and other then an app description
    public static ArrayList<ActionConfig> getPowerMenuConfigWithDescription(
            Context context, String values, String entries) {
        return (ConfigSplitHelper.getActionConfigValues(context,
            getPowerMenuProvider(context), values, entries, true));
    }

    private static String getPowerMenuProvider(Context context) {
        String config = Settings.System.getStringForUser(
                    context.getContentResolver(),
                    Settings.System.POWER_MENU_BUTTONS,
                    UserHandle.USER_CURRENT);
        if (config == null) {
            config = PowerMenuConstants.POWER_MENU_BUTTONS_DEFAULT;
        }
        return config;
    }

    public static void setPowerMenuConfig(Context context,
            ArrayList<ActionConfig> actionConfig, boolean reset) {
        String config;
        if (reset) {
            config = PowerMenuConstants.POWER_MENU_BUTTONS_DEFAULT;
        } else {
            config = ConfigSplitHelper.setActionConfig(actionConfig, true);
        }
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.POWER_MENU_BUTTONS,
                    config);
    }

    public static Drawable getPowerMenuIconImage(Context context, String clickAction) {
        return getPowerMenuSystemIcon(context, clickAction);
    }

    private static Drawable getPowerMenuSystemIcon(Context context, String clickAction) {
        Resources res = context.getResources();
        if (clickAction.equals(PowerMenuConstants.ACTION_POWER_OFF)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_power_off);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_REBOOT)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_reboot);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_AIRPLANE)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_airplane_mode_off);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_USERS)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_users);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_SYSTEM_SETTINGS)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_settings);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_LOCK_DOWN)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_lock);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_EXPANDED_DESKTOP)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_expanded_desktop);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_SCREENSHOT)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_lock_screenshot);
        } else if (clickAction.equals(PowerMenuConstants.ACTION_SOUND)) {
            return res.getDrawable(
                com.android.internal.R.drawable.ic_audio_ring_notif);
        }
        return null;
    }

}
