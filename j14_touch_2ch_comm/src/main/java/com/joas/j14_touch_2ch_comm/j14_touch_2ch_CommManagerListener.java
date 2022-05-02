/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 12. 14 오후 5:45
 *
 */

package com.joas.j14_touch_2ch_comm;

import java.util.Date;

public interface j14_touch_2ch_CommManagerListener {
    public void onSystemTimeUpdate(Date d);
    public void onCommConnected();
    public void onCommDisconnected();
    public void onAuthResultEvent(String cardNum, boolean isSuccess);
    public void onResetRequest(int kind);
    public void onRecvVersionResp(String swVersion, String costVersion);
    public void onCostInfoUpdateEvent(boolean isReservationCost);
    public void onFirmwareDownCompleted(byte[] fwDownData);
}
