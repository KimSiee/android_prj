/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:14
 *
 */

package com.joas.joasui_mobile_charger.ui.page;

public enum PageID {
    SETTING(0),
    SELECT_FAST(1),
    CONNECTOR_WAIT(2),
    CONNECT_CAR_WAIT(3),
    CHARGING(4),
    FINISH_CHARGING(5),
    PAGE_END(6);

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
