/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 6 오후 5:04
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.UIFlowManager;

public class CreditCancelPrePayView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TextView tvPrePayValue;

    public CreditCancelPrePayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_credit_cancel_prepay, this, true);

        initComponents();
    }

    void initComponents() {

        tvPrePayValue = findViewById(R.id.tvPrePayValue);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        tvPrePayValue.setText(""+flowManager.getChargeData().reqPayCost+"원");
    }

    @Override
    public void onPageDeactivate() {
    }

}
