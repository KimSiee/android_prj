/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 8:35
 */

package com.joas.hw.dsp;

import com.joas.utils.BitUtil;
import com.joas.utils.CRC16;

import java.util.Arrays;

/**
 * Created by user on 2017-08-18.
 */

public class DSPTxData {

    public enum DSP_UI_STATE {
        UI_NONE(0),
        UI_READY(1),
        UI_AUTH_FIRST(2),
        UI_CONNECT(3),
        UI_COST_DONE(4),
        UI_START_READY(5),
        UI_START_CHARGE(6),
        UI_AUTH_SECOND(7),
        UI_FINISH_CHARGE(8),
        UI_FINISH_CHARGE2(9),
        UI_DISCONNECT(10),
        UI_FINISH_SEQ(11),
        UI_ERROR_CHARGE(12),
        UI_CHECK(13);
        private int value;

        private DSP_UI_STATE(int val) {
            this.value = val;
        }
        public int getValue() {
            return value;
        }
    };

    public enum STATUS200 {
        READY(0),
        START_CHARGE(3),
        FINISH_CHARGE(4),
        TEST_FLAG(5),
        DOOR_OPEN(6),
        UI_FAULT(7),
        TEST_BIT(9),
        BOOT_MODE(10),
        RESET_REQ(11),
        UI_MODEM_COMM(13),
        CH2_LED1(14),
        CH2_LED2(15);

        private int idx;

        private STATUS200(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
    };

    public static int CHARGER_SELECT_FAST_AC3 = 1;
    public static int CHARGER_SELECT_FAST_DCCHADEMO = 2;
    public static int CHARGER_SELECT_FAST_DCCOMBO = 3;
    public static int CHARGER_SELECT_FAST_DCCHADEMO_TEST = 0x04;
    public static int CHARGER_SELECT_FAST_DCCOMBO_TEST = 0x05;
    public static int CHARGER_SELECT_SLOW_BTYPE = 1;
    public static int CHARGER_SELECT_SLOW_CTYPE = 2;

    public static int CHARGER_SELECT_MOBILE_DCCHADEMO = 1;
    public static int CHARGER_SELECT_MOBILE_DCCOMBO1 = 2;
    public static int CHARGER_SELECT_MOBILE_DCCOMBO2 = 3;


    public Object lock = new Object(); // For Sync
    public static final int START_REG_ADDR = 200;
    public static final int DATA_OFFSET = 7;

    private int address;
    public int regCount = 10;

    public byte[] rawData;

    private int status200 = 0;
    public int chargeSelect = 0;
    public int uiState = 0;
    public int runCount = 0;
    public float voltageCmd = 0;
    public float ampareCmd = 0;
    public int meterUpdateCmdFast = 0;
    public int powerLimit  = 0;

    public DSPTxData(int addr, int regCnt) {
        this.address = addr;
        regCount = regCnt;

        // 1 addr + 1 fcn + 2 start + 2 reg + 1 count + 2 * reg vals + 2 CRC
        rawData = new byte[9 + 2* regCount];
    }

    public void InitData() {
        chargeSelect = 1;
    }

    public void set200Reg(STATUS200 idx, boolean tf) {
        if ( tf ) status200 = BitUtil.setBit(status200, idx.getidx());
        else status200 = BitUtil.clearBit(status200, idx.getidx());
    }

    public boolean get200Reg(STATUS200 idx) {
        return BitUtil.getBitBoolean(status200, idx.getidx());
    }

    public byte[] encode(int channel, DSPTXDataExtInterface extInterface) {
        int idx = 0;

        Arrays.fill(rawData, (byte)0);

        rawData[idx++] = (byte)address;
        rawData[idx++] = 16; // function code

        // start address ( 200 )
        rawData[idx++] = (byte)((START_REG_ADDR >> 8 ) & 0xff);
        rawData[idx++] = (byte)(START_REG_ADDR & 0xff);

        // register count
        rawData[idx++] = (byte)((regCount >> 8 ) & 0xff);
        rawData[idx++] = (byte)(regCount & 0xff);

        // Byte Size
        rawData[idx++] = (byte)(regCount * 2);

        // Stauts200 (0)
        rawData[idx++] = (byte)((status200 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status200 & 0xff);

        // chargeSelect (1)
        rawData[idx++] = (byte)((chargeSelect >> 8 ) & 0xff);
        rawData[idx++] = (byte)(chargeSelect & 0xff);

        // uiState (2)
        rawData[idx++] = (byte)((uiState >> 8 ) & 0xff);
        rawData[idx++] = (byte)(uiState & 0xff);

        // runCount (3)
        rawData[idx++] = (byte)((runCount >> 8 ) & 0xff);
        rawData[idx++] = (byte)(runCount & 0xff);

        runCount = (runCount + 1) % 6500; // 매 요청시 증가, 6500까지 순환

        // Reserved (204)
        rawData[idx++] = 0;
        rawData[idx++] = (byte)(meterUpdateCmdFast & 0xff); // 급속 전력량계 값 얻어오는 명령

        // voltageCmd (5,6)
        int cvtValue = Float.floatToIntBits(voltageCmd);

        // Low first
        rawData[idx++] = (byte)((cvtValue >> 8 ) & 0xff);
        rawData[idx++] = (byte)(cvtValue & 0xff);

        // High
        rawData[idx++] = (byte)((cvtValue >> 24 ) & 0xff);
        rawData[idx++] = (byte)((cvtValue >> 16 ) & 0xff);

        // ampareCmd (7,8)
        cvtValue = Float.floatToIntBits(ampareCmd);

        // Low first
        rawData[idx++] = (byte)((cvtValue >> 8 ) & 0xff);
        rawData[idx++] = (byte)(cvtValue & 0xff);

        // High
        rawData[idx++] = (byte)((cvtValue >> 24 ) & 0xff);
        rawData[idx++] = (byte)((cvtValue >> 16 ) & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((powerLimit >> 8 ) & 0xff);
        rawData[idx++] = (byte)(powerLimit & 0xff);

        if ( extInterface != null ) extInterface.dspTxDataEncodeExt(channel, rawData, DATA_OFFSET);

        int crc = CRC16.calc(0xFFFF, rawData, 0, rawData.length-2);
        // CRC
        rawData[rawData.length-2] = (byte)(crc & 0xff);
        rawData[rawData.length-1] = (byte)((crc >> 8 ) & 0xff);

        return rawData;
    }
}
