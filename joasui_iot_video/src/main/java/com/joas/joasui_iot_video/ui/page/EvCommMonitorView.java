/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 26 오후 5:22
 *
 */

package com.joas.joasui_iot_video.ui.page;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.CircularArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.joas.evcomm.EvCommMonitorListener;
import com.joas.evcomm.EvPacket;
import com.joas.joasui_iot_video.ui.R;
import com.joas.joasui_iot_video.ui.UIFlowManager;


public class EvCommMonitorView extends FrameLayout implements EvCommMonitorListener {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;

    Activity mainActivity;
    UIFlowManager flowManager;

    CircularArray<EvPacket> packetQueue;
    int packetQueueMax = PACKET_QUEUE_MAX_CNT_DEFAULT;

    ListView lvPacketList;
    JoasCommPacketListAdapter packetListAdapter;

    RelativeLayout frameCommViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    public EvCommMonitorView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_comm_monitor, this, true);

        packetQueue = new CircularArray<EvPacket>();

        initCommMonitor();
    }

    void initCommMonitor() {
        lvPacketList = (ListView) findViewById(R.id.lvCommMonPacketTable);
        packetListAdapter = new JoasCommPacketListAdapter(mainActivity, packetQueue);
        lvPacketList.setAdapter(packetListAdapter);

        frameCommViewBox = (RelativeLayout) findViewById(R.id.frameCommViewBox);

        imageViewMove = (ImageView) findViewById(R.id.imageCommArrowMove);
        imageViewMove.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                moveView(motionEvent);
                return true;
            }
        });

        Button btCommMNTHide = (Button) findViewById(R.id.btCommMNTHide);
        btCommMNTHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHide();
            }
        });

        Button btCommViewClear = (Button) findViewById(R.id.btCommViewClear);
        btCommViewClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onClear();
            }
        });

    }

    public void addPacket(EvPacket packet) {
        final EvPacket evPacket =  packet;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                packetQueue.addFirst(evPacket);
                if (packetQueue.size() > packetQueueMax) packetQueue.popLast();

                packetListAdapter.notifyDataSetChanged();
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
                orgParam = (LayoutParams)frameCommViewBox.getLayoutParams();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = xpos - orgX;
                float diffY = ypos - orgY;
                orgParam.leftMargin += diffX;
                orgParam.topMargin += diffY;
                frameCommViewBox.setLayoutParams(orgParam);
                orgX = xpos;
                orgY = ypos;
                break;
        }
    }

    public void onHide() {
        this.setVisibility(INVISIBLE);
    }

    public void onClear() {
        packetQueue.clear();
        packetListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRecvEvPacket(EvPacket packet) {
        addPacket(packet);
    }

    @Override
    public void onTransEvPacket(EvPacket packet) {
        addPacket(packet);
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
        } else {

        }
    }
}
