/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:35
 *
 */

package com.joas.joasui_mobile_charger.ui;

public class TypeDefine {
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

    public static final int DEFAULT_UNIT_COST = 100; // 100원

    // Timer 값 정의
    public static final int DEFAULT_AUTH_TIMEOUT = 60; // 60 sec
    public static final int DEFAULT_CONNECT_CAR_TIMEOUT = 30; // 60 sec
    public static final int MESSAGEBOX_TIMEOUT_SHORT = 3; // 4 sec

    // 충전중 상태 정보 주기
    public static final int COMM_CHARGE_STATUS_PERIOD = 300; // 5 min


    // 시간 오차값 허용 범위 정의
    public static final int TIME_SYNC_GAP_MS	= 10*1000; // 10 sec

    //프로그램 시작후 WatchDog 타이머 시작까지 시간
    public static final int WATCHDOG_START_TIMEOUT	= 5*60; // 5 min

    public static final String REPOSITORY_BASE_PATH = "/SmartChargerData";
    public static final String FORCE_CLOSE_LOG_PATH = REPOSITORY_BASE_PATH + "/ForceCloseLog";
    public static final String CP_CONFIG_PATH = REPOSITORY_BASE_PATH + "/CPConfig";
}
