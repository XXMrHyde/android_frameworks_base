/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;
import com.android.systemui.darkkat.util.QSColorHelper;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

/** Quick settings tile: Do not disturb **/
public class DndTile extends QSTile<QSTile.BooleanState> {

    private static final Intent ZEN_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);

    private static final Intent ZEN_PRIORITY_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS);

    private static final String ACTION_SET_VISIBLE = "com.android.systemui.dndtile.SET_VISIBLE";
    private static final String EXTRA_VISIBLE = "visible";

    private static final QSTile.Icon TOTAL_SILENCE =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on_total_silence);

    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_dnd_disable_animation);
    private final AnimationIcon mDisableTotalSilence =
            new AnimationIcon(R.drawable.ic_dnd_total_silence_disable_animation);

    private final ZenModeController mController;
    private final DndDetailAdapter mDetailAdapter;

    private boolean mListening;
    private boolean mShowingDetail;

    public DndTile(Host host) {
        super(host);
        mController = host.getZenModeController();
        mDetailAdapter = new DndDetailAdapter();
        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_SET_VISIBLE));
    }

    public static void setVisible(Context context, boolean visible) {
        Prefs.putBoolean(context, Prefs.Key.DND_TILE_VISIBLE, visible);
    }

    public static boolean isVisible(Context context) {
        return Prefs.getBoolean(context, Prefs.Key.DND_TILE_VISIBLE, false /* defaultValue */);
    }

    public static void setCombinedIcon(Context context, boolean combined) {
        Prefs.putBoolean(context, Prefs.Key.DND_TILE_COMBINED_ICON, combined);
    }

    public static boolean isCombinedIcon(Context context) {
        return Prefs.getBoolean(context, Prefs.Key.DND_TILE_COMBINED_ICON,
                false /* defaultValue */);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        if (mController.isVolumeRestricted()) {
            // Collapse the panels, so the user can see the toast.
            mHost.collapsePanels();
            SysUIToast.makeText(mContext, mContext.getString(
                    com.android.internal.R.string.error_message_change_not_allowed),
                    Toast.LENGTH_LONG).show();
            return;
        }
        mDisable.setAllowAnimation(true);
        mDisableTotalSilence.setAllowAnimation(true);
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        if (mState.value) {
            mController.setZen(Global.ZEN_MODE_OFF, null, TAG);
        } else {
            int zen = Prefs.getInt(mContext, Prefs.Key.DND_FAVORITE_ZEN, Global.ZEN_MODE_ALARMS);
            mController.setZen(zen, null, TAG);
            showDetail(true);
        }
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int zen = arg instanceof Integer ? (Integer) arg : mController.getZen();
        final boolean newValue = zen != Global.ZEN_MODE_OFF;
        final boolean valueChanged = state.value != newValue;
        state.value = newValue;
        state.visible = isVisible(mContext);
        switch (zen) {
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_dnd_on);
                state.label = mContext.getString(R.string.quick_settings_dnd_priority_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_priority_on);
                break;
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                state.icon = TOTAL_SILENCE;
                state.label = mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_none_on);
                break;
            case Global.ZEN_MODE_ALARMS:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_dnd_on);
                state.label = mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_alarms_on);
                break;
            default:
                state.icon = TOTAL_SILENCE.equals(state.icon) ? mDisableTotalSilence : mDisable;
                state.label = mContext.getString(R.string.quick_settings_dnd_label);
                state.contentDescription =  mContext.getString(
                        R.string.accessibility_quick_settings_dnd_off);
                break;
        }
        if (mShowingDetail && !state.value) {
            showDetail(false);
        }
        if (valueChanged) {
            fireToggleStateChanged(state.value);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_DND;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_dnd_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_dnd_changed_off);
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            mController.addCallback(mZenCallback);
            Prefs.registerListener(mContext, mPrefListener);
        } else {
            mController.removeCallback(mZenCallback);
            Prefs.unregisterListener(mContext, mPrefListener);
        }
    }

    public void updateDeatailBackgroundColor() {
        mDetailAdapter.updateDeatailBackgroundColor();
    }

    public void updateDeatailTextColor() {
        mDetailAdapter.updateDeatailTextColor();
    }

    public void updateDeatailIconColor() {
        mDetailAdapter.updateDeatailIconColor();
    }

    public void updateDeatailRippleColor() {
        mDetailAdapter.updateDeatailRippleColor();
    }

    private final OnSharedPreferenceChangeListener mPrefListener
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                @Prefs.Key String key) {
            if (Prefs.Key.DND_TILE_COMBINED_ICON.equals(key) ||
                    Prefs.Key.DND_TILE_VISIBLE.equals(key)) {
                refreshState();
            }
        }
    };

    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        public void onZenChanged(int zen) {
            refreshState(zen);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean visible = intent.getBooleanExtra(EXTRA_VISIBLE, false);
            setVisible(mContext, visible);
            refreshState();
        }
    };

    private final class DndDetailAdapter implements DetailAdapter, OnAttachStateChangeListener {
        private ZenModePanel mZenModePanel = null;

        @Override
        public int getTitle() {
            return R.string.quick_settings_dnd_label;
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public Intent getSettingsIntent() {
            return ZEN_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsLogger.QS_DND_TOGGLE, state);
            if (!state) {
                mController.setZen(Global.ZEN_MODE_OFF, null, TAG);
                showDetail(false);
            }
        }

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_DND_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            mZenModePanel = convertView != null ? (ZenModePanel) convertView
                    : (ZenModePanel) LayoutInflater.from(context).inflate(
                            R.layout.zen_mode_panel, parent, false);
            if (convertView == null) {
                mZenModePanel.init(mController);
                mZenModePanel.addOnAttachStateChangeListener(this);
                mZenModePanel.setCallback(mZenModePanelCallback);
                mZenModePanel.setBackgroundColor(
                        QSColorHelper.getDndModePanelBackgroundColor(context));
                mZenModePanel.setTextColor(QSColorHelper.getDndModeButtonTextColors(context),
                        QSColorHelper.getQSTextColor(mContext), QSColorHelper.getQSAccentColor(mContext));
                mZenModePanel.setIconColor(
                        QSColorHelper.getDndModeConditionsIconColors(mContext),
                                QSColorHelper.getIconColorStateList(mContext));
                mZenModePanel.setRippleColor(QSColorHelper.getRippleColorStateList(mContext));
            }
            return mZenModePanel;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            mShowingDetail = true;
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            mShowingDetail = false;
        }

        private void updateDeatailBackgroundColor() {
            if (mZenModePanel != null) {
                mZenModePanel.setBackgroundColor(
                        QSColorHelper.getDndModePanelBackgroundColor(mContext));
            }
        }

        private void updateDeatailTextColor() {
            if (mZenModePanel != null) {
                mZenModePanel.setTextColor(QSColorHelper.getDndModeButtonTextColors(mContext),
                        QSColorHelper.getQSTextColor(mContext), QSColorHelper.getQSAccentColor(mContext));
            }
        }

        private void updateDeatailIconColor() {
            if (mZenModePanel != null) {
                mZenModePanel.setIconColor(
                        QSColorHelper.getDndModeConditionsIconColors(mContext),
                                QSColorHelper.getIconColorStateList(mContext));
            }
        }

        private void updateDeatailRippleColor() {
            if (mZenModePanel != null) {
                mZenModePanel.setRippleColor(QSColorHelper.getRippleColorStateList(mContext));
            }
        }
    }

    private final ZenModePanel.Callback mZenModePanelCallback = new ZenModePanel.Callback() {
        @Override
        public void onPrioritySettings() {
            mHost.startActivityDismissingKeyguard(ZEN_PRIORITY_SETTINGS);
        }

        @Override
        public void onInteraction() {
            // noop
        }

        @Override
        public void onExpanded(boolean expanded) {
            // noop
        }
    };

}
