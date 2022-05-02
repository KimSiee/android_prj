/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 19 오전 9:45
 *
 */

package com.joas.hw.dsptest;

public interface DSPTestMonitorListener {
    void onTestDspTxDataEvent(int channel, DSPTestTxData txData);
    void onTestDspRxDataEvent(int channel, DSPTestRxData rxData);
}
