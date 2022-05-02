/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.joas.minsu_ui.CPConfig;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.UIFlowManager;

public class ConnectorWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public ConnectorWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connector_wait, this, true);

        initComponents();
    }

    void initComponents() {
        ImageButton btHome = (ImageButton) findViewById(R.id.btConnectorWaitHome);

        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        ImageButton btNext= (ImageButton) findViewById(R.id.btConnectorWaitNext);
        btNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClick();
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
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.isTestMode) flowManager.onPageCommonEvent(PageEvent.GO_PREV_CONNECTOR_WAIT_TO_TAG_CARD);
        else flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }

    // for Test!! // To..Do.. Disable
    public void onNextClick() {
        CPConfig cpConfig = flowManager.getCpConfig();

        if(cpConfig.isTestMode){
            flowManager.onConnectedCableEvent(true);
        }
        else{
            flowManager.setUIConnectorWaitStat();
        }

    }
}
