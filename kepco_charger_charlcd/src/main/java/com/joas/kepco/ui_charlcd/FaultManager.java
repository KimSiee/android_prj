/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kepco.ui_charlcd;

import android.content.Context;


import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.kepco.ui.R;
import com.joas.kepcocomm.FaultInfo;
import com.joas.utils.BitUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Vector;

/**
 * DSP/EIM 에서 오는 Fault 정보를 처리
 */

public class FaultManager {
    public static final String TAG = "FaultManager";
    DSPControl2 dspControl;
    Context context;
    int channel = 0;

    int preFault423 = 0;
    int preFault424 = 0;
    int preFault425 = 0;
    boolean isFastCharger = false;

    public FaultManager(DSPControl2 control, Context ctx, int ch, boolean isFast) {
        dspControl = control;
        context = ctx;
        channel = ch;
        this.isFastCharger = isFast;
    }

    public synchronized Vector<FaultInfo> sacnFault(int channel) {

        Vector<FaultInfo> faultList = new Vector<FaultInfo>();
        DSPRxData2 dspRxData = dspControl.getDspRxData2(channel);

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

                // 423 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_0_emergency_bt);
                        fInfo.tp = 90;
                        fInfo.cd = 901;
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_1_mc1_err);
                        fInfo.tp = 20;
                        fInfo.cd = 201;
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_2_mc2_err);
                        fInfo.tp = 20;
                        fInfo.cd = 201;
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_3_mc3_err);
                        fInfo.tp = 20;
                        fInfo.cd = 201;
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_4_relay1_err);
                        fInfo.tp = 20;
                        fInfo.cd = 202;
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_5_relay2_err);
                        fInfo.tp = 20;
                        fInfo.cd = 202;
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_6_relay3_err);
                        fInfo.tp = 20;
                        fInfo.cd = 202;
                        break;
                    case 7:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_7_relay4_err);
                        fInfo.tp = 20;
                        fInfo.cd = 202;
                        break;
                    case 8:
                        fInfo.errorMsg = "Reserved";
                        break;
                    case 9:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_9_inp_overvoltage);
                        fInfo.tp = 10;
                        fInfo.cd = 102;
                        break;
                    case 10:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_10_temperature_err);
                        fInfo.tp = 30;
                        fInfo.cd = 303;
                        break;
                    case 11:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_11_module_control_comm_err);
                        fInfo.tp = 50;
                        fInfo.cd = 47;
                        break;
                    case 12:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_12_crtlboard1_ctrlboard2_comm_err);
                        fInfo.tp = 50;
                        fInfo.cd = 47;
                        break;
                    case 13:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_13_meter_comm_err);
                        fInfo.tp = 90;
                        fInfo.cd = 909;
                        break;
                    case 14:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_14_meter_comm_err);
                        fInfo.tp = 90;
                        fInfo.cd = 909;
                        break;
                    case 15:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_423_15_inp_lowvoltage);
                        fInfo.tp = 10;
                        fInfo.cd = 103;
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

                // 425 에러 정의
                switch ( i ) {
                    case 0:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_0_rcd_off);
                        fInfo.tp = 10;
                        fInfo.cd = 101;
                        break;
                    case 1:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_1_ac_relay_err);
                        break;
                    case 2:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_2_ac_leak);
                        fInfo.tp = 90;
                        fInfo.cd = 909;
                        break;
                    case 3:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_3_ac_rcm_err);
                        fInfo.tp = 90;
                        fInfo.cd = 909;
                        break;
                    case 4:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_4_out_overcurrent);
                        fInfo.tp = 10;
                        fInfo.cd = 107;
                        break;
                    case 5:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_5_ac_fg_err);
                        fInfo.tp = 90;
                        fInfo.cd = 909;
                        break;
                    case 6:
                        fInfo.errorMsg = context.getResources().getString(R.string.faultstr_425_6_ac_cp_error);
                        fInfo.tp = 40;
                        fInfo.cd = 402;
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

    public void saveFaultStatus(String filepath, String filename, Vector<FaultInfo> faultList) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("preFault423", preFault423);
            obj.put("preFault424", preFault424);
            obj.put("preFault425", preFault425);
            JSONArray list = new JSONArray();
            for (FaultInfo info: faultList) {
                JSONObject objInfo = new JSONObject();
                objInfo.put("id", info.id);
                objInfo.put("tp", info.tp);
                objInfo.put("cd", info.cd);
                objInfo.put("id", info.id);
                objInfo.put("occurDate", info.occurDate);
                objInfo.put("errorMsg", info.errorMsg);

                list.put(objInfo);
            }
            obj.put("list", list);

        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(filepath, filename, obj.toString(), false);
    }

    public Vector<FaultInfo> loadFaultStatus(String filepath, String filename) {
        Vector<FaultInfo> faultList = new Vector<>();
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( filepath + "/" + filename);
        } catch (Exception ex) {
            return faultList;
        }

        try {
            JSONObject obj = new JSONObject(loadString);
            preFault423 = obj.getInt("preFault423");
            preFault424 = obj.getInt("preFault424");
            preFault425 = obj.getInt("preFault425");

            JSONArray list = obj.getJSONArray("list");

            for (int i=0; i<list.length(); i++) {
                FaultInfo fInfo = new FaultInfo(
                        list.getJSONObject(i).getString("occurDate"),
                        list.getJSONObject(i).getInt("id"),
                        list.getJSONObject(i).getString("errorMsg"),
                        false,
                        list.getJSONObject(i).getInt("tp"),
                        list.getJSONObject(i).getInt("cd"));
                faultList.add(fInfo);
            }

        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Parse Err:"+ex.toString());
        }
        return faultList;
    }
}
