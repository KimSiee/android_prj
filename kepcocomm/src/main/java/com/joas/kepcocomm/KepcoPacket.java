/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 19 오후 12:03
 *
 */

package com.joas.kepcocomm;

import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;

import java.util.Calendar;

public class KepcoPacket {
    public byte[] raw;

    public byte[] version = new byte[4];
    public byte[] dstip = new byte[4];
    public byte[] srcip = new byte[4];
    public byte[] messageID = new byte[8];

    public int vdSize = 0;
    public int cmd;
    public int ret;
    public int cpid;
    public byte[] vdDataRaw;
    public String vdData;
    public String[] dataList;

    public boolean isRecvAck = false;
    public int retryCnt = 0;

    public KepcoPacket() {
    }

    // 새로운 패킷을 생성
    public KepcoPacket(int chargerID, byte[] ver, byte[] dst, byte[] src, int seq) {
        initHeader(chargerID, ver, dst, src, seq);
    }

    public KepcoPacket(byte[] buffer, int size) {
        parsePacket(buffer, size);
    }

    public void initHeader(int chargerID, byte[] ver, byte[] dst, byte[] src, int seq) {
        cpid = chargerID;
        System.arraycopy(ver, 0, version, 0, version.length);
        System.arraycopy(dst, 0, dstip, 0, dstip.length);
        System.arraycopy(src, 0, srcip, 0, srcip.length);

        // messageID 생성
        Calendar c = Calendar.getInstance();
        messageID[0] = (byte)(c.get(Calendar.YEAR)%100);
        messageID[1] = (byte)(c.get(Calendar.MONTH)+1);
        messageID[2] = (byte)(c.get(Calendar.DAY_OF_MONTH));
        messageID[3] = (byte)(c.get(Calendar.HOUR_OF_DAY));
        messageID[4] = (byte)(c.get(Calendar.MINUTE));
        messageID[5] = (byte)(c.get(Calendar.SECOND));
        messageID[6] = (byte)(seq / 256);
        messageID[7] = (byte)(seq % 256);
    }

    public void parsePacket(byte[] buffer, int size) {
        try {
            raw = new byte[size];
            System.arraycopy(buffer, 0, raw, 0, size);

            int idx = KepcoProtocol.HEADER_MAGIC.length;

            System.arraycopy(raw, idx, version, 0, version.length);
            idx += version.length;

            System.arraycopy(raw, idx, dstip, 0, dstip.length);
            idx += dstip.length;

            System.arraycopy(raw, idx, srcip, 0, srcip.length);
            idx += srcip.length;

            System.arraycopy(raw, idx, messageID, 0, messageID.length);
            idx += messageID.length;

            vdSize = ByteUtil.makeInt(raw, idx, true);
            idx += 4;

            cmd = (int) raw[idx++];
            ret = (int) raw[idx++];

            cpid = ByteUtil.makeInt(raw, idx, true);
            idx += 4;

            vdDataRaw = new byte[vdSize-7];
            System.arraycopy(raw, idx, vdDataRaw, 0, vdDataRaw.length);

            vdData = new String(vdDataRaw, "MS949").replace("\0", "");
            dataList = vdData.split("\\|");
        }
        catch(Exception e) {
            LogWrapper.e("KepcoPacket", "parsePacket():"+e.toString());
        }
    }

    public String[] getDataList() {
        return dataList;
    }
    public String getDataAt(int idx) {
        if ( dataList == null ) return "";
        if ( idx >= dataList.length ) return "";
        if (dataList[idx] == null ) return "";

        return dataList[idx];
    }

    public void encodeAck(int cmd, int ret, String[] listData, KepcoPacket recvPacket) {
        // 응답에서는 받은 패킷의 messageID를 그대로 복사하여 넣어야 한다.
        System.arraycopy(recvPacket.messageID, 0, messageID, 0, messageID.length);
        encode(cmd, ret, listData);
    }

    public void encode(int cmd, int ret, String[] listData) {
        try {
            this.cmd = cmd;
            this.ret = ret;

            dataList = listData;
            vdData = "";

            if (listData != null) {
                for (int i = 0; i < listData.length; i++) {
                    if (i > 0) vdData += "|";
                    vdData += listData[i];
                }
            }

            if (vdData.length() > 0) {
                try {
                    vdDataRaw = vdData.getBytes("MS949");
                } catch (Exception e) {
                    LogWrapper.e("KepcoPacket", "parsePacket():" + e.toString());
                }
            } else vdDataRaw = null;

            int packetSize = KepcoProtocol.HEADER_SIZE + 7;
            if (vdDataRaw != null) {
                packetSize += vdDataRaw.length;
            }

            raw = new byte[packetSize];

            // MAGIC(EMBLEM)
            int idx = 0;
            System.arraycopy(KepcoProtocol.HEADER_MAGIC, 0, raw, idx, KepcoProtocol.HEADER_MAGIC.length);
            idx += KepcoProtocol.HEADER_MAGIC.length;

            System.arraycopy(version, 0, raw, idx, version.length);
            idx += version.length;

            System.arraycopy(dstip, 0, raw, idx, dstip.length);
            idx += dstip.length;

            System.arraycopy(srcip, 0, raw, idx, srcip.length);
            idx += srcip.length;

            System.arraycopy(messageID, 0, raw, idx, messageID.length);
            idx += messageID.length;

            int vdLength = vdDataRaw == null ? 0 : vdDataRaw.length;
            ByteUtil.intToByteArray(vdLength + 7, raw, idx);
            idx += 4;

            raw[idx++] = (byte) cmd;
            raw[idx++] = (byte) ret;

            ByteUtil.intToByteArray(cpid, raw, idx);
            idx += 4;

            if (vdDataRaw != null) {
                System.arraycopy(vdDataRaw, 0, raw, idx, vdDataRaw.length);
                idx += vdDataRaw.length;
            }

            raw[idx++] = (byte) 0x00; // EOT
        }
        catch(Exception e) {
            LogWrapper.e("KepcoPacket", "encode():"+e.toString());
        }
    }

    @Override
    public String toString() {
        String str ="";
        try {
            str =   "cmd:0x" + Integer.toHexString(cmd) + "("+ KepcoProtocol.KepcoCmd.getValue(cmd).name() +"), " +
                    "ret:0x" + Integer.toHexString(ret) + "("+ KepcoProtocol.KepcoRet.getValue(ret).name() +"), " +
                    /*"v:" + ByteUtil.byteArrayToHexString(version, 0, version.length) + ", " +
                    "mid:" + ByteUtil.byteArrayToHexString(messageID, 0, messageID.length) + ", " + */
                    "vd(" + vdData.length() + "):";
            int vdLength = vdData.length();
            String extra = "";
            if (cmd == KepcoProtocol.KepcoCmd.GLOB_FIRM_UPDATE_71.getID() &&
                    (ret == KepcoProtocol.KepcoRet.PKT_DATA.getID() || ret == KepcoProtocol.KepcoRet.PKT_COMPT.getID())) {
                vdLength = dataList[0].length() + dataList[1].length() + dataList[2].length() + 3;
                extra = "binary skip..";
            } else if (cmd == KepcoProtocol.KepcoCmd.GLOB_QRIMG_DOWN_73.getID() &&
                    (ret == KepcoProtocol.KepcoRet.PKT_DATA.getID() || ret == KepcoProtocol.KepcoRet.PKT_COMPT.getID())) {
                vdLength = dataList[0].length() + dataList[1].length() + dataList[2].length() + dataList[3].length() + dataList[4].length() + 5;
                extra = "binary skip..";
            }
            str += vdData.substring(0, vdLength) + extra;
        }
        catch (Exception e) {

        }
        return str;
    }
}
