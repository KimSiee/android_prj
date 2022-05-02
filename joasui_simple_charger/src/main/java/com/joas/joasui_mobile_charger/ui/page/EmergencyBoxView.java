/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 22 오전 11:15
 *
 */

package com.joas.joasui_mobile_charger.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.joas.joasui_mobile_charger.ui.R;
import com.joas.joasui_mobile_charger.ui.UIFlowManager;

public class EmergencyBoxView extends FrameLayout {
    Activity mainActivity;
    UIFlowManager flowManager;

    public EmergencyBoxView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_emergency_box, this, true);
    }


    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
        }
        else {
        }
    }

}