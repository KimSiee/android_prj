/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 16 오후 2:52
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
import android.widget.Toast;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.UIFlowManager;

public class InputPasswordView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvInputPwd;

    String inpPassword = "";

    public InputPasswordView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_pwd,this, true);

        initComponents();
    }

    void initComponents() {
        tvInputPwd = (TextView)findViewById(R.id.tvInputPwd);

        ImageButton btPwdBS  = (ImageButton) findViewById(R.id.btPwdBS);
        ImageButton btPwdRefresh  = (ImageButton) findViewById(R.id.btPwdRefresh);
        ImageButton btPwd0 = (ImageButton) findViewById(R.id.btPwd0);
        ImageButton btPwd1 = (ImageButton) findViewById(R.id.btPwd1);
        ImageButton btPwd2 = (ImageButton) findViewById(R.id.btPwd2);
        ImageButton btPwd3 = (ImageButton) findViewById(R.id.btPwd3);
        ImageButton btPwd4 = (ImageButton) findViewById(R.id.btPwd4);
        ImageButton btPwd5 = (ImageButton) findViewById(R.id.btPwd5);
        ImageButton btPwd6 = (ImageButton) findViewById(R.id.btPwd6);
        ImageButton btPwd7 = (ImageButton) findViewById(R.id.btPwd7);
        ImageButton btPwd8 = (ImageButton) findViewById(R.id.btPwd8);
        ImageButton btPwd9 = (ImageButton) findViewById(R.id.btPwd9);

        ImageButton btOK = (ImageButton) findViewById(R.id.btPwdOK);
        ImageButton btCancel = (ImageButton) findViewById(R.id.btPwdCancel);

        btPwdBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBSClick();
            }
        });
        btPwdRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRefreshClick();
            }
        });
        btPwd0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("0");
            }
        });
        btPwd1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("1");
            }
        });
        btPwd2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("2");
            }
        });
        btPwd3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("3");
            }
        });
        btPwd4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("4");
            }
        });
        btPwd5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("5");
            }
        });
        btPwd6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("6");
            }
        });
        btPwd7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("7");
            }
        });
        btPwd8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("8");
            }
        });
        btPwd9.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addPassword("9");
            }
        });
        btOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onOKClick();
            }
        });
        btCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });
    }

    public void initInputPassword() {
        inpPassword = "";
        tvInputPwd.setText("");
        tvInputPwd.invalidate();
    }

    public void onBSClick() {
        if ( inpPassword.length() > 0) {
            inpPassword = inpPassword.substring(0, inpPassword.length() - 1);
            displayInputPwdber();
        }
    }

    public void onRefreshClick() {
        initInputPassword();
    }

    public void onOKClick() {
        if ( inpPassword.length() < 4 ) {
            Toast.makeText(this.getContext(), "비밀 번호는 4자리를 입력하셔야 합니다.", Toast.LENGTH_LONG).show();
        }
        else {
            flowManager.onPageInputPasswordComplete(inpPassword);
        }
    }

    public void onCancelClick() {
        flowManager.onPageCommonEvent(PageEvent.COMMON_GO_HOME);
    }

    public void addPassword(String Pwd) {
        if ( inpPassword.length() < 4 ) {
            inpPassword += Pwd;
            displayInputPwdber();
        }
    }

    public void displayInputPwdber() {
        String newString = inpPassword.replaceAll(".", "*");
        tvInputPwd.setText(newString);
        tvInputPwd.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initInputPassword();
    }

    @Override
    public void onPageDeactivate() {
    }
}
