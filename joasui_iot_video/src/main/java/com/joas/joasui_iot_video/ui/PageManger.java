/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 8:38
 *
 */

package com.joas.joasui_iot_video.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.joas.joasui_iot_video.ui.page.AuthWaitView;
import com.joas.joasui_iot_video.ui.page.CardTagView;
import com.joas.joasui_iot_video.ui.page.ChargingView;
import com.joas.joasui_iot_video.ui.page.ConnectCarWaitView;
import com.joas.joasui_iot_video.ui.page.ConnectorWaitView;
import com.joas.joasui_iot_video.ui.page.FinishChargingView;
import com.joas.joasui_iot_video.ui.page.EvCommMonitorView;
import com.joas.joasui_iot_video.ui.page.JoasDSPMonitorView;
import com.joas.joasui_iot_video.ui.page.JoasDebugMsgView;
import com.joas.joasui_iot_video.ui.page.MessageBoxView;
import com.joas.joasui_iot_video.ui.page.PageActivateListener;
import com.joas.joasui_iot_video.ui.page.PageID;
import com.joas.joasui_iot_video.ui.page.SelectFastView;
import com.joas.joasui_iot_video.ui.page.SettingView;
import com.joas.joasui_iot_video.ui.page.SelectSlowView;
import com.joas.joasui_iot_video.ui.page.StartView;

public class PageManger extends ViewPager {
    public static final String TAG = "PageManager";

    Context baseContext;
    UIFlowManager flowManager;

    SelectSlowView selectSlowView;
    SelectFastView selectFastView;
    CardTagView cardTagView;
    AuthWaitView authWaitView;
    ConnectorWaitView connectorWaitView;
    ConnectCarWaitView connectCarWaitView;
    ChargingView chargingView;
    FinishChargingView finishChargingView;
    SettingView settingView;
    StartView startView;

    MessageBoxView messageBoxView;
    EvCommMonitorView commMonitorView;
    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;

    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;
    PagerAdapterClass pagerAdapter;

    public PageManger(Context context, AttributeSet attrs) {
        super(context, attrs);
        baseContext = context;
    }

    public void init(UIFlowManager uiManager, Activity activity) {
        flowManager = uiManager;
        mainActivity = activity;
        initPages();
    }

    void initPages() {
        startView = new StartView(baseContext, flowManager, mainActivity);
        selectSlowView = new SelectSlowView(baseContext, flowManager, mainActivity);
        selectFastView = new SelectFastView(baseContext, flowManager, mainActivity);
        cardTagView = new CardTagView(baseContext, flowManager, mainActivity);
        authWaitView = new AuthWaitView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        connectCarWaitView = new ConnectCarWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);
        settingView = new SettingView(baseContext, flowManager, mainActivity);

        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        commMonitorView = new EvCommMonitorView(baseContext, flowManager, mainActivity);
        joasDSPMonitorView = new JoasDSPMonitorView(baseContext, flowManager, mainActivity);
        joasDebugMsgView = new JoasDebugMsgView(baseContext, flowManager, mainActivity);

        //flowManager.getJoasCommManager().setMonitorListener(commMonitorView); // 모니터링 리스너 등록
        flowManager.getDspControl().setDspMonitorListener(joasDSPMonitorView);

        CoordinatorLayout MainLayout = (CoordinatorLayout)mainActivity.findViewById(R.id.layoutMain);
        MainLayout.addView(messageBoxView);
        messageBoxView.setVisibility(View.INVISIBLE);

        MainLayout.addView(commMonitorView);
        commMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDSPMonitorView);
        joasDSPMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDebugMsgView);
        joasDebugMsgView.setVisibility(View.INVISIBLE);

        pagerAdapter = new PagerAdapterClass(this);
        this.setAdapter(pagerAdapter);

        if (flowManager.getCpConfig().isStartVideo ) changePage(PageID.START);
        else {
            if (flowManager.getCpConfig().isFastCharger ) changePage(PageID.SELECT_FAST);
            else changePage(PageID.SELECT_SLOW);
        }
    }

    public StartView getStartView() { return startView; }
    public SelectSlowView getSelectSlowView() { return selectSlowView; }
    public SelectFastView getSelectFastView() { return selectFastView; }
    public CardTagView getCardTagView() { return cardTagView; }
    public AuthWaitView getAuthWaitView() { return authWaitView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ConnectCarWaitView getConnectCarWaitView() { return connectCarWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }
    public SettingView getSettingView() { return settingView; }

    public void doUiChangePage(PageID prev, PageID cur) {
        if ( prev != PageID.PAGE_END) {
            ((PageActivateListener) getPageViewByID(prev)).onPageDeactivate();
            Log.v(TAG, "UiChange Deact:"+prev.name());
        }
        if ( cur != PageID.PAGE_END) {
            setCurrentItem(cur.getID());
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
            case START: v = getStartView();
                break;
            case SELECT_SLOW: v = getSelectSlowView();
                break;
            case SELECT_FAST: v = getSelectFastView();
                break;
            case CARD_TAG: v = getCardTagView();
                break;
            case AUTH_WAIT: v = getAuthWaitView();
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
        setCurrentItem(prevPageID.getID());
    }

    public void showMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showJoasCommMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideJoasCommMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commMonitorView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.INVISIBLE);
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

    private class PagerAdapterClass extends PagerAdapter {
        PageManger pageManager;
        public PagerAdapterClass(PageManger _pageManager) {
            this.pageManager = _pageManager;
        }

        @Override
        public int getCount() {
            return PageID.PAGE_END.getID();
        }

        @Override
        public Object instantiateItem(ViewGroup pager, int position) {
            View v = (View)pageManager.getPageViewByID(PageID.getValue(position));
            ((ViewPager)pager).addView(v, 0);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup pager, int position, Object view) {
            ((ViewPager)pager).removeView((View)view);
        }

        @Override
        public boolean isViewFromObject(View pager, Object obj) {
            return pager == obj;
        }

    }
}
