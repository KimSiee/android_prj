/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 12 오전 10:15
 *
 */

package com.joas.paymenttestui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.joas.hw.payment.tl3500s.*;

import java.util.Map;

public class PaymentTestActivity extends AppCompatActivity implements TL3500SListener {

    TL3500S tl3500s;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_test);

        tvResult = findViewById(R.id.tvResult);

        tl3500s = new TL3500S(1, "/dev/ttyS3");
        tl3500s.setListener(this);
        tl3500s.start();
    }

    public void onTermReady(View v) {
        tl3500s.termReadyReq();
    }

    public void onTermCheckReq(View v) {
        tl3500s.termCheckReq(0);
    }

    public void onCardInfoReq(View v) {
        tl3500s.cardInfoReq(0);
    }

    public void onPayReq(View v) {
        tl3500s.payReq(100, 0, true, 0);
    }

    public void onPayReq_G(View v) {
        tl3500s.payReq_G(100, 0, true, 0);
    }

    public void onCancelLastPay(View v) {
        tl3500s.cancelPrePay(0);
    }

    public void onVersionReq(View v) {
        tl3500s.getVersionReq();
    }

    public void onConfigReq(View v) {
        tl3500s.getConfigReq();
    }

    public void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch) {
        String dispStr = "Result : "+ type.name() + "\n";
        for(String key: retVal.keySet()) {
            dispStr += key + " = " + retVal.get(key) + "\n";
        }

        final String finalStr = dispStr;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvResult.setText(finalStr);
            }
        });
    }
}
