/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 29 오후 2:14
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

public class PaymentCancelpayView extends LinearLayout implements PageActivateListener {

    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "PaymentCancelpayView";

    public String isPaymentSuccess = "";
    TextView tvCancelpaycost;
    TextView tvAlarm;
    TimeoutTimer timer = null;
    public boolean isRFPay = false;

    public PaymentCancelpayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_creditcard_cancelpay,this, true);

        initComponents();

//        timer = new TimeoutTimer(1000, new TimeoutHandler() {
//            @Override
//            public void run() {
//                waitPayResultStat();
//            }
//        });
    }
    void initComponents() {
        tvAlarm = (TextView)findViewById(R.id.tvMsg_cancelpay);
        tvCancelpaycost = (TextView)findViewById(R.id.tvtitle1_cancelpay);

        Button btHome = (Button)findViewById(R.id.btHome_cancelpay);
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
        isRFPay = false;
        ChargeData chargeData = flowManager.getChargeData();
        if(chargeData.realpayflag) {
            onCardIn();
        }
        else tvAlarm.setVisibility(INVISIBLE);



        isPaymentSuccess = "";
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        //미처리된 결제건 있는지 먼저 확인
        if(pinfo.nomemAuthResultFlag == 0x01 || pinfo.nomemAuthResultFlag == 0x02) {
            int cancelpaycost = pinfo.recvfromserver_prepayPrice;
            tvCancelpaycost.setText("미처리된 취소결제건이 있습니다.\n취소금액은 " + "" + cancelpaycost + "원 입니다.");
            //미처리 결제취소건 취소 요청
            pinfo.paymentResultStat = "";
            TL3500S tl3500S = flowManager.getTL3500S();
            tl3500S.cancelPayFromServer(pinfo.recvfromserver_prepayPrice, 0, pinfo.recvfromserver_prepayauthnum, pinfo.recvfromserver_prepayDatetime, 0);
        }
        else {
            pinfo.paymentDealCancelApprovalCost = pinfo.paymentPreDealApprovalCost;
            //취소결제 금액 띄우기
            int cancelpaycost = pinfo.paymentDealCancelApprovalCost;
            tvCancelpaycost.setText("취소하실 금액은 " + "" + cancelpaycost + "원 입니다.");
            //선결제취소 요청
            pinfo.paymentResultStat = "";
            TL3500S tl3500S = flowManager.getTL3500S();
            tl3500S.cancelPrePay(0);
        }

    }

    @Override
    public void onPageDeactivate() {

    }

    //카드 RF이벤트 수신
    public void onRFTouch() {
        isPaymentSuccess = "";
        isRFPay = true;
        String msg = "[결제 진행중]\n\n결제 진행중입니다. 잠시만 기다려주세요.";
        onAlarmOccured(msg, VISIBLE);
    }

    //카드 INPUT 이벤트 수신
    public void onCardIn(){
        isPaymentSuccess = "";
        String msg = "[취소 진행중]\n\n결제취소 진행중입니다. 잠시만 기다려주세요.";
        onAlarmOccured(msg,VISIBLE);
    }
    //카드 OUT 이벤트 수신
    public void onCardOut(){
        if(isPaymentSuccess.equals("01")) flowManager.onCancelPaySuccess();
        else if(isPaymentSuccess.equals("02")){
            PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
            TL3500S tl3500S = flowManager.getTL3500S();
            if(pinfo.nomemAuthResultFlag == 0x01 || pinfo.nomemAuthResultFlag == 0x02) {
                //미처리 취소진행중이었다면 서버요청전문 취소 요청
                tl3500S.cancelPayFromServer(pinfo.recvfromserver_prepayPrice, 0, pinfo.recvfromserver_prepayauthnum, pinfo.recvfromserver_prepayDatetime, 0);
            }
            else {
                //결제 재요청
                tl3500S.cancelPrePay(0);
            }
        }
        String msg = "";
        onAlarmOccured(msg,INVISIBLE);
    }
    //결제 성공 이벤트 수신
    public void onCancelSuccess(){
        isPaymentSuccess = "01";
        if(isRFPay){
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAlarm.setText("[취소완료]\n\n결제취소가 완료되었습니다. 잠시만 기다려주세요.");
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
            String msg = "[취소완료]\n\n결제취소가 완료되었습니다. 카드를 빼주세요.";
            onAlarmOccured(msg, VISIBLE);
        }
    }
    //결제 실패 이벤트 수신
    public void onCancelFailed(){
        isPaymentSuccess = "02";
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        String msg = "[취소실패]\n\n" + pinfo.paymentErrmsg + "\n오류코드:" + pinfo.paymentErrCode;
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
