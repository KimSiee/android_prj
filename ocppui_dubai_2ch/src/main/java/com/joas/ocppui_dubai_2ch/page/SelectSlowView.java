/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_dubai_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.joas.ocppui_dubai_2ch.R;
import com.joas.ocppui_dubai_2ch.UIFlowManager;


public class SelectSlowView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public SelectSlowView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_slow,this, true);

        initComponents();
    }

    void initComponents() {
        Button btBtype = (Button) findViewById(R.id.btAC3);
        Button btCtype  = (Button) findViewById(R.id.btChaDemo);

        btBtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBTypeClick();
            }
        });
        btCtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCTypeClick();
            }
        });
    }

    public void onBTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_BTYPE_CLICK);
    }
    public void onCTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CTYPE_CLICK);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }
}
