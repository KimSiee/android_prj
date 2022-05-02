/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd.page;

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

import com.joas.gridwiz_slow_charlcd.R;
import com.joas.gridwiz_slow_charlcd.UIFlowManager;
import com.joas.utils.ByteUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class JoasCommMonitorView extends FrameLayout {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;

    Activity mainActivity;
    UIFlowManager flowManager;

    CircularArray<CommMonitorMsg> packetQueue;
    int packetQueueMax = PACKET_QUEUE_MAX_CNT_DEFAULT;

    ListView lvPacketList;
    JoasCommPacketListAdapter packetListAdapter;

    RelativeLayout frameCommViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault());

    static public class CommMonitorMsg {
        public String trx;
        public String time;
        public String data;
    }

    public JoasCommMonitorView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_comm_monitor, this, true);

        packetQueue = new CircularArray<CommMonitorMsg>();

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

    public CircularArray<CommMonitorMsg> getPacketQueue() { return packetQueue; }


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
