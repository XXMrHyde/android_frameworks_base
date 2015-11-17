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

import android.content.Context;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GreetingTextHelper {

    public static String getDefaultGreetingText(Context context) {

        final String welcome = context.getResources().getString(
                com.android.internal.R.string.default_greeting_welcome);
        Date date = new Date(Build.TIME); 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        String buildDate = sdf.format(date);

        String defaultGreetingText =
                welcome + " " + Build.VERSION.RELEASE
                + " (" + Build.ID + "), " + buildDate;

        return defaultGreetingText;
    }
}
