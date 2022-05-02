/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 25 오후 5:45
 *
 */

package com.joas.joasui_iot_video.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import com.joas.joasui_iot_video.ui.R;
import com.joas.joasui_iot_video.ui.UIFlowManager;

public class SelectFastView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    ImageView imageAC3;
    ImageView imageChademo;
    ImageView imageCombo;
    Space spaceAC3;
    Space spaceChademo;
    Space spaceCombo;

    Button btAC3;
    Button btChaDemo;
    Button btDCCombo;

    public SelectFastView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_fast,this, true);

        initComponents();
    }

    void initComponents() {
        imageAC3 = (ImageView) findViewById(R.id.imageAC3);
        imageChademo = (ImageView) findViewById(R.id.imageChademo);
        imageCombo = (ImageView) findViewById(R.id.imageCombo);
        spaceAC3 = (Space) findViewById(R.id.spaceAC3);
        spaceChademo = (Space) findViewById(R.id.spaceChademo);
        spaceCombo = (Space) findViewById(R.id.spaceCombo);

        btAC3 = (Button) findViewById(R.id.btAC3);
        btChaDemo  = (Button) findViewById(R.id.btChaDemo);
        btDCCombo = (Button) findViewById(R.id.btDCCombo);

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
        if ( flowManager.getCpConfig().ac3Use ) {
            imageAC3.setVisibility(VISIBLE);
            btAC3.setVisibility(VISIBLE);
            spaceAC3.setVisibility(VISIBLE);
        }
        else {
            imageAC3.setVisibility(GONE);
            btAC3.setVisibility(GONE);
            spaceAC3.setVisibility(GONE);
        }

        if ( flowManager.getCpConfig().chademoUse ) {
            imageChademo.setVisibility(VISIBLE);
            btChaDemo.setVisibility(VISIBLE);
            spaceChademo.setVisibility(VISIBLE);
        }
        else {
            imageChademo.setVisibility(GONE);
            btChaDemo.setVisibility(GONE);
            spaceChademo.setVisibility(GONE);
        }

        if ( flowManager.getCpConfig().ac3Use == false && flowManager.getCpConfig().chademoUse == false ) {
            LinearLayout.LayoutParams param =  (LinearLayout.LayoutParams)spaceChademo.getLayoutParams();
            param.weight = 0.5f;
            spaceChademo.requestLayout();

            param =  (LinearLayout.LayoutParams)spaceCombo.getLayoutParams();
            param.weight = 0.5f;
            spaceChademo.requestLayout();

            spaceChademo.setVisibility(VISIBLE);
            spaceCombo.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onPageDeactivate() {
    }
}
