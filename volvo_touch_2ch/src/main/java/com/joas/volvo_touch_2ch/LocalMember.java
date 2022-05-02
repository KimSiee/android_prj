/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

import android.os.Environment;

import com.joas.utils.LogWrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;

public class LocalMember {
    public static final String TAG = "LocalMember";

    public static final String LOCMEMBER_FILE_NAME = "/member.txt";

    public String readCardNum = "0000000000000000";

    public LocalMember() {
    }


    //write member
    public boolean memberFileWrite(String cardnum) {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + TypeDefine.LOCAL_MEMBER_PATH;
            File file = new File(path);

            if (!file.exists()) {
                file.mkdir();
            }

            FileOutputStream fos = new FileOutputStream(path + LOCMEMBER_FILE_NAME, true);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos));

            bufferedWriter.write(cardnum);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
            fos.close();
            LogWrapper.e(TAG, "Member " + cardnum + " write Success!");

            return true;

        } catch (Exception e) {
            LogWrapper.e(TAG, "Member File write Error." + e.toString());
            return false;
        }
    }

    //search local member
    public boolean memberFileRead(String cardnum) {
        try {
            String path = Environment.getExternalStorageDirectory() + TypeDefine.LOCAL_MEMBER_PATH + "/" + LOCMEMBER_FILE_NAME;
            File file = new File(path);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals(cardnum)) {
                    LogWrapper.e(TAG, "Found Member " + cardnum + " Success!");
                    return true;
                }
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "Member File read Error." + e.toString());
        }
        LogWrapper.e(TAG, "Member " + cardnum + " Not exist.");
        return false;
    }
}
