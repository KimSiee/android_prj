/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui;

public class FaultInfo {
    public int id = 0;
    public int errorCode = 0;
    public int alarmCode = 0;
    public String errorMsg = "";
    public boolean isRepair = false;

    public FaultInfo() {

    }

    public FaultInfo(int faultId, int code, String msg, boolean repair, int alarmCode) {
        this.id = faultId;
        this.errorCode = code;
        this.errorMsg = msg;
        this.isRepair = repair;
        this.alarmCode = alarmCode;
    }
}
