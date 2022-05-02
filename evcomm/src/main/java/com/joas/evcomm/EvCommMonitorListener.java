/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 26 오후 5:08
 *
 */

package com.joas.evcomm;

/**
 * Created by user on 2017-12-26.
 */

public interface EvCommMonitorListener {
    void onRecvEvPacket(EvPacket packet);
    void onTransEvPacket(EvPacket packet);
}
