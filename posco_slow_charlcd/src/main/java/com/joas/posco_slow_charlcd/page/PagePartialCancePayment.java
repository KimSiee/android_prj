/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 9. 30. 오후 12:16
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.ChargeData;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;

public class PagePartialCancePayment extends LinearLayout implements PageActivateListener{
    UIFlowManager flowManager;
    Activity mainActivity;
    public static final String TAG = "PagePartialCancelPayment";

    TextView tvMsg;
    public PagePartialCancePayment(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_partialpayment_wait,this, true);

        initComponents();

    }

    void initComponents() {
        tvMsg = (TextView)findViewById(R.id.tvmsg_payment);
    }

    @Override
    public void onPageActivate() {
        String msg = "충전금액을 정산중입니다.\n잠시만 기다려주세요.";
        tvMsg.setText(msg);

        ChargeData chargeData = flowManager.getChargeData();
        TL3500S tl3500S = flowManager.getTL3500S();
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();

        int cancelType = chargeData.partialCancelType;

        switch (cancelType) {
            case 4:
                //무카드취소(선결제 전체 취소)
                tl3500S.cancelPay_Partial(0, pinfo.paymentPreDealApprovalCost, 0,
                        pinfo.paymentPreDealApprovalNo, pinfo.paymentPreDealApprovalTime, pinfo.paymentPreDealSerialNo, 4);
                break;

            case 5:
                //부분취소
                int partialCancelCost = pinfo.paymentPreDealApprovalCost - (int)chargeData.chargingCost;
                tl3500S.cancelPay_Partial(0, partialCancelCost, 0,
                        pinfo.paymentPreDealApprovalNo, pinfo.paymentPreDealApprovalTime, pinfo.paymentPreDealSerialNo, 5);
                break;
        }
    }

    //결제취소 성공 이벤트 수신
    public void onCancelSuccess(){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMsg.setText("충전금액 정산이 완료되었습니다.\n잠시만 기다려주세요.");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //3초뒤 수행 코드
                        flowManager.onPartialCancelPaySuccess();
                    }
                },8000);
            }
        });
    }

    //결제취소 실패 이벤트 수신
    public void onCancelFailed(){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
                String msg = "취소결제 실패\n\n" + pinfo.paymentErrmsg + "\n오류코드:" + pinfo.paymentErrCode;
                tvMsg.setText(msg);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //3초뒤 수행 코드
                        flowManager.onPartialCancelPaySuccess();
                    }
                },8000);
            }
        });
    }

    @Override
    public void onPageDeactivate() {

    }
}
