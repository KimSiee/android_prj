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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui_charlcd.UIFlowManager;

public class InputCostView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvInputCost;
    TextView tvInpCostUnit;
    TextView tvChargeAmount;
    TextView tvErrorDisp;

    String inpCost = "";
    double costUnit = 100.5;

    public InputCostView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_cost,this, true);

        initComponents();
    }

    void initComponents() {
        tvInputCost = (TextView)findViewById(R.id.tvInputCost);

        ImageButton btCostRefresh  = (ImageButton) findViewById(R.id.btCostRefresh);
        ImageButton btCost0 = (ImageButton) findViewById(R.id.btCost0);
        ImageButton btCost1 = (ImageButton) findViewById(R.id.btCost1);
        ImageButton btCost2 = (ImageButton) findViewById(R.id.btCost2);
        ImageButton btCost3 = (ImageButton) findViewById(R.id.btCost3);
        ImageButton btCost4 = (ImageButton) findViewById(R.id.btCost4);
        ImageButton btCost5 = (ImageButton) findViewById(R.id.btCost5);
        ImageButton btCost6 = (ImageButton) findViewById(R.id.btCost6);
        ImageButton btCost7 = (ImageButton) findViewById(R.id.btCost7);
        ImageButton btCost8 = (ImageButton) findViewById(R.id.btCost8);
        ImageButton btCost9 = (ImageButton) findViewById(R.id.btCost9);

        ImageButton btCostBS = (ImageButton) findViewById(R.id.btCostBS);
        ImageButton btCostWon = (ImageButton) findViewById(R.id.btCostWon);

        ImageButton btOK = (ImageButton) findViewById(R.id.btCostOK);
        ImageButton btCancel = (ImageButton) findViewById(R.id.btCostCancel);

        tvInpCostUnit = findViewById(R.id.tvInpCostUnit);
        tvChargeAmount = findViewById(R.id.tvChargeAmount);
        tvErrorDisp = findViewById(R.id.tvErrorDisp);

        btCostRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRefreshClick();
            }
        });
        btCost0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( inpCost.length() > 0) addCost("0");
            }
        });
        btCost1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("1");
            }
        });
        btCost2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("2");
            }
        });
        btCost3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("3");
            }
        });
        btCost4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("4");
            }
        });
        btCost5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("5");
            }
        });
        btCost6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("6");
            }
        });
        btCost7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("7");
            }
        });
        btCost8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("8");
            }
        });
        btCost9.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addCost("9");
            }
        });
        btCostBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBSClick();
            }
        });
        btCostWon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onWonClick();
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

    public void initInputCost() {
        tvErrorDisp.setVisibility(INVISIBLE);
        tvErrorDisp.invalidate();

        inpCost = "";
        tvInputCost.setText("0원");
        tvInputCost.invalidate();
        tvChargeAmount.setText("0kWh");
        tvChargeAmount.invalidate();

        // 단가 설정
        costUnit = flowManager.getKepcoComm().getCurrentCostUnit();
        tvInpCostUnit.setText(String.format("%.1f원/kWh", costUnit));
        tvInpCostUnit.invalidate();
    }

    public void onRefreshClick() {
        initInputCost();
    }

    void onBSClick() {
        if ( inpCost.length() > 0) {
            inpCost = inpCost.substring(0, inpCost.length() - 1);
            displayInputCost();
        }
    }

    void onWonClick() {
        if ( inpCost.length() > 0 && inpCost.length() < 3 ) {
            inpCost += "000";
            displayInputCost();
        }
    }

    public void onOKClick() {
        int calcInpCost = inpCost.length() == 0 ? 0 : Integer.parseInt(inpCost);

        //100원 이하일때 결제 안됨
        if ( calcInpCost < 100 ) {
            tvErrorDisp.setVisibility(VISIBLE);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tvErrorDisp.setVisibility(INVISIBLE);
                }
            }, 3000);

            return;
        }

        double amountKwh = (double)calcInpCost/costUnit;
        flowManager.getChargeData().reqPayCost = calcInpCost;
        flowManager.getChargeData().reqPayKwh = amountKwh;

        flowManager.onPageInputCostComplete();
    }

    public void onCancelClick() {
        flowManager.onPageCommonEvent(PageEvent.COMMON_GO_HOME);
    }

    public void addCost(String num) {
        if ( inpCost.length() < 5 ) {
            inpCost += num;
            displayInputCost();
        }
    }

    public void displayInputCost() {
        String newString = inpCost.replaceAll("(\\d)(?=(\\d{3})+$)", "$1,");
        tvInputCost.setText(newString + "원");
        tvInputCost.invalidate();

        int calcInpCost = inpCost.length() == 0 ? 0 : Integer.parseInt(inpCost);
        double amountKwh = (double)calcInpCost/costUnit;
        tvChargeAmount.setText(String.format("%.2fkWh", amountKwh));
        tvChargeAmount.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initInputCost();
    }

    @Override
    public void onPageDeactivate() {
    }
}
