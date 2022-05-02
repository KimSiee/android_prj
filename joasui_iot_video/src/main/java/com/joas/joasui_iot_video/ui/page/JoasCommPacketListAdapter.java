/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 26 오후 5:24
 *
 */

package com.joas.joasui_iot_video.ui.page;

import android.app.Activity;
import android.support.v4.util.CircularArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joas.evcomm.EvPacket;
import com.joas.joasui_iot_video.ui.R;
import com.joas.utils.ByteUtil;

public class JoasCommPacketListAdapter extends BaseAdapter {
    CircularArray<EvPacket> packetQueue;
    Activity activity;

    private class ViewHolder {
        TextView tvTRX;
        TextView tvDate;
        TextView tvSeq;
        TextView tvID;
        TextView tvType;
        TextView tvINS;
        TextView tvVdData;
    }

    public JoasCommPacketListAdapter(Activity activity, CircularArray<EvPacket> queue) {
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
            holder.tvSeq  = (TextView) convertView.findViewById(R.id.tvCommMonSeq);
            holder.tvID = (TextView) convertView.findViewById(R.id.tvCommMonId);
            holder.tvType = (TextView) convertView.findViewById(R.id.tvCommMonType);
            holder.tvINS = (TextView) convertView.findViewById(R.id.tvCommMonINS);
            holder.tvVdData = (TextView) convertView.findViewById(R.id.tvCommMonVdData);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        EvPacket item = (EvPacket)getItem(i);
        holder.tvTRX.setText(item.isRxPacket ? "RX" : "TX");
        holder.tvDate.setText(item.sendDate);
        holder.tvSeq.setText(""+item.seq);
        holder.tvID.setText(item.stationId+":"+item.chargerId);
        holder.tvType.setText(String.format("%02X",item.type));
        holder.tvINS.setText(item.ins);
        holder.tvVdData.setText(ByteUtil.byteArrayToHexStringDiv(item.vd, 0, item.ml, '-'));
        return convertView;
    }
}
