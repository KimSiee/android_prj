/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 17 오전 9:54
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kevcs.ui.ChargeData;
import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.UIFlowManager;
import com.joas.kevcscomm.KevcsProtocol;

public class CreditCardPayView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    ChargeData chargeData;

    TextView tvPrePayValue;
    TextView tvPrekWhValue;
    TextView tvPreUcostValue;

    private AnimationDrawable frameAnimation;
    private ImageView view;
    int inputcost;
    double costUnit = 100.5;
    public CreditCardPayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        double costUnit = flowManager.getKevcsComm().getCostManager().getNoMemberCost();

        chargeData = flowManager.getChargeData();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_credit_card_pay_old,this, true);

        initComponents();
    }

    void initComponents() {

        tvPrePayValue = (TextView)findViewById(R.id.tvPrePayValue);
        tvPrekWhValue = (TextView)findViewById(R.id.tvPrekWhValue);
        tvPreUcostValue = (TextView)findViewById(R.id.tvPreUcostValue);

        // 컨트롤 ImageView 객체를 가져온다
        view = (ImageView) findViewById(R.id.imageCreditCardTag);
        // 이미지를 동작시키기위해  AnimationDrawable 객체를 가져온다.
        frameAnimation = (AnimationDrawable) view.getBackground();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        frameAnimation.start();
        costUnit = flowManager.getKevcsComm().getCostManager().getNoMemberCost();

        if(flowManager.getKevcsComm().getChargeReqCfmMethod() == KevcsProtocol.CHARGE_REQ_CFM_METHOD_FULL){ //풀충전이면
            inputcost = flowManager.getKevcsComm().getCostManager().getBasePaymAmt();
        }else{
            inputcost = chargeData.reqPayCost;//입력금액
        }

        tvPrePayValue.setText(String.format("%d원",inputcost));
        tvPrePayValue.invalidate();
        tvPrekWhValue.setText(String.format("%.1fkwH",inputcost/costUnit));
        tvPrekWhValue.invalidate();
        tvPreUcostValue.setText(String.format("%.1f원/kWh",costUnit));  //단가
        tvPreUcostValue.invalidate();

    }

    @Override
    public void onPageDeactivate() {
        frameAnimation.stop();
    }
}
