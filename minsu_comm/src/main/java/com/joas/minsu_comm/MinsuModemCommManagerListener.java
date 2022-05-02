/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 6. 3 오후 3:38
 *
 */

package com.joas.minsu_comm;

public interface MinsuModemCommManagerListener {

    public void onRecvModemMDNInfo(String pnum);
    public void onRecvModemRSSiInfo(String rssi);
}
