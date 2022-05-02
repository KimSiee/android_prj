/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_tardis.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.ocppui_tardis.R;
import com.joas.ocppui_tardis.UIFlowManager;

public class FaultBoxView extends FrameLayout {
    Activity mainActivity;
    UIFlowManager flowManager;

    public FaultBoxView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_fault_box, this, true);
    }

    void initMessageBox() {
        TextView tvFaultBoxContent = findViewById(R.id.tvFaultBoxContent);
        tvFaultBoxContent.setText(flowManager.getChargeData().faultBoxContent );
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            initMessageBox();
        }
        else {
        }
    }

}