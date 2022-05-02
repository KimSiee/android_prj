/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 8. 11. 오후 2:19
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

public class PageAskStopCharging extends LinearLayout implements PageActivateListener{
    UIFlowManager flowManager;
    Button btStopchg;
    Button btCancel;

    public PageAskStopCharging(Context context, UIFlowManager manager, Activity activity){
        super(context);

        flowManager = manager;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_chgstop_notice,this, true);

        initComponents();
    }

    public void initComponents(){

        btStopchg = (Button)findViewById(R.id.btStopCharging);
        btCancel = (Button)findViewById(R.id.btCancel);

        btStopchg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChgStopClick();
            }
        });

        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onKeepChg();
            }
        });
    }

    public void onChgStopClick() {
        flowManager.onChargingStopAskNoticePageEvent(PageEvent.SELECT_STOP_CHARGING);
    }
    public void onKeepChg(){
        flowManager.onChargingStopAskNoticePageEvent(PageEvent.SELECT_KEEP_CHARGING);
    }


    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {

    }
}
