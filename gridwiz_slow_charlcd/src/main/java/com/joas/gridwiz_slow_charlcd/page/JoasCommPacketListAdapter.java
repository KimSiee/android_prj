/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd.page;

import android.app.Activity;
import android.support.v4.util.CircularArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joas.gridwiz_slow_charlcd.R;

public class JoasCommPacketListAdapter extends BaseAdapter {
    CircularArray<JoasCommMonitorView.CommMonitorMsg> packetQueue;
    Activity activity;

    private class ViewHolder {
        TextView tvTRX;
        TextView tvDate;
        TextView tvVdData;
    }

    public JoasCommPacketListAdapter(Activity activity, CircularArray<JoasCommMonitorView.CommMonitorMsg> queue) {
        this.activity = activity;
        this.packetQueue = queue;
    }

    @Override
    public int getCount() {
        return packetQueue.size();
    }

    @Override
    public Object getItem(int i) {
        return packetQueue.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        LayoutInflater inflater = activity.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_comm_packet_list, null);
            holder = new ViewHolder();
            holder.tvTRX = (TextView) convertView.findViewById(R.id.tvCommMonTrx);
            holder.tvDate = (TextView) convertView.findViewById(R.id.tvCommMonDate);
            holder.tvVdData = (TextView) convertView.findViewById(R.id.tvCommMonVdData);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        JoasCommMonitorView.CommMonitorMsg item = (JoasCommMonitorView.CommMonitorMsg)getItem(i);

        holder.tvTRX.setText(item.trx);
        holder.tvDate.setText(item.time);
        holder.tvVdData.setText(item.data);
        return convertView;
    }
}
