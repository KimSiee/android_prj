/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 12 오전 10:20
 *
 */

package com.joas.hw.payment.tl3500s;

import java.util.Map;

public interface TL3500SListener {
    void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch);
}
