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

import com.joas.minsu_comm.MinsuChargerInfo;
import com.joas.minsu_ui.CPConfig;
import com.joas.minsu_ui.ChargeData;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.UIFlowManager;


public class FinishChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvFinishChargingTime;
    TextView tvFinishChargingAmount;
    TextView tvFinishChargingCost;

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
        tvFinishChargingCost = (TextView)findViewById(R.id.tvFinishChargingCost);

        ImageButton btCompleteOk = (ImageButton) findViewById(R.id.btChargingFinishOK);
        btCompleteOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onUnplugCheckClick();
            }
        });

    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        CPConfig cpConfig = flowManager.getCpConfig();

        if(cpConfig.isTestMode){}
        else{
            // 충전량 표시
            String measureVal = String.format("%.2f", (double)(cData.measureWh / 1000.0) );
            tvFinishChargingAmount.setText(measureVal);

            // 충전 시간 표시
            int timeSec = (int)(cData.chargingTime / 1000);
            String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
            tvFinishChargingTime.setText(strTime);

            //충전시간 서버로 전송
            MinsuChargerInfo minfo = flowManager.getMinsuChargerInfo();
            minfo.chargingTime = timeSec;

            tvFinishChargingCost.setText((int)cData.chargingCost+"");


        }

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        updateDispInfo();
    }

    @Override
    public void onPageDeactivate() {
    }

    public void onUnplugCheckClick() {
        flowManager.onPageCommonEvent(PageEvent.GO_UNPLUG);
    }
}
