/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오전 8:29
 *
 */

package com.joas.gridwiz_slow_charlcd;

import android.content.Context;

import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class GridwizEvChargerInfo {
    public static final int PAY_METHOD_BY_SERVER = 0x00; // 서버결제
    public static final int PAY_METHOD_BY_CHARGER = 0x01; // 현장결제

    public static final int CHARGING_REQ_AMOUNT_FULL = 0x01; // 충전요구량 FULL
    public static final int CHARGING_REQ_AMOUNT_KWH = 0x02; // 충전요구량 FULL
    public static final int CHARGING_REQ_AMOUNT_COST = 0x03; // 충전요구량 FULL

    // 충전 완료 상태
    public static final int CHARGING_END_SUCCESS = 0x01;        // 정상적으로 충전이 완료된 경우
    public static final int CHARGING_END_PRESS_STOP_BTN = 0x02; // 고객이 Stop Button을 누른 경우
    public static final int CHARGING_END_PRESS_EMERGENCY = 0x03;// 고객이 Emergency Button 을 누른 경우
    public static final int CHARGING_END_CAR_STOP = 0x04;       // 전기차량에서 충전 종료를 요구한 경우 (급속)
    public static final int CHARGING_END_CHARGING_ERR = 0x05;   // 충전기 Error로 충전이 종료된 경우
    public static final int CHARGING_END_STOP_OPERATE = 0x07;   // 충전정보시스템으로부터 "운영중지" 수신할 경우

    public int cpMode = 0; // 충전기 모드
    public long cpStatus = 0;
    public int meterVal = 0;        //충전기 사용전력량 (kwh)단위 소수점 2자리까지 표시함 (12.34 -> 1234)

    // For 1B
    public int mode2 = 0x01; // 0x00:RF Card Reader 사용중지, 0x01: RF Card Reader 사용가능

    // 인증, 충전상태 (a1, e1, 1R)
    public String cardNum = "";
    public int reqAmoundSel = CHARGING_REQ_AMOUNT_FULL; // 충전 요구량 선택
    public int reqAmountKwh = 0;
    public int reqAmountCost = 0;
    public int payMethod = 0;
    public int socBatt = 0;
    public int curChargingKwh = 0;      //현재 충전량
    public int curChargingCost = 0;     //현재 충전요금
    public int curChargingCostUnit = 0; //현재 충전단가
//    public double curChargingCostUnit = 0; //현재 충전단가        //단가 타입 변환

    public int dayAllChargingNum = 0;       // 총 충전건수 (일 마감)
    public int dayAllChargingCost = 0;      // 총 충전금액 (일 마감)
    public int dayAllChargingKwh = 0;       // 총 충전전력량 (일 마감)


    public int battStatus = 0;      //배터리 상태
    public int battTotalAmount = 0;
    public int curBattRemain = 0;       //현재배터리 잔량
    public int battVoltage = 0;
    public int battAmpare = 0;
    public int battTemperature = 0;
    public String BMSVer = "0000";
    public int remainTime = 0; // 분단위
    public int availableLineCurrent = 30; // 공급 가능한 전류량(완속인경우 7000/220)
    public int chargingTime = 0;                //충전시간
    public Date chargingStartTime;              //충전시작시간
    public byte chargingFinishStatus = 0;       //충전완료상태

    public Date dayFinishTime; //일 마감 20200715

    // 원격 충전
    public String orderNum = "";
    public int remoteStartChargingTimeLimit = 0;

    // DB 코드
    public byte[] dbCode = new byte[2]; // 8자리

    //단가정보
    public String costVersion = "";
    public String memberCostUnitApplyDate = "";
    public int[] memberCostUnit = new int[24];      //단가 타입 변환
    public String nonMemberCostUnitApplyDate = "";
    public int[] nonMemberCostUnit = new int[24];

    // 예약 단가정보
    public String reservation_costVersion = "";
    public String reservation_memberCostUnitApplyDate = "";
    public int[] reservation_memberCostUnit = new int[24];      //단가 타입 변환
    public String reservation_nonMemberCostUnitApplyDate = "";
    public int[] reservation_nonMemberCostUnit = new int[24];

    // 운영시간(L1)
    public String openTime = "";
    public String closeTime = "";


    //설치정보
    public String stationId = "10000000";
    public String chargerId = "01";
    public String portNumber = "2003";
    public String serverIp = "218.149.156.57";
    public String paymentPortNumber = "0000";
    public String paymentServerIp = "0.0.0.0";
    public String mtoPhoneNumber = "01010001234";       //수정 필요 (M2M 전화번호(MDM))
    public String gpsLocInfo = "";
    public byte manufacturerCode = 'Y';     // 중앙제어 제조사 코드
    public byte mtomManufacturerCode = 'R'; // 새한RF 코드
    public byte rfManufacturerCode = 'R';   // 새한RF 코드
    public String chargerFwVersion = "1.0";

    //m1
    public int m1_idx = 0;

    public GridwizEvChargerInfo() {
        initChargingInfoParam();
        initCpModeReady();
        initCpStatus();
    }

    public void initChargingInfoParam() {
        cardNum = "0000000000009999"; // 자동인증시 마지막 9999
        reqAmoundSel = CHARGING_REQ_AMOUNT_FULL; // 충전 요구량 선택
        reqAmountKwh = 0;
        reqAmountCost = 0;
        payMethod = 0;
        socBatt = 0;
        curChargingKwh = 0;
        curChargingCost = 0;
        curChargingCostUnit = 0;
        battStatus = 0;
        battTotalAmount = 0;
        curBattRemain = 0;
        battVoltage = 0;
        battAmpare = 0;
        battTemperature = 0;
        BMSVer = "0000";
        remainTime = 0; // 분단위
        availableLineCurrent = 30; // 공급 가능한 전류량(완속인경우 7000/220)
        chargingTime = 0;
        chargingFinishStatus = 0;
    }

    //충전기 모드 - 충전 대기
    public void initCpModeReady() {
        //cpMode = EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_READY;
    }

    //충전기 모드 변경 - 충전 중
    public void cpModeCharging()
    {
        //cpMode = EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_CHARGING;
    }

    //충전기 모드 - DSP TestMode
    public void cpModeDspTestMode() {
        //cpMode = EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_TESTING;
    }


    public void initCpStatus() {
        cpStatus = 0x1FFFFFFFFFL; // 초기값
    }

    public void initDbCode() {
        dbCode = new byte[2];
    }

    public boolean isDbCodeEmpty() {
        if (dbCode == null) return true;

        return ByteUtil.isByteArrayAllZero(dbCode);
    }

    /**
     * 단가 저장
     * 회원 / 비회원
     */
    public void saveCostInfo(Context context, String path, String filename, boolean isReservation) {
        JSONObject obj = new JSONObject();
        try {
            if (isReservation)
            {
                // 예약 단가 적용
                obj.put("CostVersion", reservation_costVersion);
                obj.put("MemberCostUnitApplyDate", reservation_memberCostUnitApplyDate);
                JSONArray list = new JSONArray();
                for (int i = 0; i < 24; i++)
                {
                    list.put(reservation_memberCostUnit[i]);
                }
                obj.put("MemberCostUnit", list);

                obj.put("NonMemberCostUnitApplyDate", reservation_nonMemberCostUnitApplyDate);
                list = new JSONArray();
                for (int i = 0; i < 24; i++)
                {
                    list.put(reservation_nonMemberCostUnit[i]);
                }
                obj.put("NonMemberCostUnit", list);
            }
            else
            {
                // 현재 단가 적용
                obj.put("CostVersion", costVersion);
                obj.put("MemberCostUnitApplyDate", memberCostUnitApplyDate);
                JSONArray list = new JSONArray();
                for (int i = 0; i < 24; i++)
                {
                    list.put(memberCostUnit[i]);
                }
                obj.put("MemberCostUnit", list);

                obj.put("NonMemberCostUnitApplyDate", nonMemberCostUnitApplyDate);
                list = new JSONArray();
                for (int i = 0; i < 24; i++)
                {
                    list.put(nonMemberCostUnit[i]);
                }
                obj.put("NonMemberCostUnit", list);
            }
        } catch (Exception ex) {
            LogWrapper.e("JejuEvChargerInfo" , "Json Make Err:"+ex.toString());
        }


        if (isReservation)
            FileUtil.stringToFile(path, "costInfo_reservation.json", obj.toString(), false);
        else
            FileUtil.stringToFile(path, filename, obj.toString(), false);

    }

    public void loadCostInfo(Context context, String fullPathName) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile(fullPathName);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null ) {
            return;
        }

        try {
            JSONObject obj = new JSONObject(loadString);
            costVersion = obj.getString("CostVersion");
            memberCostUnitApplyDate = obj.getString("MemberCostUnitApplyDate");
            JSONArray array = obj.getJSONArray("MemberCostUnit");
            for (int i=0; i<24; i++) {
                memberCostUnit[i] = array.getInt(i);
//                memberCostUnit[i] = array.getDouble(i);       //단가 타입 변환
            }
            nonMemberCostUnitApplyDate = obj.getString("NonMemberCostUnitApplyDate");
            array = obj.getJSONArray("NonMemberCostUnit");
            for (int i=0; i<24; i++) {
                nonMemberCostUnit[i] = array.getInt(i);
//                nonMemberCostUnit[i] = array.getDouble(i);        //단가 타입 변환
            }
        } catch (Exception ex) {
            LogWrapper.e("JejuEvChargerInfo" , "Json Parse Err:"+ex.toString());
        }
    }




    /**
     * 예약 파일 적용 체크 함수
     *
     * by Lee 20200810
     * @throws ParseException
     */
    public boolean checkApplyReservationFile(String applyDate) throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        java.util.Date time = new java.util.Date();
        String time1 = sdf.format(time);

        java.util.Date curDate = sdf.parse(time1);
        java.util.Date ResDate = sdf.parse(applyDate);

        if(curDate.equals(ResDate) || curDate.after(ResDate)){
            System.out.println("Date1 is after Date2");
            //예약단가 파일미적용
            return false;

        }
        else /*if (curDate.before(ResDate))*/{
            System.out.println("Date1 is before Date2");
            // 예약단가 파일 적용
            return true;
        }

    }

    /**
     * 현재일시 >= 예약일시 인 경우,
     * 단가예약파일 -> 단가파일 로 변경 및 단가예약파일 삭제
     *
     * by Lee 20200810
     *
     * @param fullPathName
     * @param fullPathReserveFileName
     * @return
     */
    public boolean CheckCopyReserveFileToCostFIle(String fullPathName, String fullPathReserveFileName)
    {
        String reserveFile = fullPathReserveFileName;
        String costFile = fullPathName;

        File f = new File(reserveFile);

        if (f.exists())
        {
            try
            {
                // 현재일자 >= 예약일시
                if (!checkApplyReservationFile(ReserveCostSddFileRead(fullPathReserveFileName)))
                {
                    synchronized (this)
                    {
                        fileCopy(reserveFile, costFile); // 1. reserveFile -> costFile 복사
                        fileDelete(reserveFile);         // 2. 예약 단가 파일 삭제

                        return true;
                    }
                }
            }
            catch (Exception e1)
            {
            }
        }

        return false;
    }

    /**
     * 회원 단가 적용일자 가져오기
     *
     * by Lee 20200810
     * @param fullPathReserveFileName
     * @return
     */
    private String ReserveCostSddFileRead(String fullPathReserveFileName)
    {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile(fullPathReserveFileName);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null ) {
            return loadString;
        }

        try {
            JSONObject obj = new JSONObject(loadString);

            return obj.getString("MemberCostUnitApplyDate");

        } catch (Exception ex) {
            LogWrapper.e("StarkoffChargerInfo" , "Json Parse Err:"+ex.toString());
            return  null;
        }

    }


    /**
     * 파일 복사
     *
     * by Lee 20200810
     * @param inFileName
     * @param outFileName
     */
    public void fileCopy(String inFileName, String outFileName) {

        try {

            FileInputStream fis = new FileInputStream(inFileName);
            FileOutputStream fos = new FileOutputStream(outFileName);

            int data = 0;

            while((data=fis.read())!=-1) {
                fos.write(data);
            }
            fis.close();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 파일 삭제
     *
     * by Lee 20200810
     * @param filePath
     */
    public void fileDelete(String filePath) {

        if(null!=filePath){

            File del_File = new File(filePath);

            if(del_File.exists()){
                // System.out.println(del_File.getName() + " 파일을 삭제합니다");
                del_File.delete();
            }else{
                System.out.println(del_File.getName() + " 파일이 존재하지 않습니다.");
            }
        }
    }
}
