/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 14 오후 12:28
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

public class PageAuthSelectView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TextView tvKakaoQRCost;
    TextView tvKakaoCreditCost;

    public PageAuthSelectView(Context context, UIFlowManager manager, Activity activity){
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_auth,this, true);

        initComponents();
    }
    void initComponents() {
        CPConfig cpConfig = flowManager.getCpConfig();

        //단가표시(카카오향일때만 보이기)
        tvKakaoQRCost = (TextView)findViewById(R.id.tvQRCost);
        tvKakaoCreditCost = (TextView)findViewById(R.id.tvCreditCost);


        //처음으로 버튼
        Button btHome = (Button) findViewById(R.id.btHome_Authselect);
        if(cpConfig.useKakaoNavi){
            btHome.setVisibility(INVISIBLE);        //기존 사용하던 처음버튼 사라지게
        }
        else{
            btHome.setVisibility(VISIBLE);

        }
        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHomeClick();
            }
        });


        //회원카드 사용 버튼
        Button btMember = (Button) findViewById(R.id.btMember);

        //신용카드 사용 버튼
        Button btNomem = (Button) findViewById(R.id.btNomember);


        //버튼 이미지 변경(차지비, 카카오)
        if(cpConfig.useKakaoNavi){
            btMember.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));
            btNomem.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector_kakao));

            //카카오 충전기일 경우 카카오내비 QR 인증으로 동작되도록
            btMember.setText("카카오내비");
        }
        else{
            btMember.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));
            btNomem.setBackground(ActivityCompat.getDrawable(getContext(),R.drawable.button_selector));

            //차지비 충전기일 경우 회원카드로 문구 변경
            btMember.setText("회원 카드");
        }

        btMember.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMemberClick();
            }
        });

        btNomem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onNomemberClick();
            }
        });
    }
    public void onHomeClick(){flowManager.onPageCommonEvent(PageEvent.GO_HOME);}
    public void onMemberClick(){
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.useKakaoNavi) flowManager.onPageCommonEvent(PageEvent.SELECT_QRCERT_CLICK);
        else flowManager.onPageCommonEvent(PageEvent.SELECT_MEMBER_CLICK);
    }
    public void onNomemberClick(){flowManager.onPageCommonEvent(PageEvent.SELECT_NOMEMBER_CLICK);}
    @Override
    public void onPageActivate() {
        CPConfig cpConfig = flowManager.getCpConfig();
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        if(cpConfig.useKakaoNavi) {
            tvKakaoQRCost.setVisibility(VISIBLE);
            tvKakaoCreditCost.setVisibility(VISIBLE);
            if (cpConfig.useKakaoCost) {
                //카카오 현장설정단가 보이기
                tvKakaoQRCost.setText("충전단가 : " + "" + cpConfig.kakaoQRCost + "원/kWh");
                tvKakaoCreditCost.setText("충전단가 : " + "" + cpConfig.kakaoCreditCost + "원/kWh");
            } else {
                //서버에서 내려준 단가 보이기
                tvKakaoQRCost.setText("충전단가 : " + "" + (pinfo.memberCostUnit / 100.0) + "원/kWh");
                tvKakaoCreditCost.setText("충전단가 : " + "" + (pinfo.nonMemberCostUnit / 100.0) + "원/kWh");
            }
        }
        else{
            tvKakaoQRCost.setVisibility(INVISIBLE);
            tvKakaoCreditCost.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void onPageDeactivate() {

    }
}
