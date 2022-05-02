/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 28. 오후 5:32
 *
 */

package com.joas.volvo_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PageReadyView extends LinearLayout implements PageActivateListener{

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

    void initComponents(){

        //현재시간
        tvCurrentTime = (TextView)findViewById(R.id.tvCurrTime);
        //예약자 이름
        tvReservName = (TextView)findViewById(R.id.tvReservName);
        //예약자 예약시간
        tvReservTime = (TextView)findViewById(R.id.tvReservTime);


        //충전 시작 버튼
        Button btStart = (Button) findViewById(R.id.btStartMain);
        if(flowManager.getUIFlowChannel() == 0) btStart.setText("PLUG A 시작");
        else btStart.setText("PLUG B 시작");
        btStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChargeStartClick();
            }
        });
    }

    public void onChargeStartClick(){
        flowManager.onPageCommonEvent(PageEvent.SELECT_MEMBER_CLICK);
    }

    @Override
    public void onPageActivate() {
        updateReservationInfo();

        startTimer();
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
        Volvo2chChargerInfo pinfo = flowManager.getChargevReservInfo();

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

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
