/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:40
 */

package com.joas.ocpp.stack;

public class OCPPStackStatus {
    static public final int  REG_STATUS_REJECTED   = 0x00;
    static public final int  REG_STATUS_ACCEPTED = 0x01;
    static public final int  REG_STATUS_PENDING  = 0x02;

    public int regStatus = REG_STATUS_REJECTED;
}
