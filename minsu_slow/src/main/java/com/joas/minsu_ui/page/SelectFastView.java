/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.minsu_ui.ChargeData;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.UIFlowManager;


public class SelectFastView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvCostInfo;
    public SelectFastView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_fast,this, true);

        initComponents();
    }

    void initComponents() {

//        tvCostInfo = (TextView)findViewById(R.id.tvCostInfo);

//        Button btAC3 = (Button) findViewById(R.id.btAC3);
//        Button btChaDemo  = (Button) findViewById(R.id.btChaDemo);
        ImageButton btDCCombo = (ImageButton) findViewById(R.id.btDCCombo);

//        btAC3.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onAC3Click();
//            }
//        });
//        btChaDemo.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onChaDemoClick();
//            }
//        });
        btDCCombo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDCComboClick();
            }
        });
    }

    public void onAC3Click() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_AC3_CLICK);
    }
    public void onChaDemoClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CHADEMO_CLICK);
    }
    public void onDCComboClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_DCCOMBO_CLICK);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        ChargeData chargeData = flowManager.getChargeData();
        String dispmsg = "고객님의 충전단가는 " + chargeData.chargingUnitCost + "원/kWh 입니다.";
//        tvCostInfo.setText(dispmsg);
    }

    @Override
    public void onPageDeactivate() {
    }
}
