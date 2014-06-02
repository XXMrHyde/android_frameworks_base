/*
* Copyright (C) 2013 SlimRoms Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class LockscreenShortcutHelper {

    private static final String SYSTEM_METADATA_NAME = "android";
    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    // get and set the lockcreen shortcut configs from provider and return propper arraylist objects
    // @ButtonConfig
    public static ArrayList<ButtonConfig> getLockscreenShortcutConfig(Context context) {
        String config = Settings.System.getStringForUser(
                    context.getContentResolver(),
                    Settings.System.LOCKSCREEN_SHORTCUTS,
                    UserHandle.USER_CURRENT);
        if (config == null) {
            config = "";
        }

        return (ConfigSplitHelper.getButtonsConfigValues(context, config, null, null, true));
    }

    public static void setLockscreenShortcutConfig(Context context,
            ArrayList<ButtonConfig> buttonsConfig, boolean reset) {
        String config;
        if (reset) {
            config = "";
        } else {
            config = ConfigSplitHelper.setButtonsConfig(buttonsConfig, true);
        }
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.LOCKSCREEN_SHORTCUTS, config);
    }

    public static Drawable getLockscreenShortcutIconImage(Context context,
            String clickAction, String customIcon) {
        int resId = -1;
        int defaultIconColor = 0xffffffff;
        int iconColor = defaultIconColor;
        int colorMode = 3;
        Drawable d = null;
        Drawable dError = null;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("ButtonsHelper:", "can't access systemui resources",e);
            return null;
        }

        colorMode = Settings.System.getIntForUser(
                context.getContentResolver(),
                Settings.System.LOCKSCREEN_SHORTCUTS_ICON_COLOR_MODE, 3,
                UserHandle.USER_CURRENT);
        iconColor = Settings.System.getIntForUser(
                context.getContentResolver(),
                Settings.System.LOCKSCREEN_SHORTCUTS_ICON_COLOR, defaultIconColor,
                UserHandle.USER_CURRENT);

        if (!clickAction.startsWith("**")) {
            try {
                String extraIconPath = clickAction.replaceAll(".*?hasExtraIcon=", "");
                if (extraIconPath != null && !extraIconPath.isEmpty()) {
                    File f = new File(Uri.parse(extraIconPath).getPath());
                    if (f.exists()) {
                        d = new BitmapDrawable(context.getResources(),
                                f.getAbsolutePath());
                    }
                }
                if (d == null) {
                    d = pm.getActivityIcon(Intent.parseUri(clickAction, 0));
                }
            } catch (NameNotFoundException e) {
                resId = systemUiResources.getIdentifier(
                    SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_null", null, null);
                if (resId > 0) {
                    dError = systemUiResources.getDrawable(resId);
                    if (colorMode != 3 && colorMode == 0) {
                        dError = new BitmapDrawable(
                            ImageHelper.getColoredBitmap(dError, iconColor));
                    }
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        boolean coloring = false;
        if (customIcon != null && customIcon.startsWith(ButtonsConstants.SYSTEM_ICON_IDENTIFIER)) {
            resId = systemUiResources.getIdentifier(customIcon.substring(
                        ButtonsConstants.SYSTEM_ICON_IDENTIFIER.length()), "drawable", "android");
            if (resId > 0) {
                d = systemUiResources.getDrawable(resId);
                if (colorMode != 3) {
                    coloring = true;
                }
            }
        } else if (customIcon != null && !customIcon.equals(ButtonsConstants.ICON_EMPTY)) {
            File f = new File(Uri.parse(customIcon).getPath());
            if (f.exists()) {
                d = new BitmapDrawable(context.getResources(),
                        ImageHelper.getRoundedCornerBitmap(
                        new BitmapDrawable(context.getResources(),
                        f.getAbsolutePath()).getBitmap()));
                if (colorMode != 3 && colorMode != 1) {
                    coloring = true;
                }
            } else {
                Log.e("LockscreenShortcutHelper:", "can't access custom icon image");
                return null;
            }
        } else if (clickAction.startsWith("**")) {
            d = systemUiResources.getDrawable(
                    getLockscreenShortcutSystemIcon(systemUiResources, clickAction));
            if (colorMode != 3) {
                coloring = true;
            }
        } else if (colorMode != 3 && colorMode == 0) {
            coloring = true;
        }
        if (dError == null) {
            if (coloring) {
                d = new BitmapDrawable(ImageHelper.getColoredBitmap(d, iconColor));
            }
            return d;
        } else {
            return dError;
        }
    }

    private static int getLockscreenShortcutSystemIcon(Resources systemUiResources, String clickAction) {
        int resId = -1;

        if (clickAction.equals(ButtonsConstants.ACTION_RECENTS)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_recent", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_ASSIST)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_search", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_SCREENSHOT)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_screenshot", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_IME)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_ime_switcher", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_EXPANDED_DESKTOP)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_expanded_desktop", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_KILL)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_killtask", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_POWER)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_power", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_POWER_MENU)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_power_menu", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_NOTIFICATIONS)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_notifications", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_LAST_APP)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_lastapp", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_QS)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_qs", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_VIB)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_vib", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_SILENT)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_silent", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_VIB_SILENT)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_ring_vib_silent", null, null);
        } else if (clickAction.equals(ButtonsConstants.ACTION_TORCH)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_torch", null, null);
        } else {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_null", null, null);
        }
        return resId;
    }

}
