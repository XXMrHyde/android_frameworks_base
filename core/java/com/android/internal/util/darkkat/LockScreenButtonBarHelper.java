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
import android.provider.Settings;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.util.slim.ActionConfig;
import com.android.internal.util.slim.ActionConstants;
import com.android.internal.util.slim.ConfigSplitHelper;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class LockScreenButtonBarHelper {

    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    // get and set the lockcreen button bar configs from provider and return propper arraylist objects
    // @ActionConfig
    public static ArrayList<ActionConfig> getButtonBarConfig(Context context) {
        String config = Settings.System.getStringForUser(
                    context.getContentResolver(),
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_BUTTONS,
                    UserHandle.USER_CURRENT);
        if (config == null) {
            config = "";
        }

        return (ConfigSplitHelper.getActionConfigValues(context, config, null, null, true));
    }

    public static void setButtonBarConfig(Context context,
            ArrayList<ActionConfig> actionConfig, boolean reset) {
        String config;
        if (reset) {
            config = "";
        } else {
            config = ConfigSplitHelper.setActionConfig(actionConfig, true);
        }
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.LOCK_SCREEN_BUTTON_BAR_BUTTONS, config);
    }

    public static Drawable getButtonBarIconImage(Context context,
            String clickAction, String customIcon) {
        int resId = -1;
        Drawable d = null;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("ActionHelper:", "can't access systemui resources",e);
            return null;
        }

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
                    d = systemUiResources.getDrawable(resId);
                    return d;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (customIcon != null && customIcon.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)) {
            resId = systemUiResources.getIdentifier(customIcon.substring(
                        ActionConstants.SYSTEM_ICON_IDENTIFIER.length()), "drawable", "android");
            if (resId > 0) {
                return systemUiResources.getDrawable(resId);
            }
        } else if (customIcon != null && !customIcon.equals(ActionConstants.ICON_EMPTY)) {
            File f = new File(Uri.parse(customIcon).getPath());
            if (f.exists()) {
                return new BitmapDrawable(context.getResources(),
                    ImageHelper.getRoundedCornerBitmap(
                        new BitmapDrawable(context.getResources(),
                        f.getAbsolutePath()).getBitmap()));
            } else {
                Log.e("ActionHelper:", "can't access custom icon image");
                return null;
            }
//        } else if (clickAction.startsWith("**")) {
//            resId = getActionSystemIcon(systemUiResources, clickAction);

//            if (resId > 0) {
//                return systemUiResources.getDrawable(resId);
//            }
        }
        return d;
    }
}
