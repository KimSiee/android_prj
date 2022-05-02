/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.utils.LogWrapper;
import com.joas.volvo_touch_2ch.ChargeData;
import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;


public class SelectSlowView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "SelectSlowView";

    //components
    TextView tvChargeCost;
    TextView tvHour;
    TextView tvMin;
    TextView tvAlarm;
    Button btCtype;
    Button btHome;
    ImageView btHourPlus;
    ImageView btHourMinus;
    ImageView btMinPlus;
    ImageView btMinMinus;

    Volvo2chChargerInfo pinfo;
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
        tvChargeCost = (TextView)findViewById(R.id.tvUnitCost);
        tvHour = (TextView)findViewById(R.id.tvHour);
        tvMin = (TextView)findViewById(R.id.tvMin);
        tvAlarm = (TextView)findViewById(R.id.tvMsg_TIMESELECT);

        btCtype = (Button)findViewById(R.id.btCtype);
        btHome = (Button)findViewById(R.id.btHome);
        btHourPlus = (ImageView)findViewById(R.id.btHourPlus);
        btHourMinus = (ImageView)findViewById(R.id.btHourMinus);
        btMinPlus = (ImageView)findViewById(R.id.btMinPlus);
        btMinMinus = (ImageView)findViewById(R.id.btMinMinus);

        btCtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCTypeClick();
            }
        });
        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHomeClick();
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

    public void onBTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_BTYPE_CLICK);
    }
    public void onCTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CTYPE_CLICK);
    }
    public void onHomeClick(){flowManager.onPageCommonEvent(PageEvent.GO_HOME);}
    void onHourPlusClick() {
        if (pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute += 60;
        }
        timeControl();
    }
    void onHourMinusClick(){
        if (pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute-=60;
        }
        timeControl();
    }
    void onMinPlusClick(){
        if (pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute+=10;
        }
        timeControl();
    }
    void onMinMinusClick() {
        if (pinfo.rsv_flag == 3) {
            //예약자 시간설정 불가 알림 띄우기
            tvAlarm.setText("예약시간 변경이 불가능합니다.\n설정하신 예약시간 동안만 충전이 가능합니다.");
            onAlarmOccured();
        } else {
            hour_minute -= 10;
        }
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
     * TIME SELECT PAGE 모든 버튼 활성화/비활성화 컨트롤 함수
     */
    void setAllButtonsClickable(boolean setval){
        btCtype.setClickable(setval);
        btHome.setClickable(setval);
        btHourMinus.setClickable(setval);
        btHourPlus.setClickable(setval);
        btMinMinus.setClickable(setval);
        btMinPlus.setClickable(setval);
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

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        //충전 최대,최소 설정 시간 로드
        pinfo = flowManager.getVolvoChargerInfo();
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

    //시간설정 처리 함수
    void timeControl() {
        try {
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

//            //비회원 최초결제 금액 산정
//            ChargeData chgdata = flowManager.getChargeData();
//            if(chgdata.authType == TypeDefine.AuthType.NOMEMBER) {
//                pinfo.paymentPreDealApprovalCost = (int) (0.2 * hour_minute * nomemUnitcost);
////                pinfo.paymentPreDealApprovalCost = 200;     //test
//            }

        } catch (Exception e) {
            LogWrapper.d(TAG, " :" + e.toString());
        }
    }

    void printTimeSelectInfo(){
        setHour = hour_minute / 60;
        setMinute = hour_minute % 60;
        tvHour.setText(String.valueOf(setHour));
        tvMin.setText(String.valueOf(setMinute));
    }

    @Override
    public void onPageDeactivate() {
    }
}
