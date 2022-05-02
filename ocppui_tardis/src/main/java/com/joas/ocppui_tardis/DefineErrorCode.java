/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_tardis;

public class DefineErrorCode {
    public static final String ERR_CODR_SVR_DISCONNECT = "S-001";       // 서버 Disconnect

    public static final String ERR_CODR_423_0 = "E-423-0";      // 비상정지버튼 눌림
    public static final String ERR_CODR_423_1 = "1";      // MC1 접점 에러 or 융착 / AC MC(Relay) 접점에러 or 융착
    public static final String ERR_CODR_423_2 = "1";      // MC2 접점 에러 or 융착
    public static final String ERR_CODR_423_3 = "E-423-3";      // MC3 접점 에러 or 융착
    //    public static final String ERR_CODR_423_4 = "E-423-4";      // Relay 1 접점 에러 or 융착
    public static final String ERR_CODR_423_4 = " 9";      // edit by si. 릴레이 융착, 에러코드 9
    public static final String ERR_CODR_423_5 = " 9";      // Relay 2 접점 에러 or 융착
    public static final String ERR_CODR_423_6 = " 1";      // Relay 3 접점 에러 or 융착
    public static final String ERR_CODR_423_7 = " 1";      // Relay 4 접점 에러 or 융착
    public static final String ERR_CODR_423_8 = "E-423-8";      // Reserved
    public static final String ERR_CODR_423_9 = " 6";      // 입력 과전압
    public static final String ERR_CODR_423_10 = "13";    // 내부 과온도E-423-10
    public static final String ERR_CODR_423_11 = "E-423-11";    // 모듈 - 컨트롤보드 통신 오류
    public static final String ERR_CODR_423_12 = "E-423-12";    // 컨트롤보드1 - 컨트롤보드2 통신 오류
    public static final String ERR_CODR_423_13 = " 12";    // 전력량계 1 오류
    public static final String ERR_CODR_423_14 = "E-423-14";    // 전력량계 2 오류
    public static final String ERR_CODR_423_15 = " 5";    // 입력 저전압

    public static final String ERR_CODR_424_0 = "E-424-0";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_1 = "E-424-1";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_2 = "E-424-2";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_3 = "E-424-3";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_4 = "E-424-4";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_5 = "E-424-5";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_6 = "E-424-6";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_7 = "E-424-7";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_8 = "E-424-8";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_9 = "E-424-9";      // Reserved for 공통 에러
    public static final String ERR_CODR_424_10 = "E-424-10";    // Reserved for 공통 에러
    public static final String ERR_CODR_424_11 = "E-424-11";    // Reserved for 공통 에러
    public static final String ERR_CODR_424_12 = "E-424-12";    // Reserved for 공통 에러
    public static final String ERR_CODR_424_13 = "E-424-13";    // Reserved for 공통 에러
    public static final String ERR_CODR_424_14 = "E-424-14";    // Reserved for 공통 에러
    public static final String ERR_CODR_424_15 = "E-424-15";    // Reserved for 공통 에러

    public static final String ERR_CODR_425_0 = " 7";      // 차단기 (RCD) OFF 상태 / 퓨즈 OFF 상태
    public static final String ERR_CODR_425_1 = "E-425-1";      // AC Relay 상태 이상
    public static final String ERR_CODR_425_2 = " 2";      // AC 누설 발생
    public static final String ERR_CODR_425_3 = " 4";      // AC RCM 불량
    public static final String ERR_CODR_425_4 = " 8";      // AC 과전류
    public static final String ERR_CODR_425_5 = " 3";      // AC FG 불량
    public static final String ERR_CODR_425_6 = " 10";      // AC CP 에러
    public static final String ERR_CODR_425_9 = " 11";      // 내부통신장애

    public static final String ERR_EIM_COMMERR = " 11";     //내부통신장애
}
