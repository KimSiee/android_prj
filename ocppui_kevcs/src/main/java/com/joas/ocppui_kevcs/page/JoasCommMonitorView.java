/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui_kevcs.page;

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

import com.joas.ocppui_kevcs.R;
import com.joas.ocppui_kevcs.UIFlowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class JoasCommMonitorView extends FrameLayout {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;

    Activity mainActivity;
    UIFlowManager flowManager;

    CircularArray<OCPPMonitorMsg> packetQueue;
    int packetQueueMax = PACKET_QUEUE_MAX_CNT_DEFAULT;

    ListView lvPacketList;
    JoasCommPacketListAdapter packetListAdapter;

    RelativeLayout frameCommViewBox;

    ImageView imageViewMove;

    float orgX = 0;
    float orgY = 0;
    LayoutParams orgParam;

    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS: ", Locale.getDefault());

    static public class OCPPMonitorMsg {
        public String trx;
        public String time;
        public String data;
    }

    public CircularArray<JoasCommMonitorView.OCPPMonitorMsg> getPacketQueue() { return packetQueue; }

    public JoasCommMonitorView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_comm_monitor, this, true);

        packetQueue = new CircularArray<OCPPMonitorMsg>();

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

    public void addPacket(OCPPMonitorMsg packet) {
        final OCPPMonitorMsg msg =  packet;

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

    public void addOCPPRawMsg(String trx, String data) {
        OCPPMonitorMsg msg = new OCPPMonitorMsg();
        msg.trx = trx;
        msg.time = formatter.format(new Date());
        msg.data = data;

        addPacket(msg);
    }

}
