/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 18 오전 10:24
 *
 */

package com.joas.kepco.ui;

import android.app.Activity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class StatusBarControl {
    Activity mainActivity;
    UIFlowManager flowManager;

    ImageView iconStatus;
    ImageView iconDSPCommError;
    ImageView iconServerCommError;
    ImageView iconMeterStatusError;
    ImageView iconPayReaderStatus;
    ImageView iconPayReaderStatusError;

    TextView tvChargerID;
    ImageButton btLogo;
    ImageButton btHome;

    TextView tvOpenEnclosure;

    AlphaAnimation chargingIconAnimation;
    boolean isStartChargingStatusAnimation  = false;

    public int adminCount = 0;
    TimeoutTimer timerAdmin;
    static Toast toast;

    public StatusBarControl(Activity activity, UIFlowManager manager) {
        mainActivity = activity;
        flowManager = manager;
        initComponents();

        timerAdmin = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                adminCount = 0;
            }
        });
    }

    public void initComponents() {
        iconStatus = (ImageView)mainActivity.findViewById(R.id.iconStatus);
        iconDSPCommError = (ImageView)mainActivity.findViewById(R.id.iconDSPCommError);
        iconServerCommError = (ImageView)mainActivity.findViewById(R.id.iconServerCommError);
        iconMeterStatusError = (ImageView)mainActivity.findViewById(R.id.iconMeterStatusError);
        iconPayReaderStatus = (ImageView)mainActivity.findViewById(R.id.iconPayReaderStatus);
        iconPayReaderStatusError = (ImageView)mainActivity.findViewById(R.id.iconPayReaderError);

        tvChargerID = (TextView)mainActivity.findViewById(R.id.tvChargerID);
        btLogo  = (ImageButton) mainActivity.findViewById(R.id.btLogo);
        btHome = (ImageButton) mainActivity.findViewById(R.id.btHome);

        iconStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAdminModeClick();
            }
        });

        btLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( btHome.getVisibility() == View.VISIBLE ) onHomeClick();
            }
        });

        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        chargingIconAnimation = new AlphaAnimation(0.0f, 1.0f);
        chargingIconAnimation.setRepeatCount(Animation.INFINITE);
        chargingIconAnimation.setDuration(1000);
        chargingIconAnimation.setRepeatMode(Animation.REVERSE);

        if ( flowManager.getCpConfig().usePayTerminal == false ) {
            setUsePayTeminal(false);
        }

        tvOpenEnclosure = mainActivity.findViewById(R.id.tvOpenEnclosure);
        tvOpenEnclosure.setVisibility(View.INVISIBLE);
    }

    public void setUsePayTeminal(boolean tf) {
        if ( tf ) {
            iconPayReaderStatus.setVisibility(View.VISIBLE);
            iconPayReaderStatusError.setVisibility(View.VISIBLE);
        }
        else {
            iconPayReaderStatus.setVisibility(View.INVISIBLE);
            iconPayReaderStatusError.setVisibility(View.INVISIBLE);
        }
    }

    // Home Button Click
    void onHomeClick() {
        flowManager.onHomeBtClick();
    }

    public void setChargerID(String id) {
        tvChargerID.setText("ID " + id);
    }

    public void setHomeVisible(boolean tf) {
        if ( tf == true ) btHome.setVisibility(View.VISIBLE);
        else btHome.setVisibility(View.GONE);
    }

    public void setDSPCommError(boolean tf) {
        final boolean isError = tf;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( isError == true ) iconDSPCommError.setVisibility(View.VISIBLE);
                else iconDSPCommError.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setServerCommError(boolean tf) {
        final boolean isError = tf;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( isError  == true ) iconServerCommError.setVisibility(View.VISIBLE);
                else iconServerCommError.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setMeterStatusError(boolean tf) {
        final boolean isError = tf;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isError == true) iconMeterStatusError.setVisibility(View.VISIBLE);
                else iconMeterStatusError.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setPayTerminalStatusError(boolean tf) {
        final boolean isError = tf;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( isError == true ) iconPayReaderStatusError.setVisibility(View.VISIBLE);
                else iconPayReaderStatusError.setVisibility(View.INVISIBLE);
            }
        });

    }

    public void setChargingStatus(boolean tf) {
        if ( tf == true ) {
            if (isStartChargingStatusAnimation == false) {
                iconStatus.setImageResource(R.drawable.status_charging);
                iconStatus.startAnimation(chargingIconAnimation);
                isStartChargingStatusAnimation = true;
            }
        }
        else {
            if (isStartChargingStatusAnimation == true) {
                iconStatus.setImageResource(R.drawable.status_normal);
                chargingIconAnimation.cancel();
                iconStatus.clearAnimation();
                isStartChargingStatusAnimation = false;
            }
        }
    }

    public void setOpenEnclosure(boolean tf) {
        final boolean isOpen = tf;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOpen) tvOpenEnclosure.setVisibility(View.VISIBLE);
                else tvOpenEnclosure.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void onAdminModeClick() {

        adminCount++;
        if ( adminCount > 4) {
            flowManager.onEnterAdminMode();
        }
        else if( adminCount > 2) {
            if ( toast != null ) toast.cancel();
            toast = Toast.makeText(mainActivity, "Admin Mode Remains : "+(4-adminCount), Toast.LENGTH_SHORT);
            toast.show();
        }
        timerAdmin.startOnce(); // 1회만 수행
    }
}
