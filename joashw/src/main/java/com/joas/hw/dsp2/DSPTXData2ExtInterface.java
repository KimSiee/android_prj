/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 5. 21 오전 10:49
 *
 */

package com.joas.hw.dsp2;

public interface DSPTXData2ExtInterface {
    void dspTxDataEncodeExt(int channel, byte[] rawData, int startOffset);
}
