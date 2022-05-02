/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.j14_touch_2ch.MultiChannelUIManager;
import com.joas.j14_touch_2ch.R;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class MessageSingleBoxView extends FrameLayout {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    MultiChannelUIManager uiManager;

    String singleMessageBoxTitle = "";
    String singleMessageBoxContent = "";
    int messageTimeout = 15;

    public MessageSingleBoxView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        uiManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_message_box, this, true);

        final View viewThis = this;
        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount == 0 ) {
                    viewThis .setVisibility(INVISIBLE);
                    if ( timer != null ) timer.cancel();
                }
            }
        });
    }

    public void setSingleMessageBoxTitle(String title) { singleMessageBoxTitle = title;}
    public void setSingleMessageBoxContent(String content) { singleMessageBoxContent = content; }
    public String getSingleMessageBoxTitle() { return singleMessageBoxTitle; }
    public String getSingleMessageBoxContent() { return singleMessageBoxContent; }
    public void setMessageTimeout(int timeout) { messageTimeout = timeout; }

    void initMessageBox() {
        TextView tvMessageBoxTitle = findViewById(R.id.tvMessageBoxTitle);
        tvMessageBoxTitle.setText(singleMessageBoxTitle);
        TextView tvMessageBoxContent = findViewById(R.id.tvMessageBoxContent);
        tvMessageBoxContent.setText(singleMessageBoxContent);
        startTimer();
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timerCount = messageTimeout;
        timer.start();
    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            initMessageBox();
        }
        else {
            if ( timer != null ) timer.cancel();
        }
    }

}