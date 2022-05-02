/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 4:30
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.TypeDefine;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.kepcocomm.KepcoComm;

import java.io.File;

public class StartView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;

    TextView tvAppQR;
    ImageView imageQR;
    Activity mainActivity;

    public StartView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_start,this, true);

        initComponents();
    }

    void initComponents() {
        ImageButton btStart = (ImageButton) findViewById(R.id.btStart);

        btStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartClick();
            }
        });
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText(TypeDefine.SW_VERSION.toLowerCase());

        tvAppQR = findViewById(R.id.tvAppQR);
        imageQR = findViewById(R.id.imageQR);
        tvAppQR.setVisibility(INVISIBLE);
        imageQR.setVisibility(INVISIBLE);

        //기존에 QR 이미지가 있으면 보여준다.
        try {
            boolean isQRImgFind = false;
            String filePath = Environment.getExternalStorageDirectory()+TypeDefine.KEPCO_COMM_PATH+"/"+KepcoComm.FILE_QR_IMG+".png";
            File file = new File(filePath);
            if ( file.exists() == false ) {
                filePath = Environment.getExternalStorageDirectory()+TypeDefine.KEPCO_COMM_PATH+"/"+KepcoComm.FILE_QR_IMG+".jpg";
                if ( file.exists()) isQRImgFind = true;
            }
            else {
                isQRImgFind = true;
            }

            if ( isQRImgFind ) {
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                imageQR.setImageBitmap(bitmap);
                tvAppQR.setVisibility(VISIBLE);
                imageQR.setVisibility(VISIBLE);
            }
        }
        catch (Exception e) {}
    }

    public void onStartClick() {
        flowManager.onPageCommonEvent(PageEvent.START_CLICK);
    }

    public void onQRImageDownUpdate(final String qrFile) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeFile(qrFile);
                imageQR.setImageBitmap(bitmap);
                tvAppQR.setVisibility(VISIBLE);
                imageQR.setVisibility(VISIBLE);
                imageQR.invalidate();
            }
        });
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
    }

    @Override
    public void onPageDeactivate() {
    }
}
