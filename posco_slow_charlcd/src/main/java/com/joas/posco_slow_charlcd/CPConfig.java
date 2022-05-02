/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd;

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
    public static final String CP_CONFIG_TL3500BS_USE = "TL3500BS";
    public static final String CP_CONFIG_SEHANRF_USE = "SEHANRF";
    public static final String CP_CONFIG_ACMRF_USE = "ACMRF";
    public static final String CP_CONFIG_KAKAONAVI_USE = "KAKAONAVI";
    public static final String CP_CONFIG_KAKAONAVI_COST_USE = "KakaoNaviCost";
    public static final String CP_CONFIG_CHARGERKIND = "ChargerKind";
    public static final String CP_CONFIG_KAKAO_QR_COST = "KakaoQrCost";
    public static final String CP_CONFIG_KAKAO_CREDIT_COST = "KakaoCreditCost";

    //ocpp관련(v1.6) - add by si. 220111
    public static final String CP_CONFIG_OCPP_USE = "UseOcpp";
    public static final String CP_CONFIG_OCPP_USE_SSL = "UseSSL";
    public static final String CP_CONFIG_OCPP_USE_BASICAUTH = "UseBasicAuth";
    public static final String CP_CONFIG_OCPP_CP_ID = "OcppChagerID";
    public static final String CP_CONFIG_OCPP_SERVER_URI = "OcppServerURI";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_ID = "HttpBasicAuthID";
    public static final String CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD = "HttpBasicAuthPassword";
    public static final String CP_CONFIG_OOCPP_CHARGER_SERIALNUM = "OcppChargerSerialNum";

    public static final String CP_CONFIG_SLOW_CHARGERTYPE = "slowChargerType";


    public String stationID = "00000000";
    public String chargerID = "00";
    public String serverIP = "192.168.0.48";
    public int serverPort = 9000;

    public String settingPassword = "1234";
    public boolean isAuthSkip = false;
    public boolean useWatchDogTimer = false;
    public boolean useTL3500BS = false;
    public boolean useSehanRF = false;
    public boolean useACMRF = true;
    public boolean useKakaoNavi = false;
    public boolean useKakaoCost = false;
    public String chargerKind = "CL";
    public int  kakaoQRCost = 250;
    public int kakaoCreditCost = 420;

    //완속 충전기타입 선택기능 추가 - 220418 - 이전모델들은 해당값에대해 영향을 받지않음
    public int slowChargerType = 0;

    public String ocpp_chargerID = "0000";
    public String ocpp_serverURI = "ws://192.168.0.48:9000/ocpp";
    public String httpBasicAuthID = "joas";
    public String httpBasicAuthPassword = "joas";
    public String ocpp_chargePointSerialNumber = "";
    public boolean useOcpp = false;
    public boolean useSSL = false;
    public boolean useBasicAuth = false;

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
                useTL3500BS = obj.getBoolean(CP_CONFIG_TL3500BS_USE);
                useSehanRF = obj.getBoolean(CP_CONFIG_SEHANRF_USE);
//                useSehanRF = true;
                useACMRF = obj.getBoolean(CP_CONFIG_ACMRF_USE);
//                useACMRF = false;
                useKakaoNavi = obj.getBoolean(CP_CONFIG_KAKAONAVI_USE);
                useKakaoCost = obj.getBoolean(CP_CONFIG_KAKAONAVI_COST_USE);
                chargerKind = obj.getString(CP_CONFIG_CHARGERKIND);
//                chargerKind = "CL";
                kakaoQRCost = obj.getInt(CP_CONFIG_KAKAO_QR_COST);
                kakaoCreditCost = obj.getInt(CP_CONFIG_KAKAO_CREDIT_COST);
                slowChargerType = obj.getInt(CP_CONFIG_SLOW_CHARGERTYPE);

//                //ocpp관련(test)
//                useOcpp = true;
//                ocpp_chargerID = "JOAS01";
//                ocpp_serverURI = "ws://ocpp-ws.rnd.starlabs.co.kr:80";
//                httpBasicAuthID =  "joas";
//                httpBasicAuthPassword =  "joas";

                //ocpp관련
                useOcpp = obj.getBoolean(CP_CONFIG_OCPP_USE);
                useSSL = obj.getBoolean(CP_CONFIG_OCPP_USE_SSL);
                useBasicAuth = obj.getBoolean(CP_CONFIG_OCPP_USE_BASICAUTH);
                ocpp_chargerID = obj.getString(CP_CONFIG_OCPP_CP_ID);
                ocpp_serverURI = obj.getString(CP_CONFIG_OCPP_SERVER_URI);
                httpBasicAuthID =  obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_ID);
                httpBasicAuthPassword =  obj.getString(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD);
                ocpp_chargePointSerialNumber = obj.getString(CP_CONFIG_OOCPP_CHARGER_SERIALNUM);

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
            obj.put(CP_CONFIG_TL3500BS_USE, useTL3500BS);
            obj.put(CP_CONFIG_SEHANRF_USE, useSehanRF);
            obj.put(CP_CONFIG_ACMRF_USE, useACMRF);
            obj.put(CP_CONFIG_KAKAONAVI_USE, useKakaoNavi);
            obj.put(CP_CONFIG_KAKAONAVI_COST_USE, useKakaoCost);
            obj.put(CP_CONFIG_CHARGERKIND, chargerKind);
            obj.put(CP_CONFIG_KAKAO_QR_COST, kakaoQRCost);
            obj.put(CP_CONFIG_KAKAO_CREDIT_COST, kakaoCreditCost);
            obj.put(CP_CONFIG_SLOW_CHARGERTYPE, slowChargerType);

            //ocpp관련
            obj.put(CP_CONFIG_OCPP_USE, useOcpp);
            obj.put(CP_CONFIG_OCPP_USE_SSL,useSSL);
            obj.put(CP_CONFIG_OCPP_USE_BASICAUTH,useBasicAuth);
            obj.put(CP_CONFIG_OCPP_CP_ID, ocpp_chargerID);
            obj.put(CP_CONFIG_OCPP_SERVER_URI, ocpp_serverURI);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_ID, httpBasicAuthID);
            obj.put(CP_CONFIG_HTTP_BASIC_AUTH_PASSWORD, httpBasicAuthPassword);
            obj.put(CP_CONFIG_OOCPP_CHARGER_SERIALNUM,ocpp_chargePointSerialNumber);

        } catch (Exception ex) {
            LogWrapper.e(TAG, "Json Make Err:" + ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
