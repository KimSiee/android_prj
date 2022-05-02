/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd;

import android.content.Context;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.posco_slow_charlcd.R;
import com.joas.utils.BitUtil;

import java.util.Vector;

/**
 * DSP/EIM 에서 오는 Fault 정보를 처리
 *
 * 신규프로토콜 적용 (2020-02-19)
 * Protocol_Joas_20200219_Ver_1_0.xlsx
 *
 * by Lee 20200518
 */

public class FaultManager {
    DSPControl2 dspControl;
    Context context;
    int channel = 0;

    int preFault423 = 0;
    int preFault424 = 0;
    int preFault425 = 0;

    public FaultManager(DSPControl2 control, Context ctx, int ch) {
        dspControl = control;
        context = ctx;
        channel = ch;
    }

    public synchronized Vector<FaultInfo> scanFaultV2(int channel) {

        Vector<FaultInfo> faultList = new Vector<FaultInfo>();
        DSPRxData2 dspRxData = dspControl.getDspRxData2(channel);

        //TODO
        if ( preFault423 != dspRxData.fault423 ) {
            scanFault423(faultList, dspRxData.fault423);
        }

        if ( preFault424 != dspRxData.fault424 ) {
            scanFault424(faultList, dspRxData.fault424);
        }

        if ( preFault425 != dspRxData.fault425 ) {
            scanFault425(faultList, dspRxData.fault425);
        }

        return faultList;
    }

    public boolean isFaultEmergency(int channel) {
        DSPRxData2 dspRxData = dspControl.getDspRxData2(channel);
        return BitUtil.getBitBoolean(dspRxData.fault423, 0);
    }

    public void scanFault423(Vector<FaultInfo> faultList, int fault423) {
        int diffEvent = preFault423 ^ fault423;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault423, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 423*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 423 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_0_emergency_bt);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_1_mc1_err);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_2_mc2_err);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_3_mc3_err);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_4_relay1_err);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_5_relay2_err);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_6_relay3_err);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_7_relay4_err);
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_9_inp_overvoltage);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_10_temperature_err);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_11_module_control_comm_err);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_12_crtlboard1_ctrlboard2_comm_err);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_13_meter_comm_err);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_14_meter_comm_err);
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_15_inp_lowvoltage);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault423 = fault423;
    }

    public void scanFault424(Vector<FaultInfo> faultList, int fault424) {
        int diffEvent = preFault424 ^ fault424;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault424, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 424*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 407 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 1:
                        fInfo.errorMsg =" Reserved";
                        break;
                    case 2:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 3:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 4:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 5:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 6:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 7:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 9:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 11:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 12:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 13:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 14:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 15:
                        fInfo.errorMsg = "Reserved";
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault424 = fault424;
    }

    public void scanFault425(Vector<FaultInfo> faultList, int fault425) {
        int diffEvent = preFault425 ^ fault425;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault425, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 425*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 425 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_0_rcd_off);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_1_ac_relay_err);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_2_ac_leak);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_3_ac_rcm_err);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_4_out_overcurrent);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_5_ac_fg_err);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_6_ac_cp_error);
                        break;
                    case 7:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 9:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 11:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 12:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 13:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 14:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 15:
                        fInfo.errorMsg = "Reserved";
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault425 = fault425;
    }
}