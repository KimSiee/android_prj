/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 30 오후 4:19
 *
 */

package com.joas.ocppui_dubai_slow.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.joas.hw.dsp.DSPMonitorListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;
import com.joas.ocppui_dubai_slow.R;
import com.joas.ocppui_dubai_slow.UIFlowManager;


public class JoasDSPMonitorView extends FrameLayout implements DSPMonitorListener {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;

    Activity mainActivity;
    UIFlowManager flowManager;

    ListView lvPacketList;
    JoasDSPPacketListAdapter packetListAdapter;

    RelativeLayout frameDSPViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    public JoasDSPMonitorView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_dsp_monitor, this, true);

        initCommMonitor();
    }

    void initCommMonitor() {
        lvPacketList = (ListView) findViewById(R.id.lvDspMonPacketTable);
        packetListAdapter = new JoasDSPPacketListAdapter(mainActivity);
        lvPacketList.setAdapter(packetListAdapter);

        frameDSPViewBox = (RelativeLayout) findViewById(R.id.frameDSPViewBox);

        imageViewMove = (ImageView) findViewById(R.id.imageDSPViewArrowMove);
        imageViewMove.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                moveView(motionEvent);
                return true;
            }
        });

        Button btCommMNTHide = (Button) findViewById(R.id.btDSPMNTHide);
        btCommMNTHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHide();
            }
        });

        Button btCommViewClear = (Button) findViewById(R.id.btDSPView1);
        btCommViewClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onClear();
            }
        });

    }

    void moveView(MotionEvent motionEvent) {
        //LogWrapper.v("MNT", "act:"+motionEvent.getAction()+", x:"+motionEvent.getX() + ", y:"+motionEvent.getY());
        float xpos = motionEvent.getRawX();
        float ypos = motionEvent.getRawY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                orgX = xpos;
                orgY = ypos;
                orgParam = (LayoutParams)frameDSPViewBox .getLayoutParams();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = xpos - orgX;
                float diffY = ypos - orgY;
                orgParam.leftMargin += diffX;
                orgParam.topMargin += diffY;
                frameDSPViewBox .setLayoutParams(orgParam);
                orgX = xpos;
                orgY = ypos;
                break;
        }
    }

    public void onHide() {
        this.setVisibility(INVISIBLE);
    }

    public void onClear() {
        packetListAdapter.notifyDataSetChanged();
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
        } else {

        }
    }

    @Override
    public void onDspTxDataEvent(int channel, DSPTxData txData) {
        packetListAdapter.updateTxData(txData.rawData);
    }

    @Override
    public void onDspRxDataEvent(int channel, DSPRxData rxData) {
        packetListAdapter.updateRxData(rxData.rawData);

        if ( this.getVisibility() == VISIBLE ) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    packetListAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
