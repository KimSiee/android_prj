/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 25 오전 11:36
 *
 */

package com.joas.kepco.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;

public class KepcoUIActivity extends ImmersiveAppCompatActivity {
    public static final String uiVersion = "v0.1";
    static KepcoUIActivity mainActivity = null;

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

    private IMeterAidlInterface meterService;
    Handler bindingMeterHandler;

    /*private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogWrapper.d("MeterAidl", " Service Connected!!");
            meterService = IMeterAidlInterface.Stub.asInterface(iBinder);
            try {
                meterService.startApp(1);
                //meterService.setCharLCDDisp(2, "Rotate 1", "Second 2", "","");

                //meterService.setCharLCDBacklight(false);

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
    };*/

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogWrapper.d("MeterAidl", " Service Connected!!");
            meterService = IMeterAidlInterface.Stub.asInterface(iBinder);
            try {
                //meterService.startAppNewPos(1, 0, 0, 1024, 25, 14.0f,0xaa000000, 0xFF00FF72);
                meterService.startApp(1);
                meterService.startAppNewPos(1, 0, 690, 2000, 25, 14.0f,0xaa000000, 0xFF00FF72);
                //meterService.setCharLCDRotatePeriod(2); // 초
                //flowManager.dispLastMeteringString();
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

    /*public void startBindConnect() {
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
    }*/

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

    public UIFlowManager getFlowManager() { return flowManager; }
    public PageManger getPageManger() { return pageManger; }
    static public KepcoUIActivity getMainActivity() { return mainActivity; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        hideNavBar();

        setContentView(R.layout.activity_kepco_ui);

        pageManger = new PageManger(this);

        cpConfig = new CPConfig();
        chargeData = new ChargeData();

        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        flowManager = new UIFlowManager(this, chargeData, cpConfig);

        pageManger.init(flowManager, this);
        flowManager.setPageManager(pageManger);
        flowManager.showInitDisplay();
        setNavigationBarHomeIconDisable(true);
        startBindConnect();
    }

    // 마우스 오른쪽 버턴을 누를때 세팅모드.
    @Override
    public void onBackPressed() {
        pageManger.showSettingView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setNavigationBarHomeIconDisable(false);
        flowManager.closeManager();
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
