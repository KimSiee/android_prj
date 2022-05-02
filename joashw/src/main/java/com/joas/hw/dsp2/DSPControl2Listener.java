/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 9 오후 2:41
 *
 */

package com.joas.hw.dsp2;

/**
 * Created by user on 2018-01-09.
 */

public interface DSPControl2Listener {
    void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val);
    void onDspMeterChange(int channel, long meterVal);
    void onDspCommErrorStstus(boolean isError);
}
