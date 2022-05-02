/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_tardis.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.ocppui_tardis.R;
import com.joas.ocppui_tardis.UIFlowManager;
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

        count = flowManager.getOcppConfigConnectorTimeout();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                if ( count == 0 )  {
                    stopTimer();
                    count = flowManager.getChargeData().authTimeout;
                    flowManager.onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });

    }
    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }
    void startTimer() {
        stopTimer();
        timer.start();
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

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        count = flowManager.getOcppConfigConnectorTimeout();
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
