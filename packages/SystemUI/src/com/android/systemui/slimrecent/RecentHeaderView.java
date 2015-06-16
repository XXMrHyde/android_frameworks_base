/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.systemui.slimrecent;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.android.cards.view.component.CardHeaderView;

import com.android.internal.util.darkkat.ColorHelper;

import com.android.systemui.R;

public class RecentHeaderView extends CardHeaderView {

    public RecentHeaderView(Context context) {
        this(context, null, 0);
    }

    public RecentHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initView() {
        super.initView();

        if (mImageButtonExpand != null) {
            if (mImageButtonExpand.getDrawable() != null) {
                mImageButtonExpand.getDrawable().setTint(getExpandIconColor());
            }
        }
    }

    private int getExpandIconColor() {
        ContentResolver resolver = mContext.getContentResolver();
        Resources res = mContext.getResources();

        boolean useExpandIconColor = Settings.System.getIntForUser(resolver,
                Settings.System.SLIM_RECENTS_USE_EXPAND_ICON_COLOR, 0,
                UserHandle.USER_CURRENT) == 1;
        int defaultCardColor = res.getColor(R.color.card_background);
        int cardColor = Settings.System.getIntForUser(resolver,
                Settings.System.SLIM_RECENTS_CARD_BG_COLOR,
                defaultCardColor, UserHandle.USER_CURRENT);
        int expandIconColorLight = res.getColor(R.color.recents_icon_light_color);
        int expandIconColorDark = res.getColor(R.color.recents_icon_dark_color);
        int defaultExpandIconColor = ColorHelper.isColorDark(cardColor) ?
                expandIconColorLight : expandIconColorDark;
        int expandIconColor = Settings.System.getIntForUser(resolver,
                Settings.System.SLIM_RECENTS_EXPAND_ICON_COLOR,
                defaultExpandIconColor, UserHandle.USER_CURRENT);

        if (useExpandIconColor) {
            return expandIconColor;
        } else {
            return defaultExpandIconColor;
        }
    }
}
