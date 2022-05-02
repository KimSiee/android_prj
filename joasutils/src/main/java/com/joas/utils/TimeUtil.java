/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 8. 13 오후 3:02
 *
 */

package com.joas.utils;

import java.text.SimpleDateFormat;

/**
 * Created by user on 2018-08-13.
 */

public class TimeUtil {
    public static String getCurrentTimeAsString(String format) {
        return new SimpleDateFormat(format).format(new java.util.Date());
    }

    public static String getDateAsString(String format, java.util.Date date) {
        return new SimpleDateFormat(format).format(date);
    }
}
