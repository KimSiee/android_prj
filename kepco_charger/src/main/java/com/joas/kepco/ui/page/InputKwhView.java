/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 16 오후 3:44
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.UIFlowManager;

public class InputKwhView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvInputKwh;
    TextView tvInpKwhCostUnit;
    TextView tvInpKwhCost;

    String inpKwh = "";
    double costUnit = 100.5;

    public InputKwhView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_input_kwh,this, true);

        initComponents();
    }

    void initComponents() {
        tvInputKwh = (TextView)findViewById(R.id.tvInputKwh);

        ImageButton btKwhRefresh  = (ImageButton) findViewById(R.id.btKwhRefresh);
        ImageButton btKwh0 = (ImageButton) findViewById(R.id.btKwh0);
        ImageButton btKwh1 = (ImageButton) findViewById(R.id.btKwh1);
        ImageButton btKwh2 = (ImageButton) findViewById(R.id.btKwh2);
        ImageButton btKwh3 = (ImageButton) findViewById(R.id.btKwh3);
        ImageButton btKwh4 = (ImageButton) findViewById(R.id.btKwh4);
        ImageButton btKwh5 = (ImageButton) findViewById(R.id.btKwh5);
        ImageButton btKwh6 = (ImageButton) findViewById(R.id.btKwh6);
        ImageButton btKwh7 = (ImageButton) findViewById(R.id.btKwh7);
        ImageButton btKwh8 = (ImageButton) findViewById(R.id.btKwh8);
        ImageButton btKwh9 = (ImageButton) findViewById(R.id.btKwh9);

        ImageButton btOK = (ImageButton) findViewById(R.id.btKwhOK);
        ImageButton btCancel = (ImageButton) findViewById(R.id.btKwhCancel);

        tvInpKwhCostUnit = findViewById(R.id.tvInpKwhCostUnit);
        tvInpKwhCost = findViewById(R.id.tvInpKwhCost);


        btKwhRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRefreshClick();
            }
        });
        btKwh0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( inpKwh.length() > 0) addKwh("0");
            }
        });
        btKwh1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("1");
            }
        });
        btKwh2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("2");
            }
        });
        btKwh3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("3");
            }
        });
        btKwh4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("4");
            }
        });
        btKwh5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("5");
            }
        });
        btKwh6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("6");
            }
        });
        btKwh7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("7");
            }
        });
        btKwh8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("8");
            }
        });
        btKwh9.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addKwh("9");
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

    public void initInputKwh() {
        inpKwh = "";
        tvInputKwh.setText("0kWh");
        tvInputKwh.invalidate();

        tvInpKwhCost.setText("0원");
        tvInpKwhCost.invalidate();

        // 단가 설정
        costUnit = flowManager.getKepcoComm().getCurrentCostUnit();
        tvInpKwhCostUnit.setText(String.format("%.1f원/kWh", costUnit));
    }

    public void onRefreshClick() {
        initInputKwh();
    }

    public void onOKClick() {
        int calcInpKwh = inpKwh.length() == 0 ? 0 : Integer.parseInt(inpKwh);
        if ( calcInpKwh < 1 ) return;

        int costTotal = (int)((double)calcInpKwh * costUnit);

        flowManager.getChargeData().reqPayCost = costTotal;
        flowManager.getChargeData().reqPayKwh = (double)calcInpKwh;

        flowManager.onPageInputCostComplete();
    }

    public void onCancelClick() {
        flowManager.onPageCommonEvent(PageEvent.COMMON_GO_HOME);
    }

    public void addKwh(String num) {
        if ( inpKwh.length() < 2 ) {
            inpKwh += num;
            displayInputKwh();
        }
    }

    public void displayInputKwh() {
        tvInputKwh.setText(inpKwh + "kWh");
        tvInputKwh.invalidate();

        int calcInpKwh = inpKwh.length() == 0 ? 0 : Integer.parseInt(inpKwh);
        int costTotal = (int)((double)calcInpKwh * costUnit);

        tvInpKwhCost.setText(""+costTotal+"원");
        tvInpKwhCost.invalidate();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initInputKwh();
    }

    @Override
    public void onPageDeactivate() {
    }
}
