/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.utils.LogWrapper;
import com.joas.volvo_touch_2ch.webservice.WebService;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class VolvoTouch2chUIActivity extends ImmersiveAppCompatActivity {
    public static final String uiVersion = "v0.1";
    static VolvoTouch2chUIActivity mainActivity = null;

    PageManager[] pageManagers = new PageManager[TypeDefine.MAX_CHANNEL];
    MultiChannelUIManager multiChannelUIManager;
    WebService webService;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData[] chargeDatas = new ChargeData[TypeDefine.MAX_CHANNEL];
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

                meterService.startAppNewPos((int)(1/*TypeDefine.UI_VERSION*10*/), 0,690,2000,30, 20, 0xAA0000BB, 0xFFFFFFFF);

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
    };

    private void commandSu( String commands ) {
        try {

            Process p = Runtime.getRuntime().exec( "su" );
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes(commands + "\n");

            os.writeBytes("exit\n");
            os.flush();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            Log.e("commandSu", "Output:"+output);

            p.waitFor();
        } catch (IOException e) {
            Log.e("commandSu", e.toString());
        } catch (InterruptedException e) {
            Log.e("commandSu", e.toString());
        }
    }

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

        setContentView(R.layout.activity_built_in_touch_2ch_main);


        for ( int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            pageManagers[i] = new PageManager(this);
            chargeDatas[i] = new ChargeData();
            chargeDatas[i].dspChannel = i;
            chargeDatas[i].curConnectorId = i+1;
        }

        cpConfig = new CPConfig();

        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        TextView tvVersion = (TextView) findViewById(R.id.tvVersion);

        String chargerID = "충전기ID : "+ cpConfig.stationID+cpConfig.chargerID+"/"+cpConfig.stationID2+cpConfig.chargerID2;
        tvVersion.setText(chargerID);

        //전력량계 프로그램 설정파일 로드
        meterConfig = new MeterConfig();
        meterConfig.loadConfig(this);

        String restartReason = getIntent().getStringExtra("RestartReason");

        multiChannelUIManager = new MultiChannelUIManager(this, pageManagers, chargeDatas, cpConfig, meterConfig, restartReason);

        initComponents();

        setNavigationBarHomeIconDisable(true);

        webService = new WebService(this.getApplicationContext());

        startBindConnect();

        //si.200826 - 통신속도 10mbps강제 변환
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                commandSu("/system/bin/ethtool -s eth0 speed 10 duplex full autoneg on");
//                commandSu("/system/bin/ethtool -s eth0 speed 100 duplex full autoneg on");
            }
        }, 3000);       //3초후 명령어 실행
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

    static public VolvoTouch2chUIActivity getMainActivity() { return mainActivity; }

    public MultiChannelUIManager getMultiChannelUIManager() { return multiChannelUIManager; }

    public boolean getIsCommConnected() { return commConnStatus; }

    void initComponents() {
//        ImageButton btHome  = (ImageButton) findViewById(R.id.btEvCarIcon);
        ImageView btHome = (ImageView)findViewById(R.id.imageCommStatus);
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
                imageCommStatus.setImageResource(R.drawable.w1);
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
        if ( commConnStatus ) imageCommStatus.setImageResource(R.drawable.w3);
        else imageCommStatus.setImageResource(R.drawable.server_off);
    }

    public void onCheckAdminMode() {
        multiChannelUIManager.showAdminPasswrodInputView();
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

        multiChannelUIManager.destoryManager();
        stopTimer();
        setNavigationBarHomeIconDisable(false);
        super.onDestroy();
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        if ( multiChannelUIManager.isShowAdminPassswordInputView() ) multiChannelUIManager.onFinishApp();
        else multiChannelUIManager.showAdminPasswrodInputView();
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
