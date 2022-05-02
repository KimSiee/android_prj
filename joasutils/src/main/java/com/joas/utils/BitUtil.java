/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 9:31
 */

package com.joas.utils;

/**
 * Created by user on 2017-08-18.
 */

public class BitUtil {
    public static int setBit(int value, int index)
    {
        return value |= 1 << index;
    }
    public static int clearBit(int value, int index)
    {
        return value &= ~(1 << index);
    }

    public static long setBit(long value, int index)
    {
        return value |= 1 << index;
    }
    public static long clearBit(long value, int index)
    {
        return value &= ~(1 << index);
    }

    public static boolean getBitBoolean(int value, int index)
    {
        return (value & ( 1 << index )) != 0;
    }
}
