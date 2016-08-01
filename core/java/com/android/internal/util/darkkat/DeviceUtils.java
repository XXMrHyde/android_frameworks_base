/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
 *
 * Copyright (C) 2013 SlimRoms Project
 *
 * Copyright (C) 2016 DarkKat
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
import android.net.ConnectivityManager;

public class DeviceUtils {

    public static boolean deviceSupportsMobileData(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }
}
