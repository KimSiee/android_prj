/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 1. 24. 오후 2:08
 *
 */

package com.joas.posco_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.CircularArray;
import android.widget.ListView;

import com.joas.posco_slow_charlcd.UIFlowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OcppCommMonitorView {
    public static final int PACKET_QUEUE_MAX_CNT_DEFAULT = 200;


    Activity mainActivity;
    UIFlowManager flowManager;

    CircularArray<OCPPMonitorMsg> packetQueue;
    int packetQueueMax = PACKET_QUEUE_MAX_CNT_DEFAULT;

    ListView lvPacketList;
    OcppCommPacketListAdapter packetListAdapter;

    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS: ", Locale.getDefault());

    static public class OCPPMonitorMsg {
        public String trx;
        public String time;
        public String data;
    }

    public CircularArray<OcppCommMonitorView.OCPPMonitorMsg> getPacketQueue() { return packetQueue; }

    public OcppCommMonitorView(Context context, UIFlowManager manager, Activity activity) {

        mainActivity = activity;
        flowManager = manager;

        packetQueue = new CircularArray<OCPPMonitorMsg>();

        packetListAdapter = new OcppCommPacketListAdapter(mainActivity, packetQueue);
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

    public void addOCPPRawMsg(String trx, String data) {
        OCPPMonitorMsg msg = new OCPPMonitorMsg();
        msg.trx = trx;
        msg.time = formatter.format(new Date());
        msg.data = data;

        addPacket(msg);
    }
}
