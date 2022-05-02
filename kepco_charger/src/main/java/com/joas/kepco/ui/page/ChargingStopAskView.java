/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 14 오후 5:28
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.TypeDefine;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ChargingStopAskView extends FrameLayout implements PageActivateListener {
    Activity mainActivity;
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 10;

    public ChargingStopAskView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_charging_stop_yn, this, true);

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                if ( count == 0 || flowManager.getIsDspPlug() == false)  {
                    timer.cancel();
                    flowManager.onChargingStopAskResult(false);
                }
            }
        });
    }

    void initMessageBox() {
        ImageButton btCancel = findViewById(R.id.btCancel);
        ImageButton btStop= findViewById(R.id.btStop);

        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.onChargingStopAskResult(false);
            }
        });

        btStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.onChargingStopAskResult(true);
            }
        });
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initMessageBox();
        count = 10;
        timer.start();
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
    }
}