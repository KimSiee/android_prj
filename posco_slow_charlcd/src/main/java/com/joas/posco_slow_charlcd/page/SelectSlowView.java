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
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.CPConfig;
import com.joas.posco_slow_charlcd.ChargeData;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.TypeDefine;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.LogWrapper;


public class SelectSlowView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "SelectSlowView";

    TextView tvChargeCost;
    TextView tvHour;
    TextView tvMin;
    TextView tvAlarm;
    Button btBtype;
    Button btCtype;
    Button btHome;
    ImageView btHourPlus;
    ImageView btHourMinus;
    ImageView btMinPlus;
    ImageView btMinMinus;

    PoscoChargerInfo pinfo;
    CPConfig cpConfig;
    int maxsettime = 0;
    int minsettime = 0;

    int hour_minute = 0;
    int setHour = 0;
    int setMinute = 0;
    double nomemUnitcost = 0.0;

    public SelectSlowView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_slow,this, true);

        initComponents();
    }

    void initComponents() {
        CPConfig cpConfig = flowManager.getCpConfig();
        tvChargeCost = (TextView) findViewById(R.id.tvCost_timeselect);
        tvHour = (TextView) findViewById(R.id.tvHour_select);
        tvMin = (TextView) findViewById(R.id.tvMin_select);
        tvAlarm = (TextView) findViewById(R.id.tvMsg_TIMESELECT);

        btBtype = (Button) findViewById(R.id.btBtype);
        btCtype  = (Button) findViewById(R.id.btCtype);
        btHome = (Button) findViewById(R.id.btHome_TimeSelect);
        btHourPlus = (ImageView) findViewById(R.id.btHourPlus);
        btHourMinus = (ImageView) findViewById(R.id.btHourMinus);
        btMinPlus = (ImageView) findViewById(R.id.btMinPlus);
        btMinMinus = (ImageView) findViewById(R.id.btMinMinus);

        if(cpConfig.useKakaoNavi) {
            btBtype.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
            btCtype.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
            btHome.setVisibility(INVISIBLE);
        }
        else{
            btBtype.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));
            btCtype.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));
            btHome.setVisibility(VISIBLE);
            btHome.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onHomeClick();
                }
            });
        }
        btBtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBTypeClick();
            }
        });
        btCtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCTypeClick();
            }
        });

        btHourPlus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHourPlusClick();
            }
        });
        btHourMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHourMinusClick();
            }
        });
        btMinPlus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMinPlusClick();
            }
        });
        btMinMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMinMinusClick();
            }
        });
    }

    //시간설정 처리 함수
    void timeControl() {
        try {
            if(cpConfig.useKakaoNavi){
                if (hour_minute < minsettime) hour_minute = minsettime;
                if (hour_minute > maxsettime) {
                    hour_minute = maxsettime;
                    //최대시간 초과 알람 띄우기(아래)
                    tvAlarm.setText("1회 최대 " + String.valueOf(maxsettime) + "분 사용가능합니다.");
                    onAlarmOccured();
                }

                printTimeSelectInfo();

                //설정된 충전요구시간(분) 저장(RUN_CHECK시 종료시간 설정시 사용)
                pinfo.userSetTime = hour_minute;
            }
            else{
                if (pinfo.rsv_flag == 0) {
                    //예약 없을 경우
                    if (hour_minute < minsettime) hour_minute = minsettime;
                    if (hour_minute > maxsettime) {
                        hour_minute = maxsettime;
                        //최대시간 초과 알람 띄우기(아래)
                        tvAlarm.setText("1회 최대 " + String.valueOf(maxsettime) + "분 사용가능합니다.");
                        onAlarmOccured();
                    }

                    printTimeSelectInfo();

                    //설정된 충전요구시간(분) 저장(RUN_CHECK시 종료시간 설정시 사용)
                    pinfo.userSetTime = hour_minute;
                } else if (pinfo.rsv_flag == 1) {
                    int m_rsv_leftmin = (int)pinfo.rsv_leftMin;
                    if (hour_minute < 0) hour_minute = m_rsv_leftmin - 30;
                    if (hour_minute > (m_rsv_leftmin - 30)) {
                        hour_minute = m_rsv_leftmin - 30;
                        //예약시간 중복 알람 띄우기
                        tvAlarm.setText("설정하신 시간에 예약건이 존재합니다.\n최대 " + String.valueOf(hour_minute) + "분 충전 가능합니다.");
                        onAlarmOccured();
                    }

                    printTimeSelectInfo();

                    //설정된 충전요구시간(분) 저장(RUN_CHECK시 종료시간 설정시 사용)
                    pinfo.userSetTime = hour_minute;
                }
                else if(pinfo.rsv_flag == 3){
                    //예약자일경우 플러그 타입선택이 필요하므로 예약시간만 보여주기
                    hour_minute = pinfo.rsv_chargingTimeMin;
                    printTimeSelectInfo();
                }
            }


            //비회원 최초결제 금액 산정
            ChargeData chgdata = flowManager.getChargeData();
            if(chgdata.authType == TypeDefine.AuthType.NOMEMBER) {
                pinfo.paymentPreDealApprovalCost = (int) (0.2 * hour_minute * nomemUnitcost);
//                pinfo.paymentPreDealApprovalCost = 200;     //test
            }

        } catch (Exception e) {
            LogWrapper.d(TAG, " :" + e.toString());
        }
    }

    /**
     * TIME SELECT PAGE 모든 버튼 활성화/비활성화 컨트롤 함수
     */
    void setAllButtonsClickable(boolean setval){
        btBtype.setClickable(setval);
        btCtype.setClickable(setval);
        btHome.setClickable(setval);
        btHourMinus.setClickable(setval);
        btHourPlus.setClickable(setval);
        btMinMinus.setClickable(setval);
        btMinPlus.setClickable(setval);
    }

    void printTimeSelectInfo(){
        setHour = hour_minute / 60;
        setMinute = hour_minute % 60;
        tvHour.setText(String.valueOf(setHour));
        tvMin.setText(String.valueOf(setMinute));
    }


    void onHourPlusClick() {
        if (!cpConfig.useKakaoNavi && pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute += 60;
        }
        timeControl();
    }
    void onHourMinusClick(){
        if (!cpConfig.useKakaoNavi && pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute-=60;
        }
        timeControl();
    }
    void onMinPlusClick(){
        if (!cpConfig.useKakaoNavi && pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute+=10;
        }
        timeControl();
    }
    void onMinMinusClick() {
        if (!cpConfig.useKakaoNavi && pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute -= 10;
        }
        timeControl();
    }

    public void onBTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_BTYPE_CLICK);
    }
    public void onCTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CTYPE_CLICK);
    }
    public void onHomeClick(){flowManager.onPageCommonEvent(PageEvent.GO_HOME);}

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        //충전 최대,최소 설정 시간 로드
        pinfo = flowManager.getPoscoChargerInfo();
        cpConfig = flowManager.getCpConfig();
        maxsettime = Integer.valueOf(pinfo.maxsetTime);
        minsettime = Integer.valueOf(pinfo.minsetTime);

        //단가정보 로드
        getChargerCostInfo();

        //알람메시지 안보이게 설정
        tvAlarm.setVisibility(INVISIBLE);

        //초기 세팅시간 설정
        hour_minute = 300;
        timeControl();
    }

    /**
     * 단가정보 표시함수
     */
    void getChargerCostInfo(){
        ChargeData chgdata = flowManager.getChargeData();
        flowManager.setMemberCostUnit();
        tvChargeCost.setText("고객님의 충전단가는 "+ String.valueOf(chgdata.chargingUnitCost)+"원/kWh 입니다.");
        nomemUnitcost = chgdata.chargingUnitCost;
    }

    /**
     * 메시지 박스 표시 함수
     */
    public void onAlarmOccured() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setAllButtonsClickable(false);
                tvAlarm.setVisibility(VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setAllButtonsClickable(true);
                        tvAlarm.setVisibility(INVISIBLE);
                    }
                },5000);        //5초후 메시지박스 안보이게
            }
        });
    }

    @Override
    public void onPageDeactivate() {
    }
}
