/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 6. 22 오전 8:16
 *
 */

package com.joas.posco_comm;

import android.provider.BaseColumns;

public class MemberDatabase {
    public static final class MemberTable implements BaseColumns {
        public static final String CARD_NO = "card_no";
        public static final String TABLENAME = "poscomember";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +CARD_NO+" CHAR(32) PRIMARY KEY NOT NULL);";
    }
}
