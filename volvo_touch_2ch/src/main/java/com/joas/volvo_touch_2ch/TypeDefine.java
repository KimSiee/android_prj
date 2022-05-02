/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

public class TypeDefine {
    public enum ConnectorType {
        AC3,
        CHADEMO,
        DCCOMBO,
        BTYPE,
        CTYPE
    }

    public enum AuthType {
        NONE,
        MEMBER,
        NOMEMBER
    }

    public static final String MODEL_NAME = "JC-91B2-14-0O4";
    public static String SW_VER = "";
    public static String SW_RELEASE_DATE = "";

    public static final int CP_TYPE_SLOW_AC     = 0x02;
    public static final int CP_TYPE_FAST_3MODE  = 0x06;

    public static final int CP_AC_POWER_OFFERED = 20*1000; // wh
    public static final int CP_DC_POWER_OFFERED = 50*1000; //wh
    public static final int CP_AC_CURRENT_OFFERED = 50;
    public static final int CP_DC_CURRENT_OFFERED = 100;
    public static final int METERING_CHANGE_TIMEOUT = 3 * 60;     //초(180) 3분

    public static final int MAX_CHANNEL = 2;

    public static final int DEFAULT_CHANNEL = 0;

    public static final int DEFAULT_UNIT_COST = 100;

    public static final int OCPP_DEFAULT_CONNECTOR_ID = 1;

    // Timer 값 정의
    public static final int DEFAULT_AUTH_TIMEOUT = 60; // secZ
    public static final int DEFAULT_CONNECT_CAR_TIMEOUT = 60; // sec
    public static final int DEFAULT_CONNECTOR_WAIT_TIMEOUT = 120; // sec
    public static final int MESSAGEBOX_TIMEOUT_SHORT = 3; // sec

    public static final int FIRMWARE_UPDATE_COUNTER = 30; // 펌웨어 다운로드 후 업데이트 카운트

    // 충전중 상태 정보 주기
    public static final int COMM_CHARGE_STATUS_PERIOD = 300; // 5 min
    public static final int METER_COMM_ERR_TIMEOUT = 60; // 1 min

    // 시간 오차값 허용 범위 정의
    public static final int TIME_SYNC_GAP_MS	= 10*1000; // 10 sec

    //프로그램 시작후 WatchDog 타이머 시작까지 시간
    //public static final int WATCHDOG_START_TIMEOUT	= 5*60; // 5 min
    public static final int WATCHDOG_START_TIMEOUT	= 10; // 5 min

    public static final String REPOSITORY_BASE_PATH = "/SmartChargerData";
    public static final String FORCE_CLOSE_LOG_PATH = REPOSITORY_BASE_PATH + "/ForceCloseLog";
    public static final String CP_CONFIG_PATH = REPOSITORY_BASE_PATH + "/CPConfig";
    public static final String LOCAL_MEMBER_PATH = REPOSITORY_BASE_PATH+"/LocalMember";
    public static final String COST_INFO_FILENAME = "costInfo.txt";
    public static final String INFO_DOWN_PATH = REPOSITORY_BASE_PATH + "/InfoDown";
    public static final String INFO_DOWN_CH0_PATH = INFO_DOWN_PATH + "/CH0";
    public static final String INFO_DOWN_CH1_PATH = INFO_DOWN_PATH + "/CH1";
    //add by si. 210330
    public static final String METERCONFIG_BASE_PATH = "/MeterViewConfig";
    public static final String METER_CONFIG_FILENAME = "MeterViewConfig.txt";
    public static final String CHGSETTIME_INFO_FILENAME = "chargerTime.txt";
    public static final String UPDATE_PATH = REPOSITORY_BASE_PATH + "/Update";
}
