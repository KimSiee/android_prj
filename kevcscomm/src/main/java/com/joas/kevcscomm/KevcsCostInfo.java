/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 8. 17 오후 4:59
 *
 */

package com.joas.kevcscomm;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by user on 2018-08-17.
 */

public class KevcsCostInfo {
    public static final int KEVCS_COST_TIME_INFO_CNT = 48;
    public static final int KEVCS_LOAD_CL_MIN = 1; // 경부하
    public static final int KEVCS_LOAD_CL_MID = 2; // 중부하
    public static final int KEVCS_LOAD_CL_MAX = 3; // 최대부하

    public String version = "";       //단가버젼
    //public string last_membdate;

    public double[] KWHBILL_UCOST = new double[KEVCS_COST_TIME_INFO_CNT];
    public double[] INFRABILL_UCOST = new double[KEVCS_COST_TIME_INFO_CNT];
    public double[] SVC_BILL_UCOST = new double[KEVCS_COST_TIME_INFO_CNT];
    public int[] LOAD_CL = new int[KEVCS_COST_TIME_INFO_CNT];

    public KevcsCostInfo() {
        for (int i = 0; i < KEVCS_COST_TIME_INFO_CNT; i++) {
            KWHBILL_UCOST[i] = 173.8; // 혹시 잘못된 경우 0원으로 계산되지 않기 위함
            INFRABILL_UCOST[i] = 0.0;
            SVC_BILL_UCOST[i] = 0.0;
            LOAD_CL[i] = KEVCS_LOAD_CL_MID;
        }
    }



    public JSONObject getJsonObject() {
        JSONObject json = new JSONObject();

        JSONArray array_kwhbill = new JSONArray();
        JSONArray array_infrabill = new JSONArray();
        JSONArray array_svcbill = new JSONArray();
        JSONArray array_loadcl = new JSONArray();

        try {
            json.put("version", version);

            for (int i = 0; i < KEVCS_COST_TIME_INFO_CNT; i++) {
                array_kwhbill.put(KWHBILL_UCOST[i]);
                array_infrabill.put(INFRABILL_UCOST[i]);
                array_svcbill.put(SVC_BILL_UCOST[i]);
                array_loadcl.put(LOAD_CL[i]);
            }

            json.put("kwhbill", array_kwhbill);
            json.put("infrabill", array_infrabill);
            json.put("svcbill", array_svcbill);
            json.put("loadcl", array_loadcl);

        } catch(Exception e) {

        }

        return json;
    }

    public void parseJsonObject(JSONObject json) {
        try {
            version = json.getString("version");

            JSONArray array_kwhbill = json.getJSONArray("kwhbill");
            JSONArray array_infrabill = json.getJSONArray("infrabill");
            JSONArray array_svcbill = json.getJSONArray("svcbill");
            JSONArray array_loadcl = json.getJSONArray("loadcl");

            for (int i = 0; i < KEVCS_COST_TIME_INFO_CNT; i++) {
                KWHBILL_UCOST[i] = array_kwhbill.getDouble(i);
                INFRABILL_UCOST[i] = array_infrabill.getDouble(i);
                SVC_BILL_UCOST[i] = array_svcbill.getDouble(i);
                LOAD_CL[i] = array_loadcl.getInt(i);
            }
        } catch(Exception e) {
            return;
        }
    }
}
