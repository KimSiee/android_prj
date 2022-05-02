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
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.UIFlowManager;

public class SelectAmountTypeView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public SelectAmountTypeView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_amount_type,this, true);

        initComponents();
    }

    void initComponents() {
        ImageButton btCost  = (ImageButton) findViewById(R.id.btSelAmountCost);
        ImageButton btKwh  = (ImageButton) findViewById(R.id.btSelAmountKwh);

        btCost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCostClick();
            }
        });
        btKwh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onKwhClick();
            }
        });

    }

    public void onCostClick() {
        flowManager.onPageSelectAmountType(PageEvent.SELECT_AMOUNT_COST_CLICK);
    }

    public void onKwhClick() {
        flowManager.onPageSelectAmountType(PageEvent.SELECT_AMOUNT_KWH_CLICK);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }
}
