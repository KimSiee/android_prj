/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kevcs_charger_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.UIFlowManager;

public class FaultBoxView extends FrameLayout implements PageActivateListener {
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
        tvFaultBoxContent.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initMessageBox();
    }

    @Override
    public void onPageDeactivate() {

    }
}