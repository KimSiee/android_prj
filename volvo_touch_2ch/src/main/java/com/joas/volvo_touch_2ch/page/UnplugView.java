/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 8. 11. 오후 12:05
 *
 */

package com.joas.volvo_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;

public class UnplugView extends LinearLayout implements PageActivateListener{
    UIFlowManager flowManager;

    public UnplugView(Context context, UIFlowManager manager, Activity activity){
        super(context);

        flowManager = manager;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_unplug,this, true);

        initComponents();
    }
    void initComponents() {

    }
    @Override
    public void onPageActivate() {

    }

    @Override
    public void onPageDeactivate() {

    }
}
