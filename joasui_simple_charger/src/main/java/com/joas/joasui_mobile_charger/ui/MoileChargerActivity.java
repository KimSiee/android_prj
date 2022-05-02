/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:04
 *
 */

package com.joas.joasui_mobile_charger.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.joas.joasui_mobile_charger.ui.page.PageEvent;
import com.joas.joasui_mobile_charger.ui.page.PageID;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MoileChargerActivity  extends ImmersiveAppCompatActivity {
    PageManger pageManger;
    UIFlowManager flowManager;

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData chargeData;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Force Close 이벤트 발생시 처리 (개발시에는 끄고/ 릴리즈시에 적용)
        /*
        Thread.setDefaultUncaughtExceptionHandler(
                new ForceCloseHandler(  this,
                        Environment.getExternalStorageDirectory()+TypeDefine.FORCE_CLOSE_LOG_PATH,
                        true));
                        */

        hideNavBar();

        setContentView(R.layout.activity_moile_charger);

        pageManger = new PageManger(this.getBaseContext());

        cpConfig = new CPConfig();
        chargeData = new ChargeData();

        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        flowManager = new UIFlowManager(this, chargeData, cpConfig);

        pageManger.init(flowManager, this);
        flowManager.setPageManager(pageManger);

        initButtonEvent();

        setNavigationBarHomeIconDisable(true);
    }

    void initButtonEvent() {
        ImageButton btHome  = (ImageButton) findViewById(R.id.btEvCarIcon);
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

    }

    void onHomeClick() {
        adminCount++;
        if ( adminCount > 6) {
            onCheckAdminMode();
        }
        else if( adminCount > 3) {
            if ( toast != null ) toast.cancel();
            toast = Toast.makeText(this, "관리자모드 진입 : "+(6-adminCount), Toast.LENGTH_SHORT);
            toast.show();
        }
        startTimer();
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                adminCount = 0;
                timer.cancel();
            }
        }, 1000, 1000);
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    public void onCheckAdminMode() {
        final EditText txtPassword = new EditText(this);
        final Context context = this.getBaseContext();
        txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(txtPassword, 1);

        new AlertDialog.Builder(this)
                .setTitle("Admin Password")
                .setMessage(getResources().getString(R.string.string_input_admin_password))
                .setView(txtPassword)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String pwd = txtPassword.getText().toString();

                        if ( pwd.equals(cpConfig.settingPassword) == true ) {
                            pageManger.changePage(PageID.SETTING);
                        }
                        else {
                            Toast.makeText(context , getResources().getString(R.string.string_password_incorrect), Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setNavigationBarHomeIconDisable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setNavigationBarHomeIconDisable(true);
    }

    @Override
    protected void onDestroy() {
        flowManager.destory();
        super.onDestroy();
        setNavigationBarHomeIconDisable(false);
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        flowManager.onFinishApp();
    }

    public void hideNavBar() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }

    public void setNavigationBarHomeIconDisable(boolean tf) {
        try {

            Process p = Runtime.getRuntime().exec( "su" );
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("setprop persist.sys.navbar.disable "+(tf ? "true" : "false") + "\n");

            os.writeBytes("exit\n");
            os.flush();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            Log.e("SetProp", "Output:"+output);

            p.waitFor();
        } catch (IOException e) {
            Log.e("SetProp", e.toString());
        } catch (InterruptedException e) {
            Log.e("SetProp", e.toString());
        }
    }
}
