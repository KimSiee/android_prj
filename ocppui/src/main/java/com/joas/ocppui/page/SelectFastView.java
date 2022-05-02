/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.ocppui.R;
import com.joas.ocppui.UIFlowManager;


public class SelectFastView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public SelectFastView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_fast,this, true);

        initComponents();
    }

    void initComponents() {
        Button btAC3 = (Button) findViewById(R.id.btAC3);
        Button btChaDemo  = (Button) findViewById(R.id.btChaDemo);
        Button btDCCombo = (Button) findViewById(R.id.btDCCombo);

        btAC3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onAC3Click();
            }
        });
        btChaDemo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onChaDemoClick();
            }
        });
        btDCCombo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDCComboClick();
            }
        });
    }

    public void onAC3Click() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_AC3_CLICK);
    }
    public void onChaDemoClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CHADEMO_CLICK);
    }
    public void onDCComboClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_DCCOMBO_CLICK);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }
}
