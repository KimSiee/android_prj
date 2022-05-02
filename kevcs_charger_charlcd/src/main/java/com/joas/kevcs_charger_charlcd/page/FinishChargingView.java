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
import android.widget.TextView;

import com.joas.kevcs_charger_charlcd.ChargeData;
import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.UIFlowManager;


public class FinishChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvFinishChargingTime;
    TextView tvFinishChargingAmount;
    TextView tvFinishChargingCost;

    // 결제시 사용
    TextView tvFinishChargingTime2;
    TextView tvFinishChargingAmount2;
    TextView tvFinishChargingCost2;
    TextView tvPrePayCost;
    TextView tvCancelPayCost;

    LinearLayout layoutMember;
    LinearLayout layoutPay;

    ImageButton btFinishOK;

    public FinishChargingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_finish_charging, this, true);

        initComponents();
    }

    void initComponents() {
        tvFinishChargingTime = (TextView)findViewById(R.id.tvFinishChargingTime);
        tvFinishChargingAmount = (TextView)findViewById(R.id.tvFinishChargingAmount);
        tvFinishChargingCost = (TextView)findViewById(R.id.tvFinishChargingCost );
        btFinishOK = (ImageButton)findViewById(R.id.btFinishOK);

        btFinishOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.onPageEventFinishOKClick();
            }
        });

        tvFinishChargingTime2 = findViewById(R.id.tvFinishChargingTime2);
        tvFinishChargingAmount2 = findViewById(R.id.tvFinishChargingAmount2);
        tvFinishChargingCost2 = findViewById(R.id.tvFinishChargingCost2);
        tvPrePayCost = findViewById(R.id.tvPrePayCost);
        tvCancelPayCost = findViewById(R.id.tvCancelPayCost);

        layoutMember = findViewById(R.id.layoutMember);
        layoutPay = findViewById(R.id.layoutPay);
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

            layoutMember.setVisibility(INVISIBLE);
            layoutPay.setVisibility(VISIBLE);

            tvPrePayCost.setText(""+cData.reqPayCost+"원");
            tvCancelPayCost.setText(""+(cData.reqPayCost-(int)cData.chargingCost)+"원");
        }
        else {
            layoutMember.setVisibility(VISIBLE);
            layoutPay.setVisibility(INVISIBLE);
        }

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", cData.measureKwh);
        tvFinishChargingAmount.setText(measureVal);
        tvFinishChargingAmount2.setText(measureVal);

        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvFinishChargingTime.setText(strTime);
        tvFinishChargingTime2.setText(strTime);

        // 충전금액
        strTime = String.format("%,d원", (int)cData.chargingCost);
        tvFinishChargingCost.setText(strTime);
        tvFinishChargingCost2.setText(strTime);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        updateDispInfo();
    }

    @Override
    public void onPageDeactivate() {
    }
}
