/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 11. 4 오전 10:12
 *
 */

package com.joas.kevcs.ui;

import android.app.Activity;
import android.content.Context;
import android.media.SoundPool;
import android.media.AudioManager;
import android.os.Handler;

import com.joas.utils.LogWrapper;

/*
한전 사운드 문구 정리
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

    SoundPool soundPool;

    int lastSoundId = 0;

    int[] poolArray;
    float volume = 1.0f;

    public SoundManager(Activity activity, CPConfig config) {
        mainActivity = activity;
        cpConfig = config;

        soundPool = new SoundPool.Builder()
                    .setMaxStreams(SoundKind.End.getID())
                    .build();
        poolArray = new int[SoundKind.End.getID()];

        poolArray[SoundKind.Ready.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.ready, 1);
        poolArray[SoundKind.CableSelect.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.cable_select, 1);
        poolArray[SoundKind.AuthSelect.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_select, 1);
        poolArray[SoundKind.AuthCard.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_card, 1);
        poolArray[SoundKind.AuthNumber.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_number, 1);
        poolArray[SoundKind.AuthPassword.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_password, 1);
        poolArray[SoundKind.SelectMethod.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.select_method, 1);
        poolArray[SoundKind.AuthCredit.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_credit, 1);
        poolArray[SoundKind.AuthCreditKwh.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_credit_kwh, 1);
        poolArray[SoundKind.CreditCardTag.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.credit_cardtag, 1);
        poolArray[SoundKind.AuthWait.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_wait, 1);
        poolArray[SoundKind.AuthCreditWait.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.auth_credit_wait, 1);
        poolArray[SoundKind.ConnectWait.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.connect_wait, 1);
        poolArray[SoundKind.Connecting.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.connecting, 1);
        poolArray[SoundKind.Charging.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.charging, 1);
        poolArray[SoundKind.AskStopCharging.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.ask_stop_charging, 1);
        poolArray[SoundKind.FinishCharging.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.finish_charging, 1);
        poolArray[SoundKind.ChargingError.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.charging_error, 1);
        poolArray[SoundKind.Unplug.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.unplug, 1);
        poolArray[SoundKind.Emergency.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.emergency, 1);
        poolArray[SoundKind.ConnectorOrg.getID()] = soundPool.load(mainActivity.getApplicationContext(), R.raw.connector_org, 1);

        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AudioManager am = (AudioManager) mainActivity.getSystemService(Context.AUDIO_SERVICE);
                        am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                                0);
                    }
                }, 30000);
            }
        });

        volume = ((float)cpConfig.soundVol)/10.0f;
    }

    public void setVolume(float vol) {
        volume = vol;
    }

    public void playSound(SoundKind kind) {
        if ( cpConfig.useSound == false ) return;

        stopLastPlay();
        if ( kind.getID() < SoundKind.End.getID()) {
            lastSoundId = soundPool.play(poolArray[kind.getID()], volume, volume, 1, 0, 1);
        }
    }

    public void playSound(SoundKind kind, float vol) {
        stopLastPlay();
        if ( kind.getID() < SoundKind.End.getID()) {
            lastSoundId = soundPool.play(poolArray[kind.getID()], vol, vol, 1, 0, 1);
        }
    }

    public void stopLastPlay() {
        soundPool.stop(lastSoundId);
    }
}
