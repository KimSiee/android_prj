/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;

public class CardTagView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Button btHome;
    public CardTagView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_cardtag, this, true);

        initComponents();
    }

    void initComponents() {
        btHome = (Button) findViewById(R.id.btCardTagHome);

        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }

    public void onHomeClick() {
        Volvo2chChargerInfo vinfo = flowManager.getVolvoChargerInfo();
        if(vinfo.isChgStopAsk) flowManager.onPageCommonEvent(PageEvent.SELECT_KEEP_CHARGING);
        else flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }
}
