/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui_chargev;

import android.content.Context;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.utils.BitUtil;

import java.util.Vector;

/**
 * DSP/EIM 에서 오는 Fault 정보를 처리
 */

public class FaultManager {
    DSPControl2 dspControl;
    Context context;
    int channel = 0;

    int preFault423 = 0;
    int preFault424 = 0;
    int preFault425 = 0;
    int preFault426 = 0;
    int preFault427 = 0;
    int preFault428 = 0;
    int preFault429 = 0;
    int preFault430 = 0;
    int preFault431 = 0;
    int preFault432 = 0;
    int preFault433 = 0;
    int preFault434 = 0;
    int preFault435 = 0;
    int preFault436 = 0;
    int preFault437 = 0;
    int preFault438 = 0;
    int preFault439 = 0;

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

        if(preFault426!= dspRxData.fault426){
            scanFault426(faultList, dspRxData.fault426);
        }

        if(preFault427!= dspRxData.fault427){
            scanFault427(faultList, dspRxData.fault427);
        }

        if(preFault428!= dspRxData.fault428){
            scanFault428(faultList, dspRxData.fault428);
        }

        if(preFault429!= dspRxData.fault429){
            scanFault429(faultList, dspRxData.fault429);
        }

        if(preFault430!= dspRxData.fault430){
            scanFault430(faultList, dspRxData.fault430);
        }

        if(preFault431!= dspRxData.fault431){
            scanFault431(faultList, dspRxData.fault431);
        }

        if(preFault432!= dspRxData.fault432){
            scanFault432(faultList, dspRxData.fault432);
        }

        if(preFault433!= dspRxData.fault433){
            scanFault433(faultList, dspRxData.fault433);
        }

        if(preFault434!= dspRxData.fault434){
            scanFault434(faultList, dspRxData.fault434);
        }

        if(preFault435!= dspRxData.fault435){
            scanFault435(faultList, dspRxData.fault435);
        }

        if(preFault436!= dspRxData.fault436){
            scanFault436(faultList, dspRxData.fault436);
        }

        if(preFault437!= dspRxData.fault437){
            scanFault437(faultList, dspRxData.fault437);
        }

        if(preFault438!= dspRxData.fault438){
            scanFault438(faultList, dspRxData.fault438);
        }

        if(preFault439!= dspRxData.fault439){
            scanFault439(faultList, dspRxData.fault439);
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
//                        //fInfo.alarmCode = 0x2300;
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_1_mc1_err);
                        ////fInfo.alarmCode = 0x2301;
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_2_mc2_err);
                        //fInfo.alarmCode = 0x2302;
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_3_mc3_err);
                        //fInfo.alarmCode = 0x2303;
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_4_relay1_err);
                        //fInfo.alarmCode = 0x2304;
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_5_relay2_err);
                        //fInfo.alarmCode = 0x2305;
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_6_relay3_err);
                        //fInfo.alarmCode = 0x2306;
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_7_relay4_err);
                        //fInfo.alarmCode = 0x2307;
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved";
                        //fInfo.alarmCode = 0x2308;
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_9_inp_overvoltage);
                        //fInfo.alarmCode = 0x2309;
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_10_temperature_err);
                        //fInfo.alarmCode = 0x2310;
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_11_module_control_comm_err);
                        //fInfo.alarmCode = 0x2311;
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_12_crtlboard1_ctrlboard2_comm_err);
                        //fInfo.alarmCode = 0x2312;
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_13_meter_comm_err);
                        //fInfo.alarmCode = 0x2313;
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_14_meter_comm_err);
                        //fInfo.alarmCode = 0x2314;
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_15_inp_lowvoltage);
                        //fInfo.alarmCode = 0x2315;
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
                        //fInfo.alarmCode = 0x2500;
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_1_ac_relay_err);
                        //fInfo.alarmCode = 0x2501;
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_2_ac_leak);
                        //fInfo.alarmCode = 0x2502;
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_3_ac_rcm_err);
                        //fInfo.alarmCode = 0x2503;
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_4_out_overcurrent);
                        //fInfo.alarmCode = 0x2504;
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_5_ac_fg_err);
                        //fInfo.alarmCode = 0x2505;
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_6_ac_cp_error);
                        //fInfo.alarmCode = 0x2506;
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

    public void scanFault426(Vector<FaultInfo> faultList, int fault426) {
        int diffEvent = preFault426 ^ fault426;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault426, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 426*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 426 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "(콤보차량에러)충전 커플러 잠김상태 불량";
                        //fInfo.alarmCode = 0x2600;
                        break;
                    case 1:
                        fInfo.errorMsg = "(콤보차량에러)충전전류 편차 오류";
                        //fInfo.alarmCode = 0x2601;
                        break;
                    case 2:
                        fInfo.errorMsg = "(콤보차량에러)충전전압 범위/편차 오류";
                        //fInfo.alarmCode = 0x2602;
                        break;
                    case 3:
                        fInfo.errorMsg = "(콤보차량에러)충전시스템 호환성 오류";
                        //fInfo.alarmCode = 0x2603;
                        break;
                    case 4:
                        fInfo.errorMsg = "(콤보차량에러)차량 배터리 온도문제 발생";
                        //fInfo.alarmCode = 0x2604;
                        break;
                    case 5:
                        fInfo.errorMsg = "(콤보차량에러)차량 기어상태 확인";
                        //fInfo.alarmCode = 0x2605;
                        break;
                    case 6:
                        fInfo.errorMsg = "(콤보차량에러)차량상태/BMS 오류";
                        //fInfo.alarmCode = 0x2606;
                        break;
                    case 7:
                        fInfo.errorMsg = "Reserved for CCS";
                        //fInfo.alarmCode = 0x2607;
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved for CCS";
                        break;
                    case 9:
                        fInfo.errorMsg = "(콤보에러)차량-컨트롤보드 통신 오류";
                        //fInfo.alarmCode = 0x2609;
                        break;
                    case 10:
                        fInfo.errorMsg = "(콤보에러)커넥터 과온도 오류";
                        //fInfo.alarmCode = 0x2610;
                        break;
                    case 11:
                        fInfo.errorMsg = "(콤보에러)커넥터 잠김 오류";
                        //fInfo.alarmCode = 0x2611;
                        break;
                    case 12:
                        fInfo.errorMsg = "(콤보에러)과전압";
                        //fInfo.alarmCode = 0x2612;
                        break;
                    case 13:
                        fInfo.errorMsg = "(콤보에러)과전류";
                        //fInfo.alarmCode = 0x2613;
                        break;
                    case 14:
                        fInfo.errorMsg = "(콤보에러)PLC모뎀상태 에러 - 재부팅or교체";
                        //fInfo.alarmCode = 0x2614;
                        break;
                    case 15:
                        fInfo.errorMsg = "(콤보에러)출력 누설전류 발생";
                        //fInfo.alarmCode = 0x2615;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault426 = fault426;
    }

    public void scanFault427(Vector<FaultInfo> faultList, int fault427) {
        int diffEvent = preFault427 ^ fault427;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault427, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 427*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 427 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2700;
                        break;
                    case 1:
                        fInfo.errorMsg = "PLC SequenceError";
                        //fInfo.alarmCode = 0x2701;
                        break;
                    case 2:
                        fInfo.errorMsg = "PLC ServiceID Invalid";
                        //fInfo.alarmCode = 0x2702;
                        break;
                    case 3:
                        fInfo.errorMsg = "PLC Unknown Session";
                        //fInfo.alarmCode = 0x2703;
                        break;
                    case 4:
                        fInfo.errorMsg = "PLC Service Selection Invalid";
                        //fInfo.alarmCode = 0x2704;
                        break;
                    case 5:
                        fInfo.errorMsg = "PLC Payment Selection Invalid";
                        //fInfo.alarmCode = 0x2705;
                        break;
                    case 6:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2706;
                        break;
                    case 7:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2707;
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2708;
                        break;
                    case 9:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2709;
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2710;
                        break;
                    case 11:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2711;
                        break;
                    case 12:
                        fInfo.errorMsg = "PLC Wrong Charge Parameter";
                        //fInfo.alarmCode = 0x2712;
                        break;
                    case 13:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2713;
                        break;
                    case 14:
                        fInfo.errorMsg = "PLC Traffic selection Invalid";
                        //fInfo.alarmCode = 0x2714;
                        break;
                    case 15:
                        fInfo.errorMsg = "PLC Charging Profile Invalid";
                        //fInfo.alarmCode = 0x2715;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault427 = fault427;
    }

    public void scanFault428(Vector<FaultInfo> faultList, int fault428) {
        int diffEvent = preFault428 ^ fault428;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault428, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 428*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 428 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2800;
                        break;
                    case 1:
                        fInfo.errorMsg = "PLC No ChargeServiceSelected";
                        //fInfo.alarmCode = 0x2801;
                        break;
                    case 2:
                        fInfo.errorMsg = "PLC WrongEnergyTransferMode";
                        //fInfo.alarmCode = 0x2802;
                        break;
                    case 3:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2803;
                        break;
                    case 4:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2804;
                        break;
                    case 5:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2805;
                        break;
                    case 6:
                        fInfo.errorMsg = "PLC TIMEOUT_CommunicationSetup";
                        //fInfo.alarmCode = 0x2806;
                        break;
                    case 7:
                        fInfo.errorMsg = "PLC TIMEOUT_Sequence";
                        //fInfo.alarmCode = 0x2807;
                        break;
                    case 8:
                        fInfo.errorMsg = "PLC TIMEOUT_NotificationMaxDelay";
                        //fInfo.alarmCode = 0x2808;
                        break;
                    case 9:
                        fInfo.errorMsg = "PLC WrongCPLevel";
                        //fInfo.alarmCode = 0x2809;
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved for PLC";
                        //fInfo.alarmCode = 0x2810;
                        break;
                    case 11:
                        fInfo.errorMsg = "PLC HLCError";
                        //fInfo.alarmCode = 0x2811;
                        break;
                    case 12:
                        fInfo.errorMsg = "PLC HeartBeatError";
                        //fInfo.alarmCode = 0x2812;
                        break;
                    case 13:
                        fInfo.errorMsg = "PLC EVSECANInit";
                        //fInfo.alarmCode = 0x2813;
                        break;
                    case 14:
                        fInfo.errorMsg = "FAILED_NONegotiation";
                        //fInfo.alarmCode = 0x2814;
                        break;
                    case 15:
                        fInfo.errorMsg = "TIMEOUT_WeldingDetection";
                        //fInfo.alarmCode = 0x2815;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault428 = fault428;
    }

    public void scanFault429(Vector<FaultInfo> faultList, int fault429) {
        int diffEvent = preFault429 ^ fault429;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault429, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 429*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 429 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "PCL 초기화 에러";
                        //fInfo.alarmCode = 0x2900;
                        break;
                    case 1:
                        fInfo.errorMsg = "SLAC 응답없음 에러";
                        //fInfo.alarmCode = 0x2901;
                        break;
                    case 2:
                        fInfo.errorMsg = "PLC CP 에러";
                        //fInfo.alarmCode = 0x2902;
                        break;
                    case 3:
                        fInfo.errorMsg = "Protocol 응답없음 에러";
                        //fInfo.alarmCode = 0x2903;
                        break;
                    case 4:
                        fInfo.errorMsg = "차징타입 응답없음 에러";
                        //fInfo.alarmCode = 0x2904;
                        break;
                    case 5:
                        fInfo.errorMsg = "세션셋업 응답없음 에러";
                        //fInfo.alarmCode = 0x2905;
                        break;
                    case 6:
                        fInfo.errorMsg = "서비스디스커버리 응답없음 에러";
                        //fInfo.alarmCode = 0x2906;
                        break;
                    case 7:
                        fInfo.errorMsg = "페이먼트 응답없음 에러";
                        //fInfo.alarmCode = 0x2907;
                        break;
                    case 8:
                        fInfo.errorMsg = "인증단계 응답없음 에러";
                        //fInfo.alarmCode = 0x2908;
                        break;
                    case 9:
                        fInfo.errorMsg = "디스커버리 정보 에러";
                        //fInfo.alarmCode = 0x2909;
                        break;
                    case 10:
                        fInfo.errorMsg = "디스커버리 응답없음 에러";
                        //fInfo.alarmCode = 0x2910;
                        break;
                    case 11:
                        fInfo.errorMsg = "케이블체크 응답없음 에러";
                        //fInfo.alarmCode = 0x2911;
                        break;
                    case 12:
                        fInfo.errorMsg = "케이블체크 CP 에러";
                        //fInfo.alarmCode = 0x2912;
                        break;
                    case 13:
                        fInfo.errorMsg = "케이블체크 1 에러";
                        //fInfo.alarmCode = 0x2913;
                        break;
                    case 14:
                        fInfo.errorMsg = "케이블체크 2 에러";
                        //fInfo.alarmCode = 0x2914;
                        break;
                    case 15:
                        fInfo.errorMsg = "프리차지 응답없음 에러";
                        //fInfo.alarmCode = 0x2915;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault429 = fault429;
    }

    public void scanFault430(Vector<FaultInfo> faultList, int fault430) {
        int diffEvent = preFault430 ^ fault430;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault430, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 430*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 430 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "프리차지 에러";
                        //fInfo.alarmCode = 0x3000;
                        break;
                    case 1:
                        fInfo.errorMsg = "FAULT_HPGPLinkDown";
                        //fInfo.alarmCode = 0x3001;
                        break;
                    case 2:
                        fInfo.errorMsg = "카페이먼트 인증 실패";
                        //fInfo.alarmCode = 0x3002;
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        fInfo.errorMsg = "Reserved for sequence error_CCS";
                        //fInfo.alarmCode = 0x3015;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault430 = fault430;
    }

    public void scanFault431(Vector<FaultInfo> faultList, int fault431) {
        int diffEvent = preFault431 ^ fault431;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault431, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 431*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 431 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "(차데모에러)충전 커플러 잠김상태 불량";
                        //fInfo.alarmCode = 0x3100;
                        break;
                    case 1:
                        fInfo.errorMsg = "(차데모차량에러)충전 시스템 호환성 오류";
                        //fInfo.alarmCode = 0x3101;
                        break;
                    case 2:
                        fInfo.errorMsg = "(차데모차량에러)차량 배터리 과전압";
                        //fInfo.alarmCode = 0x3102;
                        break;
                    case 3:
                        fInfo.errorMsg = "(차데모차량에러)차량 배터리 저전압";
                        //fInfo.alarmCode = 0x3103;
                        break;
                    case 4:
                        fInfo.errorMsg = "(차데모차량에러)충전전류 편차 오류";
                        //fInfo.alarmCode = 0x3104;
                        break;
                    case 5:
                        fInfo.errorMsg = "(차데모차량에러)춛전전압 범위/편차 오류";
                        //fInfo.alarmCode = 0x3105;
                        break;
                    case 6:
                        fInfo.errorMsg = "(차데모차량에러)차량 배터리 온도문제 발생";
                        //fInfo.alarmCode = 0x3106;
                        break;
                    case 7:
                        fInfo.errorMsg = "(차데모차량에러)차량 기어상태 확인(P)";
                        //fInfo.alarmCode = 0x3107;
                        break;
                    case 8:
                        fInfo.errorMsg = "(차데모에러)BMS 전압상태 오류";
                        //fInfo.alarmCode = 0x3108;
                        break;
                    case 9:
                        fInfo.errorMsg = "(차데모차량에러)차량상태/BMS 오류";
                        //fInfo.alarmCode = 0x3109;
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved for CHADEMO";
                        //fInfo.alarmCode = 0x3110;
                        break;
                    case 11:
                        fInfo.errorMsg = "(차데모에러)차량-컨트롤보드 통신 오류";
                        //fInfo.alarmCode = 0x3111;
                        break;
                    case 12:
                        fInfo.errorMsg = "(차데모에러)프리차지 오류";
                        //fInfo.alarmCode = 0x3112;
                        break;
                    case 13:
                        fInfo.errorMsg = "(차데모에러)과전압";
                        //fInfo.alarmCode = 0x3113;
                        break;
                    case 14:
                        fInfo.errorMsg = "(차데모에러)과전류";
                        //fInfo.alarmCode = 0x3114;
                        break;
                    case 15:
                        fInfo.errorMsg = "(차데모에러)출력 누설전류 발생";
                        //fInfo.alarmCode = 0x3115;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault431 = fault431;
    }

    public void scanFault432(Vector<FaultInfo> faultList, int fault432) {
        int diffEvent = preFault432 ^ fault432;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault432, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 432*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 432 에러 정의
                switch ( i ) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        fInfo.errorMsg = "Reserved for sequence error_CHADEMO";
                        //fInfo.alarmCode = 0x3215;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault432 = fault432;
    }

    public void scanFault433(Vector<FaultInfo> faultList, int fault433) {
        int diffEvent = preFault432 ^ fault433;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault433, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 433*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 433 에러 정의
                switch ( i ) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        fInfo.errorMsg = "Reserved for sequence error_CHADEMO";
                        //fInfo.alarmCode = 0x3315;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault433 = fault433;
    }

    public void scanFault434(Vector<FaultInfo> faultList, int fault434) {
        int diffEvent = preFault434 ^ fault434;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault434, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 434*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 434 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "모듈#1 오류(Warnning)";
                        //fInfo.alarmCode = 0x3400;
                        break;
                    case 1:
                        fInfo.errorMsg = "모듈#2 오류(Warnning)";
                        //fInfo.alarmCode = 0x3401;
                        break;
                    case 2:
                        fInfo.errorMsg = "모듈#3 오류(Warnning)";
                        //fInfo.alarmCode = 0x3402;
                        break;
                    case 3:
                        fInfo.errorMsg = "모듈#4 오류(Warnning)";
                        //fInfo.alarmCode = 0x3403;
                        break;
                    case 4:
                        fInfo.errorMsg = "모듈#5 오류(Warnning)";
                        //fInfo.alarmCode = 0x3404;
                        break;
                    case 5:
                        fInfo.errorMsg = "모듈#6 오류(Warnning)";
                        //fInfo.alarmCode = 0x3405;
                        break;
                    case 6:
                        fInfo.errorMsg = "모듈#7 오류(Warnning)";
                        //fInfo.alarmCode = 0x3406;
                        break;
                    case 7:
                        fInfo.errorMsg = "모듈#8 오류(Warnning)";
                        //fInfo.alarmCode = 0x3407;
                        break;
                    case 8:
                        fInfo.errorMsg = "모듈#9 오류(Warnning)";
                        //fInfo.alarmCode = 0x3408;
                        break;
                    case 9:
                        fInfo.errorMsg = "모듈#10 오류(Warnning)";
                        //fInfo.alarmCode = 0x3409;
                        break;
                    case 10:
                        fInfo.errorMsg = "모듈#11 오류(Warnning)";
                        //fInfo.alarmCode = 0x3410;
                        break;
                    case 11:
                        fInfo.errorMsg = "모듈#12 오류(Warnning)";
                        //fInfo.alarmCode = 0x3411;
                        break;
                    case 12:
                        fInfo.errorMsg = "모듈#13 오류(Warnning)";
                        //fInfo.alarmCode = 0x3412;
                        break;
                    case 13:
                        fInfo.errorMsg = "모듈#14 오류(Warnning)";
                        //fInfo.alarmCode = 0x3413;
                        break;
                    case 14:
                        fInfo.errorMsg = "모듈#15 오류(Warnning)";
                        //fInfo.alarmCode = 0x3414;
                        break;
                    case 15:
                        fInfo.errorMsg = "모듈#16 오류(Warnning)";
                        //fInfo.alarmCode = 0x3415;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault434 = fault434;
    }

    public void scanFault435(Vector<FaultInfo> faultList, int fault435) {
        int diffEvent = preFault435 ^ fault435;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault435, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 435*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 435 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "모듈#17 오류(Warnning)";
                        //fInfo.alarmCode = 0x3500;
                        break;
                    case 1:
                        fInfo.errorMsg = "모듈#18 오류(Warnning)";
                        //fInfo.alarmCode = 0x3501;
                        break;
                    case 2:
                        fInfo.errorMsg = "모듈#19 오류(Warnning)";
                        //fInfo.alarmCode = 0x3502;
                        break;
                    case 3:
                        fInfo.errorMsg = "모듈#20 오류(Warnning)";
                        //fInfo.alarmCode = 0x3503;
                        break;
                    case 4:
                        fInfo.errorMsg = "모듈#21 오류(Warnning)";
                        //fInfo.alarmCode = 0x3504;
                        break;
                    case 5:
                        fInfo.errorMsg = "모듈#22 오류(Warnning)";
                        //fInfo.alarmCode = 0x3505;
                        break;
                    case 6:
                        fInfo.errorMsg = "모듈#23 오류(Warnning)";
                        //fInfo.alarmCode = 0x3506;
                        break;
                    case 7:
                        fInfo.errorMsg = "모듈#24 오류(Warnning)";
                        //fInfo.alarmCode = 0x3507;
                        break;
                    case 8:
                        fInfo.errorMsg = "모듈#25 오류(Warnning)";
                        //fInfo.alarmCode = 0x3508;
                        break;
                    case 9:
                        fInfo.errorMsg = "모듈#26 오류(Warnning)";
                        //fInfo.alarmCode = 0x3509;
                        break;
                    case 10:
                        fInfo.errorMsg = "모듈#27 오류(Warnning)";
                        //fInfo.alarmCode = 0x3510;
                        break;
                    case 11:
                        fInfo.errorMsg = "모듈#28 오류(Warnning)";
                        //fInfo.alarmCode = 0x3511;
                        break;
                    case 12:
                        fInfo.errorMsg = "모듈#29 오류(Warnning)";
                        //fInfo.alarmCode = 0x3512;
                        break;
                    case 13:
                        fInfo.errorMsg = "모듈#30 오류(Warnning)";
                        //fInfo.alarmCode = 0x3513;
                        break;
                    case 14:
                        fInfo.errorMsg = "모듈#31 오류(Warnning)";
                        //fInfo.alarmCode = 0x3514;
                        break;
                    case 15:
                        fInfo.errorMsg = "모듈#32 오류(Warnning)";
                        //fInfo.alarmCode = 0x3515;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault435 = fault435;
    }

    public void scanFault436(Vector<FaultInfo> faultList, int fault436) {
        int diffEvent = preFault436 ^ fault436;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault436, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 436*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 436 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "모듈#33 오류(Warnning)";
                        //fInfo.alarmCode = 0x3600;
                        break;
                    case 1:
                        fInfo.errorMsg = "모듈#34 오류(Warnning)";
                        //fInfo.alarmCode = 0x3601;
                        break;
                    case 2:
                        fInfo.errorMsg = "모듈#35 오류(Warnning)";
                        //fInfo.alarmCode = 0x3602;
                        break;
                    case 3:
                        fInfo.errorMsg = "모듈#36 오류(Warnning)";
                        //fInfo.alarmCode = 0x3603;
                        break;
                    case 4:
                        fInfo.errorMsg = "모듈#37 오류(Warnning)";
                        //fInfo.alarmCode = 0x3604;
                        break;
                    case 5:
                        fInfo.errorMsg = "모듈#38 오류(Warnning)";
                        //fInfo.alarmCode = 0x3605;
                        break;
                    case 6:
                        fInfo.errorMsg = "모듈#39 오류(Warnning)";
                        //fInfo.alarmCode = 0x3606;
                        break;
                    case 7:
                        fInfo.errorMsg = "모듈#40 오류(Warnning)";
                        //fInfo.alarmCode = 0x3607;
                        break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        fInfo.errorMsg = "Reserved";
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault436 = fault436;
    }

    public void scanFault437(Vector<FaultInfo> faultList, int fault437) {
        int diffEvent = preFault437 ^ fault437;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault437, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 437*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 437 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "PowerUnit Relay #1 Error";
                        //fInfo.alarmCode = 0x3700;
                        break;
                    case 1:
                        fInfo.errorMsg = "PowerUnit Relay #2 Error";
                        //fInfo.alarmCode = 0x3701;
                        break;
                    case 2:
                        fInfo.errorMsg = "PowerUnit Relay #3 Error";
                        //fInfo.alarmCode = 0x3702;
                        break;
                    case 3:
                        fInfo.errorMsg = "PowerUnit Relay #4 Error";
                        //fInfo.alarmCode = 0x3703;
                        break;
                    case 4:
                        fInfo.errorMsg = "PowerUnit Relay #5 Error";
                        //fInfo.alarmCode = 0x3704;
                        break;
                    case 5:
                        fInfo.errorMsg = "PowerUnit Relay #6 Error";
                        //fInfo.alarmCode = 0x3705;
                        break;
                    case 6:
                        fInfo.errorMsg = "PowerUnit Relay #7 Error";
                        //fInfo.alarmCode = 0x3706;
                        break;
                    case 7:
                        fInfo.errorMsg = "PowerUnit Relay #8 Error";
                        //fInfo.alarmCode = 0x3707;
                        break;
                    case 8:
                        fInfo.errorMsg = "PowerUnit Relay #9 Error";
                        //fInfo.alarmCode = 0x3708;
                        break;
                    case 9:
                        fInfo.errorMsg = "PowerUnit Relay #10 Error";
                        //fInfo.alarmCode = 0x3709;
                        break;
                    case 10:
                        fInfo.errorMsg = "PowerUnit Relay #11 Error";
                        //fInfo.alarmCode = 0x3710;
                        break;
                    case 11:
                        fInfo.errorMsg = "PowerUnit Relay #12 Error";
                        //fInfo.alarmCode = 0x3711;
                        break;
                    case 12:
                        fInfo.errorMsg = "PowerUnit Relay #13 Error";
                        //fInfo.alarmCode = 0x3712;
                        break;
                    case 13:
                        fInfo.errorMsg = "PowerUnit Relay #14 Error";
                        //fInfo.alarmCode = 0x3713;
                        break;
                    case 14:
                        fInfo.errorMsg = "PowerUnit Relay #15 Error";
                        //fInfo.alarmCode = 0x3714;
                        break;
                    case 15:
                        fInfo.errorMsg = "PowerUnit Relay #16 Error";
                        //fInfo.alarmCode = 0x3715;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault437 = fault437;
    }

    public void scanFault438(Vector<FaultInfo> faultList, int fault438) {
        int diffEvent = preFault438 ^ fault438;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault438, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 438*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 438 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = "PowerUnit Relay #17 Error";
                        //fInfo.alarmCode = 0x3800;
                        break;
                    case 1:
                        fInfo.errorMsg = "PowerUnit Relay #18 Error";
                        //fInfo.alarmCode = 0x3801;
                        break;
                    case 2:
                        fInfo.errorMsg = "PowerUnit Relay #19 Error";
                        //fInfo.alarmCode = 0x3802;
                        break;
                    case 3:
                        fInfo.errorMsg = "PowerUnit Relay #20 Error";
                        //fInfo.alarmCode = 0x3803;
                        break;
                    case 4:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 5:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 6:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 7:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 8:
                        fInfo.errorMsg = "PowerUnit PM1 Error";
                        //fInfo.alarmCode = 0x3808;
                        break;
                    case 9:
                        fInfo.errorMsg = "PowerUnit PM2 Error";
                        //fInfo.alarmCode = 0x3809;
                        break;
                    case 10:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 11:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 12:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 13:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 14:
                        fInfo.errorMsg = "Reserved for PowerUnit";
                        break;
                    case 15:
                        fInfo.errorMsg = "PowerUnit 입력저전압";
                        //fInfo.alarmCode = 0x3815;
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault438 = fault438;
    }

    public void scanFault439(Vector<FaultInfo> faultList, int fault439) {
        int diffEvent = preFault439 ^ fault439;
        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault439, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 439*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 439 에러 정의
                switch ( i ) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        fInfo.errorMsg = "Reserved for Distrubution";
                        break;
                }
                faultList.add(fInfo);
            }
        }
        preFault439 = fault439;
    }
}
