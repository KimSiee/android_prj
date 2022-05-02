/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 10:47
 *
 */

package com.joas.joasui_iot_video.ui.page;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.VideoView;

import com.joas.joasui_iot_video.ui.R;
import com.joas.joasui_iot_video.ui.UIFlowManager;
import com.joas.utils.LogWrapper;

public class StartView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    VideoView advVideoView;

    public StartView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_start,this, true);

        initComponents();
    }

    void initComponents() {
        advVideoView = (VideoView) findViewById(R.id.advVideoView);
        advVideoView.setOnPreparedListener (new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        Button btStart = (Button) findViewById(R.id.btStart);
        btStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStart();
            }
        });

        advVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                LogWrapper.e("VideoView", "Can't start video");
                return true;
            }
        });
    }

    public void onStart() {
        flowManager.onPageStartEvent();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        String videoPath = Environment.getExternalStorageDirectory()+"/Movies/1.mp4";
        advVideoView.setVideoPath(videoPath);
        advVideoView.seekTo(0);

        advVideoView.start();
    }

    @Override
    public void onPageDeactivate() {
        advVideoView.stopPlayback();
    }
}
