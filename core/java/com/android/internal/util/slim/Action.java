/*
* Copyright (C) 2014 SlimRoms Project
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

package com.android.internal.util.slim;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.internal.statusbar.IStatusBarService;

import java.net.URISyntaxException;

public class Action {

    public static void processAction(Context context, String action, boolean isLongpress) {
        processActionWithOptions(context, action, isLongpress, true);
    }

    public static void processActionWithOptions(Context context,
            String action, boolean isLongpress, boolean collapseShade) {

        if (action == null || action.equals(ActionConstants.ACTION_NULL)) {
            return;
        }

        boolean isKeyguardShowing = false;
        try {
            isKeyguardShowing =
                    WindowManagerGlobal.getWindowManagerService().isKeyguardLocked();
        } catch (RemoteException e) {
            Log.w("Action", "Error getting window manager service", e);
        }

        IStatusBarService barService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        if (barService == null) {
            return; // ouch
        }

        final IWindowManager windowManagerService = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        if (windowManagerService == null) {
           return; // ouch
        }

        // process the actions
        if (action.equals(ActionConstants.ACTION_SCREENSHOT)) {
            try {
                barService.toggleScreenshot();
            } catch (RemoteException e) {
            }
            return;
        } else {
            // we must have a custom uri
            Intent intent = null;
            try {
                intent = Intent.parseUri(action, 0);
            } catch (URISyntaxException e) {
                Log.e("Action:", "URISyntaxException: [" + action + "]");
                return;
            }
            startActivity(context, intent, barService, isKeyguardShowing);
            return;
        }

    }

    private static void startActivity(Context context, Intent intent,
            IStatusBarService barService, boolean isKeyguardShowing) {
        if (intent == null) {
            return;
        }
        if (isKeyguardShowing) {
            // Have keyguard show the bouncer and launch the activity if the user succeeds.
            try {
                barService.showCustomIntentAfterKeyguard(intent);
            } catch (RemoteException e) {
                Log.w("Action", "Error starting custom intent on keyguard", e);
            }
        } else {
            // otherwise let us do it here
            try {
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
            } catch (RemoteException e) {
                Log.w("Action", "Error dismissing keyguard", e);
            }
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivityAsUser(intent,
                    new UserHandle(UserHandle.USER_CURRENT));
        }
    }
}
