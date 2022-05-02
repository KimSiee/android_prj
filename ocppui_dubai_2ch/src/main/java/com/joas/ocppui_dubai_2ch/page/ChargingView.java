/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_dubai_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_dubai_2ch.ChargeData;
import com.joas.ocppui_dubai_2ch.R;
import com.joas.ocppui_dubai_2ch.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    TimeoutTimer aniTimer = null;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvChargeCost;
    TextView tvFinishChargingTagCounter;

    FrameLayout frameFinishing;
    FrameLayout frameCardTag;
    FrameLayout frameNotice;

    ImageView imageAni;

    Button btStopCharging;
    Button btCancelStopCharging;
    int aminationCnt = 0;
    int tagCounter = 0;
    int msgCounter = 0;

    int[] charging_icons = { R.drawable.charging_icon0, R.drawable.charging_icon1, R.drawable.charging_icon2, R.drawable.charging_icon3, R.drawable.charging_icon4,
            R.drawable.charging_icon5, R.drawable.charging_icon6, R.drawable.charging_icon7, R.drawable.charging_icon8, R.drawable.charging_icon9,
            R.drawable.charging_icon10, R.drawable.charging_icon11 };

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

        aniTimer = new TimeoutTimer(200, new TimeoutHandler() {
            @Override
            public void run() {
                doAnimation();
            }
        });
    }

    void initComponents() {
        frameFinishing = findViewById(R.id.frameFinishing);
        frameCardTag = findViewById(R.id.frameCardTag);
        frameNotice = findViewById(R.id.frameNotice);

        tvChargeKwhVal = findViewById(R.id.tvChargeKwhVal);
        tvChargeTime = findViewById(R.id.tvChargeTime);
        tvChargeCost = findViewById(R.id.tvChargeCost);
        tvFinishChargingTagCounter = findViewById(R.id.tvFinishChargingTagCounter);
        tvFinishChargingTagCounter.setText("30");

        btStopCharging = findViewById(R.id.btStopCharging);
        btStopCharging.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopCharging();
            }
        });

        btCancelStopCharging = findViewById(R.id.btCancelStopCharging);
        btCancelStopCharging.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelStopCharging();
            }
        });

        imageAni = findViewById(R.id.imgChargingAni);
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", (double)(cData.measureWh / 1000.0) );
        tvChargeKwhVal.setText( measureVal);

        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvChargeTime.setText(strTime);
        tvChargeCost.setText((int)cData.chargingCost+"원");

        if ( tagCounter > 0 ) {
            tagCounter--;
            tvFinishChargingTagCounter.setText(""+tagCounter);
            if ( tagCounter == 0 ) flowManager.getMultiChannelUIManager().rfidReaderRelease(flowManager.getChargeData().dspChannel);
        }

        if ( msgCounter > 0 ) {
            msgCounter--;
            if ( msgCounter == 0 ) frameNotice.setVisibility(INVISIBLE);
        }
    }

    void doAnimation() {
        imageAni.setImageResource(charging_icons[aminationCnt]);
        aminationCnt = (aminationCnt+1) % charging_icons.length;
        imageAni.invalidate();
    }

    void startTimer() {
        stopTimer();
        timer.start();
        aniTimer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
        if ( aniTimer != null ) aniTimer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        int tagCounter = 0;

        ChargeData cData = flowManager.getChargeData();
        aminationCnt = 0;
        frameFinishing.setVisibility(INVISIBLE);
        frameCardTag.setVisibility(INVISIBLE);
        frameNotice.setVisibility(INVISIBLE);

        updateDispInfo();
        doAnimation();

        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();

        tvChargeKwhVal.setText("0 kWh");
        tvChargeTime.setText("00 : 00 : 00");
    }

    void onStopCharging() {
        if (flowManager.getMultiChannelUIManager().rfidReaderRequest(flowManager.getChargeData().dspChannel) == false ) {
            msgCounter = 3;
            frameNotice.setVisibility(VISIBLE);
        }
        else {
            tagCounter = 30;
            tvFinishChargingTagCounter.setText(""+tagCounter);
            frameCardTag.setVisibility(VISIBLE);
        }
    }


    public void onChargingStop() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                frameCardTag.setVisibility(INVISIBLE);
                frameFinishing.setVisibility(VISIBLE);
            }
        });
    }

    public void onCancelStopCharging() {
        frameCardTag.setVisibility(INVISIBLE);
        flowManager.getMultiChannelUIManager().rfidReaderRelease(flowManager.getChargeData().dspChannel);
        tagCounter = 0;
    }

    public void onFailStopCharging() {
        frameCardTag.setVisibility(INVISIBLE);
        tagCounter = 0;
    }
}
