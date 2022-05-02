/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.gridwiz_slow_charlcd.R;
import com.joas.gridwiz_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class ConnectorWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;

    public ConnectorWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connector_wait, this, true);

        initComponents();

        count = flowManager.getChargeData().connectorWaitTimeout;

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;

                if ( count == 0 )  {
                    stopTimer();
                    count = flowManager.getChargeData().connectorWaitTimeout;
                    flowManager.onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });
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
        Button btNext= (Button) findViewById(R.id.btConnectorWaitNext);
        btNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClick();
            }
        });
    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        count = flowManager.getChargeData().authTimeout;
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

    public void onHomeClick() {
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }

    // for Test!! // To..Do.. Disable
    public void onNextClick() {
        flowManager.onConnectedCableEvent(true);
    }
}
