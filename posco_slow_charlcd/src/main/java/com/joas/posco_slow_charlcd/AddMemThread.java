/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 6. 29. 오후 5:50
 *
 */

package com.joas.posco_slow_charlcd;

public class AddMemThread extends Thread{

    UIFlowManager flowManager;
    int listcnt = 0;
    String[] cardList;
    public static final String TAG = "AddMemThread";
    AddMemThread(UIFlowManager flowManager, int cnt, String[] cardlist){
        this.flowManager = flowManager;
        this.listcnt = cnt;
        this.cardList = cardlist;
    }
    @Override
    public void run() {
        super.run();

        flowManager.poscoCommManager.doAddMember(cardList, listcnt);

        flowManager.completeMemAdd();

    }
}
