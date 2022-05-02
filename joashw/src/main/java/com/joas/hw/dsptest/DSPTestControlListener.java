/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 19 오전 9:44
 *
 */

package com.joas.hw.dsptest;

public interface DSPTestControlListener {
    void onTestDspStatusChange(int channel, DSPTestRxData.STATUS500 idx, boolean val);
    void onTestDspMeterChange(int channel, long meterVal);
    void onTestDspCommErrorStstus(boolean isError);
}
