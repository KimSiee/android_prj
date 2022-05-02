/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 18 오후 1:46
 *
 */

package com.joas.minsu_comm;

import android.content.Context;
import android.util.Log;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvCommManager;
import com.joas.evcomm.EvPacket;
import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import java.util.Date;

public class MinsuCommManager extends EvCommManager {
    public static final String TAG = "MinsuCommManager";
    public static final String INFODOWN_PATH = "/InfoDown/";
    public static final String COMM_LOG_PATH = "/Log";
    public static final int COMM_LOG_RECORD_DAYS = 180; // 180일(6개월)

    public static final int MINSU_F1_DBCODE_IDX = 50; //
    public static final int MINSU_G1_DBCODE_IDX = 21; //

    MinsuCommManagerListener minsuCommManagerListener = null;

    MinsuMemberManager memberManager;

    MinsuChargerInfo chargerInfo;
    boolean isFirstConnect = false;
    boolean isDowningInfo = false;


    byte[] infoDownData = null;
    int infoDownBlockIdx = -1;
    int infoDownIdx = 0;
    String infoDownFilename = "";

    String infoFilePath = "";
    String baseFilePath = "";
    boolean isNonTransPacketSending = false;
    EvPacket lastNonTransPacket = null;


    //@param timeout add by si. - 200812 : 서버 재접속 대기 시간
    public MinsuCommManager(Context context, String svrip, int svrport, String sid, String cid, int type, MinsuChargerInfo info, String filePath, int timeout)  {
        super(context, svrip, svrport, sid, cid, type, filePath, timeout);

        //미전송 저장 사용
        setUseNontransPacketRecord(true);

        chargerInfo = info;
        baseFilePath = filePath;
        infoFilePath = filePath + INFODOWN_PATH;

        memberManager = new MinsuMemberManager(infoFilePath, info, context);
        FileUtil.pastDateLogRemove(baseFilePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제
    }

    public void setMinsuCommManagerListener(MinsuCommManagerListener listener) { minsuCommManagerListener = listener; }

    public boolean localAuthentication(String cardNum) {
        return memberManager.localAuthentication(cardNum);
    }

    public void getReservInfo()
    {
        memberManager.getNextReservedInfo();
    }

    public void doDelMember(String[] cardlist, int listcnt){memberManager.deleteMembers(cardlist,listcnt);}
    public void doAddMember(String[] cardlist, int listcnt){memberManager.addMembers(cardlist,listcnt);}
    public void doRemoveAllMember(){memberManager.removeMemberAll();}

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

            case "G1":
                onRecvCostDownReqResp_G1_1j(packet, true);
                break;

            case "I1":
                onRecvResetReq_I1(packet);
                break;

            case "K1":
                onRecvInfoDwon_K1(packet);
                break;

            case "K2":
                onRecvInfDown_K2(packet);
                break;

            case "L1":
                onRecvOpenCloseTimeReq_L1(packet);
                break;

            case "M1":
                onRecvMeterValReq_M1(packet);
                break;

            case "N1":
                onChargerInfoChangeReq_N1(packet);
                break;

            case "Q1":
                onRemoteStartStopReq_Q1(packet);
                break;

            case "R1":
                onChargingStatusReq_R1(packet);
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

            case "1p":
                onRecvCellNumAuthRequest_1p(packet);
                break;

            case "1q":
                onRecvMissingPaymentResp_1q(packet);
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
                break;

            case "1l":
                break;

            case "1m":
                break;

            case "1r":
                onRecvVersionResp_1r(packet);
                break;

            case "1s":
                break;
        }
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
            MinsuPacket.BasicResponse response = new MinsuPacket.BasicResponse();
            sendReponse(packet, response.encode(chargerInfo));

            // A1을 받으면 r1을 요청한다.(만약 업데이트 중이라면 생략)
            if (isDowningInfo == false) {
                sendVersionReq();
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChargerStatusReq_A1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvChargerModeReq_B1(EvPacket packet) {
        try {
            MinsuPacket.ChargerModeRequestAck_1B response = new MinsuPacket.ChargerModeRequestAck_1B();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChargerModeReq_B1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvChangeModeReq_C1(EvPacket packet) {
        try {
            MinsuPacket.ChargerModeChange_C1 request = new MinsuPacket.ChargerModeChange_C1();
            request.decode(packet.vd);

            chargerInfo.cpMode = request.mode;

            MinsuPacket.ChargerModeChangeAck_1C response = new MinsuPacket.ChargerModeChangeAck_1C();
            sendReponse(packet, response.encode(chargerInfo));

            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onChangeChargerMode(request.mode);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChangeModeReq_C1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvCostDownReqResp_G1_1j(EvPacket packet, boolean isRequestPacket) {
        try {
            MinsuPacket.CostInfoDown_G1_1j request = new MinsuPacket.CostInfoDown_G1_1j();
            request.decode(packet.vd);

            chargerInfo.costversion = request.version;
            chargerInfo.costxx = request.costunit;


            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onRecvCostInfo(chargerInfo);

            if ( isRequestPacket ) {
                MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
                sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCostDownReqResp_G1_1j:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvInfoDwon_K1(EvPacket packet) {
        try {
            MinsuPacket.DataDown_K1 request = new MinsuPacket.DataDown_K1();
            request.decode(packet.vd);

            // 다운로드 플레그(A1 에 대한 버전정보 요청 플레그)
            if ( request.div == (byte)0xF0 ) isDowningInfo = true;
            else if ( request.div == (byte)0xFF ) isDowningInfo = false;


            // 충전기 Fw이면 업데이트 다운로드 진행
            if ( request.dest1 == (byte)0x01 ) {
                // 회원 정보 다운로드 일때 별도 처리함
                if ((request.dest2 == (byte) 0x05) || (request.dest2 == (byte) 0x06) || (request.dest2 == (byte) 0x07)) {
                    memberManager.memberInfoDown(request.dest2, request.div, request.rawData, request.blockIdx);
                } else if (request.div == (byte) 0xF0) {
                    // 버퍼 생성
                    infoDownData = new byte[request.filesize];
                    infoDownIdx = 0;
                    infoDownBlockIdx = -1;
                    infoDownFilename = request.filename;
                } else {
                    if (infoDownBlockIdx < request.blockIdx) {
                        System.arraycopy(request.rawData, 0, infoDownData, infoDownIdx, request.rawData.length);
                        infoDownBlockIdx = request.blockIdx;
                        infoDownIdx += request.rawData.length;

                        if (request.div == (byte) 0xFF) {
                            if (request.dest2 == (byte) 0x01)
                                minsuCommManagerListener.onFirmwareDownCompleted(infoDownData);
                            else if (request.dest2 == (byte) 0x08) { // 예약 정보 다운로드 완료
                                // 파일저장
                                FileUtil.bufferToFile(infoFilePath, infoDownFilename, infoDownData, false);
                                memberManager.updateReservedInfoList();
                            }
                        }
                    }
                }
            }

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            //Send download complete flag(c1, 0x1029) - add by si. 20200605
            if ( request.div == (byte)0xFF ) SendAlarm(AlarmCode.DOWNLOAD_COMPLETE, AlarmCode.STATE_OCCUR);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvInfoDwon_K1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvResetReq_I1(EvPacket packet) {
        try {
            MinsuPacket.ResetRequest_I1 request = new MinsuPacket.ResetRequest_I1();
            request.decode(packet.vd);

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onResetRequest(request.kind);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvResetReq_I1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvOpenCloseTimeReq_L1(EvPacket packet) {
        try {
            MinsuPacket.OpenCloseTime_L1 request = new MinsuPacket.OpenCloseTime_L1();
            request.decode(packet.vd);

            chargerInfo.openTime = request.openTime;
            chargerInfo.closeTime = request.closeTime;

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvOpenCloseTimeReq_L1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvMeterValReq_M1(EvPacket packet) {
        MinsuPacket.BasicResponse response = new MinsuPacket.BasicResponse();
        sendReponse(packet, response.encode(chargerInfo));
    }

    void onChargerInfoChangeReq_N1(EvPacket packet) {
        try {
            MinsuPacket.ChargerInfoChange_N1 request = new MinsuPacket.ChargerInfoChange_N1();
            request.decode(packet.vd);

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onChargerInfoChangeReq(request.kind, request.changeVal);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargerInfoChangeReq_N1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }
    //차지비 http download - ui fota, 회원파일일
    void onRecvInfDown_K2(EvPacket packet){

        boolean ret = false;

        MinsuPacket.DataDown_K2 request = new MinsuPacket.DataDown_K2();
        request.decode(packet.vd);

        if(!request.remoteURL.equals("")){
            chargerInfo.destFlag1 = request.dest1;
            chargerInfo.destFlag2 = request.dest2;
            chargerInfo.remoteURL = request.remoteURL.trim();

            if ( minsuCommManagerListener != null ) ret = minsuCommManagerListener.onReqInfoDownByHttp(request.dest1,request.dest2,request.remoteURL);

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }
        else{
            ret = false;

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }


    }

    void onRemoteStartStopReq_Q1(EvPacket packet) {
        try {
            MinsuPacket.RemoteStartStop_Q1 request = new MinsuPacket.RemoteStartStop_Q1();
            request.decode(packet.vd);

            // 충전 시작/종료일 때 정보저장(
            if ( request.chargingCmd == 0x01 || request.chargingCmd == 0x02 || request.chargingCmd == 0x03 ) {
                chargerInfo.remote_cardNum = request.cardNum;
                chargerInfo.orderNum = request.orderNum;
                chargerInfo.remoteStartChargingTimeLimit = request.chargingTime; //(초단위)
//                chargerInfo.isRemoteCharge = true;        //edit by si. 211006 : 카드태깅,원격 UID가 혼용되어 올라가는 부분 수정
            }
            else if(request.chargingCmd == 0x05){
                //예약취소 관련 커맨드
//                chargerInfo.isRemoteCharge = false;       //edit by si. 211006 : 카드태깅,원격 UID가 혼용되어 올라가는 부분 수정
            }

            boolean ret = false;

            if ( minsuCommManagerListener != null ) ret = minsuCommManagerListener.onRemoteStartStop(request.chargingCmd);

            MinsuPacket.BasicResponseCode response = new MinsuPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRemoteStartStopReq_Q1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onChargingStatusReq_R1(EvPacket packet) {
        try {

            MinsuPacket.ChargingStatusRequestAck_1R response = new MinsuPacket.ChargingStatusRequestAck_1R();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargingStatusReq_R1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onChargerInstallationInfoReq_U1(EvPacket packet) {
        try {

            MinsuPacket.ChargerInstallationInfoAck_1U response = new MinsuPacket.ChargerInstallationInfoAck_1U();
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
            MinsuPacket.AlarmInfo_c1 request = new MinsuPacket.AlarmInfo_c1();
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
            MinsuPacket.ChargerInstallationInfo_h1 request = new MinsuPacket.ChargerInstallationInfo_h1();

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
            MinsuPacket.CostInfoDownReq_j1 request = new MinsuPacket.CostInfoDownReq_j1();

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
        if (chargerInfo.cpMode != 0x09) {       //add by si.충전중일때는 b1 보내지 않기
            try {
                MinsuPacket.ChargerStatus_b1 request = new MinsuPacket.ChargerStatus_b1();
                sendRequest("b1", request.encode(chargerInfo));
            } catch (Exception e) {
                LogWrapper.e(TAG, "sendChargerStatus:" + e.toString() + ", " + Log.getStackTraceString(e));
            }
        }
    }

    public void sendAuthReq() {
        try {
            MinsuPacket.Authentication_a1 request = new MinsuPacket.Authentication_a1();
            sendRequest("a1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendAuthReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 차지비 비회원 미처리 결제건 처리 완료 전문 요청(q1)
     */
    public void sendMissingPaymentCompleteReq(){
        try{
            MinsuPacket.MissingPaymentCompleteReq_q1 request = new MinsuPacket.MissingPaymentCompleteReq_q1();
            sendRequest("q1",request.encode(chargerInfo));

        } catch (Exception e){
            LogWrapper.e(TAG, "sendMissingPaymentCompleteReq:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    /**
     * 차지비 비회원 핸드폰번호(p1)에 대한 인증번호 요청
     */
    public void sendCellphoneAuthReq() {
        try {
            MinsuPacket.CellNumAuthRequest_p1 request = new MinsuPacket.CellNumAuthRequest_p1();
            sendRequest("p1", request.encode(chargerInfo));

        } catch (Exception e) {
            LogWrapper.e(TAG, "sendCellphoneAuthReq:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    /**
     * 충전 시작 메시지 전송 d1
     */
    public void sendStartCharging() {
        try {
            MinsuPacket.StartCharging_d1 request = new MinsuPacket.StartCharging_d1();
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
            MinsuPacket.ChargingStatus_e1 request = new MinsuPacket.ChargingStatus_e1();
            sendRequest("e1", request.encode(chargerInfo));
            LogWrapper.v(TAG, "sendChargingStatus(e1)");
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendChargingStatusError:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 충전 완료 전송 f1
     */
    public void sendFinishCharging() {
        try {
            MinsuPacket.FinishCharging_f1 request = new MinsuPacket.FinishCharging_f1();
            sendRequest("f1", request.encode(chargerInfo));
            LogWrapper.v(TAG, "sendChargingStatus(f1)");

//            // db코드가 없으면 패킷을 보내지 않고 미전송으로 처리한다.
//            byte[] vdData = request.encode(chargerInfo);
//            sendRequest("f1", vdData, vdData.length, chargerInfo.isDbCodeEmpty());
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
            MinsuPacket.Unplug_g1 request = new MinsuPacket.Unplug_g1();
            sendRequest("g1", request.encode(chargerInfo));
            LogWrapper.v(TAG, "sendChargingStatus(g1)");

//            // db코드가 없으면 패킷을 보내지 않고 미전송으로 처리한다.
//            byte[] vdData = request.encode(chargerInfo);
//            sendRequest("g1", vdData, vdData.length, chargerInfo.isDbCodeEmpty());
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendUnplug:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    public void sendFirmwareDownReq() {
        try {
            MinsuPacket.ChargerFirmwareReq_m1 request = new MinsuPacket.ChargerFirmwareReq_m1();
            sendRequest("m1", request.encode(chargerInfo, (byte)0x01, (byte)0x01)); // 기본 UI 만
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendFirmwareDownReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 버전 정보 요청 전송
     */
    public void sendVersionReq() {
        try {
            MinsuPacket.ChargerVersionReq_r1 request = new MinsuPacket.ChargerVersionReq_r1();
            sendRequest("r1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendVersionReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 비정상 종료(원격충전 시도후 종료됐을 경우)
     */
    public void sendAbnormalTemination() {
        try {
            MinsuPacket.AbnormalTermination_t1 request = new MinsuPacket.AbnormalTermination_t1();
            sendRequest("t1", request.encode(chargerInfo));
            LogWrapper.v(TAG, "sendAbnormarTermination:"+chargerInfo.remote_cardNum);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendAbnormalTemination:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    /**
     * 미전송 전문 패킷 전송
     */
    public void sendNonTransPacket(EvPacket packet) {
        lastNonTransPacket = packet;

        try {

            // 패킷을 재 인코딩 해서 보냄
            MinsuPacket.NonTransPacket_i1 request = new MinsuPacket.NonTransPacket_i1();
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
            MinsuPacket.AuthenticationAck_1a ack = new MinsuPacket.AuthenticationAck_1a();
            ack.decode(packet.vd);

            minsuCommManagerListener.onAuthResultEventFromServer(ack.authResult, 0);

//            // 단가 적용
//            chargerInfo.curChargingCostUnit = ack.costUnit;
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvAuthResp_1a:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvChargerStatusResp_1b(EvPacket packet) {
        // 미전송 전문 체크
        if ( isNonTransPacketSending == false ) nonTransPacketCheck();
    }

    void onRecvCellNumAuthRequest_1p(EvPacket packet) {
        try {
            MinsuPacket.CellNumAuthRequestAck_1p ack = new MinsuPacket.CellNumAuthRequestAck_1p();
            ack.decode(packet.vd);

            if (minsuCommManagerListener != null)
                minsuCommManagerListener.onRecvCellAuthResp(ack.recvAuthnum, ack.authResult, ack.recvPrepayApprovalNum, ack.recvPrepayDatetime, ack.recvPrepayPrice,
                        ack.recvusePrice);
            System.arraycopy(ack.dbcode, 0, chargerInfo.payment_dbcode, 0, 8);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCellNumAuthRequest_1p:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    void onRecvMissingPaymentResp_1q(EvPacket packet) {
        try {
            MinsuPacket.MissingPaymentCompleteRes_1q ack = new MinsuPacket.MissingPaymentCompleteRes_1q();
            ack.decode(packet.vd);

            if (minsuCommManagerListener != null)
                minsuCommManagerListener.onRecvMissingPaymentCompleteResp(ack.authResult, ack.recvPrepayApprovalNum, ack.recvPrepayDatetime
                        , ack.recvPrepayPrice, ack.recvusePrice);
            System.arraycopy(ack.dbcode,0,chargerInfo.payment_dbcode,0,8);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onRecvMissingPaymentResp_1q:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    void onRecvStartChargingResp_1d(EvPacket packet) {
        try {
            MinsuPacket.StartChargingAck_1d ack = new MinsuPacket.StartChargingAck_1d();
            ack.decode(packet.vd);

            if(minsuCommManagerListener!=null) minsuCommManagerListener.onRecvStartChargingResp(ack.retCode,ack.reasonCode);
            System.arraycopy(ack.dbCode,0, chargerInfo.dbCode,0, 2);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvStartChargingResp_1d:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvFinishChargingResp_1f(EvPacket packet) {
        try {
            MinsuPacket.FinishChargingAck_1f ack = new MinsuPacket.FinishChargingAck_1f();
            ack.decode(packet.vd);

            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onRecvFinishChargingResp(ack.retCode, ack.reasonCode);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvFinishChargingResp_1f:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvNonTransResp_1i(EvPacket packet) {
        try {
            MinsuPacket.NonTransPacketAck_1i ack = new MinsuPacket.NonTransPacketAck_1i();
            ack.decode(packet.vd);

            // 예외처리
            if ( lastNonTransPacket == null ) return;

            // 마지막 미전송 패킷 응답처리

            // 2. DB에 저장된 미전송 제거
            nonTransPacketRecord.removeRecord(lastNonTransPacket.getUniqueId());

            // 3. g1이 아닌겨우에 계속 요청(d->f->g 까지 한 싸이클)
            if ( lastNonTransPacket.ins.equals("g1")) {
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

    void onRecvVersionResp_1r(EvPacket packet) {
        try {
            MinsuPacket.ChargerVersionReqAck_1r ack = new MinsuPacket.ChargerVersionReqAck_1r();
            ack.decode(packet.vd);

            if ( minsuCommManagerListener != null ) minsuCommManagerListener.onRecvVersionResp(ack.verinfo);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvFinishChargingResp_1f:" + e.toString()+", "+ Log.getStackTraceString(e));
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
        if (minsuCommManagerListener != null) minsuCommManagerListener.onSystemTimeUpdate(d);
    }

    @Override
    public void onCommConnected() {
        // 첫번째 연결시에 패킷을 전송한다.
        if ( isFirstConnect == false ) {

            //PowerOn Alarm
            SendAlarm(AlarmCode.POWER_ON, AlarmCode.STATE_OCCUR);
            SendInstallationInfo();     //h1
            SendCostUnitReq();          //j1

//            //TOdo : c1(1029) 전송 구현
//            SendAlarm(AlarmCode.DOWNLOAD_COMPLETE, AlarmCode.STATE_OCCUR);

            isFirstConnect = true;
        }
        sendChargerStatus(); // b1 전송


        if (minsuCommManagerListener != null) minsuCommManagerListener.onCommConnected();
    }

    @Override
    public void onCommDisconnected() {
        if (minsuCommManagerListener != null) minsuCommManagerListener.onCommDisconnected();
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
