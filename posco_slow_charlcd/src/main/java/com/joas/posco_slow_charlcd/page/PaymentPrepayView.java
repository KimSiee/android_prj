/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 29 오전 11:34
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
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutTimer;

public class PaymentPrepayView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "PaymentPrepayView";

    TextView tvPrepaycost;
    TextView tvAlarm;
    TimeoutTimer timer = null;
    public String isPaymentSuccess = "";
    public boolean isRFPay = false;

    public PaymentPrepayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_creditcard_prepay,this, true);

        initComponents();

    }
    void initComponents() {
        tvPrepaycost = (TextView)findViewById(R.id.tvtitle1_prepay);
        tvAlarm = (TextView)findViewById(R.id.tvMsg_prepay);

        Button btHome = (Button)findViewById(R.id.btHome_prepay);
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useKakaoNavi) btHome.setVisibility(INVISIBLE);
        else{
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
        isRFPay = false;        //RF결제(삼성페이)시에 화면 넘기기 위한 변수
        tvAlarm.setVisibility(INVISIBLE);
        isPaymentSuccess = "";

        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        //책정된 선결제 금액 띄우기
        int prepaycost = pinfo.paymentPreDealApprovalCost;
        tvPrepaycost.setText("결제하실 충전요금은\n"+""+prepaycost+"원 입니다.");

        //최초결제 요청
        pinfo.paymentResultStat = "";
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.payReq_G(pinfo.paymentPreDealApprovalCost,0,true,0);
    }

    @Override
    public void onPageDeactivate() {
//        stopTimer();
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
        if(isPaymentSuccess.equals("01")) flowManager.onPrepaySuccess();
        else if(isPaymentSuccess.equals("02")){
            //결제 재요청
            TL3500S tl3500S = flowManager.getTL3500S();
            PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
            tl3500S.payReq_G(pinfo.paymentPreDealApprovalCost, 0, true, 0);
        }

        String msg = "";
        onAlarmOccured(msg,INVISIBLE);
        isRFPay = false;
    }
    //결제 성공 이벤트 수신
    public void onPaySuccess(){
        isPaymentSuccess = "01";
        if(isRFPay){
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAlarm.setText("[결제완료]\n\n결제가 완료되었습니다. 잠시만 기다려주세요.");
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
        else {
            String msg = "[결제완료]\n\n결제가 완료되었습니다. 카드를 빼주세요.";
            onAlarmOccured(msg, VISIBLE);
        }
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
