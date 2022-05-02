/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.joas.kepco.ui.ChargeData;
import com.joas.kepco.ui.R;
import com.joas.kepco.ui.TypeDefine;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.kepco.ui.TypeDefine;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvRemainTime;
    TextView tvPercent;
    TextView tvPercentLabel;
    TextView tvChargingRemainTimeLabel;
    TextView tvChargeCost;
    RelativeLayout layerFinishingText;
    ProgressBar progressBarCharge;
    int socAminationVal = 0;

    public ChargingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_charging, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateDispInfo();
            }
        });
    }

    void initComponents() {
        layerFinishingText = (RelativeLayout)findViewById(R.id.layerFinishingText);
        tvChargeKwhVal = (TextView)findViewById(R.id.tvChargeKwhVal);
        tvChargeTime = (TextView)findViewById(R.id.tvChargeTime);
        tvPercent = (TextView)findViewById(R.id.tvPercentCharge);
        tvPercentLabel = (TextView)findViewById(R.id.tvPercentLabel);
        tvRemainTime = (TextView)findViewById(R.id.tvChargeRemainTime);
        tvChargingRemainTimeLabel = (TextView)findViewById(R.id.tvChargingRemainTimeLabel);
        tvChargeCost = (TextView)findViewById(R.id.tvChargeCost);

        progressBarCharge = (ProgressBar)findViewById(R.id.progressBarCharge);

        ImageButton btStop = (ImageButton) findViewById(R.id.btChargingStop);
        btStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopClick();
            }
        });
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", cData.measureWh );
        tvChargeKwhVal.setText( measureVal);
        tvChargeKwhVal.invalidate();

        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvChargeTime.setText(strTime);
        tvChargeTime.invalidate();

        // Percent, SOC
        if ( cData.isConnectorHasSOC() == false ) { // soc, remaintime 삭제
            progressBarCharge.setProgress(socAminationVal);
            socAminationVal += 20;
            if ( socAminationVal > 100) socAminationVal = 0;
            tvPercent.setText("");
            tvPercent.invalidate();
        }
        else {
            progressBarCharge.setProgress(cData.soc);
            tvPercent.setText(""+cData.soc);
            tvPercent.invalidate();
        }

        // 남은시간
        timeSec = (int)(cData.remainTime);
        strTime = String.format("%02d : %02d : %02d", timeSec / 60, timeSec % 60, 00 );
        tvRemainTime.setText(strTime);
        tvRemainTime.invalidate();

        // 충전금액
        strTime = String.format("%,d원", (int)cData.chargingCost);
        tvChargeCost.setText(strTime);
        tvChargeCost.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        ChargeData cData = flowManager.getChargeData();

        //AC3 상이거나 완속 인 경우에
        if ( cData.isConnectorHasSOC() == false ) { // soc, remaintime 삭제
            tvRemainTime.setVisibility(INVISIBLE);
            tvChargingRemainTimeLabel.setVisibility(INVISIBLE);
            tvPercent.setVisibility(INVISIBLE);
            tvPercentLabel.setVisibility(INVISIBLE);
            socAminationVal = 0;

            updateDispInfo();
        }
        else {
            tvPercent.setVisibility(VISIBLE);
            tvPercentLabel.setVisibility(VISIBLE);
            tvRemainTime.setVisibility(VISIBLE);
            tvChargingRemainTimeLabel.setVisibility(VISIBLE);
        }
        layerFinishingText.setVisibility(GONE);
        timer.start();
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
        tvChargeKwhVal.setText("0 kWh");
        tvChargeTime.setText("00 : 00 : 00");
        tvPercent.setText("0");
        tvRemainTime.setText("00 : 00 : 00");
        tvChargeCost.setText("0원");

        progressBarCharge.setProgress(0);
        layerFinishingText.setVisibility(GONE);
    }

    void onStopClick() {
        flowManager.onChargingStopAsk();
    }

    public void onChargingStop() {
        layerFinishingText.setVisibility(VISIBLE);
    }
}
