/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 14 오전 11:29
 *
 */

package com.joas.hw.dsptest;

public interface DSPTestTXDataExInterface {
    void dspTestTxDataEncodeExt(int channel, byte[] rawData, int startOffset);
}
