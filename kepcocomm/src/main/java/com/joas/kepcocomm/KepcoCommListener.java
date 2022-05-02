/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 19 오전 9:19
 *
 */

package com.joas.kepcocomm;

/**
 * Created by user on 2017-12-22.
 */

public interface KepcoCommListener {
    void onTimeUpdate(String strTime);
    void onOnlineModeAndPwdUpdate(String password);
    void onKepcoCommConnected();
    void onKepcoCommConnectedFirst();
    void onKepcoCommDisconnected();
    void onKepcoReadCardNum(String cardnum);
    void onKepcoCommAuthResult(boolean success);
    void onChargeCtl(KepcoChargeCtl chargeCtl);
    boolean onRemoteAuthReq(String connType);
    void onFinishUpdateDownload();
    void onQRImageDownFinish(String qrImgFile);
    void onCommLocalMode(boolean tf);
    void onPayResult(KepcoChargerInfo.KepcoPayRetInfo payRetInfo);
    void onPaymStatErr(boolean isError);
    void onKepcoTL3500CardEvent(String event);
    void onKepcoCommLogEvent(String tag, String logData);
}
