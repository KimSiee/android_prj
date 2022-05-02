/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd;

import com.joas.utils.ByteUtil;

/**
 * Created by user on 2018-01-18.
 */

public class JoasCommMsg {
    public static class ChargerStart_d1 {
        public static final int VD_DATA_SIZE = 100;

        public int cpMode = 0;
        public long cpStatus = 0;

        public int meterVal = 0;
        public String cardNum = "0000000000000000";
        public int reqAmountSel = 0;
        public int reqAmountKwh = 0;
        public int reqAmountPay = 0;
        public int payMethod = 0;
        public int battStatus = 0;
        public int socBatt = 0;
        public int battTotalAmount = 0;
        public int battCurAmount = 0;
        public int battCurVoltage = 0;
        public String BMSVersion = "0000";
        public int expectEndTime = 0x9900; // BCD hh mm
        public int avalLineCurrent = 32; // Ampha ??

        public byte[] encode() {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = 0;

            ByteUtil.wordToByteArray(cpMode , vdData, idx);
            idx += 2;

            ByteUtil.longToByteArray(cpStatus, vdData, 8, idx);
            idx += 8;

            ByteUtil.intToByteArray(meterVal, vdData, idx);
            idx += 4;

            System.arraycopy(cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            vdData[idx++] = (byte)reqAmountSel;

            ByteUtil.intToByteArray(reqAmountKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(reqAmountPay, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)payMethod;
            vdData[idx++] = (byte)battStatus;

            ByteUtil.wordToByteArray(socBatt , vdData, idx);
            idx += 2;

            ByteUtil.intToByteArray(battTotalAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurVoltage, vdData, idx);
            idx += 4;

            System.arraycopy(BMSVersion.getBytes(), 0, vdData, idx, 4);
            idx += 4;

            ByteUtil.wordToByteArray(expectEndTime , vdData, idx);
            idx += 2;

            ByteUtil.intToByteArray(avalLineCurrent, vdData, idx);
            idx += 4;

            return vdData;
        }
    }

    public static class ChargerState_e1 {
        public static final int VD_DATA_SIZE = 100;

        public int cpMode = 0;
        public long cpStatus = 0;

        public int meterVal = 0;
        public String cardNum = "0000000000000000";
        public int reqAmountSel = 0;
        public int reqAmountKwh = 0;
        public int reqAmountPay = 0;
        public int payMethod = 0;
        public int socBatt = 0;
        public int curChargingKwh = 0;
        public int curChargingCost = 0;
        public int curChargingUnitCost = 0;

        public int battStatus = 0;
        public int battTotalAmount = 0;
        public int battCurAmount = 0;
        public int battCurVoltage = 0;
        public int battCurAmpare = 0;
        public int battCurTemperature = 0;
        public String BMSVersion = "0000";
        public int remainTime  = 0x9900; // BCD hh mm
        public int dbUniqueCode  = 0;

        public byte[] encode() {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = 0;

            ByteUtil.wordToByteArray(cpMode , vdData, idx);
            idx += 2;

            ByteUtil.longToByteArray(cpStatus, vdData, 8, idx);
            idx += 8;

            ByteUtil.intToByteArray(meterVal, vdData, idx);
            idx += 4;

            System.arraycopy(cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            vdData[idx++] = (byte)reqAmountSel;

            ByteUtil.intToByteArray(reqAmountKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(reqAmountPay, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)payMethod;

            ByteUtil.wordToByteArray(socBatt , vdData, idx);
            idx += 2;

            ByteUtil.intToByteArray(curChargingKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(curChargingCost, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(curChargingUnitCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)battStatus;

            ByteUtil.intToByteArray(battTotalAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurVoltage, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurAmpare, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(battCurTemperature, vdData, idx);
            idx += 4;

            System.arraycopy(BMSVersion.getBytes(), 0, vdData, idx, 4);
            idx += 4;

            ByteUtil.wordToByteArray(remainTime, vdData, idx);
            idx += 2;

            ByteUtil.wordToByteArray(dbUniqueCode, vdData, idx);
            idx += 2;

            return vdData;
        }
    }

    public static class ChargerEnd_f1 {
        public static final int VD_DATA_SIZE = 100;

        public int cpMode = 0;
        public long cpStatus = 0;

        public int meterVal = 0;
        public String cardNum = "0000000000000000";

        public int chargingKwh = 0;
        public int chargingTime = 0;
        public int chargingCost = 0;
        public int payMethod = 0;
        public int chargeEndStatus = 0;
        public long chargeStartTime = 0; // BCD
        public int dbUniqueCode  = 0;

        public byte[] encode() {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = 0;

            ByteUtil.wordToByteArray(cpMode , vdData, idx);
            idx += 2;

            ByteUtil.longToByteArray(cpStatus, vdData, 8, idx);
            idx += 8;

            ByteUtil.intToByteArray(meterVal, vdData, idx);
            idx += 4;

            System.arraycopy(cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            ByteUtil.intToByteArray(chargingKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray3(chargingTime, vdData, idx);
            idx += 3;

            ByteUtil.intToByteArray(chargingCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)payMethod;
            vdData[idx++] = (byte)chargeEndStatus;

            ByteUtil.longToByteArray(chargeStartTime, vdData, 7, idx);
            idx += 7;

            ByteUtil.wordToByteArray(dbUniqueCode, vdData, idx);
            idx += 2;

            return vdData;
        }
    }
}
