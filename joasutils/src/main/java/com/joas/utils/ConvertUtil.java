/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 18 오후 4:00
 *
 */

package com.joas.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by user on 2018-01-18.
 */

public class ConvertUtil {
    public static int secTimeToBCDWord(int v) {
        return (((v/60) << 8) & 0xff00) | (v % 60);
    }

    public static long DateTimeToBCDLong(Date v) {
        long ret = 0, tmp;
        Calendar cal = Calendar.getInstance();
        cal.setTime(v);
        tmp = cal.get(Calendar.YEAR);
        ret = tmp << 40;
        tmp = cal.get(Calendar.MONTH)+1;
        ret |= tmp << 32;
        tmp = cal.get(Calendar.DAY_OF_MONTH);
        ret |= tmp << 24;
        tmp = cal.get(Calendar.HOUR_OF_DAY);
        ret |= tmp << 16;
        tmp = cal.get(Calendar.MINUTE);
        ret |= tmp << 8;
        tmp = cal.get(Calendar.SECOND);
        ret |= tmp;
        return ret;
    }
}
