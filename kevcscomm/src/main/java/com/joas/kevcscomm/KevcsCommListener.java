/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 19 오전 9:19
 *
 */

package com.joas.kevcscomm;

/**
 * Created by user on 2017-12-22.
 */

public interface KevcsCommListener {
    void onTimeUpdate(String strTime);
    void onOnlineModeAndPwdUpdate(String password);
    void onKevcsCommConnected();
    void onKevcsCommConnectedFirst();
    void onKevcsCommDisconnected();
    void onKevcsReadCardNum(String cardnum);
    void onKevcsCommAuthResult(boolean success);
    void onChargeCtl(KevcsChargeCtl chargeCtl);
    boolean onRemoteAuthReq(String connType);
    boolean onRemoteChargingStopReq();
    void onFinishUpdateDownload();
    void onQRImageDownFinish(String qrImgFile);
    void onCommLocalMode(boolean tf);
    void onPayResult(KevcsChargerInfo.KevcsPayRetInfo payRetInfo);
    void onPaymStatErr(boolean isError);
    void onKevcsTL3500CardEvent(String event);
    void onKevcsCommLogEvent(String tag, String logData);
}
