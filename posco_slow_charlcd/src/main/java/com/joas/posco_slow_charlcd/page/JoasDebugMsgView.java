/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd.page;

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

import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.LogWrapperMsg;


public class JoasDebugMsgView extends FrameLayout {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;

    Activity mainActivity;
    UIFlowManager flowManager;

    CircularArray<LogWrapperMsg> packetQueue;
    int packetQueueMax = PACKET_QUEUE_MAX_CNT_DEFAULT;

    ListView lvPacketList;
    JoasDbgMsgListAdapter packetListAdapter;

    RelativeLayout frameDebugMsgViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    public JoasDebugMsgView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_dbg_msg, this, true);

        packetQueue = new CircularArray<LogWrapperMsg>();

        initComponents();
    }

    void initComponents() {
        lvPacketList = (ListView) findViewById(R.id.lvDebugMsgTable);
        packetListAdapter = new JoasDbgMsgListAdapter(mainActivity, packetQueue);
        lvPacketList.setAdapter(packetListAdapter);

        frameDebugMsgViewBox = (RelativeLayout) findViewById(R.id.frameDbgMsgViewBox);

        imageViewMove = (ImageView) findViewById(R.id.imageDbgMsgArrowMove);
        imageViewMove.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                moveView(motionEvent);
                return true;
            }
        });

        Button btMNTHide = (Button) findViewById(R.id.btDbgMsgHide);
        btMNTHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHide();
            }
        });

        Button btViewClear = (Button) findViewById(R.id.btDbgMsgViewClear);
        btViewClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onClear();
            }
        });

    }

    public CircularArray<LogWrapperMsg> getPacketQueue() {
        return packetQueue;
    }

    public void addPacket(LogWrapperMsg packet) {
        final LogWrapperMsg msg =  packet;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                packetQueue.addFirst(msg);
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
                orgParam = (LayoutParams)frameDebugMsgViewBox.getLayoutParams();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = xpos - orgX;
                float diffY = ypos - orgY;
                orgParam.leftMargin += diffX;
                orgParam.topMargin += diffY;
                frameDebugMsgViewBox.setLayoutParams(orgParam);
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


    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
        } else {

        }
    }
}
