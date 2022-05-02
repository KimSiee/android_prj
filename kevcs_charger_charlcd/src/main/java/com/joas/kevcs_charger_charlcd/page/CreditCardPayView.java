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
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.UIFlowManager;

public class CreditCardPayView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    private AnimationDrawable frameAnimation;
    private ImageView view;

    public CreditCardPayView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_credit_card_pay,this, true);

        initComponents();
    }

    void initComponents() {

        // 컨트롤 ImageView 객체를 가져온다
        view = (ImageView) findViewById(R.id.imageCreditCardTag);
        // 이미지를 동작시키기위해  AnimationDrawable 객체를 가져온다.
        frameAnimation = (AnimationDrawable) view.getBackground();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        frameAnimation.start();
    }

    @Override
    public void onPageDeactivate() {
        frameAnimation.stop();
    }
}
