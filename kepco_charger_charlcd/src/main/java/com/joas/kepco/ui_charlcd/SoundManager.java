/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kepco.ui_charlcd;

import android.app.Activity;

/*
한전 사운드 문구 정리
CharLCD 버전 Sound 기능 삭제
*/


public class SoundManager {
    public enum SoundKind {
        Ready(0),
        CableSelect(1),
        AuthSelect(2),
        AuthCard(3),
        AuthNumber(4),
        AuthPassword(5),
        SelectMethod(6),
        AuthCredit(7),
        AuthCreditKwh(8),
        CreditCardTag(9),
        AuthWait(10),
        AuthCreditWait(11),
        ConnectWait(12),
        Connecting(13),
        Charging(14),
        AskStopCharging(15),
        FinishCharging(16),
        ChargingError(17),
        Unplug(18),
        Emergency(19),
        ConnectorOrg(20),
        End(21);

        int id;

        private SoundKind(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static SoundKind getValue(int _id) {
            SoundKind[] As = SoundKind.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return SoundKind.End;
        }
    }

    Activity mainActivity;
    CPConfig cpConfig;

    int lastSoundId = 0;

    int[] poolArray;
    float volume = 1.0f;

    public SoundManager(Activity activity, CPConfig config) {
        mainActivity = activity;
        cpConfig = config;
        volume = ((float)cpConfig.soundVol)/10.0f;
    }

    public void setVolume(float vol) {
        volume = vol;
    }

    public void playSound(SoundKind kind) {
    }

    public void playSound(SoundKind kind, float vol) {
    }

    public void stopLastPlay() {

    }
}
