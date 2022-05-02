/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 8:35
 */

package com.joas.hw.dsp2;

import com.joas.utils.BitUtil;
import com.joas.utils.ByteUtil;

/**
 * Created by user on 2017-08-18.
 *
 * Protocol_Joas_20200728_Ver_2_0 프로토콜 기반
 * 407 번지 추가
 *
 * by Lee 20200728
 *
 */

public class DSPRxData2 {
            public enum STATUS400 {
                READY(0),
                AVAL_CHARGE(1),
                STATE_PLUG(2),
                STATE_DOOR(3),
                 CHARGE_RUN(4),
                FINISH_CHARGE(5),
                FAULT(6),
                STATE_RESET(7),
                CG_STARTSTOPBT(8),
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


    /**
     *  출력 전압
     *
     *  by Lee 20200728
     */
    public enum STATUS407 {
        TEST_MODE(0),
        NO_SERVICE_MODE(1),     //add by si. 200818 - gridwiz 충전금지표시상태
        PLC_CONN_STAT(2),       //add by si. 200818 - gridwiz PLC 서버연결 상태
        AUTH_FAIL_STAT(3),      //add by si. 200821 - gridwiz 회원인증 결과 실패 상태(false,대기, true : 실패)
        AUTH_SUCCESS_STAT(4),   //add by si. 200821 - gridwiz 회원인증 결과 성공 상태(false 대기, true : 성공)
        STATUS407_NONE(16);

        private int idx;

        private STATUS407(int value) {
            this.idx = value;
        }
        public int getidx() {
            return idx;
        }
        public boolean Compare(int i){return idx == i;}
        public static STATUS407 getValue(int _id)
        {
            STATUS407[] As = STATUS407.values();
            for(int i = 0; i < As.length; i++)
            {
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS407.STATUS407_NONE;
        }
    };


    public Object lock = new Object(); // For Sync
    public int address;

    public static final int START_REG_ADDR = 400;
    public static final int DATA_OFFSET = 3;

    public byte[] rawData;

    public int status400 = 0;
    public int runCount = 0;
    public int uiVersion = 0;
    public int version = 0;
    public int remainTime = 0;
    public int batterySOC = 0;
    public int stopCode = 0;
    public int status407 = 0;//407 출력전압 추가 20200728
    public int address408 = 0;//
    public int address409 = 0;//
    public int address410 = 0;//
    public int address411 = 0;//
    public int address412 = 0;//
    public int address413 = 0;//
    public int address414 = 0;//
    public int address415 = 0;//
    public int address416 = 0;//
    public int address417 = 0;//
    public int address418 = 0;//
    public int address419 = 0;
    public int address420 = 0;
    public int address421 = 0;
    public int address422 = 0;
    public int fault423 = 0;
    public int fault424 = 0;
    public int fault425 = 0;
    public int fault426 = 0;
    public int fault427 = 0;
    public int fault428 = 0;
    public int fault429 = 0;
    public int fault430 = 0;
    public int fault431 = 0;
    public int fault432 = 0;
    public int fault433 = 0;
    public int fault434 = 0;
    public int fault435 = 0;
    public int fault436 = 0;
    public int fault437 = 0;
    public int fault438 = 0;
    public int fault439 = 0;


    private int regCount =  40;     //신규  dsp 0~ 439 까지
    private int meterScale = 1;

    public DSPRxData2(int addr, int regcount, int meterScaleVal) {
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

    /**
     * 출력전압 추가
     * by Lee 20200728
     */
    public boolean get407Reg(STATUS407 idx) {
        return BitUtil.getBitBoolean(status407, idx.getidx());
    }

    public void decode() {
        int idx = DATA_OFFSET; // First Data

        // 400
        status400 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 401 Run Count
        runCount = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 402 종료코드
        stopCode = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 403 UI version
        uiVersion = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 404 F/W version
        version = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 405 남은시간(분)
        remainTime = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 406 SOC
        batterySOC = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 407 출력전압
        status407 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;



        // reserved (408 ~ 422)
        // 408
        address408 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 409
        address409 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 410
        address410 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 411
        address411 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 412
        address412 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 413
        address413 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 414
        address414 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 415
        address415 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 416
        address416 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 417
        address417 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 418
        address418 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 419
        address419 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 420
        address420 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 421
        address421 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 422
        address422 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        // Reserved
        //////////////////////////////////////////////////////////////


        // Fault
        fault423 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault424 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault425 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault426 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault427 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault428 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault429 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault430 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault431 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault432 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault433 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault434 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault435 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault436 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault437 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault438 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
        fault439 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;
    }
}
