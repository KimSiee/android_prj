/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch.page;

public enum PageID {
    PAGE_READY(0),
    SELECT_SLOW(1),
    CARD_TAG(2),
    AUTH_WAIT(3),
    CONNECTOR_WAIT(4),
    CONNECT_CAR_WAIT(5),
    CHARGING(6),
    PAGE_ASK_STOP_CHG(7),
    FINISH_CHARGING(8),
    UNPLUG(9),
    PAGE_END(10);

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
