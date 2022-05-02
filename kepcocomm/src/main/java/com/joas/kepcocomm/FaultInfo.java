/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 11 오전 9:47
 *
 */

package com.joas.kepcocomm;

import com.joas.utils.TimeUtil;

public class FaultInfo {
    public static final int FAULT_OCCUR = 1;
    public static final int FAULT_REPAIR = 2;
    public static final int FAULT_OCCUR_END = 3;

    public int id = 0;
    public String errorMsg = "";
    public int tp=0; // 구분
    public int cd=0; // 고장코드
    public boolean isRepair = false;
    public String occurDate = "";

    public FaultInfo() {
        occurDate = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
    }

    public FaultInfo(int faultId, String msg, boolean repair, int tp, int cd) {
        this(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss"), faultId, msg, repair, tp, cd);
    }

    public FaultInfo(String occurDate, int faultId, String msg, boolean repair, int tp, int cd) {
        this.id = faultId;
        this.errorMsg = msg;
        this.isRepair = repair;
        this.tp = tp;
        this.cd = cd;

        this.occurDate = occurDate;
    }
}
