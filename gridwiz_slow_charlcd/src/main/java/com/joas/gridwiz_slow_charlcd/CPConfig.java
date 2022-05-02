/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd;

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

    public static final String CP_CONFIG_GPS_X = "GPS_X";
    public static final String CP_CONFIG_GPS_Y = "GPS_Y";



    public String stationID = "00000000";
    public String chargerID = "00";
    public String serverIP = "218.149.156.57";
    public int serverPort = 2003;

    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = false;

    // gps 추가
    // by Lee 20200528
    public String gps_X = "";
    public String gps_Y = "";

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
                gps_X = obj.getString(CP_CONFIG_GPS_X);
                gps_Y = obj.getString(CP_CONFIG_GPS_Y);
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
            obj.put(CP_CONFIG_GPS_X, gps_X);
            obj.put(CP_CONFIG_GPS_Y, gps_Y);
        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
