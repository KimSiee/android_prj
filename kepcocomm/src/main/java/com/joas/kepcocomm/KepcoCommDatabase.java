/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 9. 30 오후 2:04
 *
 */

package com.joas.kepcocomm;

import android.provider.BaseColumns;

public final class KepcoCommDatabase {
    public static final class CardMember implements BaseColumns {
        public static final String CARD_NO = "card_no";
        public static final String CARD_PWD = "card_pass";
        public static final String ADD_CL = "add_cl";
        public static final String TABLENAME = "cardmember";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +CARD_NO+" char(16) primary key not null, "
                        +CARD_PWD+" text null, "
                        +ADD_CL +" text not null);";
    }

    public static final class LostTrnsData implements BaseColumns {
        public static final String MSGID = "msgid";
        public static final String DATA = "data";
        public static final String TABLENAME = "losttransdata";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        + MSGID+" text primary key not null, "
                        + DATA +" text not null);";
    }
}
