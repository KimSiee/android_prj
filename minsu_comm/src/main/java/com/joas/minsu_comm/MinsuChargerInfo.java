/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 18 오후 2:05
 *
 */

package com.joas.minsu_comm;

import android.content.Context;

import com.joas.evcomm.EvCommDefine;
import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class MinsuChargerInfo {
    public static final int PAY_METHOD_BY_SERVER = 0x00; // 서버결제
    public static final int PAY_METHOD_BY_CHARGER = 0x01; // 현장결제

    public static final int CHARGING_REQ_AMOUNT_FULL = 0x01; // 충전요구량 FULL
    public static final int CHARGING_REQ_AMOUNT_KWH = 0x02; // 충전요구량 FULL
    public static final int CHARGING_REQ_AMOUNT_COST = 0x03; // 충전요구량 FULL

    public int cpMode = 0; // 충전기 모드
    public int pre_cpMode = 0;      //충전기 모드 백업용
    public long cpStatus = 0;
    public long pre_cpStatus = 0;
    public int meterVal = 0;

    // For 1B
    public int mode2 = 0x01; // 0x00:RF Card Reader 사용중지, 0x01: RF Card Reader 사용가능

    // 인증, 충전상태 (a1, e1, 1R)
    public String cardNum = "";
    public int reqAmoundSel = CHARGING_REQ_AMOUNT_FULL; // 충전 요구량 선택
    public int reqAmountKwh = 0;
    public int reqAmountCost = 0;
    public int payMethod = 0;
    public byte socBatt = 0;
    public byte start_socBatt = 0;
    public byte finish_socBatt = 0;
    public int curChargingKwh = 0;
    public int curChargingCost = 0;
    public int curChargingCostUnit = 0;
    public int battStatus = 0;
    public int battTotalAmount = 0;
    public int curBattRemain = 0;
    public int battVoltage = 0;
    public int battAmpare = 0;
    public int battTemperature = 0;
    public int connector_temp = 40;
    public String BMSVer = "0000";
    public int remainTime = 0; // 분단위
    public int availableLineCurrent = 30; // 공급 가능한 전류량(완속인경우 7000/220)
    public int chargingTime = 0;
//    public Date chargingStartTime;
    public Date chargingStartTime;
    public byte chargingFinishStatus = 0;
    public int userSetTime = 0;

    // 원격 충전
    public String orderNum = "";
    public int remoteStartChargingTimeLimit = 0;        //원격충전 요청시간(분)
    public boolean isRemoteCharge = false;
    public String remote_cardNum = "";

    //원격 FOTA 및 회원정보 다운로드 관련(HTTP)
    public String remoteURL = "";
    public byte destFlag1 = 0;
    public byte destFlag2 = 0;

    //예약정보 관련 - add by si.200604
    public String rsv_orderNum = "";
    public String rsv_startDay = "";
    public String rsv_startTime = "";
    public int rsv_chargingTimeMin = 0;
    public String rsv_uid = "";
    public Date rsv_startDateTime;
    public Date rsv_endDateTime;
    public byte rsv_flag = 0;
    public long rsv_leftMin = 0;

    // DB 코드
    public byte[] dbCode = new byte[2]; // 2자리
    public byte[] dbCode_i1 = new byte[8]; // 미전송 전문용 db코드 8자리

    //단가정보
    public String costversion;
    public int[] costxx = new int[24];

    //si. 충전기 시간설정 정보(서버에서 받은 값)
    public String settimeInterval = "10";
    public String minsetTime = "30";
    public String maxsetTime = "540";           //9시간(분단위)

   // 운영시간(L1)
    public String openTime = "";
    public String closeTime = "";

    //회원/비회원 정보
    public enum CommAuthType{
        NONE,
        MEMBER,
        NOMEMBER
    }
    public CommAuthType authType = CommAuthType.NONE;

    // 결제 정보
    public byte paymentCompany = 0x00; // 스마트로:0x01
    public String paymentStoreId  = ""; // 상점번호
    public String paymentPreDealId = ""; // 선결제 거래번호
    public String paymentPreDealSerialNo = ""; // 선결제 거래일련번호(PG결제)
    public String paymentPreDealApprovalNo = ""; // 선결제 승인번호
    public String paymentPreDealApprovalTime = ""; // 선결제 승인일시
    public int paymentPreDealApprovalCost = 0; // 선결제 승인 금액

    public String paymentDealId = ""; // 거래번호
    public String paymentDealSerialNo = ""; // 거래일련번호
    public String paymentDealApprovalNo = ""; // 승인번호
    public String paymentDealApprovalTime = ""; // 승인일시
    public int paymentDealApprovalCost = 0; //결제 승인 금액

    public String paymentDealCancelId = ""; // 취소거래번호
    public String paymentDealCancelNo = ""; // 취소번호
    public String paymentDealCancelTime = ""; // 취소일시
    public int paymentDealCancelApprovalCost = 0;   //취소승인금액

    public String paymentResultStat = "";   //결제 성공/실패상태
    public String paymentErrCode = "";       //결제 에러 코드
    public String paymentErrmsg = "";       //결제 에러 메시지
    public String payment_eventStat = "";   //"I": IC카드 삽입, "O": IC카드제거, "F":fallback, "R":RF카드 신호

    //p1,q1 관련 정보  String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal
    public String paymentNoMemberPhone = ""; // 비회원전화번호
    public byte[] payment_dbcode = new byte[8];
    public String payment_authNum = "";     //서버로부터 응답받은 인증번호(1p)
    public byte nomemAuthResultFlag = 0;
    public String recvfromserver_prepayauthnum = "";
    public String recvfromserver_prepayDatetime = "";
    public int recvfromserver_prepayPrice = 0;
    public int recvfromserver_usePayReal = 0;


    //설치정보
    public String stationId = "10000000";
    public String chargerId = "01";
    public String portNumber = "9000";
    public String serverIp = "192.168.0.10";
    public String paymentPortNumber = "8888";
    public String paymentServerIp = "192.168.0.10";
    public String mtoPhoneNumber = "           ";
    public String gpsLocInfo = "";
    public byte manufacturerCode = 'Y';
    public byte mtomManufacturerCode = 'R';
    public byte rfManufacturerCode = 'V';
    public String chargerFwVersion = "1.0";

    public boolean h1_mdnNumIsSpace = false;



    public String chargingAvalTime = "";


    public boolean isKakaoNavi = false;

    public MinsuChargerInfo() {
        initChargingInfoParam();
        initCpModeReady();

        initCpStatus();
    }

    public void initChargingInfoParam() {
        cardNum = "0000000000000000";
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
        chargingFinishStatus = 1;
        userSetTime = 0;
    }

    public void initCpModeReady() {
        cpMode = EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_READY;
        pre_cpMode = cpMode;
    }

    public void initCpStatus() {
//        cpStatus = 0x1FFFFFFFFFL; // 초기값
        cpStatus = 0x00000FFFFFFFFFFFL;
        pre_cpStatus = 0x00000FFFFFFFFFFFL;
    }


    public void initDbCode() {
        dbCode = new byte[2];
    }

    public void initDbCode_i1() {
        dbCode_i1 = new byte[8];
    }

    public boolean isDbCodeEmpty() {
        if (dbCode == null) return true;

        return ByteUtil.isByteArrayAllZero(dbCode);
    }

    /**
     * 단가 저장
     * 회원 / 비회원
     */
    public void saveCostInfo(Context context, String path, String filename) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("version", costversion);
            JSONArray list = new JSONArray();
            for (int i = 0; i < 24; i++)
            {
                list.put(costxx[i]);
            }
            obj.put("MemberCostUnit", list);

        } catch (Exception ex) {
            LogWrapper.e("ChargevEvChargerCostInfo" , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(path, filename, obj.toString(), false);
    }

    public void loadCostInfo(Context context, String fullPathName) {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile(fullPathName);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null) {
            return;
        }

        try {
            JSONObject obj = new JSONObject(loadString);
            costversion = obj.getString("version");
            JSONArray array = obj.getJSONArray("MemberCostUnit");
            for (int i = 0; i < 24; i++) {
                costxx[i] = array.getInt(i);
            }

        } catch (Exception ex) {
            LogWrapper.e("ChargevEvChargerCostInfo", "Json Parse Err:" + ex.toString());
        }
    }

    //si.20200526 - get charger setting file variables
    public void loadCpConfigInfo(Context context, String FullPathName)
    {
        String loadString = null;
        try{
            loadString = FileUtil.getStringFromFile(FullPathName);
        }catch (Exception ex){
            loadString = null;
        }
        if(loadString == null){
            return;
        }

        try{
            JSONObject obj = new JSONObject(loadString);
            stationId = obj.getString("StationID");
            chargerId = obj.getString("ChagerID");
            serverIp = obj.getString("ServerIP");
            portNumber = obj.getString("ServerPort");

        }catch (Exception ex){
            LogWrapper.e("ChargevEvChargerConfigInfo" , "Json Parse Err:"+ex.toString());
        }
    }

    //si.20200527 - save charger setting time info from server(N1)
    public void saveCpSetTimeInfo(Context context,String path, String filename)
    {
        JSONObject obj = new JSONObject();

        try
        {
            obj.put("SetTimeInterval",settimeInterval);
            obj.put("MinimumTimeVal",minsetTime);
            obj.put("MaximumTimaVal",maxsetTime);

        }catch (Exception ex)
        {
            LogWrapper.e("ChargevEvChargerTimeSettingInfo" , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(path, filename, obj.toString(), false);
    }
}
