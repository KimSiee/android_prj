/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 13 오후 1:52
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

import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.CPConfig;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PageReadyView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;

    TextView tvCurrentTime;
    TextView tvReservName;
    TextView tvReservTime;

    public PageReadyView(Context context, UIFlowManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_ready,this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateReservationInfo();
            }
        });
    }
    void initComponents() {

        //현재시간 textview
        tvCurrentTime = (TextView)findViewById(R.id.tvCurrTime);
        //예약자 이름
        tvReservName = (TextView)findViewById(R.id.tvReservName);
        //예약자 예약시간
        tvReservTime = (TextView)findViewById(R.id.tvReservTime);

        //충전시작 버튼
        Button btStart = (Button) findViewById(R.id.btStart);
        btStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChargeStartClick();
            }
        });

        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useKakaoNavi) btStart.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
        else btStart.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));
    }

    @Override
    public void onPageActivate() {

        updateReservationInfo();

        startTimer();
    }
    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    /***
     * 차지비 예약상태 모니터링 함수(화면 표시용)
     */
    public void updateReservationInfo() {
        //현재시간 출력
        long mNow = System.currentTimeMillis();
        Date mReDate = new Date(mNow);
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatDate = mFormat.format(mReDate);
        tvCurrentTime.setText(formatDate);
        //예약정보관련 출력
        PoscoChargerInfo pinfo = flowManager.getCharevReservInfo();

        if (pinfo.rsv_flag == 0) {
            //예약자 없음
            tvReservName.setText("예약자\n없음");
            tvReservTime.setText("00:00:00");
        }
        else {
            //예약자 UID 맨 뒤 4자리 추출
            String raw_rsv_UID = pinfo.rsv_uid;
            String parsedUID = raw_rsv_UID.substring(12);
            tvReservName.setText(parsedUID + " 님");

            //예약시작시간
            SimpleDateFormat starttime = new SimpleDateFormat("HH:mm");
            String start = starttime.format(pinfo.rsv_startDateTime).toString();
            //예약종료시간
            SimpleDateFormat endtime = new SimpleDateFormat("HH:mm");
            String end = endtime.format(pinfo.rsv_endDateTime).toString();
            tvReservTime.setText(start + "\n~\n" + end);
        }
    }


    public void onChargeStartClick(){
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.chargerKind.equals("CV")) flowManager.onPageCommonEvent(PageEvent.SELECT_MEMBER_CLICK);
        else if(cpConfig.chargerKind.equals("OP")) flowManager.onPageCommonEvent(PageEvent.SELECT_CHG_START_CLICK);
    }


}
