/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui.page;

public enum PageID {
    SELECT_SLOW(0),
    SELECT_FAST(1),
    CARD_TAG(2),
    AUTH_WAIT(3),
    CONNECTOR_WAIT(4),
    CONNECT_CAR_WAIT(5),
    CHARGING(6),
    FINISH_CHARGING(7),
    PAGE_END(8);

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
