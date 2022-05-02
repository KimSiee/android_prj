/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 26 오후 3:04
 *
 */

package com.joas.minsu_comm;

import java.util.Date;

public interface MinsuCommManagerListener {
    public void onSystemTimeUpdate(Date d);
    public void onCommConnected();
    public void onCommDisconnected();
    public void onAuthResultEventFromServer(boolean isSuccess, int reason);
    public void onResetRequest(int kind);
    public void onChargerInfoChangeReq(int kind, String value);
    public boolean onRemoteStartStop(int cmd);
    public void onRecvStartChargingResp(byte rspCode,byte rspReason);
    public void onRecvFinishChargingResp(int respcode, int reason);     //1f 수신시
    public void onRecvVersionResp(String version);
    public void onFirmwareDownCompleted(byte[] fwDownData);
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url);
    public void onChangeChargerMode(int mode);
    public void onRecvCostInfo(MinsuChargerInfo pinfo);
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal);
    public void onRecvMissingPaymentCompleteResp(byte authResult, String prepayApprovalnum, String prepayDatetime, int prepayPrice, int usePayReal);
}
