/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 26 오후 2:21
 *
 */

package com.joas.evcomm;

/**
 * Created by user on 2017-12-26.
 */

public class EvCommDefine {

    public static final int COMM_WAIT_ACK_FIRST_TIMEOUT_SEC = 30;
    public static final int COMM_WAIT_ACK_SECOND_TIMEOUT_SEC = 15;
    public static final int COMM_PACKET_RETRY_MAX_CNT = 3;

    // 충전기 모드
    public static final int CP_MODE_BIT_OPERATING  = 0x01;
    public static final int CP_MODE_BIT_STOP_OPERATING  = 0x02;
    public static final int CP_MODE_BIT_READY  = 0x04;
    public static final int CP_MODE_BIT_CHARGING = 0x08;
    public static final int CP_MODE_BIT_CHECKING = 0x10;
    public static final int CP_MODE_BIT_TESTING = 0x20;
    public static final int CP_MODE_BIT_POWEROFF = 0x40;

    // 충전기 상태
    public static final int CP_STATUS_BIT_DOOR_CLOSE = ((int)1 << 0);;
    public static final int CP_STATUS_BIT_CONNECTOR_DOOR_CLOSE = ((int)1 << 1);;
    public static final int CP_STATUS_BIT_M2M = ((int)1 << 2);;
    public static final int CP_STATUS_BIT_UPS = ((int)1 << 3);;
    public static final int CP_STATUS_BIT_RFCARD = ((int)1 << 4);;
    public static final int CP_STATUS_BIT_RTC = ((int)1 << 5);;
    public static final int CP_STATUS_BIT_DISP = ((int)1 << 6);;

    public static final long CP_STATUS_BIT_INPUT_OVER_VOLTAGE = ((long)1 <<8);
    public static final long CP_STATUS_BIT_INPUT_OVER_CURRENT = ((long)1 <<9);
    public static final long CP_STATUS_BIT_OUTPUT_OVER_VOLTAGE = ((long)1 <<10);
    public static final long CP_STATUS_BIT_OUTPUT_OVER_CURRENT = ((long)1 <<11);

    public static final long CP_STATUS_BIT_OUTPUT_SHORT = ((long)1 <<12);
    public static final long CP_STATUS_BIT_OUTPUT_GROUND = ((long)1 <<13);
    public static final long CP_STATUS_BIT_OUTPUT_EMERGENCY_SWITCH = ((long)1 <<14);
    public static final long CP_STATUS_BIT_OUTPUT_WATER_SWITCH = ((long)1 <<15);
    public static final long CP_STATUS_BIT_OUTPUT_SLOPE_SWITCH = ((long)1 <<16);
    public static final long CP_STATUS_BIT_BMS_WARNGING = ((long)1 <<17);
    public static final long CP_STATUS_BIT_BMS_FAULT = ((long)1 <<18);
    public static final long CP_STATUS_BIT_COMM_INTER = ((long)1 <<19);
    public static final long CP_STATUS_BIT_COMM_EV = ((long)1 <<20);
    public static final long CP_STATUS_BIT_INTERLOCK = ((long)1 <<21);
    public static final long CP_STATUS_BIT_INTER_TEMP = ((long)1 <<22);
    public static final long CP_STATUS_BIT_EV_BATT_LOW = ((long)1 <<23);
    public static final long CP_STATUS_BIT_EV_BATT_NS = ((long)1 <<24);
    public static final long CP_STATUS_BIT_AC_DC_CONVERTOR = ((long)1 <<25);
    public static final long CP_STATUS_BIT_INPUT_LOW_VOLTAGE = ((long)1 <<26);
    public static final long CP_STATUS_BIT_INPUT_FREQ = ((long)1 <<27);
    public static final long CP_STATUS_BIT_INPUT_VOLTAGE_CONNECT_STATUS = ((long)1 <<28);
    public static final long CP_STATUS_BIT_COMM_MEASURE = ((long)1 <<29);
    public static final long CP_STATUS_BIT_MEMORY = ((long)1 <<30);
    public static final long CP_STATUS_BIT_WAKEUP_POWER = ((long)1 <<31);
    public static final long CP_STATUS_BIT_SELF_TEST = ((long)1 <<32);
    public static final long CP_STATUS_BIT_CONNECTOR_SOLENOID = ((long)1 <<33);
    public static final long CP_STATUS_BIT_DC_CURRENT_SENSOR = ((long)1 <<34);
    public static final long CP_STATUS_BIT_AC_CURRENT_SENSOR = ((long)1 <<35);
    public static final long CP_STATUS_BIT_SEQ_TIMEOUT = ((long)1 <<36);
    public static final long CP_STATUS_BIT_MC_FUSE = ((long)1 <<37);

    // VD 사이즈 정의
    public static final int VD_DATA_SIZE_STATUS = 14; // Mode(2) + Status(8) + kwh(4)
    public static final int VD_DATA_SIZE_b1 = VD_DATA_SIZE_STATUS; // Mode(2) + Status(8) + kwh(4)
    public static final int VD_DATA_SIZE_RF_CARDNUM = 16;
    public static final int VD_DATA_SIZE_a1 = VD_DATA_SIZE_STATUS+VD_DATA_SIZE_RF_CARDNUM; // status(14) + cardnum(16)

    //Position
    public static final int VD_DATA_AUTH_RET_POS = 16;

    // Defines
    public static final int AUTH_SUCCESS = 0x01;
    public static final int AUTH_FAULED = 0x00;

    // Timer
    public static final int DEFAULT_TIMER_PERIOD_SEC_b1 = (60*5); // 5 min
}
