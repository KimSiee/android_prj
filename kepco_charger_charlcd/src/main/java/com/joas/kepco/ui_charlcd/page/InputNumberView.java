/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kepco.ui_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui_charlcd.UIFlowManager;

public class InputNumberView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvInputNumber;

    String inpNumber = "";

    public InputNumberView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_number,this, true);

        initComponents();
    }

    void initComponents() {
        tvInputNumber = (TextView)findViewById(R.id.tvInputNum);

        ImageButton btNumBS  = (ImageButton) findViewById(R.id.btNumBS);
        ImageButton btNumRefresh  = (ImageButton) findViewById(R.id.btNumRefresh);
        ImageButton btNum0 = (ImageButton) findViewById(R.id.btNum0);
        ImageButton btNum1 = (ImageButton) findViewById(R.id.btNum1);
        ImageButton btNum2 = (ImageButton) findViewById(R.id.btNum2);
        ImageButton btNum3 = (ImageButton) findViewById(R.id.btNum3);
        ImageButton btNum4 = (ImageButton) findViewById(R.id.btNum4);
        ImageButton btNum5 = (ImageButton) findViewById(R.id.btNum5);
        ImageButton btNum6 = (ImageButton) findViewById(R.id.btNum6);
        ImageButton btNum7 = (ImageButton) findViewById(R.id.btNum7);
        ImageButton btNum8 = (ImageButton) findViewById(R.id.btNum8);
        ImageButton btNum9 = (ImageButton) findViewById(R.id.btNum9);

        ImageButton btOK = (ImageButton) findViewById(R.id.btNumOK);
        ImageButton btCancel = (ImageButton) findViewById(R.id.btNumCancel);

        btNumBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBSClick();
            }
        });
        btNumRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRefreshClick();
            }
        });
        btNum0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("0");
            }
        });
        btNum1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("1");
            }
        });
        btNum2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("2");
            }
        });
        btNum3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("3");
            }
        });
        btNum4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("4");
            }
        });
        btNum5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("5");
            }
        });
        btNum6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("6");
            }
        });
        btNum7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("7");
            }
        });
        btNum8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("8");
            }
        });
        btNum9.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumber("9");
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

    public void initInputNumber() {
        inpNumber = "";
        tvInputNumber.setText("");
        tvInputNumber.invalidate();
    }

    public void onBSClick() {
        if ( inpNumber.length() > 0) {
            inpNumber = inpNumber.substring(0, inpNumber.length() - 1);
            displayInputNumber();
        }
    }

    public void onRefreshClick() {
        initInputNumber();
    }

    public void onOKClick() {
        if ( inpNumber.length() < 16 ) {
            Toast.makeText(this.getContext(), "회원 번호는 16자리를 입력하셔야 합니다.", Toast.LENGTH_LONG).show();
        }
        else {
            flowManager.onPageInputNumberComplete(inpNumber);
        }
    }

    public void onCancelClick() {
        flowManager.onPageCommonEvent(PageEvent.COMMON_GO_HOME);
    }

    public void addNumber(String num) {
        if ( inpNumber.length() < 16 ) {
            inpNumber += num;
            displayInputNumber();
        }
    }

    public void displayInputNumber() {
        String newString = inpNumber.replaceAll("(.{4})(?!$)", "$1-");
        tvInputNumber.setText(newString);
        tvInputNumber.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initInputNumber();
    }

    @Override
    public void onPageDeactivate() {
    }
}
