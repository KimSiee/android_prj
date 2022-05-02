/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 5:02
 *
 */

package com.joas.volvo_touch_2ch_comm;

import java.util.Date;

public interface VolvoCommManagerListener {
    public void onSystemTimeUpdate(Date d);
    public void onCommConnected(int ch);
    public void onCommDisconnected(int ch);
    public void onAuthResultEvent(boolean isSuccess, int reason, int costUnit, int ch);
    public void onResetRequest(int kind);
    public void onChargerInfoChangeReq(int kind, String value, int ch);
    public boolean onRemoteStartStop(int cmd, int ch);
    public void onRecvStartChargingResp(byte rspCode,byte rspReason, int ch);
    public void onRecvFinishChargingResp(int cost, int ch);     //1f 수신시
    public void onRecvVersionResp(String version, int ch);
    public void onFirmwareDownCompleted(byte[] fwDownData, int ch);
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url, int ch);
    public void onChangeChargerMode(int mode, int ch);
    public void onRecvCostInfo(Volvo2chChargerInfo pinfo, int ch);
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal);
    public void onRecvMissingPaymentCompleteResp(byte authResult, String prepayApprovalnum, String prepayDatetime, int prepayPrice, int usePayReal);
}
