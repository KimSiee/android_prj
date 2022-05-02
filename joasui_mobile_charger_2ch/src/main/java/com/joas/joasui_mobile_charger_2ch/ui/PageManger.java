/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:16
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.joas.joasui_mobile_charger_2ch.ui.page.ChargingView;
import com.joas.joasui_mobile_charger_2ch.ui.page.ConnectCarWaitView;
import com.joas.joasui_mobile_charger_2ch.ui.page.ConnectorWaitView;
import com.joas.joasui_mobile_charger_2ch.ui.page.EmergencyBoxView;
import com.joas.joasui_mobile_charger_2ch.ui.page.FaultBoxView;
import com.joas.joasui_mobile_charger_2ch.ui.page.FinishChargingView;
import com.joas.joasui_mobile_charger_2ch.ui.page.JoasDSPMonitorView;
import com.joas.joasui_mobile_charger_2ch.ui.page.JoasDebugMsgView;
import com.joas.joasui_mobile_charger_2ch.ui.page.MessageBoxView;
import com.joas.joasui_mobile_charger_2ch.ui.page.PageActivateListener;
import com.joas.joasui_mobile_charger_2ch.ui.page.PageID;
import com.joas.joasui_mobile_charger_2ch.ui.page.SelectFastView;
import com.joas.joasui_mobile_charger_2ch.ui.page.SettingView;

public class PageManger extends FrameLayout {
    public static final String TAG = "PageManager";

    Context baseContext;
    UIFlowManager flowManager;

    SelectFastView selectFastView;
    ConnectorWaitView connectorWaitView;
    ConnectCarWaitView connectCarWaitView;
    ChargingView chargingView;
    FinishChargingView finishChargingView;
    SettingView settingView;

    EmergencyBoxView emergencyBoxView;
    MessageBoxView messageBoxView;
    FaultBoxView faultBoxView;

    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;

    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;

    int channel;

    FrameLayout panelLayout;
    FrameLayout panelCh;
    ConstraintLayout mainLayout;

    public PageManger(Context context) {
        super(context);
        baseContext = context;
    }

    public void init(int chan, UIFlowManager uiManager, Activity activity) {
        channel = chan;
        flowManager = uiManager;
        mainActivity = activity;

        initPages();
    }

    void initPages() {
        selectFastView = new SelectFastView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        connectCarWaitView = new ConnectCarWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);
        settingView = new SettingView(baseContext, flowManager, mainActivity);

        emergencyBoxView = new EmergencyBoxView(baseContext, flowManager, mainActivity);
        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        faultBoxView = new FaultBoxView(baseContext, flowManager, mainActivity);

        if ( channel == 0 ) {
            joasDSPMonitorView = new JoasDSPMonitorView(baseContext, flowManager, mainActivity);
            joasDebugMsgView = new JoasDebugMsgView(baseContext, flowManager, mainActivity);
            flowManager.getDspControl().setDspMonitorListener(joasDSPMonitorView);
        }

        if ( channel == 0 ) {
            panelLayout = (FrameLayout) mainActivity.findViewById(R.id.panelCh1);
            panelCh = (FrameLayout) mainActivity.findViewById(R.id.framePageCh1);
        }
        else {
            panelLayout = (FrameLayout) mainActivity.findViewById(R.id.panelCh2);
            panelCh = (FrameLayout) mainActivity.findViewById(R.id.framePageCh2);
        }

        selectFastView.setVisibility(VISIBLE);
        connectorWaitView.setVisibility(INVISIBLE);
        connectCarWaitView.setVisibility(INVISIBLE);
        chargingView.setVisibility(INVISIBLE);
        finishChargingView.setVisibility(INVISIBLE);
        settingView.setVisibility(INVISIBLE);

        mainLayout = (ConstraintLayout)mainActivity.findViewById(R.id.layoutMain);

        messageBoxView.setVisibility(View.INVISIBLE);
        faultBoxView.setVisibility(View.INVISIBLE);
        emergencyBoxView.setVisibility(View.INVISIBLE);

        if ( channel == 0 ) {
            joasDSPMonitorView.setVisibility(View.INVISIBLE);
            joasDebugMsgView.setVisibility(View.INVISIBLE);
        }

        changePage(PageID.SELECT_FAST);
    }

    public SelectFastView getSelectFastView() { return selectFastView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ConnectCarWaitView getConnectCarWaitView() { return connectCarWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }
    public SettingView getSettingView() { return settingView; }

    public void doUiChangePage(PageID prev, PageID cur) {
        if ( prev != PageID.PAGE_END) {
            ((PageActivateListener) getPageViewByID(prev)).onPageDeactivate();
            ((View)getPageViewByID(prev)).setVisibility(INVISIBLE);
            try {
                panelCh.removeView((View)getPageViewByID(prev));
            }
            catch (Exception e) {
                Log.e(TAG, "doUIChangePage Remove ex:"+e.toString()+", name:"+cur.name());
            }
            Log.v(TAG, "UiChange Deact:"+prev.name());
        }
        if ( cur != PageID.PAGE_END) {
            //setCurrentItem(cur.getID());
            try {
                panelCh.addView((View) getPageViewByID(cur));
            }
            catch (Exception e) {
                Log.e(TAG, "doUIChangePage Add ex:"+e.toString()+", name:"+cur.name());
            }
            ((View)getPageViewByID(cur)).setVisibility(VISIBLE);
            ((PageActivateListener) getPageViewByID(cur)).onPageActivate();
            Log.v(TAG, "UiChange Act:"+cur.name());
        }
    }

    /**
     * 페이지를 바꾼다.
     * @param id 바꿀 페이지
     */
    public synchronized void changePage(PageID id) {
        //Log.d(TAG, "changePage:"+id.name());

        // * 주의점. runOnUiThread의 불려지는 시기가 일정치 않아서 같은 id가 2번 반복수행될 수 있음 따라서
        // 현재 pageID를 바꾸는 작업은 현 Thread안에서 수행되어야함.( prevPageID = curPageID; curPageID = id; )
        if ( curPageID != id ) {
            // Main Thread 체크 하여 동작 수행
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                prevPageID = curPageID;
                curPageID = id;
                doUiChangePage(prevPageID, curPageID);
            } else {
                final PageID idFinal = id;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        prevPageID = curPageID;
                        curPageID = idFinal;
                        doUiChangePage(prevPageID, curPageID);
                    }
                });
            }
        }
    }

    Object getPageViewByID(PageID id) {
        Object v = null;
        switch ( id ) {
            case SETTING: v = getSettingView();
                break;
            case SELECT_FAST: v = getSelectFastView();
                break;
            case CONNECTOR_WAIT: v = getConnectorWaitView();
                break;
            case CONNECT_CAR_WAIT: v = getConnectCarWaitView();
                break;
            case CHARGING: v = getChargingView();
                break;
            case FINISH_CHARGING: v = getFinishChargingView();
                break;
        }
        return v;
    }

    public void changePreviousPage() {
        changePage(prevPageID);
    }

    public void showEmergencyBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panelLayout.addView(emergencyBoxView);
                emergencyBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideEmergencyBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emergencyBoxView.setVisibility(View.INVISIBLE);
                panelLayout.removeView(emergencyBoxView);
            }
        });
    }

    public void showMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panelLayout.addView(messageBoxView);
                messageBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.INVISIBLE);
                panelLayout.removeView(messageBoxView);
            }
        });
    }

    public void showFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                panelLayout.addView(faultBoxView);
                faultBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.INVISIBLE);
                panelLayout.removeView(faultBoxView);
            }
        });
    }

    public void showJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.addView(joasDSPMonitorView);
                joasDSPMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.INVISIBLE);
                mainLayout.removeView(joasDSPMonitorView);
            }
        });
    }

    public void showJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.addView(joasDebugMsgView);
                joasDebugMsgView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.INVISIBLE);
                mainLayout.removeView(joasDebugMsgView);
            }
        });
    }


    //============================================
    // Drag를 해서 페이지 넘김이 일어나지 않도록함
    //============================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }
}
