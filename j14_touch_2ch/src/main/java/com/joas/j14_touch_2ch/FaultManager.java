/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch;

import android.content.Context;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPRxData;
import com.joas.utils.BitUtil;

import java.util.Vector;

/**
 * DSP/EIM 에서 오는 Fault 정보를 처리
 */

public class FaultManager {
    DSPControl dspControl;
    Context context;
    int channel = 0;

    int preFault406 = 0;
    int preFault407 = 0;
    int preFault408 = 0;
    int preFault409 = 0;
    int preFault416 = 0;

    public FaultManager(DSPControl control, Context ctx, int ch) {
        dspControl = control;
        context = ctx;
        channel = ch;
    }

    public synchronized Vector<FaultInfo> sacnFault(int channel) {

        Vector<FaultInfo> faultList = new Vector<FaultInfo>();
        DSPRxData dspRxData = dspControl.getDspRxData(channel);

        if ( dspRxData == null ) return faultList;

        //TODO
        //완속 연속 에러 처리?? preFault406이 연속으로 5번이상 다를때 scan하고 처리.

        if ( preFault406 != dspRxData.fault406 ) {
            scanFault406(faultList, dspRxData.fault406);
        }

        if ( preFault407 != dspRxData.fault407 ) {
            scanFault407(faultList, dspRxData.fault407);
        }

        if ( preFault408 != dspRxData.fault408 ) {
            scanFault408(faultList, dspRxData.fault408);
        }

        if ( preFault409 != dspRxData.fault409 ) {
            scanFault409(faultList, dspRxData.fault409);
        }

        if ( preFault416 != dspRxData.fault416 ) {
            scanFault416(faultList, dspRxData.fault416);
        }

        return faultList;
    }

    public boolean isFaultEmergency(int channel) {
        DSPRxData dspRxData = dspControl.getDspRxData(channel);
        if ( dspRxData == null ) return false;
        return BitUtil.getBitBoolean(dspRxData.fault406, 4);
    }

    public void scanFault406(Vector<FaultInfo> faultList, int fault406) {
        int diffEvent = preFault406 ^ fault406;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault406, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 406*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 406 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_inp_stack_overtemp);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_diaode_stack_overtemp);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_out_stack_overtemp);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_trans_overtemp);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_emergency_button);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_coupler_lock_err);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_pilot_signal_err);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_out_fuse_err);
                        break;
                    case 8:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_mc_err);
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_line_insulation_err);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_plc_comm_err);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_dclink_overvoltage);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_dclink_lowvoltage);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_inp_a_overcurrent);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_inp_b_overcurrent);
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_406_charging_system_err);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault406 = fault406;
    }

    public void scanFault407(Vector<FaultInfo> faultList, int fault407) {
        int diffEvent = preFault407 ^ fault407;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault407, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 407*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 407 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_car_batt_overvoltage);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_car_batt_lowvoltage);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_ct1_overcurrent);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_ct2_overcurrent);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_ct3_overcurrent);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_ct4_overcurrent);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_ct5_overcurrent);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_u_err);
                        break;
                    case 8:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_v_err);
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_w_err);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_p_err);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_n_err);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_gate_driver_err);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_board_inp_voltage_15vd_err);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_407_board_inp_voltage_15v_err);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault407 = fault407;
    }

    public void scanFault408(Vector<FaultInfo> faultList, int fault408) {
        int diffEvent = preFault408 ^ fault408;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault408, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 408*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 408 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_board_inp_voltage_24v_err);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_board_inp_voltage_n15v_err);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_out_overvoltage_combo);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_inp_freq_error);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_charging_current_gap_error);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_charging_voltage_gap_error);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_charging_system_compatibility_err);
                        break;
                    case 8:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_inp_lowvoltage_vab);
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_inp_lowvoltage_vbc);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_vd_error);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_plus_vq_error);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_minus_vq_error);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_car_battery_temp_error);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_overvoltage);
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_408_out_overvoltage_chademo);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault408 = fault408;
    }

    public void scanFault409(Vector<FaultInfo> faultList, int fault409) {
        int diffEvent = preFault409 ^ fault409;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault409, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 409*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                // 408 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_chademo_relay_fusion);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_combo_relay_fusion);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_ui_dsp_comm_err);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_charger_car_comm_err);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_control_board_err);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_inp_overvoltage_vbc);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_module_err_1);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_overcurrent);
                        break;
                    case 8:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_precharge_1);
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_precharge_2);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_precharging_err);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_module_err_2);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_combo_connector_overtemp);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_combo_connector_lock);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_module_err_3);
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_409_module_err_4);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault409 = fault409;
    }


    public void scanFault416(Vector<FaultInfo> faultList, int fault416) {
        int diffEvent = preFault416 ^ fault416;

        for ( int i=0; i<16; i++) {
            // 변화가 있다면
            if (BitUtil.getBitBoolean(diffEvent, i) == true) {
                boolean value = BitUtil.getBitBoolean(fault416, i);
                FaultInfo fInfo = new FaultInfo();
                fInfo.id = 416*100 + i;
                fInfo.isRepair = !value;
                fInfo.errorCode = fInfo.id;

                    // 408 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_emergency_bt);
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_car_battery_system_err);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_rcd_off);
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_temperature_err);
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_out_overcurrent);
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_main_breaker);
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_mc_fusion);
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_cp_fail);
                        break;
                    case 8:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_car_gear_stat);
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_can_comm_err);
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_meter_comm_err);
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_dsp_eim_comm_err);
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_module_control_comm_err);
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_bms_voltage_err);
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_car_bms_err);
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_416_internal_voltage_test_err);
                        break;
                }
                faultList.add(fInfo);
            }
        }

        preFault416 = fault416;
    }
}
