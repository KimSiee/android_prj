/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 1 오후 5:43
 *
 */

package com.joas.kepcocomm;

import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeUtil;

import org.json.JSONObject;

public class KepcoChargeCtl {
    public static String TAG = "KepcoChargeCtl";
    public enum State {
        NONE,
        READY,
        STARTED,
        FINISHED
    };

    public State state = State.NONE;

    public String ctrl_id = "";
    public String st_datetime = "";
    public String end_datetime = "";
    public String ctrl_type = "";
    public String ctrl_cd = "";
    public String pwm_rate = "";
    public String card_no = "";
    public String outlet_id = "";


    public boolean isStartCondition(boolean isCurChargingState) {

        // 즉시 제어인 경우
        if ( ctrl_type.equals("1") ) return true;
        else if ( isCurChargingState ) return false; // 충전완료후 제어인경우

        // 제어 시간 비교
        String curTime = TimeUtil.getCurrentTimeAsString("yyyyMMddHHmmss");
        if ( curTime.compareTo(st_datetime) > 0) return true;

        return false;
    }

    public void saveToFile(String filePath, String filename) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ctrl_id", ctrl_id);
            obj.put("st_datetime", st_datetime);
            obj.put("end_datetime", end_datetime);
            obj.put("ctrl_type", ctrl_type);
            obj.put("ctrl_cd", ctrl_cd);
            obj.put("pwm_rate", pwm_rate);
            obj.put("card_no", card_no);
            obj.put("outlet_id", outlet_id);

            FileUtil.stringToFile(filePath, filename, obj.toString(), false);
        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Make Err:"+ex.toString());
        }
    }


    public void loagFromFile(String filePath, String filename) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( filePath+"/"+filename);
        } catch (Exception ex) {
            loadString = null;
        }

        try {
            JSONObject obj = new JSONObject(loadString);
            ctrl_id = obj.getString("ctrl_id");
            st_datetime = obj.getString("st_datetime");
            end_datetime = obj.getString("end_datetime");
            ctrl_type = obj.getString("ctrl_type");
            ctrl_cd = obj.getString("ctrl_cd");
            pwm_rate = obj.getString("pwm_rate");
            card_no = obj.getString("card_no");
            outlet_id = obj.getString("outlet_id");
        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Parse Err:"+ex.toString());
        }
    }
}
