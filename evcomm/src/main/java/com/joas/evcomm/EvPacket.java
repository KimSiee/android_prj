/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 21 오후 3:41
 *
 */

package com.joas.evcomm;

import com.joas.utils.ByteUtil;
import com.joas.utils.CRC16;
import com.joas.utils.LogWrapper;

import java.math.BigInteger;

public class EvPacket {

    public static final String TAG = "EvPacket";
    // EV Message Header Info
    public static final int EV_MSG_HEADER_TOP_SIZE = 25;
    public static final int EV_MSG_HEADER_ML_POS   = 23; // Position of Variable Data(EVMessage)
    public static final int EV_MSG_HEADER_TAIL_SIZE = 3;
    public static final int EV_MSG_HEADER_CHARGER_ID_POS = 19;
    public static final int EV_MSG_HEADER_STX_SIZE = 1;
    public static final int EV_MSG_HEADER_SEQ_SIZE = 2;

    public static final int EV_MSG_HEADER_SENDDATE_SIZE = 7;
    public static final int EV_MSG_HEADER_STATION_ID_SIZE = 8;
    public static final int EV_MSG_HEADER_CHARGER_ID_SIZE = 2;
    public static final int EV_MSG_HEADER_INS_SIZE = 2;
    public static final int EV_MSG_HEADER_ML_SIZE = 2;

    public static final int EV_MSG_HEADER_STX = 0x02;
    public static final int EV_MSG_HEADER_ETX = 0x03;


    public static final int EV_MSG_RET_CODE_ACK = 0x06;
    public static final int EV_MSG_RET_CODE_NAK = 0x15;

    public String sendDate;
    public int seq;
    public int type;
    public String stationId;
    public String chargerId;
    public String ins;
    public byte[] insRaw;
    public int ml;
    public byte[] vd;

    public boolean isRequest = false;
    public boolean isRxPacket = false;

    public EvPacket() {

    }

    public EvPacket(byte[] buf, int offset, int size) {
        int idx = EV_MSG_HEADER_STX_SIZE+offset;

        sendDate = ByteUtil.byteArrayToHexString(buf, idx,  EV_MSG_HEADER_SENDDATE_SIZE );
        idx += EV_MSG_HEADER_SENDDATE_SIZE;

        seq = ByteUtil.makeWord(buf[idx], buf[idx+1]);
        idx += EV_MSG_HEADER_SEQ_SIZE;

        type = (int)buf[idx];
        idx++;

        stationId = new String(buf, idx, EV_MSG_HEADER_STATION_ID_SIZE);
        idx += EV_MSG_HEADER_STATION_ID_SIZE;

        chargerId = new String(buf, idx, EV_MSG_HEADER_CHARGER_ID_SIZE);
        idx += EV_MSG_HEADER_CHARGER_ID_SIZE;

        ins = new String(buf, idx, EV_MSG_HEADER_INS_SIZE);
        insRaw = new byte[EV_MSG_HEADER_INS_SIZE];
        insRaw[0] = buf[idx];
        insRaw[1] = buf[idx];
        idx += EV_MSG_HEADER_INS_SIZE;

        // 첫글자가 숫자가 온다면 응답메시지
        if ( insRaw[0] >= (byte)'0' && insRaw[0] <= (byte)'9' ) isRequest = false;
        else isRequest = true;

        ml = ByteUtil.makeWord(buf[idx], buf[idx+1]);
        idx += EV_MSG_HEADER_ML_SIZE;

        vd = new byte[ml];
        System.arraycopy(buf, idx, vd, 0, ml);
    }

    public byte[] encode() {
        byte[] buff = null;

        try {
            int packetSize = EvPacket.EV_MSG_HEADER_TOP_SIZE + ml + EvPacket.EV_MSG_HEADER_TAIL_SIZE;

            buff = new byte[packetSize];

            int idx = 0;

            // STX
            buff[idx++] = (byte) EvPacket.EV_MSG_HEADER_STX;

            byte[] hexTime = new BigInteger(sendDate, 16).toByteArray();

            //DateTime
            System.arraycopy(hexTime, 0, buff, idx, EvPacket.EV_MSG_HEADER_SENDDATE_SIZE);
            idx += EvPacket.EV_MSG_HEADER_SENDDATE_SIZE;

            //SEQ
            buff[idx++] = (byte) (seq / 256);
            buff[idx++] = (byte) (seq % 256);

            //TYPE
            buff[idx++] = (byte) type;

            // 충전소 ID
            byte[] sid = stationId.getBytes();
            System.arraycopy(sid, 0, buff, idx, EvPacket.EV_MSG_HEADER_STATION_ID_SIZE);
            idx += EvPacket.EV_MSG_HEADER_STATION_ID_SIZE;

            // 충전기 ID
            byte[] cid = chargerId.getBytes();
            System.arraycopy(cid, 0, buff, idx, EvPacket.EV_MSG_HEADER_CHARGER_ID_SIZE);
            idx += EvPacket.EV_MSG_HEADER_CHARGER_ID_SIZE;


            byte[] rawIns = ins.getBytes();
            System.arraycopy(rawIns, 0, buff, idx, EvPacket.EV_MSG_HEADER_INS_SIZE);
            idx += EvPacket.EV_MSG_HEADER_INS_SIZE;

            //SEQ
            buff[idx++] = (byte) (ml / 256);
            buff[idx++] = (byte) (ml % 256);

            if ( ml > 0 ) {
                System.arraycopy(vd, 0, buff, idx, ml);
                idx += ml;
            }

            int crcVal = CRC16.calc(CRC16.INITIAL_VALUE, buff, EvPacket.EV_MSG_HEADER_STX_SIZE, idx - EvPacket.EV_MSG_HEADER_STX_SIZE);
            //SEQ
            buff[idx++] = (byte) (crcVal / 256);
            buff[idx++] = (byte) (crcVal % 256);

            //ETX
            buff[idx++] = (byte) EvPacket.EV_MSG_HEADER_ETX;
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "encode Err:"+e.toString());
        }

        return buff;
    }

    /**
     * 날짜와 seq를 이용하여 qniqueId를 리턴한다.
     * @return
     */
    public String getUniqueId() {
        return sendDate+String.format("%05d", seq);
    }
    
    @Override
    public String toString() {
        return  "Ins:"+ins+", SDate:"+sendDate+", Seq:"+seq+", Type:"+type+
                ",SID:"+stationId+", CID:"+chargerId+", ML:"+ml +
                ", VD:"+ ByteUtil.byteArrayToHexStringDiv(vd,0, vd.length, '-');
    }
}
