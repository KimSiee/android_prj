/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 28 오후 12:34
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
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
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;

public class InputAuthNumView extends LinearLayout implements PageActivateListener{
    Timer timer = null;             //현재 뷰 보이기 시간 타이머
    UIFlowManager flowManager;
    Activity mainActivity;
    public boolean isTestmode = false;
    TimeoutTimer upate_timer = null;
    int count = 0;

    public static final String TAG = "InputAuthNumView";

    TextView tvauthnum;
    TextView tvAlarm;
    TextView tvLeftTime;
    String authNum;


    public InputAuthNumView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_auth_num, this, true);

        //인증번호 입력 잔여시간 초기 3분 세팅
        count = flowManager.getChargeData().authnumInputTimeout;        //sec

        initComponents();

        upate_timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateLeftTime();
            }
        });
        tvLeftTime.setVisibility(VISIBLE);
    }

    public void stopTimer() {
        if ( upate_timer != null ) upate_timer.cancel();
    }

    /**
     * 1p응답에 대한 승인번호 표시(Test)
     */
    void updateLeftTime(){
        //test mode일때 승인번호 화면에 표시
        if(isTestmode){
            PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
            tvLeftTime.setText(pinfo.payment_authNum);
        }
        else {
            //인증번호 입력 남은시간 표시
            count--;
            int min = count / 60;
            int sec = count % 60;
            String lefttime = String.format("%02d", min) + ":" + String.format("%02d", sec);
            tvLeftTime.setText("(남은시간 : " + lefttime + ")");

            if (count == 0) {
                stopTimer();
                count = flowManager.getChargeData().authnumInputTimeout;
                flowManager.onPageCommonEvent(PageEvent.GO_HOME);
            }
        }

    }
    void initComponents() {
        CPConfig cpConfig = flowManager.getCpConfig();
        tvauthnum = findViewById(R.id.tvauthnum_an);
        tvauthnum.setText("");
        tvAlarm = findViewById(R.id.tvMsg_authNum);
        tvLeftTime = findViewById(R.id.tvlefttime_an);

        //숫자버튼 초기화
        int[] btNumIds = { R.id.btNum0_an, R.id.btNum1_an, R.id.btNum2_an, R.id.btNum3_an, R.id.btNum4_an,
                R.id.btNum5_an, R.id.btNum6_an, R.id.btNum7_an, R.id.btNum8_an, R.id.btNum9_an };
        Button[] btNums = new Button[10];

        for (int i=0; i<10; i++) {
            btNums[i] = findViewById(btNumIds[i]);

            //버튼 이미지 변경(차지비,카모)
            if(cpConfig.useKakaoNavi) btNums[i].setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
            else btNums[i].setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));

            final int finalI = i;
            btNums[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnClickNum(""+finalI);
                }
            });
        }

        //기타 버튼 초기화
        Button btBS = findViewById(R.id.btNumBS_an);
        btBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumBS();
            }
        });

        Button btClear = findViewById(R.id.btNumClear_an);
        btClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumClear();
            }
        });

        Button btCancel= findViewById(R.id.btNumCancel_an);
        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnCancelClick();
            }
        });

        Button btOK= findViewById(R.id.btNumOK_an);
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnOKClick();
            }
        });
    }
    void OnClickNum(String number) {
        authNum += number;
        textviewControl();
    }
    void OnNumBS() {
        try {
            authNum = authNum.replaceFirst(".$", "");
            tvauthnum.setText(authNum);
        }
        catch(Exception e) {
            LogWrapper.d(TAG, " :" + e.toString());
        }

    }
    void OnNumClear() {
        authNum = "";
        tvauthnum.setText("");
    }
    void OnCancelClick(){
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }
    void OnOKClick() {
        if (authNum.length() < 2 || authNum.length() > 2 || authNum.equals("")) {
            onAlarmOccured();
        }
        else{
//            flowManager.onCheckMissingPayment();      //test
            PoscoChargerInfo info = flowManager.getPoscoChargerInfo();
            if(authNum.equals(info.payment_authNum)){
                //승인번호 일치, 시간선택 및 타입설정 화면으로 이동
                flowManager.onCheckMissingPayment();
            }
            else{
                onAlarmOccured();
            }
        }
    }

    void textviewControl() {
        try {
            if (authNum.length() > 2) {
                onAlarmOccured();
            } else tvauthnum.setText(authNum);
        } catch (Exception e) {
            LogWrapper.d(TAG, " :" + e.toString());
        }
    }

    /**
     * 메시지 박스 표시 함수
     */
    public void onAlarmOccured() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                setAllButtonsClickable(false);
                tvAlarm.setVisibility(VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        setAllButtonsClickable(true);
                        tvAlarm.setVisibility(INVISIBLE);
                    }
                },5000);        //5초후 메시지박스 안보이게
            }
        });
    }

    @Override
    public void onPageActivate() {
        tvAlarm.setVisibility(INVISIBLE);
        authNum = "";
        tvauthnum.setText("");

        stopTimer();
        upate_timer.start();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
