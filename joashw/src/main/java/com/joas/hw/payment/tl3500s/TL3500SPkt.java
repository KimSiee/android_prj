/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 12 오전 10:20
 *
 */

package com.joas.hw.payment.tl3500s;

import com.joas.utils.TimeUtil;

public class TL3500SPkt {
    public final static byte SOH = 0x01;
    public final static byte STX = 0x02;
    public final static byte ETX = 0x03;
    public final static byte EOT = 0x04;
    public final static byte ENQ = 0x05;
    public final static byte ACK = 0x06;	//정상
    public final static byte SYN = 0x16;
    public final static byte CR = 0x0d;
    public final static byte LF = 0x0a;
    public final static byte NACK = 0x15;	//오류

    public final static byte CMD_TX_TREMCHACK = (byte)'A';
    public final static byte CMD_TX_PAY = (byte)'B';
    public final static byte CMD_TX_PAY_G = (byte)'G';     //si.20180205
    public final static byte CMD_TX_PAYCANCEL = (byte)'C';
    public final static byte CMD_TX_SEARCH = (byte)'D';
    public final static byte CMD_TX_WAITNG = (byte)'E';
    public final static byte CMD_TX_UID = (byte)'F';
    public final static byte CMD_TX_RESET = (byte)'R';
    public final static byte CMD_TX_VERSION = (byte)'V';
    public final static byte CMD_TX_SET_CONFIG = (byte)'I';
    public final static byte CMD_TX_GET_CONFIG = (byte)'J';
    public final static byte CMD_TX_WRITE_CONFIG = (byte)'K';

    public final static byte CMD_RX_TREMCHACK = (byte)'a';
    public final static byte CMD_RX_PAY = (byte)'b';
    public final static byte CMD_RX_PAY_G = (byte)'g';     //si.20180205
    public final static byte CMD_RX_PAYCANCEL = (byte)'c';
    public final static byte CMD_RX_SEARCH = (byte)'d';
    public final static byte CMD_RX_WAITNG = (byte)'e';
    public final static byte CMD_RX_UID = (byte)'f';
    public final static byte CMD_RX_RESET = (byte)'r';
    public final static byte CMD_RX_EVENT = (byte)'@';
    public final static byte CMD_RX_VERSION = (byte)'v';
    public final static byte CMD_RX_SET_CONFIG = (byte)'i';
    public final static byte CMD_RX_GET_CONFIG = (byte)'j';
    public final static byte CMD_RX_WRITE_CONFIG = (byte)'k';
    public final static int HEADER_SIZE = 35;

    public String termID;
    public String dateTime;
    public byte jodCode = 0;
    public byte respCode = 0;
    public int dataLength = 0;
    public int retry = 0;
    public byte[] data = null;

    public TL3500SPkt() { }

    public TL3500SPkt(String tid, byte jcode) // Request Packet
    {
        termID = tid;
        dateTime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
        jodCode = jcode;
        respCode = 0;
    }
}
