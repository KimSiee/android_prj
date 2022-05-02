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
import com.joas.gridwiz_slow_charlcd.page.PageActivateListener;
import com.joas.gridwiz_slow_charlcd.page.PageEvent;

public class CardTagView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    public CardTagView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_cardtag, this, true);

        initComponents();
    }

    void initComponents() {
        Button btHome = (Button) findViewById(R.id.btCardTagHome);

        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        // Test Code
        Button btNext= (Button) findViewById(R.id.btCardTagNext);
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
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }

    // for Test!! // To..Do.. Disable
    public void onNextClick() {
        flowManager.onCardTagEvent("1040001060337660", true);
    }
}
