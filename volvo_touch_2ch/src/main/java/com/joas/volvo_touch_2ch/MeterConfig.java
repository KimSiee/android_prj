/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 26. 오후 5:39
 *
 */

package com.joas.volvo_touch_2ch;

import android.content.Context;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;

public class MeterConfig {
    public static final String TAG = "MeterConfig";

    public static final String METER_CONFIG_FILE_NAME = "MeterViewConfig.txt";

    public static final String METER_CONFIG_VERSION_1 = "Version1";
    public static final String METER_CONFIG_VERSION_2 = "Version2";
    public static final String METER_CONFIG_VERSION_3 = "Version3";
    public static final String METER_CONFIG_KIND = "MeterKind";
    public static final String METER_CONFIG_MAXCH = "MaxChannel";
    public static final String METER_CONFIG_LCDTYPE = "CharLcdType";

    public int version1 = 0;
    public int version2 = 1;
    public int version3 = 4;
    public String meterkind = "DDS353H";
    public int maxChannel = 1;
    public String lcdType = "RW1603";

    public MeterConfig() {
    }

    public void loadConfig(Context context) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile(Environment.getExternalStorageDirectory() + TypeDefine.METERCONFIG_BASE_PATH + "/" + METER_CONFIG_FILE_NAME);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null) {
            //Save Default Config
            saveConfig(context);
        } else {
            try {
                JSONObject obj = new JSONObject(loadString);

                version1 = obj.getInt(METER_CONFIG_VERSION_1);
                version2 = obj.getInt(METER_CONFIG_VERSION_2);
                version3 = obj.getInt(METER_CONFIG_VERSION_3);
                meterkind = obj.getString(METER_CONFIG_KIND);
                maxChannel = obj.getInt(METER_CONFIG_MAXCH);
                lcdType = obj.getString(METER_CONFIG_LCDTYPE);
            } catch (Exception ex) {
                LogWrapper.e(TAG, "Json Parse Err:" + ex.toString());
                saveConfig(context);
            }
        }
    }

    public void saveConfig(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(METER_CONFIG_VERSION_1, version1);
            obj.put(METER_CONFIG_VERSION_2, version2);
            obj.put(METER_CONFIG_VERSION_3, version3);
            obj.put(METER_CONFIG_KIND, meterkind);
            obj.put(METER_CONFIG_MAXCH, maxChannel);
            obj.put(METER_CONFIG_LCDTYPE, lcdType);

        } catch (Exception ex) {
            LogWrapper.e(TAG, "Json Make Err:" + ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory() + TypeDefine.METERCONFIG_BASE_PATH, METER_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
