/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd.page;

public enum PageID {
    PAGE_READY(0),
    SELECT_AUTH(1),
    SELECT_SLOW(2),
    SELECT_FAST(3),
    CARD_TAG(4),
    AUTH_WAIT(5),
    CONNECTOR_WAIT(6),
    CONNECT_CAR_WAIT(7),
    CHARGING(8),
    UNPLUG(9),
    FINISH_CHARGING(10),
    INPUT_CELL_NUM(11),
    INPUT_AUTH_NUM(12),
    PAYMENT_PREPAY(13),
    PAYMENT_REAL_PAY(14),
    PAYMENT_CANCEL_PAY(15),
    KAKAO_QRCERT_WAIT(16),
    PAYMENT_PARTIAL_CANCEL_PAY(17),
    PAGE_END(18);

    int id;
    private PageID(int id) { this.id = id; }
    public int getID() { return id;}
    public boolean Compare(int i){return id == i;}
    public static PageID getValue(int _id)
    {
        PageID[] As = PageID.values();
        for(int i = 0; i < As.length; i++)
        {
            if(As[i].Compare(_id))
                return As[i];
        }
        return PageID.PAGE_END;
    }
}
