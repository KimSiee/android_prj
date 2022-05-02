/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 22 오후 2:04
 *
 */

package com.joas.utils;


public interface LogWrapperListener {
    void onRecvDebugMsg(LogWrapperMsg packet);
}
