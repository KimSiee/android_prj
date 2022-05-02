/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd;

public class TypeDefine {
    public static String SW_VERSION = "";
    //public static final String SW_VERSION = "210305(CL)";
    public static String SW_RELEASE_DATE = "";
    //    public static final String MODEL_NAME = "JC-9511PS-BS-PO-BC";
//    public static final String MODEL_NAME = "JC-92C1-7-0P";
    public static String MODEL_NAME = "JC-9111KE-TP-BC";

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

    //비회원 카드번호(고정)
    public static final String NOMEMBER_AUTH_CARD_NUM = "5959595959591004";

    public static final int CP_TYPE_SLOW_AC = 0x02;
    public static final int CP_TYPE_FAST_3MODE = 0x06;


    public static final int DEFAULT_CHANNEL = 0;

    public static final double DEFAULT_UNIT_COST = 100; // 100원


    // Timer 값 정의
    public static final int DEFAULT_AUTH_TIMEOUT = 60; // sec
    public static final int DEFAULT_CONNECT_CAR_TIMEOUT = 60; // sec
    public static final int DEFAULT_CONNECTOR_WAIT_TIMEOUT = 120; // sec
    public static final int MESSAGEBOX_TIMEOUT_SHORT = 5; // sec
    public static final int NOMEM_INPUT_AUTH_NUM_TIMEOUT = 180;     //sec

    public static final int FIRMWARE_UPDATE_COUNTER = 30; // 펌웨어 다운로드 후 업데이트 카운트

    // 충전중 상태 정보 주기
    public static final int COMM_CHARGE_STATUS_PERIOD = 300; // 5 min

    // 미터프로그램 동작 모니터링 타임아웃
    public static final int COMM_METER_TIMEOUT = 60; // 1 min

    // 시간 오차값 허용 범위 정의
    public static final int TIME_SYNC_GAP_MS = 10 * 1000; // 10 sec

    //프로그램 시작후 WatchDog 타이머 시작까지 시간
    //public static final int WATCHDOG_START_TIMEOUT	= 5*60; // 5 min
    public static final int WATCHDOG_START_TIMEOUT = 10; // 5 min

    public static final int DISP_CHARGING_CHARLCD_PERIOD = 10; // 8초
    public static final int DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT = 60; //초

    public static final int METERING_CHANGE_TIMEOUT = 3 * 60;     //초(180) 3분

    public static final int METER_COMM_CHECK_TIMEOUT = 1 * 60;      //1분

    public static final String REPOSITORY_BASE_PATH = "/SmartChargerData";
    public static final String FORCE_CLOSE_LOG_PATH = REPOSITORY_BASE_PATH + "/ForceCloseLog";
    public static final String CP_CONFIG_PATH = REPOSITORY_BASE_PATH + "/CPConfig";
    public static final String UPDATE_PATH = REPOSITORY_BASE_PATH + "/Update";
    public static final String COST_INFO_FILENAME = "costInfo.txt";
    public static final String CHGSETTIME_INFO_FILENAME = "chargerTime.txt";
    public static final String INFO_DOWN_PATH = REPOSITORY_BASE_PATH + "/InfoDown";
    public static final String MEMBER_TOTAL_FILENAME = "psmember.txt";
    public static final String MEMBER_ADD_FILENAME = "psmemadd.txt";
    public static final String MEMBER_DEL_FILENAME = "psmemdel.txt";

    //add by si. 210330
    public static final String METERCONFIG_BASE_PATH = "/MeterViewConfig";
    public static final String METER_CONFIG_FILENAME = "MeterViewConfig.txt";


    //ocpp관련
    public static final int OCPP_CONNECTOR_CNT_1CH_3MODE = 1;
    public static final int OCPP_DEFAULT_CONNECTOR_ID = 1;
}
