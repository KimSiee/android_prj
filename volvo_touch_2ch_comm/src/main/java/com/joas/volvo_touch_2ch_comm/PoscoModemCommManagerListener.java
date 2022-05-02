/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 5:02
 *
 */

package com.joas.volvo_touch_2ch_comm;

public interface PoscoModemCommManagerListener {

    public void onRecvModemMDNInfo(String pnum);
    public void onRecvModemRSSiInfo(String rssi);
}
