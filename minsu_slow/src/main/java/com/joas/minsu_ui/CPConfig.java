/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui;

import android.content.Context;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;
public class CPConfig {
    public static final String TAG = "CPConfig";

    public static final String CP_CONFIG_FILE_NAME = "CPConfig.txt";

    public static final String CP_CONFIG_STATION_ID = "StationID";
    public static final String CP_CONFIG_CP_ID = "ChagerID";
    public static final String CP_CONFIG_SERVER_IP = "ServerIP";
    public static final String CP_CONFIG_SERVER_PORT = "ServerPort";
    public static final String CP_CONFIG_SETTING_PASSWORD = "SettingPassword";
    public static final String CP_CONFIG_AUTH_SKIP = "AuthSkip";
    public static final String CP_CONFIG_WATCHDOG_TIMER_USE = "WatchDogTimer";
    public static final String CP_CONFIG_IS_FAST_CHARGER = "IsFastCharger";
    public static final String CP_CONFIG_IS_TEST_MODE = "IsTestMode";
    public static final String CP_CONFIG_COST_UNIT_INFO = "CostUnit";


    public String stationID = "00000000";
    public String chargerID = "00";
    public String serverIP = "192.168.0.48";
    public int serverPort = 9000;

    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = false;
    public boolean isFastCharger = true;
    public boolean isTestMode = false;
    public int settingUnitCost = TypeDefine.DEFAULT_UNIT_COST;

    public CPConfig() {

    }

    public void loadConfig(Context context) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+CP_CONFIG_FILE_NAME);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null ) {
            //Save Default Config
            saveConfig(context);
        }
        else {
            try {
                JSONObject obj = new JSONObject(loadString);

                stationID = obj.getString(CP_CONFIG_STATION_ID);
                chargerID = obj.getString(CP_CONFIG_CP_ID);
                serverIP = obj.getString(CP_CONFIG_SERVER_IP);
                serverPort = obj.getInt(CP_CONFIG_SERVER_PORT);
                isAuthSkip = obj.getBoolean(CP_CONFIG_AUTH_SKIP);
                settingPassword = obj.getString(CP_CONFIG_SETTING_PASSWORD);
                useWatchDogTimer = obj.getBoolean(CP_CONFIG_WATCHDOG_TIMER_USE);
                isFastCharger = obj.getBoolean(CP_CONFIG_IS_FAST_CHARGER);
                isTestMode = obj.getBoolean(CP_CONFIG_IS_TEST_MODE);
                settingUnitCost = obj.getInt(CP_CONFIG_COST_UNIT_INFO);
            } catch (Exception ex) {
                LogWrapper.e(TAG , "Json Parse Err:"+ex.toString());
                saveConfig(context);
            }
        }
    }

    public void saveConfig(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(CP_CONFIG_STATION_ID, stationID);
            obj.put(CP_CONFIG_CP_ID, chargerID);
            obj.put(CP_CONFIG_SERVER_IP, serverIP);
            obj.put(CP_CONFIG_SERVER_PORT, serverPort);
            obj.put(CP_CONFIG_AUTH_SKIP, isAuthSkip);
            obj.put(CP_CONFIG_SETTING_PASSWORD, settingPassword);
            obj.put(CP_CONFIG_WATCHDOG_TIMER_USE, useWatchDogTimer);
            obj.put(CP_CONFIG_IS_FAST_CHARGER, isFastCharger);
            obj.put(CP_CONFIG_IS_TEST_MODE,isTestMode);
            obj.put(CP_CONFIG_COST_UNIT_INFO, settingUnitCost);
        } catch (Exception ex) {
            LogWrapper.e(TAG, "Json Make Err:" + ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
