/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui.page;

import android.app.Activity;
import android.support.v4.util.CircularArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joas.ocppui.R;
import com.joas.utils.LogWrapperMsg;

public class JoasDbgMsgListAdapter extends BaseAdapter {
    CircularArray<LogWrapperMsg> packetQueue;
    Activity activity;

    private class ViewHolder {
        TextView tvTime;
        TextView tvTAG;
        TextView tvLevel;
        TextView tvMsg;
    }

    public JoasDbgMsgListAdapter(Activity activity, CircularArray<LogWrapperMsg> queue) {
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
            convertView = inflater.inflate(R.layout.list_dbg_msg_list, null);
            holder = new ViewHolder();
            holder.tvTime = (TextView) convertView.findViewById(R.id.tvDbgMsgTime);
            holder.tvLevel = (TextView) convertView.findViewById(R.id.tvDbgMsgLevel);
            holder.tvTAG = (TextView) convertView.findViewById(R.id.tvDbgMsgTAG);
            holder.tvMsg = (TextView) convertView.findViewById(R.id.tvDbgMsgContent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LogWrapperMsg item = (LogWrapperMsg)getItem(i);
        holder.tvTime.setText(item.time);
        holder.tvLevel.setText(LogWrapperMsg.getLevelString(item.level));
        holder.tvTAG.setText(item.TAG);
        holder.tvMsg.setText(""+item.msg);
        return convertView;
    }
}
