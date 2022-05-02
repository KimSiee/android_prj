/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 8. 12. 오후 2:57
 *
 */

package com.joas.volvo_touch_2ch;

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

        flowManager.volvoCommManager.doAddMember(cardList, listcnt);

        flowManager.completeMemAdd();

    }
}
