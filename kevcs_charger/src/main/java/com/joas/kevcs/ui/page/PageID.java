/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kevcs.ui.page;

public enum PageID {
    NONE(0),
    START(1),
    SELECT_FAST(2),
    SELECT_SLOW(3),
    SELECT_AUTH(4),
    AUTH_MEMBER_CARD(5),
    INPUT_NUMBER(6),
    INPUT_PASSWORD(7),
    SELECT_AMOUNT_TYPE(8),
    INPUT_AMOUNT_COST(9),
    INPUT_AMOUNT_KWH(10),
    CREDIT_CARD_PAY(11),
    AUTH_PAY_WAIT(12),
    CONNECTOR_WAIT(13),
    CONNECT_CAR_WAIT(14),
    CHARGING(15),
    FINISH_CHARGING(16),
    FINISH_CHARGING_ERRROR(17),
    UNPLUG(18),
    CREDIT_REAL_PAY(19),
    CREDIT_CANCEL_PREPAY(20),
    PAGE_END(21);

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
