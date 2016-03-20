/*
 * Copyright (C) 2015 DarkKat
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

package com.android.systemui.darkkat;

public interface NetworkTrafficController {
    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    Traffic getTraffic();

    public interface Callback {
        void onNetworkTrafficChanged(Traffic traffic);
    }

    public static class Traffic {
        public long outSpeed = 0;
        public long inSpeed = 0;
        public String outSpeedInBits = null;
        public String outSpeedInBytes = null;
        public String inSpeedInBits = null;
        public String inSpeedInBytes = null;
        public String outUnitAsBits = null;
        public String outUnitAsBytes = null;
        public String inUnitAsBits = null;
        public String inUnitAsBytes = null;
        public boolean activityOut = false;
        public boolean activityIn = false;
    }
}
