/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 2. 14 오후 2:17
 *
 */

package com.joas.certtest;

public class PowerModuleInfo {
    public float outVoltage = 0;
    public float outAmpare = 0;
    public long statusInfo = 0;
    public float inpTemp = 0;
    public float pfcTemp = 0;
    public float dcdcATemp = 0;
    public float dcdcBTemp = 0;

    @Override
    public String toString() {
        return  String.format("%.2f", outVoltage) + ", "+String.format("%.2f", outAmpare)+", "+
                String.format("0x%08X", statusInfo)+", "+String.format("%.2f", inpTemp)+", "+
                String.format("%.2f", pfcTemp) + ", "+String.format("%.2f", dcdcATemp)+", "+
                String.format("%.2f", dcdcBTemp);
    }
}
