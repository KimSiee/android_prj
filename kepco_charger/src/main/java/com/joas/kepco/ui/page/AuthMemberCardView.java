/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 16 오후 5:24
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.UIFlowManager;

public class AuthMemberCardView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    private AnimationDrawable frameAnimation;
    private ImageView view;

    public AuthMemberCardView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_auth_member_card,this, true);

        initComponents();
    }

    void initComponents() {

        // 컨트롤 ImageView 객체를 가져온다
        view = (ImageView) findViewById(R.id.imageCreditCardTag);
        // 이미지를 동작시키기위해  AnimationDrawable 객체를 가져온다.
        frameAnimation = (AnimationDrawable) view.getBackground();

        // Test
        Button btAuthMemberCardNext = (Button)findViewById(R.id.btAuthMemberCardNext);
        btAuthMemberCardNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flowManager.onRfidDataReceive("1010010071615447", true);
            }
        });
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
