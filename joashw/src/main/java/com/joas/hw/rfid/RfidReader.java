/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 9. 1 오후 12:33
 */

package com.joas.hw.rfid;

/**
 * Created by user on 2017-09-01.
 */

public abstract class RfidReader {
    protected RfidReaderListener rfidReaderListener = null;

    public void setRfidReaderEvent(RfidReaderListener listener) {
        rfidReaderListener = listener;
    }

    abstract public void rfidReadRequest();
    abstract public void rfidReadRelease();
    abstract public void destory();
    abstract public void stopThread();
}

