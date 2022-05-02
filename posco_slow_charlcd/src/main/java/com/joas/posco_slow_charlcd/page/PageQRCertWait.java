/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 8. 25. 오전 11:02
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;

public class PageQRCertWait extends LinearLayout implements PageActivateListener{
    UIFlowManager flowManager;
    public PageQRCertWait(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_kakao_qrcert_wait, this, true);

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
