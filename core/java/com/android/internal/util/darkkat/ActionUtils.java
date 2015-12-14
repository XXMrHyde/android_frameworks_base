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

import com.android.internal.util.darkkat.DeviceUtils;
import com.android.internal.util.darkkat.QABConstants;
import com.android.internal.util.slim.ActionConstants;

import java.util.ArrayList;
import java.util.List;

public class ActionUtils {

    public static FilteredDeviceFeaturesArray filterUnsupportedDeviceFeatures(Context context,
            String[] valuesArray, String[] entriesArray) {
        if (valuesArray == null || entriesArray == null || context == null) {
            return null;
        }
        List<String> finalEntries = new ArrayList<String>();
        List<String> finalValues = new ArrayList<String>();
        FilteredDeviceFeaturesArray filteredDeviceFeaturesArray =
            new FilteredDeviceFeaturesArray();

        for (int i = 0; i < valuesArray.length; i++) {
            if (isSupportedFeature(context, valuesArray[i])) {
                finalEntries.add(entriesArray[i]);
                finalValues.add(valuesArray[i]);
            }
        }
        filteredDeviceFeaturesArray.entries =
            finalEntries.toArray(new String[finalEntries.size()]);
        filteredDeviceFeaturesArray.values =
            finalValues.toArray(new String[finalValues.size()]);
        return filteredDeviceFeaturesArray;
    }

    private static boolean isSupportedFeature(Context context, String action) {
        if (action.equals(ActionConstants.ACTION_TORCH)
                        && !DeviceUtils.deviceSupportsFlashLight(context)
                || action.equals(ActionConstants.ACTION_VIB)
                        && !DeviceUtils.deviceSupportsVibrator(context)
                || action.equals(ActionConstants.ACTION_VIB_SILENT)
                        && !DeviceUtils.deviceSupportsVibrator(context)
                || action.equals(QABConstants.BUTTON_BLUETOOTH)
                        && !DeviceUtils.deviceSupportsBluetooth()
                || action.equals(QABConstants.BUTTON_DATA)
                        && !DeviceUtils.deviceSupportsMobileData(context)
                || action.equals(QABConstants.BUTTON_FLASHLIGHT)
                        && !DeviceUtils.deviceSupportsFlashLight(context)
                || action.equals(QABConstants.BUTTON_HOTSPOT)
                        && !DeviceUtils.deviceSupportsMobileData(context)
                || action.equals(QABConstants.BUTTON_NFC)
                        && !DeviceUtils.deviceSupportsNfc(context)) {
            return false;
        }
        return true;
    }

    public static class FilteredDeviceFeaturesArray {
        public String[] entries;
        public String[] values;
    }

}
