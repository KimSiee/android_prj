/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kevcs_charger_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.UIFlowManager;

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
        ImageButton btBTtype  = (ImageButton) findViewById(R.id.btSelBType);
        ImageButton btCTtype  = (ImageButton) findViewById(R.id.btSelCType);

        btBTtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBTypeClick();
            }
        });
        btCTtype.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onChaDemoClick();
            }
        });

    }

    public void onBTypeClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_BTYPE_CLICK);
    }

    public void onChaDemoClick() {
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
