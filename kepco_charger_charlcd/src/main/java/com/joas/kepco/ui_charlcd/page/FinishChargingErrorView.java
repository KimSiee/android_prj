/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kepco.ui_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui_charlcd.ChargeData;
import com.joas.kepco.ui.R;
import com.joas.kepco.ui_charlcd.UIFlowManager;


public class FinishChargingErrorView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvFinishFaultTime;
    TextView tvFinishFaultAmount;
    TextView tvFinishFaultCost;
    TextView tvFaultMsg;
    ImageButton btFinishFaultOK;

    public FinishChargingErrorView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_finish_fault, this, true);

        initComponents();
    }

    void initComponents() {
        tvFinishFaultTime = (TextView)findViewById(R.id.tvFinishFaultTime);
        tvFinishFaultAmount = (TextView)findViewById(R.id.tvFinishFaultAmount);
        tvFinishFaultCost = (TextView)findViewById(R.id.tvFinishFaultCost );
        btFinishFaultOK = (ImageButton)findViewById(R.id.btFinishFaultOK);
        tvFaultMsg = (TextView)findViewById(R.id.tvFaultMsg);

        btFinishFaultOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.onPageEventFinishErrorOKClick();
            }
        });
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 비회원인증인경우
        if ( cData.isAuthCredit ) {
            // 충전 요구량보다 같거나 큰경우 해당 량만큼만 표시함
            if ( cData.measureKwh >= cData.reqPayKwh ) {
                cData.measureKwh = cData.reqPayKwh;
                cData.chargingCost = cData.reqPayCost;
            }
        }

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", cData.measureKwh);
        tvFinishFaultAmount.setText(measureVal);

        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvFinishFaultTime.setText(strTime);

        // 충전금액
        strTime = String.format("%,d원", (int)cData.chargingCost);
        tvFinishFaultCost.setText(strTime);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        updateDispInfo();
        tvFaultMsg.setText(flowManager.getChargeData().faultBoxContent);
    }

    @Override
    public void onPageDeactivate() {
    }
}
