/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 22 오전 11:37
 *
 */

package com.joas.evcomm;

/**
 * Created by user on 2017-12-22.
 */

public interface EvCommListener {
    void onJoasCommRXRawData(byte[] rawData, int size);
    void onJoasCommTXRawData(byte[] rawData, int size);
    void onJoasCommPacketRecv(EvPacket packet);
    void onTimeUpdate(String strTime);
    void onEvCommConnected();
    void onEvCommDisconnected();
}
