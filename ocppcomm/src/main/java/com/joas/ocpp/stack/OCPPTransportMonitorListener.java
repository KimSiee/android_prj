/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 26 오후 3:59
 *
 */

package com.joas.ocpp.stack;

public interface OCPPTransportMonitorListener {
    void onOCPPTransportRecvRaw(String data);
    void onOCPPTransportSendRaw(String data);
    void onOCPPTransportConnected();
    void onOCPPTransportDisconnected();
}
