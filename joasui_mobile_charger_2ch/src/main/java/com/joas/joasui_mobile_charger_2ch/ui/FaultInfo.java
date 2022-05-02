/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:16
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui;

public class FaultInfo {
    public int id = 0;
    public int errorCode = 0;
    public String errorMsg = "";
    public boolean isRepair = false;

    public FaultInfo() {

    }

    public FaultInfo(int faultId, int code, String msg, boolean repair) {
        this.id = faultId;
        this.errorCode = code;
        this.errorMsg = msg;
        this.isRepair = repair;
    }
}
