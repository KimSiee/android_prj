/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.posco_slow_charlcd.CPConfig;
import com.joas.posco_slow_charlcd.ChargeData;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.TypeDefine;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;
    ChargeData cData;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvRemainTime;
    TextView tvSubtitle;
    Button btStopTag;

    TextView tvChargingCost;


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
        CPConfig cpConfig = flowManager.getCpConfig();
        tvChargeKwhVal = (TextView)findViewById(R.id.tvchgpwr_chargingview);
        tvChargeTime = (TextView)findViewById(R.id.tvchgtime_chargingview);
        tvRemainTime = (TextView)findViewById(R.id.tvleftime_chargingview);
        tvSubtitle = (TextView)findViewById(R.id.tvsubtitle_chargingview);
        tvChargingCost = (TextView)findViewById(R.id.tvchargingcost_charging);

        cData = flowManager.getChargeData();
        btStopTag = (Button) findViewById(R.id.btChargingStop_chargingview);
        if(cpConfig.useKakaoNavi) btStopTag.setBackground(ActivityCompat.getDrawable(getContext(), R.drawable.button_selector_kakao));
        else btStopTag.setBackground(ActivityCompat.getDrawable(getContext(), R.drawable.button_selector));
        btStopTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onChargingStopClick();
            }
        });
        if(cpConfig.useKakaoNavi) btStopTag.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
        else btStopTag.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f", (double) (cData.measureWh / 1000.0));
        tvChargeKwhVal.setText(measureVal);

        // 충전 시간 표시
        int timeSec = (int) (cData.chargingTime / 1000);
        String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60));
        tvChargeTime.setText(strTime);

        //add by si.20200528 - 차지비 충전 남은시간 표시
        int remainTimeSec = (int) (cData.chargingRemainTime / 1000);
        String strRemTime = String.format("%02d:%02d:%02d", remainTimeSec / 3600, (remainTimeSec % 3600) / 60, (remainTimeSec % 60));
        tvRemainTime.setText(strRemTime);

        // 충전요금 - test
        tvChargingCost.setText(""+cData.chargingCost+" 원");

    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        CPConfig cpConfig = flowManager.getCpConfig();
        tvChargingCost.setVisibility(INVISIBLE);        //test

        if(cData.authType == TypeDefine.AuthType.NOMEMBER || cpConfig.useKakaoNavi){
            //비회원 현장결제일 경우 버튼 보이기 및 subtitle 문구 수정
            btStopTag.setVisibility(VISIBLE);
            tvSubtitle.setText("충전을 종료하시려면 충전중지\n버튼을 눌러주세요.");
        }
        else{
            //회원 충전일 경우, 충전중지버튼 안보이게
            btStopTag.setVisibility(INVISIBLE);
            //subtitle 문구 수정
            tvSubtitle.setText("충전을 종료하시려면 멤버십 카드를\n화면 아래의 센서에 터치해주십시오.");
            //테크리더 단말기를 사용하는 멤버일 경우 페이지 활성화시 카드번호 읽기 요청 한번 요청
            if(cpConfig.useTL3500BS) flowManager.tl3500s.cardInfoReq(0);
        }
        updateDispInfo();
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
        tvChargeKwhVal.setText("0.00");
        tvChargeTime.setText("00:00:00");
        tvRemainTime.setText("00:00:00");
        CPConfig cpConfig = flowManager.getCpConfig();
        ChargeData chgData = flowManager.getChargeData();
        if(cpConfig.useTL3500BS) {
            if(chgData.authType == TypeDefine.AuthType.MEMBER) flowManager.tl3500s.termReadyReq();
        }
    }

    void onChargingStopClick() {
//        flowManager.onCardTagEvent("1010010071615448", true);
        //비회원 충전종료이므로 바로 충전종료 호출
        flowManager.onChargingStop();
    }

//    public void onChargingStop() {
//        mainActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                tvMsgFinishing.setVisibility(VISIBLE);
//            }
//        });
//    }
}
