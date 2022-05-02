/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 8:35
 */

package com.joas.hw.dsp2;

import com.joas.utils.BitUtil;
import com.joas.utils.CRC16;

import java.util.Arrays;

/**
 * Created by user on 2017-08-18.
 */

public class DSPTxData2 {

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
        UI_STARTSTOPBT_CLEAR(9),
        UI_LOAD_TEST(10),
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


    public enum STATUS216 {
        SERVER_COMM(0),                 //00 서버통신이상 (0: 정상, 1: 미접속)
        MANAGE_STOP(1),                 //01 운영중지 (0: 정상, 1: 미접속)
        RF_COMM(2),                     //02 RF카드통신이상 (0: 정상, 1: 미접속)
        FIRM_DOWNLAOD_STATUS(3),        //03 펌웨어다운로드상태 (0: 정상, 1: 미접속)
        CHARGER_RESET_SIGNAL_STATUS(4), //04 충전기 리셋 신호수신상태 (0: 정상, 1: 미접속)
        FREE_MODE(5),                   //05 무료모드상태 (0: 정상, 1: 미접속)
        RF_TAG_EVENT(6),                //06 RF카드 태깅 이벤트 (0: 정상, 1: 미접속)
        TIMEOUT(7),                     //07 타임아웃 (0: 정상, 1: 미접속)
        INDOOR_COMM_ERR(8);             //08 내부통신오류 상태 (0: 정상, 1: 미접속)

        private int idx;

        private STATUS216(int value) {
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
    public static int CHARGER_SELECT_SLOW_BTYPE = 6;
    public static int CHARGER_SELECT_SLOW_CTYPE = 7;

    public static int CHARGER_SELECT_MOBILE_DCCHADEMO = 1;
    public static int CHARGER_SELECT_MOBILE_DCCOMBO1 = 2;
    public static int CHARGER_SELECT_MOBILE_DCCOMBO2 = 3;


    public Object lock = new Object(); // For Sync
    public static final int START_REG_ADDR = 200;
    public static final int DATA_OFFSET = 7;

    private int address;
    public int regCount = 17;

    public byte[] rawData;

    public int chargeSelect = 0;
    private int status200 = 0;
    public int slowChargerType = 0;//202
    public int uiState = 0;
    public int runCount = 0;
    public float voltageCmd = 0;
    public float ampareCmd = 0;
    public int meterUpdateCmdFast = 0;
    public int powerLimit  = 0;

    public float outputVoltageAC = 0;   // 출력전압(AC) 프로토콜 ver 2.0 추가
    public float outputAmpareAC = 0;    // 출력전력(AC) 프로토콜 ver 2.0 추가
    public float meterAC = 0;             // 파워미터 프로토콜 ver 2.0 추가

    public float outputVoltageDC = 0;       //출력전압(DC)
    public float outputAmpareDC = 0;        //출력전류(DC)
    public float meterDC = 0;               //파워미터(DC)

    //200번지 추가
    public int status210 = 0;
    public int status211 = 0;
    public int status212 = 0;
    public int status213 = 0;
    public int status214 = 0;
    public int status215 = 0;
    public int status216 = 0;


    public DSPTxData2(int addr, int regCnt) {
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


    /**
     * 216번지 값 쓰기
     *
     * by Lee 20200518
     * @param idx
     * @param tf
     */
    public void set216Reg(STATUS216 idx, boolean tf) {
        if ( tf ) status216 = BitUtil.setBit(status216, idx.getidx());
        else status216 = BitUtil.clearBit(status216, idx.getidx());
    }

    public boolean get216Reg(STATUS216 idx) {
        return BitUtil.getBitBoolean(status216, idx.getidx());
    }

    public byte[] encode(int channel, DSPTXData2ExtInterface extInterface) {
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

//        // uiState (2)(형식인증이후 사용안함)
//        rawData[idx++] = (byte)((uiState >> 8 ) & 0xff);
//        rawData[idx++] = (byte)(uiState & 0xff);

        // slowChargerType (2)
        rawData[idx++] = (byte)((slowChargerType >> 8 ) & 0xff);
        rawData[idx++] = (byte)(slowChargerType & 0xff);

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

        // Power Limit (9)
        rawData[idx++] = (byte)((status210 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status210 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status211 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status211 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status212 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status212 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status213 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status213 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status214 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status214 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status215 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status215 & 0xff);

        // Power Limit (9)
        rawData[idx++] = (byte)((status216 >> 8 ) & 0xff);
        rawData[idx++] = (byte)(status216 & 0xff);


        // 프로토콜 ver 2.0
        // 출력 전압 (217,218)
        int outputVoltageAC = Float.floatToIntBits(this.outputVoltageAC);

        // Low first
        rawData[idx++] = (byte)((outputVoltageAC >> 8 ) & 0xff);        //리틀엔디안
        rawData[idx++] = (byte)(outputVoltageAC & 0xff);

        // High
        rawData[idx++] = (byte)((outputVoltageAC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((outputVoltageAC >> 16 ) & 0xff);



        // 출력 전류 (219,220)
        int outputAmpareAC = Float.floatToIntBits(this.outputAmpareAC);

        // Low first
        rawData[idx++] = (byte)((outputAmpareAC >> 8 ) & 0xff);         //리틀엔디안
        rawData[idx++] = (byte)(outputAmpareAC & 0xff);

        // High
        rawData[idx++] = (byte)((outputAmpareAC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((outputAmpareAC >> 16 ) & 0xff);



        // 파워미터 (221, 222)
        int meterAC = Float.floatToIntBits(this.meterAC);

        // Low first
        rawData[idx++] = (byte)((meterAC >> 8 ) & 0xff);                //리틀엔디안
        rawData[idx++] = (byte)(meterAC & 0xff);

        // High
        rawData[idx++] = (byte)((meterAC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((meterAC >> 16 ) & 0xff);

        //DC
        // 출력 전압 (223,224)
        int outputVoltageDC = Float.floatToIntBits(this.outputVoltageDC);

        // Low first
        rawData[idx++] = (byte)((outputVoltageDC >> 8 ) & 0xff);        //리틀엔디안
        rawData[idx++] = (byte)(outputVoltageDC & 0xff);

        // High
        rawData[idx++] = (byte)((outputVoltageDC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((outputVoltageDC >> 16 ) & 0xff);

        // 출력 전류 (225,226)
        int outputAmpareDC = Float.floatToIntBits(this.outputAmpareDC);

        // Low first
        rawData[idx++] = (byte)((outputAmpareDC >> 8 ) & 0xff);         //리틀엔디안
        rawData[idx++] = (byte)(outputAmpareDC & 0xff);

        // High
        rawData[idx++] = (byte)((outputAmpareDC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((outputAmpareDC >> 16 ) & 0xff);



        // 파워미터 (227, 228)
        int meterDC = Float.floatToIntBits(this.meterDC);

        // Low first
        rawData[idx++] = (byte)((meterDC >> 8 ) & 0xff);                //리틀엔디안
        rawData[idx++] = (byte)(meterDC & 0xff);

        // High
        rawData[idx++] = (byte)((meterDC >> 24 ) & 0xff);
        rawData[idx++] = (byte)((meterDC >> 16 ) & 0xff);


        if ( extInterface != null ) extInterface.dspTxDataEncodeExt(channel, rawData, DATA_OFFSET);



        int crc = CRC16.calc(0xFFFF, rawData, 0, rawData.length-2);
        // CRC
        rawData[rawData.length-2] = (byte)(crc & 0xff);
        rawData[rawData.length-1] = (byte)((crc >> 8 ) & 0xff);

        return rawData;
    }
}
