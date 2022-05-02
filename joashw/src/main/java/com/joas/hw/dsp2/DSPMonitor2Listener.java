/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 19 오후 4:44
 *
 */

package com.joas.hw.dsp2;


public interface DSPMonitor2Listener {
    void onDspTxDataEvent(int channel, DSPTxData2 txData);
    void onDspRxDataEvent(int channel, DSPRxData2 rxData);
}