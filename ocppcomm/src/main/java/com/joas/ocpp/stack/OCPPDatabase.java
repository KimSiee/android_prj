/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:39
 */

package com.joas.ocpp.stack;

import android.provider.BaseColumns;

public final class OCPPDatabase {
    public static final class AuthCacheTable implements BaseColumns {
        public static final String IDTAG = "idtag";
        public static final String PARENTID = "parentid";
        public static final String STATUS = "status";
        public static final String EXPRIRED = "expired";
        public static final String TABLENAME = "authcache";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +"id integer primary key autoincrement, "
                        +IDTAG+" text not null , "
                        +PARENTID+" text null , "
                        +STATUS+" integer not null , "
                        +EXPRIRED+" integer null );";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+IDTAG+" ON "+TABLENAME+"("+IDTAG+");";
    }

    public static final class LocalAuthTable implements BaseColumns {
        public static final String IDTAG = "idtag";
        public static final String PARENTID = "parentid";
        public static final String STATUS = "status";
        public static final String EXPRIRED = "expired";
        public static final String TABLENAME = "localauth";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +"id integer primary key autoincrement, "
                        +IDTAG+" text not null , "
                        +PARENTID+" text null , "
                        +STATUS+" integer not null , "
                        +EXPRIRED+" integer null );";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+IDTAG+" ON "+TABLENAME+"("+IDTAG+");";
    }

    public static final class LocalConfigTable implements BaseColumns {
        public static final String KEY = "key";
        public static final String VALUE = "value";
        public static final String TYPE = "type";
        public static final String TABLENAME = "localconfig";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        + KEY + " text primary key , "
                        + VALUE + " text not null, "
                        + TYPE + " integer not null );";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+KEY+" ON "+TABLENAME+"("+KEY+");";
    }

    public static final class LostTransactionMessage implements BaseColumns {
        public static final String UNIQUEID = "uniqueid";
        public static final String ACTION = "action";
        public static final String PAYLOAD = "payload";
        public static final String STARTTIME= "startTime";
        public static final String TID = "tid";
        public static final String CONNECTOR_ID = "connectorid";
        public static final String TABLENAME = "losttransactionmsg";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +"id integer primary key autoincrement, "
                        + UNIQUEID +" text not null , "
                        +ACTION+" text null , "
                        +PAYLOAD+" text not null , "
                        +STARTTIME+" text not null , "
                        +TID+" integer not null , "
                        +CONNECTOR_ID+" integer null );";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+ UNIQUEID +" ON "+TABLENAME+"("+ UNIQUEID +");";
    }

    public static final class DiagnosticLogTable implements BaseColumns {
        public static final String TIMESTAMP = "timestamp";
        public static final String TYPE = "type";
        public static final String TAG = "tag";
        public static final String CONTENT = "content";
        public static final String TABLENAME = "diagnosticlog";

        public static final String CREATE_TABLE =
                "create table "+TABLENAME+"("
                        +"id integer primary key autoincrement, "
                        +TIMESTAMP+" text not null , "
                        +TYPE+" text not null , "
                        +TAG+" text not null , "
                        +CONTENT+" text not null );";
        public static final String CREATE_INDEX = "CREATE UNIQUE INDEX "+TABLENAME+TIMESTAMP+" ON "+TABLENAME+"("+TIMESTAMP+");";
    }
}
