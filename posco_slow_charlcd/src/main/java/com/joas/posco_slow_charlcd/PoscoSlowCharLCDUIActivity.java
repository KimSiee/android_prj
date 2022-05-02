/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.posco_slow_charlcd.page.PageEvent;
import com.joas.posco_slow_charlcd.webservice.WebService;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class PoscoSlowCharLCDUIActivity extends ImmersiveAppCompatActivity {
    static PoscoSlowCharLCDUIActivity mainActivity = null;

    PageManger pageManger;
    UIFlowManager flowManager;
    WebService webService;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    public boolean isRevisionType = false;
    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData chargeData;
    CoordinatorLayout mlayout;
    TextView tvVersion;
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

        setContentView(R.layout.activity_ocpp_main);

        pageManger = new PageManger(this);

        cpConfig = new CPConfig();
        chargeData = new ChargeData();

//        cpConfig.useSehanRF = true;
        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);


        meterConfig = new MeterConfig();
//        //미터 통신관련 설정파일 저장(미터설정 파일이 없는 충전기의 경우 강제 저장을 위함)
//        meterConfig.version1 = 0;
//        meterConfig.version2 = 1;
//        meterConfig.version3 = 4;
//        meterConfig.meterkind = "Tascon";       //Tascon, DDS353H
//        meterConfig.maxChannel = 1;
//        meterConfig.lcdType = "RW1603";         //RW1603, LCD1602, None
//        meterConfig.saveConfig(this);

        meterConfig.loadConfig(this);

        String restartReason = getIntent().getStringExtra("RestartReason");

        flowManager = new UIFlowManager(this, chargeData, cpConfig, meterConfig,restartReason);

        pageManger.init(flowManager,meterConfig, cpConfig, this);
        flowManager.setPageManager(pageManger);

        initComponents();

        setNavigationBarHomeIconDisable(true);

        startBindConnect();

        webService = new WebService(this.getApplicationContext());

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

    public UIFlowManager getFlowManager() { return flowManager; }
    public PageManger getPageManger() { return pageManger; }
    public boolean getIsCommConnected() { return commConnStatus; }

    static public PoscoSlowCharLCDUIActivity getMainActivity() { return mainActivity; }

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


//        //차지비향, 카모향에 따른 메인 프레임 이미지 변경
//        CoordinatorLayout mlayout = (CoordinatorLayout)findViewById(R.id.layoutMain);
//        ImageView comstat = (ImageView)findViewById(R.id.imageCommStatus);
//        TextView mtv = (TextView)findViewById(R.id.tvVersion)
//
//        if(cpConfig.useKakaoNavi) {
//            mlayout.setBackground(ActivityCompat.getDrawable(this,R.drawable.bg_kakao));
//            comstat.setVisibility(View.INVISIBLE);
//        }
//        else {
//            mlayout.setBackground(ActivityCompat.getDrawable(this,R.drawable.bg_cv));
//            comstat.setVisibility(View.VISIBLE);
//
//        }

        //차지비향, 카모향에 따른 메인 프레임 구성 변경
        ImageView btHome = (ImageView) findViewById(R.id.imageCommStatus);
        imageCommStatus = (ImageView) findViewById(R.id.imageCommStatus);
        mlayout = findViewById(R.id.layoutMain);
        tvVersion = findViewById(R.id.tvVersion);

        RelativeLayout.LayoutParams rlayoutParam = (RelativeLayout.LayoutParams)imageCommStatus.getLayoutParams();
        if(cpConfig.useKakaoNavi){
            //메인 배경 이미지
            mlayout.setBackground(ActivityCompat.getDrawable(this,R.drawable.bg_kakao));
            //충전기 ID
            tvVersion.setVisibility(View.INVISIBLE);
            //홈으로 버튼 위치 조절
            rlayoutParam.setMarginStart(1080);
            rlayoutParam.width = 170;
            btHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onGoToHomeClick();
                }
            });

        }
        else{
            //메인 배경 이미지
            mlayout.setBackground(ActivityCompat.getDrawable(this,R.drawable.bg_cv));
            btHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onHomeClick();
                }
            });
            //충전기 ID
            tvVersion.setVisibility(View.VISIBLE);
            //통신상태 위치 조절
            rlayoutParam.setMarginStart(1135);
            rlayoutParam.width = 100;

        }




        tvRemoteStartMsg = (TextView) findViewById(R.id.tvRemoteStartedMsg);
        tvRemoteStartMsg.setVisibility(View.INVISIBLE);
        tvReservedMsg = (TextView) findViewById(R.id.tvReservedMsg);
        tvReservedMsg.setVisibility(View.INVISIBLE);


        String chargerID = "충전기ID : " + cpConfig.stationID + "/" + cpConfig.chargerID;
        tvVersion.setText(chargerID);
//        tvVersion.setText(TypeDefine.SW_VERSION);
    }

    void onGoToHomeClick(){
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
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
                if(!cpConfig.useKakaoNavi) imageCommStatus.setImageResource(R.drawable.w1);
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
        if(!cpConfig.useKakaoNavi){
            if ( commConnStatus ) {
                imageCommStatus.setImageResource(R.drawable.w3);
            }
            else {
                imageCommStatus.setImageResource(R.drawable.server_off);
            }
        }
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
        try {
            if (meterService != null) meterService.stopApp();
        }catch (Exception e) {}

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
