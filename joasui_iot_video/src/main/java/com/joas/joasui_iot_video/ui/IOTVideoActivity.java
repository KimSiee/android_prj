/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 8:48
 *
 */

package com.joas.joasui_iot_video.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.joas.joasui_iot_video.ui.page.PageID;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.joas.metercertviewer.IMeterAidlInterface;

public class IOTVideoActivity extends ImmersiveAppCompatActivity {
    PageManger pageManger;
    UIFlowManager flowManager;

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData chargeData;

    private IMeterAidlInterface meterService;
    Handler bindingMeterHandler;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogWrapper.d("MeterAidl", " Service Connected!!");
            meterService = IMeterAidlInterface.Stub.asInterface(iBinder);
            try {
                meterService.startApp(1);
            }catch (Exception e) {
                LogWrapper.d("MeterAidl", "error:"+e.toString());
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogWrapper.d("MeterAidl", " Service Disconnected!!");
            meterService = null;

            unbindService(serviceConnection);
            startBindConnect();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Force Close 이벤트 발생시 처리 (개발시에는 끄고/ 릴리즈시에 적용)
        /*
        Thread.setDefaultUncaughtExceptionHandler(
                new ForceCloseHandler(this,
                        Environment.getExternalStorageDirectory()+TypeDefine.FORCE_CLOSE_LOG_PATH,
                        true));
                        */

        hideNavBar();

        setContentView(R.layout.activity_iotvideo_main);

        pageManger = (PageManger)findViewById(R.id.viewpager_main);

        cpConfig = new CPConfig();
        chargeData = new ChargeData();

        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        flowManager = new UIFlowManager(this, chargeData, cpConfig);

        pageManger.init(flowManager, this);
        flowManager.setPageManager(pageManger);

        initButtonEvent();

        setNavigationBarHomeIconDisable(true);

        startBindConnect();
    }

    public void startBindConnect() {
        bindingMeterHandler  = new Handler(Looper.getMainLooper());
        final Handler runHandler = bindingMeterHandler;
        bindingMeterHandler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent();
                intent.setClassName("com.joas.metercertviewer", "com.joas.metercertviewer.MeterWindow");

                if ( bindService(intent, serviceConnection, BIND_AUTO_CREATE) == false ) {
                    LogWrapper.d("IOTVideoAct", "Meter Bind Error");
                    bindingMeterHandler.postDelayed(this, 1000);
                }
                else {
                    LogWrapper.d("IOTVideoAct", "Meter Bind Success!");
                }
            }
        }, 1000);
    }

    public IMeterAidlInterface getMeterService() { return meterService; }

    void initButtonEvent() {
        ImageButton btHome  = (ImageButton) findViewById(R.id.btEvCarIcon);
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        btHome.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        onCheckAdminMode();
                        return true;
                    }
                });
    }


    void onHomeClick() {
        //Test
        //messageBoxView.setVisibility(View.VISIBLE);
    }

    public void onCheckAdminMode() {
        final EditText txtPassword = new EditText(this);
        final Context context = this.getBaseContext();
        txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
        try {
            if (meterService != null) meterService.stopApp();
        }catch (Exception e) {}
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
