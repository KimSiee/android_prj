/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 8:38
 *
 */

package com.joas.joasui_iot_video.ui.page;

public enum PageID {
    SETTING(0),
    START(1),
    SELECT_SLOW(2),
    SELECT_FAST(3),
    CARD_TAG(4),
    AUTH_WAIT(5),
    CONNECTOR_WAIT(6),
    CONNECT_CAR_WAIT(7),
    CHARGING(8),
    FINISH_CHARGING(9),
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
