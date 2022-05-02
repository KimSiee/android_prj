/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 8:35
 */

package com.joas.hw.dsp;

import com.joas.utils.BitUtil;
import com.joas.utils.ByteUtil;

/**
 * Created by user on 2017-08-18.
 */

public class DSPRxData {
    public enum STATUS400 {
        READY(0),
        AVAL_CHARGE(1),
        STATE_PLUG(2),
        STATE_DOOR(3),
        CHARGE_RUN(4),
        FINISH_CHARGE(5),
        FAULT(6),
        STATE_RESET(7),
        CONNECTOR_LOCK_A(9),
        CONNECTOR_LOCK_B(10),
        RESERVED_11(11),
        CONNECTOR_POS_COMBO_BC(12),
        CONNECTOR_POS_CHADEMO(13),
        CONNECTOR_POS_AC3(14),
        STATUS_NONE(16);

        private int idx;

        private STATUS400(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
        public boolean Compare(int i){return idx == i;}
        public static STATUS400 getValue(int _id)
        {
            STATUS400[] As = STATUS400.values();
            for(int i = 0; i < As.length; i++)
            {
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS400.STATUS_NONE;
        }
    };

    public Object lock = new Object(); // For Sync
    public int address;

    public static final int START_REG_ADDR = 400;
    public static final int DATA_OFFSET = 3;

    public byte[] rawData;

    public int status400 = 0;
    public int version = 0;
    public int seqErrorCode = 0;
    public float ampareOut = 0;
    public float voltageOut = 0;
    public long meterValue = 0;
    public long meterValueFast = 0;
    public int chargeMode = 0;
    public int remainTime = 0;
    public int batterySOC = 0;
    public int stopCode = 0;
    public int fault406 = 0;
    public int fault407 = 0;
    public int fault408 = 0;
    public int fault409 = 0;
    public int fault416 = 0;

    private int regCount =  17;
    private int meterScale = 1;

    public DSPRxData(int addr, int regcount, int meterScaleVal) {
        this.address = addr;
        regCount = regcount;
        rawData = new byte[5 + 2 * regCount];
        this.meterScale = meterScaleVal;
    }

    public void set400Reg(STATUS400 idx, boolean tf) {
        if ( tf ) status400 = BitUtil.setBit(status400, idx.getidx());
        else status400 = BitUtil.clearBit(status400, idx.getidx());
    }

    public boolean get400Reg(STATUS400 idx) {
        return BitUtil.getBitBoolean(status400, idx.getidx());
    }

    public void decode() {
        int idx = DATA_OFFSET; // First Data

        status400 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        //401, 402
        version = ByteUtil.makeInt(rawData[idx], rawData[idx+1], rawData[idx+2], rawData[idx+3]);
        idx += 4;

        // 403
        seqErrorCode = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 404,5 (low, high word), idx, idx+1 => low, idx+2, idx+3 ->High
        ampareOut = Float.intBitsToFloat(ByteUtil.makeInt(rawData[idx+2], rawData[idx+3], rawData[idx], rawData[idx+1])); // High -> Low
        idx += 4;

        fault406 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        fault407 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        fault408 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        fault409 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 410,11 (411이 상위, 410이 하위)
        voltageOut= Float.intBitsToFloat(ByteUtil.makeInt(rawData[idx+2], rawData[idx+3], rawData[idx], rawData[idx+1]));

        // 완속 미터(voltageOut과 같이사용)
        meterValue  = ByteUtil.makeLong(rawData[idx+2], rawData[idx+3], rawData[idx], rawData[idx+1]);
        idx += 4;

        meterValue = meterValue*meterScale; // 10W 단위

        // 412
        chargeMode = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 413`
        remainTime = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 414
        batterySOC = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 415
        stopCode = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        //416
        fault416 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        if ( regCount == DSPControl.DSP_VER_REG23_SIZE ) {
            // 417, 418
            meterValueFast  = ByteUtil.makeLong(rawData[idx+2], rawData[idx+3], rawData[idx], rawData[idx+1]);
            meterValueFast = meterValueFast*meterScale; // 급속 계량기 w로 변환(*10)
            idx += 4;
        }
    }
}
