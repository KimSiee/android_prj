/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 29 오후 5:27
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.CPConfig;
import com.joas.posco_slow_charlcd.ChargeData;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutTimer;

public class PaymentRealPayView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "PaymentRealPayView";

    TextView tvRealpaycost;
    TextView tvAlarm;
    TimeoutTimer timer = null;
    public String isPaymentSuccess = "";
    public boolean isRFPay = false;

    public PaymentRealPayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_creditcard_realpay,this, true);

        initComponents();

    }
    void initComponents() {
        tvRealpaycost = (TextView)findViewById(R.id.tvtitle1_realpay);
        tvAlarm = (TextView)findViewById(R.id.tvMsg_realpay);

        Button btHome = (Button)findViewById(R.id.btHome_realpay);
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useKakaoNavi) btHome.setVisibility(INVISIBLE);
        else {
            btHome.setVisibility(VISIBLE);
            btHome.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onHomeClick();
                }
            });
        }
    }
    public void onHomeClick(){flowManager.onPageCommonEvent(PageEvent.GO_HOME);}

    @Override
    public void onPageActivate() {
        tvAlarm.setVisibility(INVISIBLE);
        isPaymentSuccess = "";
        isRFPay = false;

        ChargeData chgData = flowManager.getChargeData();
        //책정된 실결제 금액 띄우기
        int realpaycost = (int)chgData.chargingCost;
        tvRealpaycost.setText("충전이 완료되었습니다.\n"+"최종 충전요금은 "+""+realpaycost+"원 입니다.");

        //실결제 요청
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        pinfo.paymentResultStat = "";
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.payReq_G((int)chgData.chargingCost,0,false,0);
    }

    @Override
    public void onPageDeactivate() {

    }

    //카드 RF이벤트 수신
    public void onRFTouch(){
        isPaymentSuccess = "";
        isRFPay = true;
        String msg = "[결제 진행중]\n\n결제 진행중입니다. 잠시만 기다려주세요.";
        onAlarmOccured(msg, VISIBLE);
    }

    //카드 INPUT 이벤트 수신
    public void onCardIn() {
        isPaymentSuccess = "";
        String msg = "[결제 진행중]\n\n결제 진행중입니다. 잠시만 기다려주세요.";
        onAlarmOccured(msg, VISIBLE);
    }
    //카드 OUT 이벤트 수신
    public void onCardOut(){
//        if(isPaymentSuccess.equals("01")) flowManager.onRealPaySuccess();
        if(isPaymentSuccess.equals("02")){
            //결제 재요청
            TL3500S tl3500S = flowManager.getTL3500S();
            ChargeData chgData = flowManager.getChargeData();
            tl3500S.payReq_G((int)chgData.chargingCost,0,false,0);
        }

        isRFPay = false;
        String msg = "";
        onAlarmOccured(msg,INVISIBLE);
    }
    //결제 성공 이벤트 수신
    public void onPaySuccess(){
        isPaymentSuccess = "01";
        flowManager.onRealPaySuccess();
//        String msg = "[결제완료]\n\n결제가 완료되었습니다. 카드를 빼주세요.";
//        onAlarmOccured(msg,VISIBLE);
    }
    //결제 실패 이벤트 수신
    public void onPayFailed() {
        isPaymentSuccess = "02";
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        String msg = "[결제실패]\n\n" + pinfo.paymentErrmsg + "\n오류코드:" + pinfo.paymentErrCode;
        if (isRFPay) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAlarm.setText(msg);
                    tvAlarm.setVisibility(VISIBLE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //3초뒤 수행 코드
                            onCardOut();
                        }
                    }, 3000);
                }
            });
        } else onAlarmOccured(msg, VISIBLE);
    }
    public void onCardFallback(){
        isPaymentSuccess = "";
        String msg = "[오류]\n카드삽입 오류\n카드방향을 확인해주세요";

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvAlarm.setText(msg);
                tvAlarm.setVisibility(VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //3초뒤 수행 코드
                        onCardOut();
                    }
                },3000);
            }
        });
    }

    public void onAlarmOccured(String msg, int visible){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvAlarm.setText(msg);
                tvAlarm.setVisibility(visible);
            }
        });
    }
}
