/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 5:02
 *
 */

package com.joas.volvo_touch_2ch_comm;

import android.content.Context;
import android.util.Log;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvCommManager;
import com.joas.evcomm.EvPacket;
import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import java.util.Date;

public class VolvoCommManager extends EvCommManager {
    public static final String TAG = "PoscoCommManager";
    public static final String INFODOWN_PATH = "/InfoDown/";
    public static final String INFODOWN_CH0_PATH = INFODOWN_PATH+"/CH0/";
    public static final String INFODOWN_CH1_PATH = INFODOWN_PATH+"/CH1/";
    public static final String COMM_LOG_PATH = "/Log";
    public static final int COMM_LOG_RECORD_DAYS = 180; // 180일(6개월)

    public static final int POSCO_F1_DBCODE_IDX = 50; //
    public static final int POSCO_G1_DBCODE_IDX = 21; //

    VolvoCommManagerListener poscoCommManagerListener = null;

    PoscoMemberManager memberManager;

    Volvo2chChargerInfo chargerInfo;
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

    int chinfo = 0;

    //@param timeout add by si. - 200812 : 서버 재접속 대기 시간
    public VolvoCommManager(Context context, String svrip, int svrport, String sid, String cid, int type, Volvo2chChargerInfo info, String filePath, int timeout, int channel)  {
        super(context, svrip, svrport, sid, cid, type, filePath, timeout);

        //미전송 저장 사용
        setUseNontransPacketRecord(true);

        chargerInfo = info;
        baseFilePath = filePath;
        if(chargerInfo.infoch == 0) infoFilePath = filePath + INFODOWN_CH0_PATH;
        else infoFilePath = filePath + INFODOWN_CH1_PATH;
//        infoFilePath = filePath + INFODOWN_PATH;

        this.chinfo = channel;

        memberManager = new PoscoMemberManager(infoFilePath, info, context);
        FileUtil.pastDateLogRemove(baseFilePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제
    }

    public void setVolvoCommManagerListener(VolvoCommManagerListener listener) { poscoCommManagerListener = listener; }

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
            PoscoPacket.BasicResponse response = new PoscoPacket.BasicResponse();
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
            PoscoPacket.ChargerModeRequestAck_1B response = new PoscoPacket.ChargerModeRequestAck_1B();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChargerModeReq_B1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvChangeModeReq_C1(EvPacket packet) {
        try {
            PoscoPacket.ChargerModeChange_C1 request = new PoscoPacket.ChargerModeChange_C1();
            request.decode(packet.vd);

            chargerInfo.cpMode[chargerInfo.infoch] = request.mode;

            PoscoPacket.ChargerModeChangeAck_1C response = new PoscoPacket.ChargerModeChangeAck_1C();
            sendReponse(packet, response.encode(chargerInfo));

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onChangeChargerMode(request.mode, chargerInfo.infoch);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvChangeModeReq_C1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvCostDownReqResp_G1_1j(EvPacket packet, boolean isRequestPacket) {
        try {
            PoscoPacket.CostInfoDown_G1_1j request = new PoscoPacket.CostInfoDown_G1_1j();
            request.decode(packet.vd);

            chargerInfo.memberCostUnit = request.memberCost;
            chargerInfo.memberCostUnitApplyDate = request.memberCostApplyDate;
            chargerInfo.nonMemberCostUnit = request.nonMemberCost;
            chargerInfo.nonMemberCostUnitApplyDate = request.nonMemberCostApplyDate;

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onRecvCostInfo(chargerInfo, chargerInfo.infoch);

            if ( isRequestPacket ) {
                PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
                sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCostDownReqResp_G1_1j:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvInfoDwon_K1(EvPacket packet) {
        try {
            PoscoPacket.DataDown_K1 request = new PoscoPacket.DataDown_K1();
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
                                poscoCommManagerListener.onFirmwareDownCompleted(infoDownData, chargerInfo.infoch);
                            else if (request.dest2 == (byte) 0x08) { // 예약 정보 다운로드 완료
                                // 파일저장
                                FileUtil.bufferToFile(infoFilePath, infoDownFilename, infoDownData, false);
                                memberManager.updateReservedInfoList();
                            }
                        }
                    }
                }
            }

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
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
            PoscoPacket.ResetRequest_I1 request = new PoscoPacket.ResetRequest_I1();
            request.decode(packet.vd);

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onResetRequest(request.kind);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvResetReq_I1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvOpenCloseTimeReq_L1(EvPacket packet) {
        try {
            PoscoPacket.OpenCloseTime_L1 request = new PoscoPacket.OpenCloseTime_L1();
            request.decode(packet.vd);

            chargerInfo.openTime = request.openTime;
            chargerInfo.closeTime = request.closeTime;

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvOpenCloseTimeReq_L1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvMeterValReq_M1(EvPacket packet) {
        PoscoPacket.BasicResponse response = new PoscoPacket.BasicResponse();
        sendReponse(packet, response.encode(chargerInfo));
    }

    void onChargerInfoChangeReq_N1(EvPacket packet) {
        try {
            PoscoPacket.ChargerInfoChange_N1 request = new PoscoPacket.ChargerInfoChange_N1();
            request.decode(packet.vd);

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, EvPacket.EV_MSG_RET_CODE_ACK, 0));

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onChargerInfoChangeReq(request.kind, request.changeVal, chargerInfo.infoch);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargerInfoChangeReq_N1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }
    //차지비 http download - ui fota, 회원파일일
    void onRecvInfDown_K2(EvPacket packet){

        boolean ret = false;

        PoscoPacket.DataDown_K2 request = new PoscoPacket.DataDown_K2();
        request.decode(packet.vd);

        if(!request.remoteURL.equals("")){
            chargerInfo.destFlag1 = request.dest1;
            chargerInfo.destFlag2 = request.dest2;
            chargerInfo.remoteURL = request.remoteURL.trim();

            if ( poscoCommManagerListener != null ) ret = poscoCommManagerListener.onReqInfoDownByHttp(request.dest1,request.dest2,request.remoteURL, chargerInfo.infoch);

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }
        else{
            ret = false;

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }


    }

    void onRemoteStartStopReq_Q1(EvPacket packet) {
        try {
            PoscoPacket.RemoteStartStop_Q1 request = new PoscoPacket.RemoteStartStop_Q1();
            request.decode(packet.vd);

            // 충전 시작/종료일 때 정보저장(
            if ( request.chargingCmd == 0x01 || request.chargingCmd == 0x02 || request.chargingCmd == 0x03 ) {
                chargerInfo.remote_cardNum = request.cardNum;
                chargerInfo.orderNum = request.orderNum;
                chargerInfo.remoteStartChargingTimeLimit = request.chargingTime; //(초단위)
//                chargerInfo.isRemoteCharge = true;
            }
            else if(request.chargingCmd == 0x05){
                //예약취소 관련 커맨드
//                chargerInfo.isRemoteCharge = false;
            }

            boolean ret = false;

            if ( poscoCommManagerListener != null ) ret = poscoCommManagerListener.onRemoteStartStop(request.chargingCmd, chargerInfo.infoch);

            PoscoPacket.BasicResponseCode response = new PoscoPacket.BasicResponseCode();
            sendReponse(packet, response.encode(chargerInfo, ret ? EvPacket.EV_MSG_RET_CODE_ACK : EvPacket.EV_MSG_RET_CODE_NAK, 0));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRemoteStartStopReq_Q1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onChargingStatusReq_R1(EvPacket packet) {
        try {

            PoscoPacket.ChargingStatusRequestAck_1R response = new PoscoPacket.ChargingStatusRequestAck_1R();
            sendReponse(packet, response.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onChargingStatusReq_R1:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onChargerInstallationInfoReq_U1(EvPacket packet) {
        try {

            PoscoPacket.ChargerInstallationInfoAck_1U response = new PoscoPacket.ChargerInstallationInfoAck_1U();
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
            PoscoPacket.AlarmInfo_c1 request = new PoscoPacket.AlarmInfo_c1();
            sendRequest("c1", request.encode(chargerInfo, code, status));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "SendAlarm:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * add by si. 200605 차지비 상태변화시 b1 전송 함수(폴트알림용)
     */
    public void SendFaultStat(int code, byte status) {
        try {
            //상태변화 저장
            SetFaultStatus(code, status);

            //상태변화시 b1 전송
            if (chargerInfo.cpStatus != chargerInfo.pre_cpStatus) {
                chargerInfo.pre_cpStatus = chargerInfo.cpStatus;

                chargerInfo.totMemnum = memberManager.getMemberNumber();
                PoscoPacket.ChargerStatus_b1 request = new PoscoPacket.ChargerStatus_b1();
                sendRequest("b1", request.encode(chargerInfo));
            }

        } catch (Exception e) {
            LogWrapper.e(TAG, "SendFaultAlarm:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }
    void SetFaultStatus(int code, byte status) {

        //도어 모니터링
        //내부통신이상
        if (code == AlarmCode.ERR_CODE_11 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFFFF7FFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000000080000L;

        //전력량계 통신이상
        if (code == AlarmCode.ERR_CODE_12 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFDFFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000020000000L;

        //비상스위치 모니터링
        if(code == AlarmCode.EMERGENCY && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFFFFFBFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000000004000L;

        //융착모니터링(릴레이,MC 융착)
        if(code == AlarmCode.ERR_CODE_9 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFDFFFFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000002000000000L;

        //rcd trip 모니터링
        if(code == AlarmCode.ERR_CODE_7 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFF7FFFFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000008000000000L;

        //접지오류(FG불량)
        if(code == AlarmCode.ERR_CODE_3 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFEFFFFFFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000100000000000L;

        //입력저전압
        if(code == AlarmCode.ERR_CODE_5 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFFBFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000004000000L;

        //입력과전압
        if(code == AlarmCode.ERR_CODE_6 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFFFFFFEFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000000000100L;

        //출력과전류
        if(code == AlarmCode.ERR_CODE_8 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFFFFFFFFF7FFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000000000000800L;

        //cp레벨상태 오류(차량통신이상)
        if(code == AlarmCode.ERR_CODE_10 && status == AlarmCode.STATE_OCCUR)
            chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] & 0xFFFFFBFFFFFFFFFFL;
        else chargerInfo.cpStatus[chargerInfo.infoch] = chargerInfo.cpStatus[chargerInfo.infoch] | 0x0000040000000000L;

    }

    /**
     * 충전기 설치정보 요청
     */
    public void SendInstallationInfo() {
        try {
            PoscoPacket.ChargerInstallationInfo_h1 request = new PoscoPacket.ChargerInstallationInfo_h1();

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
            PoscoPacket.CostInfoDownReq_j1 request = new PoscoPacket.CostInfoDownReq_j1();

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
            chargerInfo.totMemnum = memberManager.getMemberNumber();

            PoscoPacket.ChargerStatus_b1 request = new PoscoPacket.ChargerStatus_b1();
            sendRequest("b1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendChargerStatus:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    public void sendAuthReq() {
        try {
            PoscoPacket.Authentication_a1 request = new PoscoPacket.Authentication_a1();
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
            PoscoPacket.MissingPaymentCompleteReq_q1 request = new PoscoPacket.MissingPaymentCompleteReq_q1();
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
            PoscoPacket.CellNumAuthRequest_p1 request = new PoscoPacket.CellNumAuthRequest_p1();
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
            PoscoPacket.StartCharging_d1 request = new PoscoPacket.StartCharging_d1();
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
            PoscoPacket.ChargingStatus_e1 request = new PoscoPacket.ChargingStatus_e1();
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
            PoscoPacket.FinishCharging_f1 request = new PoscoPacket.FinishCharging_f1();
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
            PoscoPacket.Unplug_g1 request = new PoscoPacket.Unplug_g1();
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
            PoscoPacket.ChargerFirmwareReq_m1 request = new PoscoPacket.ChargerFirmwareReq_m1();
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
            PoscoPacket.ChargerVersionReq_r1 request = new PoscoPacket.ChargerVersionReq_r1();
            sendRequest("r1", request.encode(chargerInfo));
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "sendVersionReq:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    /**
     * 비정상 종료
     */
    public void sendAbnormalTemination() {
        try {
            PoscoPacket.AbnormalTermination_t1 request = new PoscoPacket.AbnormalTermination_t1();
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
            //만약 f1, g1이고 db코드가 empty하다면 현재 i1의 db코드를 채움
            if ( packet.ins.equals("f1") || packet.ins.equals("g1")) {
                if (packet.ins.equals("f1")) {
                    if (ByteUtil.isByteArrayAllZero(packet.vd, POSCO_F1_DBCODE_IDX, 8)) {
                        System.arraycopy(chargerInfo.dbCode_i1, 0, packet.vd, POSCO_F1_DBCODE_IDX, 8);
                    }
                }
                else if (packet.ins.equals("g1")) {
                    if (ByteUtil.isByteArrayAllZero(packet.vd, POSCO_G1_DBCODE_IDX, 8)) {
                        System.arraycopy(chargerInfo.dbCode_i1, 0, packet.vd, POSCO_G1_DBCODE_IDX, 8);
                    }
                    // i1 db 코드 초기화
                    chargerInfo.initDbCode_i1();
                }

            }
            else if ( packet.ins.equals("d1") ) {
                // i1 db 코드 초기화
                chargerInfo.initDbCode_i1();
            }

            // 패킷을 재 인코딩 해서 보냄
            PoscoPacket.NonTransPacket_i1 request = new PoscoPacket.NonTransPacket_i1();
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
            PoscoPacket.AuthenticationAck_1a ack = new PoscoPacket.AuthenticationAck_1a();
            ack.decode(packet.vd);

            poscoCommManagerListener.onAuthResultEvent(ack.authResult, (int)ack.memberKindCode, ack.costUnit, chargerInfo.infoch);

            // 단가 적용
            chargerInfo.curChargingCostUnit = ack.costUnit;
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
            PoscoPacket.CellNumAuthRequestAck_1p ack = new PoscoPacket.CellNumAuthRequestAck_1p();
            ack.decode(packet.vd);

            if (poscoCommManagerListener != null)
                poscoCommManagerListener.onRecvCellAuthResp(ack.recvAuthnum, ack.authResult, ack.recvPrepayApprovalNum, ack.recvPrepayDatetime, ack.recvPrepayPrice,
                        ack.recvusePrice);
            System.arraycopy(ack.dbcode, 0, chargerInfo.payment_dbcode, 0, 8);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onRecvCellNumAuthRequest_1p:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    void onRecvMissingPaymentResp_1q(EvPacket packet) {
        try {
            PoscoPacket.MissingPaymentCompleteRes_1q ack = new PoscoPacket.MissingPaymentCompleteRes_1q();
            ack.decode(packet.vd);

            if (poscoCommManagerListener != null)
                poscoCommManagerListener.onRecvMissingPaymentCompleteResp(ack.authResult, ack.recvPrepayApprovalNum, ack.recvPrepayDatetime
                        , ack.recvPrepayPrice, ack.recvusePrice);
            System.arraycopy(ack.dbcode,0,chargerInfo.payment_dbcode,0,8);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onRecvMissingPaymentResp_1q:" + e.toString() + ", " + Log.getStackTraceString(e));
        }
    }

    void onRecvStartChargingResp_1d(EvPacket packet) {
        try {
            PoscoPacket.StartChargingAck_1d ack = new PoscoPacket.StartChargingAck_1d();
            ack.decode(packet.vd);

            if(poscoCommManagerListener!=null) poscoCommManagerListener.onRecvStartChargingResp(ack.retCode,ack.reasonCode, chargerInfo.infoch);
            System.arraycopy(ack.dbCode,0, chargerInfo.dbCode,0, 8);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvStartChargingResp_1d:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvFinishChargingResp_1f(EvPacket packet) {
        try {
            PoscoPacket.FinishChargingAck_1f ack = new PoscoPacket.FinishChargingAck_1f();
            ack.decode(packet.vd);

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onRecvFinishChargingResp(ack.chargingCost, chargerInfo.infoch);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onRecvFinishChargingResp_1f:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }

    void onRecvNonTransResp_1i(EvPacket packet) {
        try {
            PoscoPacket.NonTransPacketAck_1i ack = new PoscoPacket.NonTransPacketAck_1i();
            ack.decode(packet.vd);

            // 예외처리
            if ( lastNonTransPacket == null ) return;

            // 마지막 미전송 패킷 응답처리

            // 1. d1인경우에 db 코드 저장
            if ( lastNonTransPacket.ins.equals("d1")) {
                System.arraycopy(ack.dbCode, 0, chargerInfo.dbCode_i1, 0, 8);
            }

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
            PoscoPacket.ChargerVersionReqAck_1r ack = new PoscoPacket.ChargerVersionReqAck_1r();
            ack.decode(packet.vd);

            if ( poscoCommManagerListener != null ) poscoCommManagerListener.onRecvVersionResp(ack.verinfo, chargerInfo.infoch);
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
        if (poscoCommManagerListener != null) poscoCommManagerListener.onSystemTimeUpdate(d);
    }

    @Override
    public void onCommConnected() {
        // 첫번째 연결시에 패킷을 전송한다.
        if ( isFirstConnect == false ) {

            int ch = chargerInfo.infoch;
            //PowerOn Alarm
            SendAlarm(AlarmCode.POWER_ON, AlarmCode.STATE_OCCUR);
            SendInstallationInfo();     //h1
            //SendCostUnitReq(); //G1만 사용..// Todo 확인 필요함

            //TOdo : c1(1029) 전송 구현
            SendAlarm(AlarmCode.DOWNLOAD_COMPLETE, AlarmCode.STATE_OCCUR);

            isFirstConnect = true;
        }
        sendChargerStatus(); // b1 전송


        if (poscoCommManagerListener != null) poscoCommManagerListener.onCommConnected(chargerInfo.infoch);
    }

    @Override
    public void onCommDisconnected() {
        if (poscoCommManagerListener != null) poscoCommManagerListener.onCommDisconnected(chargerInfo.infoch);
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
