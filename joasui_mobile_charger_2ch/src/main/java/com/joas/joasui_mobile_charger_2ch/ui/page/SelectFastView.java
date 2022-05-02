/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:17
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui.page;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.joas.joasui_mobile_charger_2ch.ui.ChargeData;
import com.joas.joasui_mobile_charger_2ch.ui.R;
import com.joas.joasui_mobile_charger_2ch.ui.TypeDefine;
import com.joas.joasui_mobile_charger_2ch.ui.UIFlowManager;

import java.lang.reflect.Type;

public class SelectFastView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TextView tvPowerLimit;

    ImageView imageCombo1;
    ImageView imageCombo2;
    Button btDCCombo1;
    Button btDCCombo2;
    Button btModeChange;
    boolean modeDual = true;
    private long lastClickTime = 0;

    public SelectFastView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_fast,this, true);

        initComponents();
    }

    void initComponents() {
        Button btChademo = (Button) findViewById(R.id.btChademo);
        btDCCombo1  = (Button) findViewById(R.id.btDCCombo1);
        btDCCombo2 = (Button) findViewById(R.id.btDCCombo2);
        imageCombo1 = (ImageView)findViewById(R.id.imageCombo1);
        imageCombo2 = (ImageView)findViewById(R.id.imageCombo2);

        btChademo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onChaDemoClick();
            }
        });
        btDCCombo1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDCCombo1Click();
            }
        });
        btDCCombo2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDCCombo2Click();
            }
        });

        btModeChange = (Button)findViewById(R.id.btModeChange);
        btModeChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // preventing double, using threshold of 500 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < 500){
                    return;
                }

                lastClickTime = SystemClock.elapsedRealtime();

                onChangeModeClick();
            }
        });
    }

    public void onChaDemoClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CHADEMO_CLICK);
    }
    public void onDCCombo1Click() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_DCCOMBO1_CLICK);
    }
    public void onDCCombo2Click() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_DCCOMBO2_CLICK);
    }
    public void onChangeModeClick() {
        ChargeData cData = flowManager.getChargeData();
        modeDual = !modeDual;
        if ( modeDual == false ) {
            cData.powerLimitValue = TypeDefine.POWER_SINGLE_MODE; // single mode
            flowManager.onModeChange(true);
            btModeChange.setText("싱글 모드(26kWh) 운영중");
        } else {
            cData.powerLimitValue = TypeDefine.POWER_DOUBLE_MODE; // single mode
            flowManager.onModeChange(false);
            btModeChange.setText("더블 모드(13kWh) 운영중");
        }
    }
    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {


        if ( flowManager.getChargeData().dspChannel == 0 ) {
            imageCombo2.setVisibility(GONE);
            btDCCombo2.setVisibility(GONE);
        }
        else {
            imageCombo1.setVisibility(GONE);
            btDCCombo1.setVisibility(GONE);
        }
    }

    @Override
    public void onPageDeactivate() {
    }
}
