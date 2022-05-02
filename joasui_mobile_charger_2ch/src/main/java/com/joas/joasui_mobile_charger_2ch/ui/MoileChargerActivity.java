/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:16
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.joasui_mobile_charger_2ch.ui.page.PageEvent;
import com.joas.joasui_mobile_charger_2ch.ui.page.PageID;
import com.joas.utils.FileUtil;
import com.joas.utils.ForceCloseHandler;
import com.joas.utils.LogWrapper;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MoileChargerActivity  extends ImmersiveAppCompatActivity
        implements DSPControlListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    PageManger[] pageManger = new PageManger[TypeDefine.CHANNEL_MAX_CNT];
    UIFlowManager[] flowManager = new UIFlowManager[TypeDefine.CHANNEL_MAX_CNT];
    CableSelectorAlt cableSelectorAlt = new CableSelectorAlt();

    /**
     * 저장용 Config UI 정의
     */
    CPConfig cpConfig;
    ChargeData[] chargeData = new ChargeData[TypeDefine.CHANNEL_MAX_CNT];

    DSPControl dspControl;

    TextView tvPlugA, tvPlugB;

    public int adminCount = 0;
    Timer timer;
    static Toast toast;

    int cfgYear = 2018;
    int cfgMonth = 1;
    int cfgDay = 1;
    int cfgHour = 1;
    int cfgMinute = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Force Close 이벤트 발생시 처리 (개발시에는 끄고/ 릴리즈시에 적용)
        /*
        Thread.setDefaultUncaughtExceptionHandler(
                new ForceCloseHandler(this,
                        Environment.getExternalStorageDirectory()+TypeDefine.FORCE_CLOSE_LOG_PATH,
                        false));
        */

        hideNavBar();

        setContentView(R.layout.activity_moile_charger);

        if ( chkPickDate() == false ) {
            runLateInit();
        }

        initComponents();

        setNavigationBarHomeIconDisable(true);
    }

    void runLateInit() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        lateInit();
                    }
                }, 200);

            }
        });
    }

    boolean chkPickDate() {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        if ( year >= 2018 ) return false;

        toast = Toast.makeText(this, "시간설정이 되지 않았습니다. 시간을 설정해 주세요.", Toast.LENGTH_LONG);
        toast.show();

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog dlg =  new DatePickerDialog(this, this, year, month, day);
        dlg.show();

        return true;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        cfgYear = year;
        cfgMonth = month;
        cfgDay = day;

        chkPickTime();
    }

    void chkPickTime() {
         // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        TimePickerDialog dlg = new TimePickerDialog(this, this, hour, minute,
                DateFormat.is24HourFormat(this));
            dlg.show();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        cfgHour = hourOfDay;
        cfgMinute = minute;

        String strTime = String.format("%d-%d-%d %d:%d:0", cfgYear, cfgMonth, cfgDay, cfgHour, cfgMinute);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date syncTime = null;

        try {
            syncTime = format.parse(strTime);
        } catch (ParseException e) {
            LogWrapper.e("MainAct", "onTimeUpdate err: "+e.toString()+", strTime:"+strTime);
            return;
        }

        Date curTime = new Date();
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        am.setTime(syncTime.getTime());

        runLateInit();
    }

    void lateInit() {
        initLogPath();

        cpConfig = new CPConfig();

        // 맨먼저 설정값을 로딩한다.
        cpConfig.loadConfig(this);

        //DSP Control Init
        dspControl = new DSPControl(2, "/dev/ttyS2", DSPControl.DSP_VER_REG23_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE,1, this);

        pageManger[0] = new PageManger(this.getBaseContext());
        pageManger[1] = new PageManger(this.getBaseContext());

        for ( int i=0; i<TypeDefine.CHANNEL_MAX_CNT; i++) {
            chargeData[i] = new ChargeData();
            chargeData[i].dspChannel = i;
            chargeData[i].logFilePath = Environment.getExternalStorageDirectory() + TypeDefine.CP_LOG_PATH;
            flowManager[i] = new UIFlowManager(this, dspControl, chargeData[i], cpConfig, cableSelectorAlt);
            pageManger[i].init(i, flowManager[i], this);
            flowManager[i].setPageManager(pageManger[i]);
        }

        FrameLayout readyMsg = (FrameLayout)findViewById(R.id.frameReadyMsg);
        readyMsg.setVisibility(View.INVISIBLE);

        dspControl.start();

        FileUtil.appendDateLog(chargeData[0].logFilePath, ", CH:X, System Initialized. Logger Started");
    }

    public UIFlowManager getFlowManager(int ch) {
        return flowManager[ch];
    }

    void initComponents() {
        ImageButton btHome  = (ImageButton) findViewById(R.id.btEvCarIcon);
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
            }
        });

        tvPlugA = (TextView) findViewById(R.id.tvPlugA);
        tvPlugB = (TextView) findViewById(R.id.tvPlugB);
    }

    void initLogPath() {
        File parent = new File(Environment.getExternalStorageDirectory() + TypeDefine.CP_LOG_PATH);
        if (!parent.exists()) {
            parent.mkdirs();
        }
    }

    public void setPlugTitle(int channel, String str) {
        if ( channel == 0 ) tvPlugA.setText("Plug A"+str);
        else tvPlugB.setText("Plug B"+str);
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
                            pageManger[0].changePage(PageID.SETTING);
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
        dspControl.interrupt();
        dspControl.stopThread();

        super.onDestroy();
        setNavigationBarHomeIconDisable(false);
    }

    // 마우스 오른쪽 버턴을 누를때 종료.
    @Override
    public void onBackPressed() {
        flowManager[0].onFinishApp();
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

    @Override
    public void onDspStatusChange(int channel, DSPRxData.STATUS400 idx, boolean val) {
        if ( channel < TypeDefine.CHANNEL_MAX_CNT) {
            flowManager[channel].onDspStatusChange(channel, idx, val);
        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        if ( channel < TypeDefine.CHANNEL_MAX_CNT) {
            flowManager[channel].onDspMeterChange(channel, meterVal);
        }
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {

    }
}
