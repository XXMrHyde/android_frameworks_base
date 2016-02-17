/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.darkkat.NotificationColorHelper;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.IconMerger;

/**
 * A view to display all the overflowing icons on Keyguard.
 */
public class NotificationOverflowIconsView extends IconMerger {

    private TextView mMoreText;
    private int mIconSize;
    private NotificationColorUtil mNotificationColorUtil;

    public NotificationOverflowIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNotificationColorUtil = NotificationColorUtil.getInstance(getContext());
        mIconSize = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_icon_size);
    }

    public void setMoreText(TextView moreText) {
        mMoreText = moreText;
    }

    public void addNotification(NotificationData.Entry notification) {
        StatusBarIconView v = new StatusBarIconView(getContext(), "",
                notification.notification.getNotification());
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.set(notification.icon.getStatusBarIcon());
        addView(v, mIconSize, mIconSize);
        applyColor(notification.notification.getNotification(), v);
        updateMoreText();
    }

    private void applyColor(Notification notification, StatusBarIconView view) {
        StatusBarIcon sbi = view.getStatusBarIcon();
        Drawable icon = StatusBarIconView.getIcon(getContext(), sbi);
        final int iconColor = NotificationColorHelper.getIconColor(getContext(), icon);
        if (iconColor != 0) {
            view.setColorFilter(iconColor, Mode.MULTIPLY);
        } else {
            view.setColorFilter(null);
        }
    }

    private void updateMoreText() {
        final int textColor = NotificationColorHelper.getCustomIconColor(getContext());
        final int bgColor = NotificationColorHelper.getAppIconBgColor(getContext(), 0);
        final int bgAlpha = NotificationColorHelper.getAppIconBgAlpha(getContext(), 0);
        mMoreText.setText(
                getResources().getString(R.string.keyguard_more_overflow_text, getChildCount()));
        mMoreText.setTextColor(textColor);
        if (mMoreText.getBackground() != null) {
            if (bgColor == Notification.COLOR_DEFAULT) {
                mMoreText.getBackground().setColorFilter(null);
            } else {
                mMoreText.getBackground().setColorFilter(bgColor, Mode.SRC_ATOP);

            }
            mMoreText.getBackground().setAlpha(bgAlpha);
        }
    }

    public void setMoreIconColor() {
        int iconColor = NotificationColorHelper.getIconColor(getContext(),
                ((ImageView) mMoreView).getDrawable());
        if (iconColor != 0) {
            ((ImageView) mMoreView).setColorFilter(iconColor, Mode.MULTIPLY);
        } else {
            ((ImageView) mMoreView).setColorFilter(null);
        }
    }
}
