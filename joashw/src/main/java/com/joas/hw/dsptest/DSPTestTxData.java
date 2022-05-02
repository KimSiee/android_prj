/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 13 오후 2:47
 *
 */

package com.joas.hw.dsptest;


import com.joas.utils.BitUtil;
import com.joas.utils.CRC16;

import java.util.Arrays;

public class DSPTestTxData {

    public enum STATUS300{
        RY1(0),
        RY2(1),
        RY3(2),
        RY4(3),
        RY5(4),
        DOOR1(5),
        DOOR2(6),
        DOOR3(7),
        MC1(8),
        MC2(9),
        MC3(10),
        TRIP(11),
        FAN(12),
        SP1(13),
        WAKE_RY1(14),
        WAKE_RY2(15);

        private int idx;

        private STATUS300(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
    };

    public enum STATUS301 {
        SOL(0),
        LED_R(1),
        LED_G(2),
        LED_B(3),
        ELCB(4),
        LIMIT(5),
        DCGF(6),
        LIMIT1(7),
        LIMIT2(8),
        LIMIT3(9),
        WELDING(10),
        RCD_TRIP(11),
        ELCB_TRIP(12),
        AC_CP(13),
        AC_SLOW_CP(14),
        A232(15);


        private int idx;

        private STATUS301(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }

    };

    public enum STATUS302{
        MODULE(0),
        PLC(1),
        PM(2),
        PLC_RESET(3),
        UI_RESET(4),
        EMG(5),
        WELDING2(6),
        RCD_TRIP2(7),
        FG(8),
        RCM(9),
        LEAK(10);

        private int idx;

        private STATUS302(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
    }

    public enum STATUS303{
        CHADEMO_IO_TEST_START(0),
        CHADEMO_NO_LOAD_TEST_START(1),
        CHADEMO_LOAD_TEST_START(2),
        CHADEMO_OVERVOLTAGE_TEST_START(3),
        CHADEMO_OVERCURR_TEST_START(4),
        CCS_NO_LOAD_TEST_START(5),
        CCS_LOAD_TEST_START(6),
        CCS_OVERVOLTAGE_START(7),
        CCS_OVERCURR_TEST_START(8),
        AC_OUTPUT_TEST(9);

        private int idx;

        private STATUS303(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
    }


    private int status300 = 0;
    private int status301 = 0;
    private int status302 = 0;
    private int status303 = 0;

    public Object lock = new Object(); // For Sync
    public static final int START_REG_ADDR = 300;
    public static final int DATA_OFFSET = 7;

    private int address;
    public int regCount = 17;

    public int meterUpdateCmdFast = 0;
    public byte[] rawData;

    public DSPTestTxData(int addr, int regCnt) {
        this.address = addr;
        regCount = regCnt;

        // 1 addr + 1 fcn + 2 start + 2 reg + 1 count + 2 * reg vals + 2 CRC
        rawData = new byte[9 + 2* regCount];
    }

    //set,get 300
    public void set300Reg(STATUS300 idx, boolean tf) {
        if ( tf ) status300 = BitUtil.setBit(status300, idx.getidx());
        else status300 = BitUtil.clearBit(status300, idx.getidx());
    }

    public boolean get300Reg(STATUS300 idx) {
        return BitUtil.getBitBoolean(status300, idx.getidx());
    }


    //set,get 301
    public void set301Reg(STATUS301 idx, boolean tf) {
        if (tf) status301 = BitUtil.setBit(status301, idx.getidx());
        else status301 = BitUtil.clearBit(status301, idx.getidx());
    }
    public boolean get301Reg(STATUS301 idx) {
        return BitUtil.getBitBoolean(status301, idx.getidx());
    }

    //set,get 302
    public void set302Reg(STATUS302 idx, boolean tf) {
        if (tf) status302 = BitUtil.setBit(status302, idx.getidx());
        else status302 = BitUtil.clearBit(status302, idx.getidx());
    }
    public boolean get302Reg(STATUS302 idx) {
        return BitUtil.getBitBoolean(status302, idx.getidx());
    }

    //set,get 303
    public void set303Reg(STATUS303 idx,boolean tf) {
        if (tf) status303 = BitUtil.setBit(status303, idx.getidx());
        else status303 = BitUtil.clearBit(status303, idx.getidx());
    }
    public boolean get303Reg(STATUS303 idx) {
        return BitUtil.getBitBoolean(status303, idx.getidx());
    }


    public byte[] encode(int channel, DSPTestTXDataExInterface extInterface) {
        int idx = 0;

        Arrays.fill(rawData, (byte)0);

        rawData[idx++] = (byte)address;
        rawData[idx++] = 16; // function code

        // start address ( 300 )
        rawData[idx++] = (byte)((START_REG_ADDR >> 8 ) & 0xff);
        rawData[idx++] = (byte)(START_REG_ADDR & 0xff);

        // register count
        rawData[idx++] = (byte)((regCount >> 8 ) & 0xff);
        rawData[idx++] = (byte)(regCount & 0xff);

        // Byte Size
        rawData[idx++] = (byte)(regCount * 2);

        // Stauts300 (0)
        rawData[idx++] = (byte)((status300 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status300 & 0xff);

        // Status300 (1)
        rawData[idx++] = (byte)((status301 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status301 & 0xff);

        // Status300 (2)
        rawData[idx++] = (byte)((status302 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status302 & 0xff);

        // Status300 (3)
        rawData[idx++] = (byte)((status303 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status303 & 0xff);

        if ( extInterface != null ) extInterface.dspTestTxDataEncodeExt(channel, rawData, DATA_OFFSET);

        int crc = CRC16.calc(0xFFFF, rawData, 0, rawData.length-2);
        // CRC
        rawData[rawData.length-2] = (byte)(crc & 0xff);
        rawData[rawData.length-1] = (byte)((crc >> 8 ) & 0xff);

        return rawData;
    }
}
