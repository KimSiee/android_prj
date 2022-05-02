/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 11 오후 3:12
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class AdminPasswordInputView extends FrameLayout implements PageActivateListener {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    UIFlowManager uiManager;
    String password = "";
    TextView tvAdminPwdInput;

    public AdminPasswordInputView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        uiManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_admin_pwd, this, true);

        initComponents();

        final View viewThis = this;
        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount == 0 ) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewThis.setVisibility(INVISIBLE);
                        }
                    });
                    timer.cancel();
                }
            }
        });
    }

    void initComponents() {
        tvAdminPwdInput = findViewById(R.id.tvAdminPwdInput);
        tvAdminPwdInput.setText("");

        int[] btNumIds = { R.id.btNum0, R.id.btNum1, R.id.btNum2, R.id.btNum3, R.id.btNum4,
                        R.id.btNum5, R.id.btNum6, R.id.btNum7, R.id.btNum8, R.id.btNum9 };
        Button[] btNums = new Button[10];

        for (int i=0; i<10; i++) {
            btNums[i] = findViewById(btNumIds[i]);
            final int finalI = i;
            btNums[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnClickNum(""+finalI);
                }
            });
        }

        Button btStar = findViewById(R.id.btNumStar);
        btStar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum("*");
            }
        });

        Button btSharp = findViewById(R.id.btNumSharp);
        btSharp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickNum("#");
            }
        });


        Button btBS = findViewById(R.id.btNumBS);
        btBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumBS();
            }
        });

        Button btClear = findViewById(R.id.btNumClear);
        btClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                OnNumClear();
            }
        });

        Button btCancel= findViewById(R.id.btNumCancel);

        final View viewThis = this;
        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                viewThis.setVisibility(INVISIBLE);
            }
        });

        Button btOK= findViewById(R.id.btNumOK);
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                uiManager.onAdminPasswordOK(password);
            }
        });
    }

    void init() {
        password = "";
        tvAdminPwdInput.setText("");
        startTimer();
    }

    void OnClickNum(String number) {
        password += number;
        tvAdminPwdInput.setText(password);
    }

    void OnNumBS() {
        try {
            password = password.replaceFirst(".$", "");
        }
        catch(Exception e) {}
        tvAdminPwdInput.setText(password);
    }

    void OnNumClear() {
        password = "";
        tvAdminPwdInput.setText(password);
    }

    void startTimer() {
        timerCount = 60;
        timer.start();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        init();
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
    }
}