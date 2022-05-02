/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;
import com.joas.kepco.ui.R;
import com.joas.utils.ByteUtil;

public class JoasDSPPacketListAdapter extends BaseAdapter {
    Activity activity;

    public static final int DSP_REG_TX_CNT = 10;
    public static final int DSP_REG_RX_CNT = 17;
    public static final int DSP_REG_TOTAL = DSP_REG_TX_CNT+DSP_REG_RX_CNT;

    int[] dspRegList = new int[DSP_REG_TOTAL];

    private class ViewHolder {
        TextView tvTrx;
        TextView tvAddr;
        TextView tvHEX;
        TextView tvDEC;
        TextView tvBIN;
    }

    public JoasDSPPacketListAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return DSP_REG_TOTAL;
    }

    @Override
    public Object getItem(int i) {
        return dspRegList[i];
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
            convertView = inflater.inflate(R.layout.list_dsp_packet_list, null);
            holder = new ViewHolder();
            holder.tvTrx = (TextView) convertView.findViewById(R.id.tvDSPMonTrx);
            holder.tvAddr = (TextView) convertView.findViewById(R.id.tvDSPMonAddr);
            holder.tvHEX = (TextView) convertView.findViewById(R.id.tvDSPMonHex);
            holder.tvDEC = (TextView) convertView.findViewById(R.id.tvDSPMonDec);
            holder.tvBIN = (TextView) convertView.findViewById(R.id.tvDSPMonBin);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvTrx.setText( i < DSP_REG_TX_CNT ? "TX" : "RX");
        holder.tvAddr.setText( "" + ((i < DSP_REG_TX_CNT) ? (200+i) : (400+i-DSP_REG_TX_CNT)));
        holder.tvHEX.setText(String.format("%04X", dspRegList[i]));
        holder.tvDEC.setText(""+dspRegList[i]);
        holder.tvBIN.setText(Integer.toString(dspRegList[i],2));
        return convertView;
    }

    public void updateRxData(byte[] rxData) {
        for (int i=0; i<DSP_REG_RX_CNT; i++ ) {
            dspRegList[DSP_REG_TX_CNT+i] = ByteUtil.makeWord(rxData[DSPRxData.DATA_OFFSET+i*2], rxData[DSPRxData.DATA_OFFSET+i*2+1]);
        }
    }

    public void updateTxData(byte[] txData) {
        for (int i=0; i<DSP_REG_TX_CNT; i++ ) {
            dspRegList[i] = ByteUtil.makeWord(txData[DSPTxData.DATA_OFFSET+i*2], txData[DSPTxData.DATA_OFFSET+i*2+1]);
        }
    }
}
