/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 14 오전 11:59
 *
 */

package com.joas.hw.dsptest;


import com.joas.utils.BitUtil;
import com.joas.utils.ByteUtil;

public class DSPTestRxData {
    public enum STATUS500 {
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
        WAKE_RY2(15),
        STATUS_NONE(16);

        private int idx;

        private STATUS500(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS500 getValue(int _id){
            STATUS500[] As = STATUS500.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS500.STATUS_NONE;
        }
    };


    public enum STATUS501 {
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
        A232(15),
        STATUS_NONE(16);

        private int idx;

        private STATUS501(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS501 getValue(int _id){
            STATUS501[] As = STATUS501.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS501.STATUS_NONE;
        }
    };


    public enum STATUS502 {
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
        LEAK(10),
        STATUS_NONE(16);

        private int idx;

        private STATUS502(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS502 getValue(int _id){
            STATUS502[] As = STATUS502.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS502.STATUS_NONE;
        }
    };


    public enum STATUS503 {
        CHADEMO_IO_TEST_START(0),
        CHADEMO_NO_LOAD_TEST_START(1),
        CHADEMO_LOAD_TEST_START(2),
        CHADEMO_OVERVOLTAGE_TEST_START(3),
        CHADEMO_OVERCURR_TEST_START(4),
        CCS_NO_LOAD_TEST_START(5),
        CCS_LOAD_TEST_START(6),
        CCS_OVERVOLTAGE_START(7),
        CCS_OVERCURR_TEST_START(8),
        AC_OUTPUT_TEST(9),
        STATUS_NONE(16);

        private int idx;

        private STATUS503(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS503 getValue(int _id){
            STATUS503[] As = STATUS503.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS503.STATUS_NONE;
        }
    };


    public enum STATUS504 {
        RY1_ERROR(0),
        RY2_ERROR(1),
        RY3_ERROR(2),
        RY4_ERROR(3),
        RY5_ERROR(4),
        DOOR1_ERROR(5),
        DOOR2_ERROR(6),
        DOOR3_ERROR(7),
        MC1_ERROR(8),
        MC2_ERROR(9),
        MC3_ERROR(10),
        TRIP_ERROR(11),
        FAN_ERROR(12),
        SP1_ERROR(13),
        WAKE_RY1_ERROR(14),
        WAKE_RY2_ERROR(15),
        STATUS_NONE(16);

        private int idx;

        private STATUS504(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS504 getValue(int _id){
            STATUS504[] As = STATUS504.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS504.STATUS_NONE;
        }
    };


    public enum STATUS505 {
        SOL_ERROR(0),
        LED_R_ERROR(1),
        LED_G_ERROR(2),
        LED_B_ERROR(3),
        ELCB_ERROR(4),
        LIMIT_ERROR(5),
        DCGF_ERROR(6),
        LIMIT1_ERROR(7),
        LIMIT2_ERROR(8),
        LIMIT3_ERROR(9),
        WELDING1_ERROR(10),
        RCD1_TRIP_ERROR(11),
        ELCB_TRIP_ERROR(12),
        AC_CP_ERROR(13),
        AC_SLOW_CP_ERROR(14),
        ERROR_232(15),
        STATUS_NONE(16);

        private int idx;

        private STATUS505(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS505 getValue(int _id){
            STATUS505[] As = STATUS505.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS505.STATUS_NONE;
        }
    };


    public enum STATUS506 {
        MODULE_ERROR(0),
        PLC_ERROR(1),
        PM_ERROR(2),
        PLC_RESET_ERROR(3),
        UI_RESET_ERROR(4),
        EMG_ERROR(5),
        WELDING2_ERROR(6),
        RCD_TRIP2_ERROR(7),
        FG_ERROR(8),
        RCM_ERROR(9),
        LEAK_ERROR(10),
        OVER_VOLTAGE(11),
        OVER_CURRENT(12),
        STATUS_NONE(16);

        private int idx;

        private STATUS506(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS506 getValue(int _id){
            STATUS506[] As = STATUS506.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS506.STATUS_NONE;
        }
    };


    public enum STATUS507 {
        MODULE1_ERROR(0),
        MODULE2_ERROR(1),
        MODULE3_ERROR(2),
        MODULE4_ERROR(3),
        MODULE5_ERROR(4),
        MODULE6_ERROR(5),
        MODULE7_ERROR(6),
        MODULE8_ERROR(7),
        MODULE9_ERROR(8),
        MODULE10_ERROR(9),
        MODULE11_ERROR(10),
        MODULE12_ERROR(11),
        MODULE13_ERROR(12),
        MODULE14_ERROR(13),
        MODULE15_ERROR(14),
        MODULE16_ERROR(15),
        STATUS_NONE(16);

        private int idx;

        private STATUS507(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS507 getValue(int _id){
            STATUS507[] As = STATUS507.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS507.STATUS_NONE;
        }
    };


    public enum STATUS508 {
        MODULE17_ERROR(0),
        MODULE18_ERROR(1),
        MODULE19_ERROR(2),
        MODULE20_ERROR(3),
        STATUS_NONE(16);

        private int idx;

        private STATUS508(int value) {
            this.idx = value;
        }
        public int getidx() { return idx; }
        public boolean Compare(int i) {return idx == i;}
        public static STATUS508 getValue(int _id){
            STATUS508[] As = STATUS508.values();
            for(int i=0;i<As.length;i++){
                if(As[i].Compare(_id))
                    return As[i];
            }
            return STATUS508.STATUS_NONE;
        }
    };

    public Object lock = new Object(); // For Sync
    public int address;

    public static final int START_REG_ADDR = 500;
    public static final int DATA_OFFSET = 3;

    public byte[] rawData;

    public int outputVoltage = 0;
    public int outputCurrent = 0;
    public int connectorTemp = 0;
    public int innerTemp = 0;
    public int status500 = 0;
    public int status501 = 0;
    public int status502 = 0;
    public int status503 = 0;
    public int status504 = 0;
    public int status505 = 0;
    public int status506 = 0;
    public int status507 = 0;
    public int status508 = 0;

    private int regCount =  40;     //신규  dsp 0~ 439 까지
    private int meterScale = 1;

    public DSPTestRxData(int addr, int regcount, int meterScaleVal) {
        this.address = addr;
        regCount = regcount;
        rawData = new byte[5 + 2 * regCount];
        this.meterScale = meterScaleVal;
    }


    //get,set 500
    public void set500Reg(STATUS500 idx, boolean tf) {
        if ( tf ) status500 = BitUtil.setBit(status500, idx.getidx());
        else status500 = BitUtil.clearBit(status500, idx.getidx());
    }
    public boolean get500Reg(STATUS500 idx) {
        return BitUtil.getBitBoolean(status500, idx.getidx());
    }

    //get,set 501
    public void set501Reg(STATUS501 idx, boolean tf) {
        if ( tf ) status501 = BitUtil.setBit(status501, idx.getidx());
        else status501 = BitUtil.clearBit(status501, idx.getidx());
    }
    public boolean get501Reg(STATUS501 idx) {
        return BitUtil.getBitBoolean(status501, idx.getidx());
    }

    //get,set 502
    public void set502Reg(STATUS502 idx, boolean tf) {
        if ( tf ) status502 = BitUtil.setBit(status502, idx.getidx());
        else status502 = BitUtil.clearBit(status502, idx.getidx());
    }
    public boolean get502Reg(STATUS502 idx) {
        return BitUtil.getBitBoolean(status502, idx.getidx());
    }

    //get,set 503
    public void set503Reg(STATUS503 idx, boolean tf) {
        if ( tf ) status503 = BitUtil.setBit(status503, idx.getidx());
        else status503 = BitUtil.clearBit(status503, idx.getidx());
    }
    public boolean get503Reg(STATUS503 idx) {
        return BitUtil.getBitBoolean(status503, idx.getidx());
    }

    //get,set 504
    public void set504Reg(STATUS504 idx, boolean tf) {
        if ( tf ) status504 = BitUtil.setBit(status504, idx.getidx());
        else status504 = BitUtil.clearBit(status504, idx.getidx());
    }
    public boolean get504Reg(STATUS504 idx) {
        return BitUtil.getBitBoolean(status504, idx.getidx());
    }

    //get,set 505
    public void set505Reg(STATUS505 idx, boolean tf) {
        if ( tf ) status505 = BitUtil.setBit(status505, idx.getidx());
        else status505 = BitUtil.clearBit(status505, idx.getidx());
    }
    public boolean get505Reg(STATUS505 idx) {
        return BitUtil.getBitBoolean(status505, idx.getidx());
    }

    //get,set 506
    public void set506Reg(STATUS506 idx, boolean tf) {
        if ( tf ) status506 = BitUtil.setBit(status506, idx.getidx());
        else status506 = BitUtil.clearBit(status506, idx.getidx());
    }
    public boolean get506Reg(STATUS506 idx) {
        return BitUtil.getBitBoolean(status506, idx.getidx());
    }

    //get,set 507
    public void set507Reg(STATUS507 idx, boolean tf) {
        if ( tf ) status507 = BitUtil.setBit(status507, idx.getidx());
        else status507 = BitUtil.clearBit(status507, idx.getidx());
    }
    public boolean get507Reg(STATUS507 idx) {
        return BitUtil.getBitBoolean(status507, idx.getidx());
    }

    //get,set 508
    public void set508Reg(STATUS508 idx, boolean tf) {
        if ( tf ) status508 = BitUtil.setBit(status508, idx.getidx());
        else status508 = BitUtil.clearBit(status508, idx.getidx());
    }
    public boolean get508Reg(STATUS508 idx) {
        return BitUtil.getBitBoolean(status508, idx.getidx());
    }

    public void decode() {
        int idx = DATA_OFFSET; // First Data

        // 500
        status500 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 501
        status501 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 502
        status502 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 503
        status503 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 504
        status504 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 505
        status505 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 506
        status506 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 507
        status507 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 508
        status508 = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 509 output voltage
        outputVoltage = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 510 output current
        outputCurrent = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 511 connector temp
        connectorTemp = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;

        // 512 inner temp
        innerTemp = ByteUtil.makeWord(rawData[idx], rawData[idx+1]);
        idx += 2;


    }

}
