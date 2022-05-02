/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 16 오전 10:39
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.UIFlowManager;

public class SelectAuthView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TextView tvCreditCard;
    ImageButton btCreditCard;

    public SelectAuthView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_select_auth,this, true);

        initComponents();
    }

    void initComponents() {
        ImageButton btMemberCard = (ImageButton)findViewById(R.id.btMemberCard);
        ImageButton btMemberNum = (ImageButton)findViewById(R.id.btMemberNum);
        btCreditCard = (ImageButton)findViewById(R.id.btCreditCard);
        tvCreditCard = findViewById(R.id.tvCreditCard);

        btMemberCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onMemberCardClick();
            }
        });
        btMemberNum.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onMemberNumClick();
            }
        });
        btCreditCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCreditCardClick();
            }
        });
    }

    public void onMemberCardClick() {
        flowManager.onPageSelectAuthEvent(PageEvent.SELECT_MEMBER_CARD);
    }
    public void onMemberNumClick() {
        flowManager.onPageSelectAuthEvent(PageEvent.SELECT_MEMBER_NUM);
    }
    public void onCreditCardClick() {
        flowManager.onPageSelectAuthEvent(PageEvent.SELECT_CREDIT_CARD);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        if (flowManager.getCpConfig().usePayTerminal) {
            tvCreditCard.setVisibility(VISIBLE);
            btCreditCard.setVisibility(VISIBLE);
        }
        else {
            tvCreditCard.setVisibility(GONE);
            btCreditCard.setVisibility(GONE);
        }
    }

    @Override
    public void onPageDeactivate() {
    }
}
