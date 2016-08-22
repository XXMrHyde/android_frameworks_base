/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
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

package com.android.internal.util.darkkat;

import android.graphics.drawable.Drawable;

import java.util.List;

public interface WeatherServiceController {
    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    void updateWeather();
    WeatherInfo getWeatherInfo();

    public interface Callback {
        void onWeatherChanged(WeatherInfo info);
    }
    public static class WeatherInfo {
        public String city = null;
        public String condition = null;
        public int conditionCode = 0;
        public Drawable conditionDrawableMonochrome = null;
        public Drawable conditionDrawableColored = null;
        public Drawable conditionDrawableVClouds = null;
        public String formattedTemperature = null;
        public String temperatureLow = null;
        public String temperatureHigh = null;
        public String formattedTemperatureLow = null;
        public String formattedTemperatureHigh = null;
        public String formattedHumidity = null;
        public String formattedWind = null;
        public String formattedPressure = null;
        public String formattedRain1H = null;
        public String formattedRain3H = null;
        public String formattedSnow1H = null;
        public String formattedSnow3H = null;
        public List<DayForecast> forecasts;
        public String timestamp = null;
    }

    public static class DayForecast {
        public String condition;
        public int conditionCode;
        public Drawable conditionDrawableMonochrome;
        public Drawable conditionDrawableColored;
        public Drawable conditionDrawableVClouds;
        public String temperatureLow;
        public String temperatureHigh;
        public String formattedTemperatureLow;
        public String formattedTemperatureHigh;
    }
}
