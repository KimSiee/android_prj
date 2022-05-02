/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 4. 8 오전 10:59
 *
 */

package com.joas.evcomm;

import android.provider.BaseColumns;

public class EVCommDatabase {
    public static final class NonTransPacketTable implements BaseColumns {
        public static final String ID = "id";
        public static final String INS = "ins";
        public static final String DATA = "data";
        public static final String TABLENAME = "nontranspacket";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +ID+" text not null, "
                        +INS+" text not null, "
                        +DATA+" text null);";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+ID+" ON "+TABLENAME+"("+ID+");";
    }
}
