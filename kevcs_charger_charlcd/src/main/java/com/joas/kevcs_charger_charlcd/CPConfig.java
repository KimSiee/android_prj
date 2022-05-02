/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kevcs_charger_charlcd;

import android.content.Context;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;

public class CPConfig {
    public static final String TAG = "CPConfig";

    public static final String CP_CONFIG_FILE_NAME = "CPConfig.txt";
    public static final String CP_CONFIG_SERVER_AUTH_KEY = "ServerAuthKey";
    public static final String CP_CONFIG_CP_ID = "ChagerID";
    public static final String CP_CONFIG_SERVER_IP = "ServerIP";
    public static final String CP_CONFIG_SERVER_PORT = "ServerPort";
    public static final String CP_CONFIG_SETTING_PASSWORD = "SettingPassword";
    public static final String CP_CONFIG_AUTH_SKIP = "AuthSkip";
    public static final String CP_CONFIG_WATCHDOG_TIMER_USE = "WatchDogTimer";
    public static final String CP_CONFIG_IS_FAST_CHARGER = "IsFastCharger";
    public static final String CP_CONFIG_USE_PAY_TERMINAL = "UsePayTerminal";
    public static final String CP_CONFIG_USE_SOUND = "UseSound";
    public static final String CP_CONFIG_SOUND_VOL = "SoundVol";


    public String serverAuthKey = "00000000";
    public String chargerID = "00";
    public String serverIP = "192.168.0.1";
    public int serverPort = 8484;
    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = true;
    public boolean isFastCharger = false;
    public boolean usePayTerminal = false;
    public boolean useSound = false;
    public int soundVol = 10;

    public CPConfig() {

    }

    public void loadConfig(Context context) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( Environment.getExternalStorageDirectory()+ TypeDefine.CP_CONFIG_PATH+"/"+CP_CONFIG_FILE_NAME);
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
                chargerID = obj.getString(CP_CONFIG_CP_ID);
                serverAuthKey = obj.getString(CP_CONFIG_SERVER_AUTH_KEY);
                serverIP = obj.getString(CP_CONFIG_SERVER_IP);
                serverPort = obj.getInt(CP_CONFIG_SERVER_PORT);
                isAuthSkip = obj.getBoolean(CP_CONFIG_AUTH_SKIP);
                settingPassword = obj.getString(CP_CONFIG_SETTING_PASSWORD);
                useWatchDogTimer = obj.getBoolean(CP_CONFIG_WATCHDOG_TIMER_USE);
                isFastCharger = obj.getBoolean(CP_CONFIG_IS_FAST_CHARGER);
                usePayTerminal = obj.getBoolean(CP_CONFIG_USE_PAY_TERMINAL);
                useSound = obj.getBoolean(CP_CONFIG_USE_SOUND);
                soundVol = obj.getInt(CP_CONFIG_SOUND_VOL);
                if ( soundVol > 10 ) soundVol = 10;
            } catch (Exception ex) {
                LogWrapper.e(TAG , "Json Parse Err:"+ex.toString());
                saveConfig(context);
            }
        }
    }

    public void saveConfig(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(CP_CONFIG_CP_ID, chargerID);
            obj.put(CP_CONFIG_SERVER_AUTH_KEY, serverAuthKey);
            obj.put(CP_CONFIG_SERVER_IP, serverIP);
            obj.put(CP_CONFIG_SERVER_PORT, serverPort);
            obj.put(CP_CONFIG_AUTH_SKIP, isAuthSkip);
            obj.put(CP_CONFIG_SETTING_PASSWORD, settingPassword);
            obj.put(CP_CONFIG_WATCHDOG_TIMER_USE, useWatchDogTimer);
            obj.put(CP_CONFIG_IS_FAST_CHARGER, isFastCharger);
            obj.put(CP_CONFIG_USE_PAY_TERMINAL, usePayTerminal);
            obj.put(CP_CONFIG_USE_SOUND, useSound);
            obj.put(CP_CONFIG_SOUND_VOL, soundVol);
        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
