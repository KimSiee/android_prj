/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 4. 21 오후 3:02
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

import java.util.Timer;

public class InputCellPhonNumView extends LinearLayout implements PageActivateListener{
    Timer timer = null;             //현재 뷰 보이기 시간 타이머
    UIFlowManager flowManager;
    Activity mainActivity;

    public static final String TAG = "InputCellPhonNumView";
    TextView tvcpnum;
    TextView tvAlarm;
    String cpnum;

    public InputCellPhonNumView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_cellphone_num, this, true);

        initComponents();
    }

    void initComponents() {
        CPConfig cpConfig = flowManager.getCpConfig();
        tvcpnum = findViewById(R.id.tvcpnum);
        tvcpnum.setText("");

        tvAlarm = findViewById(R.id.tvMsg_CP);

        //숫자버튼 초기화
        int[] btNumIds = { R.id.btNum0_cp, R.id.btNum1_cp, R.id.btNum2_cp, R.id.btNum3_cp, R.id.btNum4_cp,
                R.id.btNum5_cp, R.id.btNum6_cp, R.id.btNum7_cp, R.id.btNum8_cp, R.id.btNum9_cp };
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
        Button btBS = findViewById(R.id.btNumBS_cp);
        btBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumBS();
            }
        });

        Button btClear = findViewById(R.id.btNumClear_cp);
        btClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumClear();
            }
        });

        Button btCancel= findViewById(R.id.btNumCancel_cp);
        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnCancelClick();
            }
        });

        Button btOK= findViewById(R.id.btNumOK_cp);
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnOKClick();
            }
        });
    }

    void OnClickNum(String number) {
        cpnum += number;
        textviewControl();
    }
    void OnNumBS() {
        try {
            cpnum = cpnum.replaceFirst(".$", "");
            tvcpnum.setText(cpnum);
        }
        catch(Exception e) {
            LogWrapper.d(TAG, " :" + e.toString());
        }

    }
    void OnNumClear() {
        cpnum = "";
        tvcpnum.setText("");
    }
    void OnCancelClick(){
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }
    void OnOKClick() {
        if (cpnum.length() < 11 || cpnum.length() > 11 || cpnum.equals("")) {
            onAlarmOccured();
        }
        else {
            PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
            pinfo.paymentNoMemberPhone = cpnum + "     ";       //공백 5칸 붙임, 16 ASC
            flowManager.onCellPhoneNumInputOK();
        }
    }

    void textviewControl() {
        try {
            if (cpnum.length() > 11) {
                onAlarmOccured();
            } else tvcpnum.setText(cpnum);
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
        tvcpnum.setText("");
        cpnum = "";
    }

    @Override
    public void onPageDeactivate() {

    }
}
