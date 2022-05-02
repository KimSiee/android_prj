/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kevcs_charger_charlcd;

public class TypeDefine {
    public static final String SW_VERSION = "V1.5";
//    public static final String SW_RELEASE_DATE = "2021.11.29";
    public static final String SW_RELEASE_DATE = "2022.03.04";
    public static final String MODEL_NAME = "9511PS-BS-PO-BC";

    public enum ConnectorType {
        AC3,
        CHADEMO,
        DCCOMBO,
        BTYPE,
        CTYPE
    }

    public static final int CP_TYPE_SLOW_AC     = 0x02;
    public static final int CP_TYPE_FAST_3MODE  = 0x06;

    public static final int DEFAULT_CHANNEL = 0;

    public static final double DEFAULT_UNIT_COST = 173.8; // 100원

    // Timer 값 정의
    public static final int DEFAULT_AUTH_TIMEOUT = 60; // 60 sec
    public static final int DEFAULT_CONNECT_CAR_TIMEOUT = 60; // 60 sec
    public static final int MESSAGEBOX_TIMEOUT_SHORT = 3; // 4 sec
    public static final int METER_NOCHANGE_TIMEOUT = 60*20; // 20 min

    public static final int COMM_NOT_CONNECT_RESET_TIMEOUT = (60*60*6); // 6시간 통신 미연결시 reset

    // UI Timeout 값 정의
    public static final int UI_DEFAULT_HOME_TIMEOUT = (60*2); // 2min
    public static final int UI_CONNECTOR_WAIT_HOME_TIMEOUT = (90); // 90 sec

    public static final int UI_UNPLUG_TO_HOME_TIMEOUT = 20; // 20 sec
    public static final int UI_PLUG_ORG_POS_TIMEOUT = 120; // 2min
    public static final int UI_PLUG_ORG_POS_ALARM_PERIOD = 20; // 20sec
    public static final int UI_PLUG_ORG_POS_ALARM_CNT_MAX = 5;

    public static final int UI_DOOR_OPEN_ERROR_TIMEOUT = 10; // 10초이상 문이 열리지 않음

    // 충전중 상태 정보 주기
    public static final int COMM_CHARGE_STATUS_PERIOD = 300; // 5 min

    // 시간 오차값 허용 범위 정의
    public static final int TIME_SYNC_GAP_MS	= 10*1000; // 10 sec

    //프로그램 시작후 WatchDog 타이머 시작까지 시간
    public static final int WATCHDOG_START_TIMEOUT	=3*60; // 3 min
    public static final int WATCHDOG_TIMEOUT	= 5*60; // 5 min

    public static final String REPOSITORY_BASE_PATH = "/SmartChargerData";
    public static final String FORCE_CLOSE_LOG_PATH = REPOSITORY_BASE_PATH + "/ForceCloseLog";
    public static final String CP_CONFIG_PATH = REPOSITORY_BASE_PATH + "/CPConfig";
    public static final String KEVCS_COMM_PATH = REPOSITORY_BASE_PATH + "/Comm";
    public static final String SYSLOG_PATH = REPOSITORY_BASE_PATH + "/SysLog";
    public static final String USB_MEMERY_PATH = "/storage/usbhost";
    public static final String RESET_INFO_FILENAME = "reset_info.txt";

    public static final String FAULT_INFO_FILENAME = "fault_info.txt";


    public static final String WELCOME_MSG = "Welcome! EV User";

    public static final int DEFAULT_COST_VALUE = 20000; // 2만원
    //public static final int DEFAULT_COST_VALUE = 100; // 2만원

    public static final int DISP_CHARGING_CHARLCD_PERIOD = 8;
    public static final int DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT = 60; //초

    // For Log
    public static final int COMM_LOG_QUEUE_MAX = 200;
}
