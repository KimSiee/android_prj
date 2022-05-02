/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:22
 *
 */

package com.joas.ocppui_namboo;

import android.content.Context;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;
public class CPConfig {
    public static final String TAG = "CPConfig";

    public static final String CP_CONFIG_FILE_NAME = "CPConfig.txt";

    public static final String CP_CONFIG_CP_ID = "ChagerID";
    public static final String CP_CONFIG_SERVER_URI = "ServerURI";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_ID = "HttpBasicAuthID";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD = "HttpBasicAuthPassword";
    public static final String CP_CONFIG_USE_HTTP_BASIC_AUTH = "UseHttpBasicAuth";
    public static final String CP_CONFIG_SETTING_PASSWORD = "SettingPassword";
    public static final String CP_CONFIG_AUTH_SKIP = "AuthSkip";
    public static final String CP_CONFIG_WATCHDOG_TIMER_USE = "WatchDogTimer";
    public static final String CP_CONFIG_IS_FAST_CHARGER = "IsFastCharger";
    public static final String CP_CONFIG_USE_TRUST_CA = "UseTrustCA";
    public static final String CP_CONFIG_IS_AVAILABLE = "IsAvailable";
    public static final String CP_CONFIG_MODE_CONNECTORID = "ConnectorId";


    public String chargerID = "0000";
    public String serverURI = "ws://192.168.0.48:9000/ocpp";
    public String httpBasicAuthID = "joas";
    public String httpBasicAuthPassword = "joas";
    public boolean useHttpBasicAuth = false;

    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = false;
    public boolean isFastCharger = false;
    public boolean useTrustCA = false;
    public boolean isAvailable = false;
    public String opModeConnectorID = "0";

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

                chargerID = obj.getString(CP_CONFIG_CP_ID);
                serverURI = obj.getString(CP_CONFIG_SERVER_URI);
                httpBasicAuthID =  obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_ID);
                httpBasicAuthPassword =  obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD);
                useHttpBasicAuth = obj.getBoolean(CP_CONFIG_USE_HTTP_BASIC_AUTH);
                isAuthSkip = obj.getBoolean(CP_CONFIG_AUTH_SKIP);
                settingPassword = obj.getString(CP_CONFIG_SETTING_PASSWORD);
                useWatchDogTimer = obj.getBoolean(CP_CONFIG_WATCHDOG_TIMER_USE);
                isFastCharger = obj.getBoolean(CP_CONFIG_IS_FAST_CHARGER);
                useTrustCA = obj.getBoolean(CP_CONFIG_USE_TRUST_CA);
                isAvailable = obj.getBoolean(CP_CONFIG_IS_AVAILABLE);
                opModeConnectorID = obj.getString(CP_CONFIG_MODE_CONNECTORID);
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
            obj.put(CP_CONFIG_SERVER_URI, serverURI);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_ID, httpBasicAuthID);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD, httpBasicAuthPassword);
            obj.put(CP_CONFIG_USE_HTTP_BASIC_AUTH, useHttpBasicAuth);
            obj.put(CP_CONFIG_AUTH_SKIP, isAuthSkip);
            obj.put(CP_CONFIG_SETTING_PASSWORD, settingPassword);
            obj.put(CP_CONFIG_WATCHDOG_TIMER_USE, useWatchDogTimer);
            obj.put(CP_CONFIG_IS_FAST_CHARGER, isFastCharger);
            obj.put(CP_CONFIG_USE_TRUST_CA, useTrustCA);
            obj.put(CP_CONFIG_IS_AVAILABLE,isAvailable);
            obj.put(CP_CONFIG_MODE_CONNECTORID, opModeConnectorID);
        } catch (Exception ex) {
            LogWrapper.e(TAG , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
