/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.joas.gridwiz_slow_charlcd.R;
import com.joas.gridwiz_slow_charlcd.UIFlowManager;

public class DSPCommErrView extends FrameLayout {
    Activity mainActivity;
    UIFlowManager flowManager;

    public DSPCommErrView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_dspcomm_err, this, true);
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