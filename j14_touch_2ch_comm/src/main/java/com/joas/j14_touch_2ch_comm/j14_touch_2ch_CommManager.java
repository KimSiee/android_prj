/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 12. 14 오후 5:45
 *
 */

package com.joas.j14_touch_2ch_comm;

import android.content.Context;
import android.util.Log;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvCommManager;
import com.joas.evcomm.EvPacket;
import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import java.util.Date;

public class j14_touch_2ch_CommManager extends EvCommManager {
    public static final String TAG = "StarkoffCommManager";
    public static final String INFODOWN_PATH = "/InfoDown/";
    public static final String COMM_LOG_PATH = "/Log";
    public static final int COMM_LOG_RECORD_DAYS = 180; // 180일(6개월)

    j14_touch_2ch_CommManagerListener starkoffCommManagerListener = null;

    j14_touch_2ch_ChargerInfo chargerInfo;
    boolean isFirstConnect = false;
    boolean isDowningInfo = false;


    byte[] infoDownData = null;
    int infoDownBlockIdx = -1;
    int infoDownIdx = 0;
    String infoDownFilename = "";

    String baseFilePath = "";
    boolean isNonTransPacketSending = true;
    EvPacket lastNonTransPacket = null;

    public boolean isResetReq = false;      // 충전기 리셋 명령 (from Server)

    public String dayFinishTime = "115900"; // 일 마감 시간

    public String sendTime_r1 = "020000";   // r1 전송 주기

    public String rebootTime = "040000";    // 일일 재부팅

    public String encryptionKey = "";       // ARIA Key


    public j14_touch_2ch_CommManager(Context context, String svrip, int svrport, String sid, String cid, int type, j14_touch_2ch_ChargerInfo info, String filePath)  {
        super(context, svrip, svrport, sid, cid, type, filePath);

        //미전송 저장 사용
        setUseNontransPacketRecord(true);

        chargerInfo = info;
        baseFilePath = filePath;

        FileUtil.pastDateLogRemove(baseFilePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제
    }

    public void setStarkoffCommManagerListener(j14_touch_2ch_CommManagerListener listener) { starkoffCommManagerListener = listener; }

    public void setNonTransPacketSending(boolean tf) {
        isNonTransPacketSending = tf;
    }

    @Override
    public void processRequestMsg(EvPacket packet) {
        switch ( packet.ins ) {
            case "A1":
                onRecvChargerStatusReq_A1(packet);
                break;

            case "B1":
                onRecvChargerModeReq_B1(packet);
                break;

            case "C1":
                onRecvChangeModeReq_C1(packet);
                break;

            case "E1":
                onRecvVersionInfoReq_E1(packet);
                break;

            case "G1":
                onRecvCostDownReqResp_G1_1j(packet, true);
                break;

            case "H1":
                onRecvCostInfoReq_H1(packet);
                break;

            case "I1":
                onRecvResetReq_I1(packet);
                break;

            case "K1":
                onRecvInfoDwon_K1_1m(packet, true);
                break;

            case "L1":
                onRecvOpenCloseTimeReq_L1(packet);
                break;

            case "M1":
                onRecvMeterValReq_M1(packet);
                break;

            case "R1":
                onChargingStatusReq_R1(packet);
                break;

            case "S1":
                onEncryptionKeyReq_S1(packet);
                break;

            case "T1":
                onChargerParameterReq_T1(packet);
                break;

            case "U1":
                onChargerInstallationInfoReq_U1(packet);
                break;
        }

    }

    @Override
    public void processResponseMsg(EvPacket packet) {
        switch ( packet.ins ) {
            case "1a":
                onRecvAuthResp_1a(packet);
                break;

            case "1b":
                onRecvChargerStatusResp_1b(packet);
                break;

            case "1c":
                break;

            case "1d":
                onRecvStartChargingResp_1d(packet);
                break;

            case "1e":
                break;

            case "1f":
                onRecvFinishChargingResp_1f(packet);
                break;

            case "1g":
                break;

            case "1h":
                break;

            case "1i":
                onRecvNonTransResp_1i(packet);
                break;

            case "1j":
                onRecvCostDownReqResp_G1_1j(packet, false);
                break;

            case "1k":
                onEncryptionResp_1k(packet);
                break;

            case "1l":
                onChargerParameterResp_1l(packet);
                break;

            case "1m":
                onRecvInfoDwon_K1_1m(packet, false);
                break;

            case "1r":
                onRecvVersionResp_1r(packet);
                break;

            case "1s":
                break;
        }
    }

    @Override
    public boolean isSaveInfoPacket(EvPacket evPacket) {
        if ( evPacket.ins.equals("d1") || evPacket.ins.equals("f1") || evPacket.ins.equals("g1")
                || evPacket.ins.equals("c1") || evPacket.ins.equals("e1")) {        //충전기상태(b1), 충전진행(e1) 추가
            return true;
        }
        return false;
    }

    // 미전송 전문 체크
    void nonTransPacketCheck() {
        EvPacket evPacket = nonTransPacketRecord.getNextRecord();
        if ( evPacket != null ) {
            isNonTransPacketSending = true;
            sendNonTransPacket(evPacket);
        }
    }

    //======================================================================
    // 서버 Request Message 수신
    //======================================================================

    /**
     * 충전기 상태 요청(서버)
     * 받으면 r1(버전정보요청)을 전송한다.
     * @param packet
     */
    void onRecvChargerStatusReq_A1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.BasicResponse response = new j14_touch_2ch_Packet.BasicResponse();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChargerStatusReq_A1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    //충전기 모드 요청
    void onRecvChargerModeReq_B1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerModeRequestAck_1B response = new j14_touch_2ch_Packet.ChargerModeRequestAck_1B();
            sendReponse(packet, response.encode(chargerInfo));

            // 제주전기차 B1을 받으면 r1을 요청한다.(만약 업데이트 중이라면 생략)
//            if (isDowningInfo == false) {
//                sendVersionReq();
//            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChargerModeReq_B1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 충전기 모드 변경
     * @param packet
     */
    void onRecvChangeModeReq_C1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerModeChange_C1 request = new j14_touch_2ch_Packet.ChargerModeChange_C1();
            request.decode(packet.vd);

            chargerInfo.cpMode = request.mode;

            j14_touch_2ch_Packet.ChargerModeChangeAck_1C response = new j14_touch_2ch_Packet.ChargerModeChangeAck_1C();
            sendReponse(packet, response.encode(chargerInfo));

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChangeModeReq_C1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 버전 정보 전송
     *
     * by Lee 20200714
     * @param packet
     */
    void onRecvVersionInfoReq_E1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.VersionInfo_E1 request = new j14_touch_2ch_Packet.VersionInfo_E1();
            request.decode(packet.vd);


            j14_touch_2ch_Packet.VersionInfoAck_1E response = new j14_touch_2ch_Packet.VersionInfoAck_1E();
            sendReponse(packet, response.encode(chargerInfo));

            // E1 전문 수신 시, m1(정보다운로드 요청) 전송
            // by Lee 20200819 추가
            if ( starkoffCommManagerListener != null ) starkoffCommManagerListener.onRecvVersionResp(request.ProgramVer, request.unitCostVer);

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvVersionInfoReq_E1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 단가정보 다운로드(G1) 및 단가 정보 요청 응답 (1j)
     *
     * @param packet
     * @param isRequestPacket
     */
    // 단가 정보
    void onRecvCostDownReqResp_G1_1j(EvPacket packet, boolean isRequestPacket) {
        try {
            j14_touch_2ch_Packet.CostInfoDown_G1_1j request = new j14_touch_2ch_Packet.CostInfoDown_G1_1j();
            request.decode(packet.vd);

            boolean isReservation = false;
            if (chargerInfo.checkApplyReservationFile(request.memberCostApplyDate))
            {
                //예약 단가
                chargerInfo.reservation_costVersion = request.costVersion;
                chargerInfo.reservation_memberCostUnit = request.memberCost;
                chargerInfo.reservation_memberCostUnitApplyDate = request.memberCostApplyDate;
                chargerInfo.reservation_nonMemberCostUnit = request.nonMemberCost;
                chargerInfo.reservation_nonMemberCostUnitApplyDate = request.nonMemberCostApplyDate;
                isReservation = true;
            }
            else
            {
                // 현재 단가
                chargerInfo.costVersion = request.costVersion;
                chargerInfo.memberCostUnit = request.memberCost;
                chargerInfo.memberCostUnitApplyDate = request.memberCostApplyDate;
                chargerInfo.nonMemberCostUnit = request.nonMemberCost;
                chargerInfo.nonMemberCostUnitApplyDate = request.nonMemberCostApplyDate;
                isReservation = false;
            }

            if ( isRequestPacket ) {    //G1 인 경우 (단가정보 다운로드)
                j14_touch_2ch_Packet.BasicResponseCode response = new j14_touch_2ch_Packet.BasicResponseCode();
                sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
            }
            if ( starkoffCommManagerListener != null ) starkoffCommManagerListener.onCostInfoUpdateEvent(isReservation);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCostDownReqResp_G1_1j:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvCostInfoReq_H1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.CostInfoRequest_H1 request = new j14_touch_2ch_Packet.CostInfoRequest_H1();
            request.decode(packet.vd);
            j14_touch_2ch_Packet.CostInfoAck_1H response = new j14_touch_2ch_Packet.CostInfoAck_1H();
            sendReponse(packet, response.encode(chargerInfo, request.kind));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCostInfoReq_H1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 충전기 Reset 요청
     *
     * @param packet
     */
    void onRecvResetReq_I1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ResetRequest_I1 request = new j14_touch_2ch_Packet.ResetRequest_I1();
            request.decode(packet.vd);

            j14_touch_2ch_Packet.BasicResponseCode response = new j14_touch_2ch_Packet.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            if ( starkoffCommManagerListener != null ) starkoffCommManagerListener.onResetRequest(request.kind);

            isResetReq = true;
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvResetReq_I1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 정보 다운로드  (K1) / 정보 다운로드 요청 (1m)
     * 프로그램 다운로드
     *
     * @param packet
     * @param isRquest
     */
    void onRecvInfoDwon_K1_1m(EvPacket packet, boolean isRquest) {
        try {
            j14_touch_2ch_Packet.DataDown_K1_1m request = new j14_touch_2ch_Packet.DataDown_K1_1m();
            request.decode(packet.vd);

            // 다운로드 플레그(B1 에 대한 버전정보 요청 플레그)
            if ( request.div == (byte)0xF0 ) isDowningInfo = true;
            else if ( request.div == (byte)0xFF ) isDowningInfo = false;


            // 충전기 Fw이면 업데이트 다운로드 진행
            if ( request.dest1 == (byte)0x01 ) {
                if (request.div == (byte) 0xF0) {//다운로드 처음
                    // 버퍼 생성
                    infoDownData = new byte[request.filesize];
                    infoDownIdx = 0;
                    infoDownBlockIdx = -1;
                    infoDownFilename = request.filename;
                } else { //중간 or 마지막
                    if (infoDownBlockIdx < request.blockIdx) {
                        System.arraycopy(request.rawData, 0, infoDownData, infoDownIdx, request.rawData.length);
                        infoDownBlockIdx = request.blockIdx;
                        infoDownIdx += request.rawData.length;

                        if (request.div == (byte) 0xFF) {  //마지막
                            if (request.dest2 == (byte) 0x01) starkoffCommManagerListener.onFirmwareDownCompleted(infoDownData);
                            else if (request.dest2 >= 5 && request.dest2 <= 8 ) {
                                onInfoDownComplete(request.dest2);
                            }
                        }
                    }
                }
            }

            if ( isRquest ) {
                j14_touch_2ch_Packet.BasicResponseCode response = new j14_touch_2ch_Packet.BasicResponseCode();
                sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
            }
            else if ( request.div != (byte) 0xFF ){
                chargerInfo.m1_idx = request.blockIdx + 1;
                j14_touch_2ch_Packet.InfoDownReq_m1 m1_request = new j14_touch_2ch_Packet.InfoDownReq_m1();
                sendRequest("m1", m1_request.encode(chargerInfo, (byte)0x01, (byte)0x01)); // 기본 UI 만
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvInfoDwon_K1_1m:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onInfoDownComplete(byte dest2) {
        // 파일저장
        FileUtil.bufferToFile(baseFilePath, infoDownFilename, infoDownData, false);

        // 현재 제주전기차에서는 사용자 정보(로컬인증) 사용안함
    }



    /**
     * 운영시간 설정
     *
     * @param packet
     */
    void onRecvOpenCloseTimeReq_L1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.OpenCloseTime_L1 request = new j14_touch_2ch_Packet.OpenCloseTime_L1();
            request.decode(packet.vd);

            chargerInfo.openTime = request.openTime;
            chargerInfo.closeTime = request.closeTime;

            j14_touch_2ch_Packet.BasicResponseCode response = new j14_touch_2ch_Packet.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvOpenCloseTimeReq_L1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전기 사용전력량 요청 (폐기)
     * @param packet
     */
    void onRecvMeterValReq_M1(EvPacket packet) {
        j14_touch_2ch_Packet.BasicResponse response = new j14_touch_2ch_Packet.BasicResponse();
        sendReponse(packet, response.encode(chargerInfo));
    }

    /**
     * 충전 진행 상태 요청
     * @param packet
     */
    void onChargingStatusReq_R1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargingStatusRequestAck_1R response = new j14_touch_2ch_Packet.ChargingStatusRequestAck_1R();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargingStatusReq_R1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 암호화 Key 다운로드 응답 (1S)
     * @param packet
     */
    void onEncryptionKeyReq_S1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.EncryptionKeyRequestAck_1S response = new j14_touch_2ch_Packet.EncryptionKeyRequestAck_1S();

            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onEncryptionKeyReq_S1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }



    /**
     * 충전기 운영 파라미터 응답 (1T)
     * @param packet
     */
    void onChargerParameterReq_T1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerParameterRequestAck_1T response = new j14_touch_2ch_Packet.ChargerParameterRequestAck_1T();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargerParameterReq_T1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }



    /**
     * 충전기 설치 정보 요청
     * @param packet
     */
    void onChargerInstallationInfoReq_U1(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerInstallationInfoAck_1U response = new j14_touch_2ch_Packet.ChargerInstallationInfoAck_1U();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "ChargerInstallationInfoAck_1U:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    //======================================================================
    // 충전기 Request Message 전송
    //======================================================================
    /**
     * 충전기 알람 요청
     * @param code 알람코드
     * @param status 알람상태(발생/복구)
     */
    public void SendAlarm(int code, byte status) {
        try {
            j14_touch_2ch_Packet.AlarmInfo_c1 request = new j14_touch_2ch_Packet.AlarmInfo_c1();
            sendRequest("c1", request.encode(chargerInfo, code, status));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "SendAlarm:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전기 설치정보 요청
     */
    public void SendInstallationInfo() {
        try {
            j14_touch_2ch_Packet.ChargerInstallationInfo_h1 request = new j14_touch_2ch_Packet.ChargerInstallationInfo_h1();

            sendRequest("h1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "SendInstallationInfo:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 단가 정보 요청(j1)
     */
    public void SendCostUnitReq() {
        try {
            j14_touch_2ch_Packet.CostInfoDownReq_j1 request = new j14_touch_2ch_Packet.CostInfoDownReq_j1();

            sendRequest("j1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "SendCostUnitReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전기 상태 전송
     */
    public void sendChargerStatus() {
        try {
            j14_touch_2ch_Packet.ChargerStatus_b1 request = new j14_touch_2ch_Packet.ChargerStatus_b1();
            sendRequest("b1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendChargerStatus:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    public void sendAuthReq() {
        try {
            j14_touch_2ch_Packet.Authentication_a1 request = new j14_touch_2ch_Packet.Authentication_a1();
            sendRequest("a1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendAuthReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전 시작 메시지 전송 d1
     */
    public void sendStartCharging() {
        try {
            j14_touch_2ch_Packet.StartCharging_d1 request = new j14_touch_2ch_Packet.StartCharging_d1();
            sendRequest("d1", request.encode(chargerInfo));

            //DB 코드 초기화
            chargerInfo.initDbCode();
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendStartCharging:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전 상태 전송 e1
     */
    public void sendChargingStatus() {
        try {
            j14_touch_2ch_Packet.ChargingStatus_e1 request = new j14_touch_2ch_Packet.ChargingStatus_e1();
            sendRequest("e1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendChargingStatus:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전 완료 전송 f1
     */
    public void sendFinishCharging() {
        try {
            j14_touch_2ch_Packet.FinishCharging_f1 request = new j14_touch_2ch_Packet.FinishCharging_f1();
            byte[] vdData = request.encode(chargerInfo);

            // db코드가 없으면 패킷을 보내지 않고 미전송으로 처리한다.
            sendRequest("f1", vdData, vdData.length, chargerInfo.isDbCodeEmpty());
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendFinishCharging:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 언플러그 전송 g1
     */
    public void sendUnplug() {
        try {
            j14_touch_2ch_Packet.Unplug_g1 request = new j14_touch_2ch_Packet.Unplug_g1();
            byte[] vdData = request.encode(chargerInfo);

            // db코드가 없으면 패킷을 보내지 않고 미전송으로 처리한다.
            sendRequest("g1", vdData, vdData.length, chargerInfo.isDbCodeEmpty());
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendUnplug:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    public void sendFirmwareDownReq() {
        try {
            chargerInfo.m1_idx = 0; // Index 초기화
            j14_touch_2ch_Packet.InfoDownReq_m1 request = new j14_touch_2ch_Packet.InfoDownReq_m1();
            sendRequest("m1", request.encode(chargerInfo, (byte)0x01, (byte)0x01)); // 기본 UI 만
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendFirmwareDownReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     *  암호화 key 요청 전송
     */
    public void sendEncryptionKeyReq() {
        try {
            j14_touch_2ch_Packet.EncryptionKey_k1 request = new j14_touch_2ch_Packet.EncryptionKey_k1();
            sendRequest("k1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendEncryptionKeyReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    /**
     *  충전기 운영 파라미터 요청 전송
     */
    public void sendChargerParameterReq() {
        try {
            j14_touch_2ch_Packet.ChargerParameter_l1 request = new j14_touch_2ch_Packet.ChargerParameter_l1();
            sendRequest("l1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendChargerParameterReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 버전 정보 요청 전송
     */
    public void sendVersionReq() {
        try {
            j14_touch_2ch_Packet.ChargerVersionReq_r1 request = new j14_touch_2ch_Packet.ChargerVersionReq_r1();
            sendRequest("r1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendVersionReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    /**
     * 일 마감 자료 전송
     */
    public void sendDayFinishReq() {
        try {
            j14_touch_2ch_Packet.DayFinishReq_s1 request = new j14_touch_2ch_Packet.DayFinishReq_s1();
            sendRequest("s1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendDayFinishReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    /**
     * 비정상 종료
     */
    public void sendAbnormalTemination() {
        try {
            j14_touch_2ch_Packet.AbnormalTermination_t1 request = new j14_touch_2ch_Packet.AbnormalTermination_t1();
            sendRequest("t1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendAbnormalTemination:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    /**
     * 미전문 패킷 전송
     */
    public void sendNonTransPacket(EvPacket packet) {
        lastNonTransPacket = packet;

        try {
            // 패킷을 재 인코딩 해서 보냄
            j14_touch_2ch_Packet.NonTransPacket_i1 request = new j14_touch_2ch_Packet.NonTransPacket_i1();
            sendRequest("i1", request.encode(chargerInfo, 1, packet.encode()));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendNonTransPacket:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    //======================================================================
    // 충전기 Request Ack Message 전송
    //======================================================================
    void onRecvAuthResp_1a(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.AuthenticationAck_1a ack = new j14_touch_2ch_Packet.AuthenticationAck_1a();
            ack.decode(packet.vd);

            starkoffCommManagerListener.onAuthResultEvent(ack.cardNum, ack.authResult);

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvAuthResp_1a:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvChargerStatusResp_1b(EvPacket packet) {
        // 미전송 전문 체크
        if ( isNonTransPacketSending == true ) nonTransPacketCheck();
    }

    void onRecvStartChargingResp_1d(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.StartChargingAck_1d ack = new j14_touch_2ch_Packet.StartChargingAck_1d();
            ack.decode(packet.vd);

            System.arraycopy(ack.dbCode,0, chargerInfo.dbCode,0, 2);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvStartChargingResp_1d:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvFinishChargingResp_1f(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.FinishChargingAck_1f ack = new j14_touch_2ch_Packet.FinishChargingAck_1f();
            ack.decode(packet.vd);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvFinishChargingResp_1f:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvNonTransResp_1i(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.NonTransPacketAck_1i ack = new j14_touch_2ch_Packet.NonTransPacketAck_1i();
            ack.decode(packet.vd);

            // 예외처리
            if ( lastNonTransPacket == null ) return;

            // 마지막 미전송 패킷 응답처리

            // 1. DB에 저장된 미전송 제거
            nonTransPacketRecord.removeRecord(lastNonTransPacket.getUniqueId());

            // 2. f1이 아닌겨우에 계속 요청(d->f까지 한 싸이클)
            if ( lastNonTransPacket.ins.equals("f1") ) {
                isNonTransPacketSending = false;
                lastNonTransPacket = null;
            }
            else {
                EvPacket newPacket = nonTransPacketRecord.getNextRecord();
                if (newPacket != null) {
                    sendNonTransPacket(newPacket);
                } else {
                    isNonTransPacketSending = false;
                    lastNonTransPacket = null;
                }
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvNonTransResp_1i:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }


    void onEncryptionResp_1k(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.EncryptionKeyReqAck_1k ack = new j14_touch_2ch_Packet.EncryptionKeyReqAck_1k();
            ack.decode(packet.vd);

            this.encryptionKey = ack.encryptionKey;
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onEncryptionResp_1k:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onChargerParameterResp_1l(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerParameterReqAck_1l ack = new j14_touch_2ch_Packet.ChargerParameterReqAck_1l();
            ack.decode(packet.vd);

            this.dayFinishTime = ack.dayFinishTime;     // 일 마감 시간
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargerParameterResp_1l:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvVersionResp_1r(EvPacket packet) {
        try {
            j14_touch_2ch_Packet.ChargerVersionReqAck_1r ack = new j14_touch_2ch_Packet.ChargerVersionReqAck_1r();
            ack.decode(packet.vd);

            if ( starkoffCommManagerListener != null ) starkoffCommManagerListener.onRecvVersionResp(ack.swVerInfo, ack.costVerInfo);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvVersionResp_1r:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    public void checkChangeDay() {
        FileUtil.pastDateLogRemove(baseFilePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제
    }

    public void dataToLog(byte[] data, int size, String tag) {
        String strLog = tag+">"+ ByteUtil.byteArrayToHexStringDiv(data, 0, size, '-');
        FileUtil.appendDateLog(baseFilePath+COMM_LOG_PATH, strLog);
    }

    //======================================================================
    // EvComm 통신 이벤트 처리
    //======================================================================
    @Override
    public void onSendStatusPeriod() {
        sendChargerStatus();
    }

    @Override
    public void onSystemTimeUpdate(Date d) {
        if (starkoffCommManagerListener != null) starkoffCommManagerListener.onSystemTimeUpdate(d);
    }

    @Override
    public void onCommConnected() {
        // 재접속시에 S/W 다운로드 다시 가능하도록 한다.
        isDowningInfo = false;

        // 첫번째 연결시에 패킷을 전송한다.
        if ( isFirstConnect == false ) {

            //PowerOn Alarm (c1)
            SendAlarm(AlarmCode.POWER_ON, AlarmCode.STATE_OCCUR);

            //버전 정보 (r1)
//            sendVersionReq();

            //설치 정보 (h1)
//            SendInstallationInfo();

            //단가 정보 (j1)
            SendCostUnitReq();

            //충전기 상태 (b1)
            sendChargerStatus();        //함수콜 위치 수정

            isFirstConnect = true;
        }

        if (starkoffCommManagerListener != null) starkoffCommManagerListener.onCommConnected();
    }

    @Override
    public void onCommDisconnected() {
        if (starkoffCommManagerListener != null) starkoffCommManagerListener.onCommDisconnected();
    }

    @Override
    public void onJoasCommRXRawData(byte[] rawData, int size) {
        dataToLog(rawData, size, "CommRX");
    }

    @Override
    public void onJoasCommTXRawData(byte[] rawData, int size) {
        dataToLog(rawData, size, "CommTX");
    }
}
