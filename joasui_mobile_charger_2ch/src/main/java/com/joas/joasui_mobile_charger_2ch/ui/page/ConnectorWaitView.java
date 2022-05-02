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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.joasui_mobile_charger_2ch.ui.R;
import com.joas.joasui_mobile_charger_2ch.ui.UIFlowManager;

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
        Button btHome = (Button) findViewById(R.id.btConnectorWaitHome);

        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        // Test Code
        Button btConnectorWaitStart= (Button) findViewById(R.id.btConnectorWaitStart);
        btConnectorWaitStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartClick();
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
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }

    public void onStartClick() {
        flowManager.onPageCommonEvent(PageEvent.START_CHARGE);
    }
}
