/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 21 오후 4:01
 */

package com.joas.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ByteUtil {
    public static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3 & 0xff) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    public static int makeInt(byte[] arr, int idx, boolean isBigEndian) {
        if ( isBigEndian ) return makeInt(arr[idx], arr[idx+1], arr[idx+2], arr[idx+3]);
        else return makeInt(arr[idx+3], arr[idx+2], arr[idx+1], arr[idx]);
    }

    public static long makeLong(byte b3, byte b2, byte b1, byte b0) {
        return (((b3 & 0xFFL) << 24) |
                ((b2 & 0xFFL) << 16) |
                ((b1 & 0xFFL) <<  8) |
                ((b0 & 0xFFL)      ));
    }

    public static int makeWord(byte b1, byte b0) {
        return ((b1 & 0xff) <<  8) | (b0 & 0xff);
    }

    public static boolean arrayCompare(byte[] a, int offseta, byte[] b, int offsetb, int n) {
        for(int i = 0; i < n; i++)
            if(a[offseta+i] != b[offsetb+i])
                return false;
        return true;
    }

    public static String byteArrayToHexStringDiv(byte[] a, int offset, int size, char div) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for ( int i=0; i<size; i++ ) {
            sb.append(String.format("%02X", a[i + offset], div));
            if ( i < (size-1) )sb.append(""+div);
        }
        return sb.toString();
    }

    public static String byteArrayToHexString(byte[] a, int offset, int size) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for ( int i=0; i<size; i++ )
            sb.append(String.format("%02X", a[i+offset]));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static void longToByteArray(long v, byte[] b, int size, int offset) {
        for (int i = 0; i < size; ++i) {
            b[offset+i] = (byte) (v >> ((size - i - 1) << 3));
        }
    }

    public static void wordToByteArray(int v, byte[] b, int offset) {
        b[offset+0] = (byte) ((v >> 8) & 0xff);
        b[offset+1] =(byte) ((v >> 0) & 0xff);
    }

    public static void intToByteArray(int v, byte[] b, int offset) {
        b[offset+0] = (byte) ((v >> 24) & 0xff);
        b[offset+1] = (byte) ((v >> 16) & 0xff);
        b[offset+2] = (byte) ((v >> 8) & 0xff);
        b[offset+3] =(byte) ((v >> 0) & 0xff);
    }

    public static void intToByteArray3(int v, byte[] b, int offset) {
        b[offset+0] = (byte) ((v >> 16) & 0xff);
        b[offset+1] = (byte) ((v >> 8) & 0xff);
        b[offset+2] =(byte) ((v >> 0) & 0xff);
    }

    public static void GetDectoHexBCDFormat(byte[] decim){
        byte result_h = 0;
        byte result = 0;
        int i = 0;
        for (byte bcd : decim)
        {
            result_h = (byte) (bcd / 10);
            result = (byte)(bcd % 10);
            result |= (byte)(result_h << 4);

            decim[i++] = (byte)result;
        }
    }

    public static byte[] decimalToBcd(long num) {
        int digits = 0;

        long temp = num;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }

        int byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;

        byte bcd[] = new byte[byteLen];

        for (int i = 0; i < digits; i++) {
            byte tmp = (byte) (num % 10);

            if (i % 2 == 0) {
                bcd[i / 2] = tmp;
            } else {
                bcd[i / 2] |= (byte) (tmp << 4);
            }

            num /= 10;
        }

        for (int i = 0; i < byteLen / 2; i++) {
            byte tmp = bcd[i];
            bcd[i] = bcd[byteLen - i - 1];
            bcd[byteLen - i - 1] = tmp;
        }

        return bcd;
    }

    public static byte[] decimalToBcd(long num, int minSize) {
        byte[] srcBuf = decimalToBcd(num);      //
        if ( srcBuf.length < minSize ) {
            byte[] dstBuf = new byte[minSize];
            System.arraycopy(srcBuf, 0, dstBuf, 0, srcBuf.length);
            return dstBuf;
        }
        return srcBuf;
    }

    public static long bcdToDecimal(byte[] bcd) {
        return Long.valueOf(bcdToString(bcd));
    }

    public static String bcdToString(byte bcd) {
        StringBuffer sb = new StringBuffer();

        byte high = (byte) (bcd & 0xf0);
        high >>>= (byte) 4;
        high = (byte) (high & 0x0f);
        byte low = (byte) (bcd & 0x0f);

        sb.append(high);
        sb.append(low);

        return sb.toString();
    }

    public static String bcdToString(byte[] bcd) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < bcd.length; i++) {
            sb.append(bcdToString(bcd[i]));
        }

        return sb.toString();
    }


    public static byte[] dateToBCD(Date time, String format) {
        DateFormat sdf = new SimpleDateFormat(format);
        long tmpVal = Long.valueOf(sdf.format(time));
        return decimalToBcd(tmpVal);
    }

    public static boolean isByteArrayAllZero(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isByteArrayAllZero(byte[] data, int offset, int size) {
        int end = offset+size;
        for (int i = offset; i<end; i++) {
            if (data[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
