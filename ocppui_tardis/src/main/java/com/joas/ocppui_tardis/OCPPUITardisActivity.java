/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:29
 *
 */

package com.joas.ocppui_tardis;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.ocppui_tardis.webservice.WebService;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;




public class OCPPUITardisActivity extends ImmersiveAppCompatActivity {
    static OCPPUITardisActivity mainActivity = null;

    public static final String uiVersion = "v0.1";
    PageManger pageManger;
    UIFlowManager flowManager;
    WebService webService;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData chargeData;
    TextView tvRemoteStartMsg;
    TextView tvReservedMsg;
    ImageView imageCommStatus;
    MeterConfig meterConfig;

    boolean commConnStatus = false;

    private IMeterAidlInterface meterService;
    Handler bindingMeterHandler;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogWrapper.d("MeterAidl", " Service Connected!!");
            meterService = IMeterAidlInterface.Stub.asInterface(iBinder);
            try {
                meterService.startApp(1);
                meterService.startAppNewPos(1, 0, 555, 1024, 25, 14.0f,0xaa000000, 0xFF00FF72);

                if(!meterConfig.lcdType.equals("None")) {
                    meterService.setCharLCDRotatePeriod(2); // 초
                    flowManager.dispLastMeteringString();
                }
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

        mainActivity = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Force Close 이벤트 발생시 처리 (개발시에는 끄고/ 릴리즈시에 적용)
        /*
        Thread.setDefaultUncaughtExceptionHandler(
                new ForceCloseHandler(this,
                        Environment.getExternalStorageDirectory()+TypeDefine.FORCE_CLOSE_LOG_PATH,
                        true));
                        */

        hideNavBar();

        setContentView(R.layout.activity_ocpp_main);

        TextView tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvVersion.setText(uiVersion);

        pageManger = new PageManger(this);

        cpConfig = new CPConfig();
        chargeData = new ChargeData();



        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        meterConfig = new MeterConfig();
        meterConfig.loadConfig(this);

        String restartReason = getIntent().getStringExtra("RestartReason");

        flowManager = new UIFlowManager(this, chargeData, cpConfig, meterConfig, restartReason);

        pageManger.init(flowManager, this);
        flowManager.setPageManager(pageManger);

        initComponents();

        setNavigationBarHomeIconDisable(true);

        startBindConnect();

        webService = new WebService(this.getApplicationContext());
    }

    public UIFlowManager getFlowManager() { return flowManager; }
    public PageManger getPageManger() { return pageManger; }
    public boolean getIsCommConnected() { return commConnStatus; }

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

    void initComponents() {
        ImageButton btHome  = (ImageButton) findViewById(R.id.btEvCarIcon);
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        tvRemoteStartMsg = (TextView) findViewById(R.id.tvRemoteStartedMsg);
        tvRemoteStartMsg.setVisibility(View.INVISIBLE);
        tvReservedMsg = (TextView) findViewById(R.id.tvReservedMsg);
        tvReservedMsg .setVisibility(View.INVISIBLE);
        imageCommStatus = (ImageView) findViewById(R.id.imageCommStatus);
    }

    static public OCPPUITardisActivity getMainActivity() { return mainActivity; }

    void onHomeClick() {
        adminCount++;
        if ( adminCount > 6) {
            onCheckAdminMode();
        }
        else if( adminCount > 3) {
            if ( toast != null ) toast.cancel();
            toast = Toast.makeText(this, "Admin Mode Remains : "+(6-adminCount), Toast.LENGTH_SHORT);
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
        try {
            if (timer != null) timer.cancel();
        }
        catch(Exception e) {}
    }

    public void setRemoteStartedVisible(int v) {
        final int visible = v;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( visible == View.VISIBLE) tvRemoteStartMsg.setText(getResources().getString(R.string.string_remote_started));
                tvRemoteStartMsg.setVisibility(visible);
            }
        });
    }

    public void setReservedVisible(int v) {
        final int visible = v;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( visible == View.VISIBLE) tvReservedMsg.setText(getResources().getString(R.string.string_reserved));
                tvReservedMsg.setVisibility(visible);
            }
        });
    }

    public void setCommConnStatus(boolean status) {
        commConnStatus = status;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshCommConStatus();
            }
        });
    }

    public void setCommConnActive() {
        commConnStatus = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageCommStatus.setImageResource(R.drawable.commicon_active);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshCommConStatus();
                            }
                        });
                    }
                }, 300);
            }
        });
    }

    public void refreshCommConStatus() {
        if ( commConnStatus ) imageCommStatus.setImageResource(R.drawable.commicon);
        else imageCommStatus.setImageResource(R.drawable.commicon_fail);
    }

    public void onCheckAdminMode() {
        pageManger.showAdminPasswrodInputView();
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
        flowManager.destoryManager();
        stopTimer();
        setNavigationBarHomeIconDisable(false);
        super.onDestroy();
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        if ( pageManger.isShowAdminPassswordInputView() ) flowManager.onFinishApp();
        else pageManger.showAdminPasswrodInputView();
    }

    public void hideNavBar() {
        int currentApiVersion = Build.VERSION.SDK_INT;

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
