/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 26 오후 2:48
 *
 */

package com.joas.evcomm;

public class AlarmCode {
    public static final byte STATE_OCCUR = 0x31;            // 발생
    public static final byte STATE_RESTORE = 0x32;          // 복구

    public static final int POWER_ON = 0x1000;              // 충전기 POWER ON
    public static final int DOWNLOAD_COMPLETE = 0x1029;      // K1 다운로드 완료 알림 flag

    public static final int EMERGENCY = 0x1013;             // 비상정지버튼
    public static final int ETC_ERR = 0x1078;               // 기타 에러
    public static final int CHGR_INDOOR_TEMP_ERR = 0x1079;  // 충전기 내부 온도 이상
    public static final int CHGR_INDOOR_COMM_ERR = 0x1082;  // 충전기 내부 통신 이상
    public static final int METER_COMM_ERR = 0x1084;        // 전력량계 통신 이상

    /**
     * add by jmlee.
     */
    public static final int CONNECOTR_DOOR_OPEN = 0x1021;   // 커넥터 도어 Open
    public static final int CONNECOTR_DOOR_CLOSE = 0x1022;  // 커넥터 도어 Close


    /**
     * add by si. 차지비 폴트 알람코드
     */
    public static final int ERR_CODE_1 = 0x01; //errcode 1 : 완속 전자접촉기/릴레이 이상
    public static final int ERR_CODE_2 = 0x02; //errcode 2 : 완속 누설전류 발생
    public static final int ERR_CODE_3 = 0x03; //errcode 3 : 완속 충전기 접지 이상(FG falut)
    public static final int ERR_CODE_4 = 0x04; //errcode 4 : 완속 AC RCM 불량
    public static final int ERR_CODE_5 = 0x05; //errcode 5 : 완속 저전압 경보
    public static final int ERR_CODE_6 = 0x06; //errcode 6 : 완속 과전압 경보
    public static final int ERR_CODE_7 = 0x07; //errcode 7 : 완속 입력/휴즈 이상(RCD OFF/TRIP)
    public static final int ERR_CODE_8 = 0x08; //errcode 8 : 완속 출력과전류
    public static final int ERR_CODE_9 = 0x09; //errcode 9 : 완속 전자접촉기/릴레이 융착
    public static final int ERR_CODE_10 = 0x10; //errcode 10 : 완속 차량통신이상(CP레벨)
    public static final int ERR_CODE_11 = 0x11; //errcode 11 : 내부통신 이상
    public static final int ERR_CODE_12 = 0x12; //errcode 12 : 전력량계 통신이상dd

}