/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.UIFlowManager;

public class SelectFastView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public SelectFastView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_fast,this, true);

        initComponents();
    }

    void initComponents() {
        ImageButton btAc3  = (ImageButton) findViewById(R.id.btSelAC3);
        ImageButton btChademo = (ImageButton) findViewById(R.id.btSelChademo);
        ImageButton btDCCombo = (ImageButton) findViewById(R.id.btSelCombo);

        btAc3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onAC3Click();
            }
        });
        btChademo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onChaDemoClick();
            }
        });
        btDCCombo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDCComboClick();
            }
        });

    }

    public void onAC3Click() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_AC3_CLICK);
    }

    public void onChaDemoClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_CHADEMO_CLICK);
    }

    public void onDCComboClick() {
        flowManager.onPageSelectEvent(PageEvent.SELECT_DCCOMBO_CLICK);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }
}
