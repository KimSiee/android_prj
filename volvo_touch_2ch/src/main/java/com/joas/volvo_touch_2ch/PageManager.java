/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.joas.volvo_touch_2ch.page.AuthWaitView;
import com.joas.volvo_touch_2ch.page.CardTagView;
import com.joas.volvo_touch_2ch.page.ChargingView;
import com.joas.volvo_touch_2ch.page.ConnectCarWaitView;
import com.joas.volvo_touch_2ch.page.ConnectorWaitView;
import com.joas.volvo_touch_2ch.page.DSPCommErrView;
import com.joas.volvo_touch_2ch.page.EmergencyBoxView;
import com.joas.volvo_touch_2ch.page.FaultBoxView;
import com.joas.volvo_touch_2ch.page.FinishChargingView;
import com.joas.volvo_touch_2ch.page.MessageBoxView;
import com.joas.volvo_touch_2ch.page.PageActivateListener;
import com.joas.volvo_touch_2ch.page.PageAskStopCharging;
import com.joas.volvo_touch_2ch.page.PageID;
import com.joas.volvo_touch_2ch.page.PageReadyView;
import com.joas.volvo_touch_2ch.page.SelectSlowView;
import com.joas.volvo_touch_2ch.page.UnavailableConView;
import com.joas.volvo_touch_2ch.page.UnplugView;

public class PageManager {
    public static final String TAG = "PageManager";

    Context baseContext;
    UIFlowManager flowManager;

    PageReadyView pageReadyView;
    SelectSlowView selectSlowView;
    CardTagView cardTagView;
    AuthWaitView authWaitView;
    ConnectorWaitView connectorWaitView;
    ConnectCarWaitView connectCarWaitView;
    ChargingView chargingView;
    FinishChargingView finishChargingView;
    UnplugView unplugView;
    PageAskStopCharging pageAskStopCharging;


    EmergencyBoxView emergencyBoxView;
    MessageBoxView messageBoxView;
    FaultBoxView faultBoxView;
    DSPCommErrView dspCommErrView;
    UnavailableConView unavailableConView;


    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;

    FrameLayout panelLayout;
    FrameLayout panelCh;
    RelativeLayout panelOver;


    int channel = 0;

    public PageManager(Context context) {
        baseContext = context;
    }


    public void init(int chan, UIFlowManager uiManager, Activity activity) {
        channel = chan;
        flowManager = uiManager;
        mainActivity = activity;
        initPages();
    }

    void initPages() {
        pageReadyView = new PageReadyView(baseContext,flowManager,mainActivity);
        selectSlowView = new SelectSlowView(baseContext, flowManager, mainActivity);
        cardTagView = new CardTagView(baseContext, flowManager, mainActivity);
        authWaitView = new AuthWaitView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        connectCarWaitView = new ConnectCarWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);
        unplugView = new UnplugView(baseContext,flowManager,mainActivity);
        pageAskStopCharging = new PageAskStopCharging(baseContext,flowManager,mainActivity);

        emergencyBoxView = new EmergencyBoxView(baseContext, flowManager, mainActivity);
        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        faultBoxView = new FaultBoxView(baseContext, flowManager, mainActivity);
        dspCommErrView = new DSPCommErrView(baseContext, flowManager, mainActivity);
        unavailableConView = new UnavailableConView(baseContext, flowManager, mainActivity);

        if ( channel == 0 ) {
            panelLayout = mainActivity.findViewById(R.id.panelCh1);
            panelCh = mainActivity.findViewById(R.id.framePageCh1);
            panelOver = mainActivity.findViewById(R.id.PanelCh1Over);
        }
        else {
            panelLayout = mainActivity.findViewById(R.id.panelCh2);
            panelCh = mainActivity.findViewById(R.id.framePageCh2);
            panelOver = mainActivity.findViewById(R.id.PanelCh2Over);
        }

        panelOver.addView(messageBoxView);
        messageBoxView.setVisibility(View.INVISIBLE);

        panelOver.addView(faultBoxView);
        faultBoxView.setVisibility(View.INVISIBLE);

        panelOver.addView(emergencyBoxView);
        emergencyBoxView.setVisibility(View.INVISIBLE);

        panelOver.addView(dspCommErrView);
        dspCommErrView.setVisibility(View.INVISIBLE);

        panelOver.addView(unavailableConView);
        unavailableConView.setVisibility(View.INVISIBLE);

        //changePage(PageID.CONNECTOR_WAIT);
//        changePage(PageID.SELECT_SLOW);
        //초기 페이지 설정
        changePage(PageID.PAGE_READY);
    }

    public PageReadyView getPageReadyView(){return pageReadyView;}
    public SelectSlowView getSelectSlowView() { return selectSlowView; }
    public CardTagView getCardTagView() { return cardTagView; }
    public AuthWaitView getAuthWaitView() { return authWaitView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ConnectCarWaitView getConnectCarWaitView() { return connectCarWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }
    public UnplugView getUnplugView(){return unplugView;}
    public PageAskStopCharging getPageAskStopCharging(){return pageAskStopCharging;}

    public DSPCommErrView getDspCommErrView() { return dspCommErrView; }

    public synchronized void doUiChangePage(PageID prev, PageID cur) {
        if ( prev != PageID.PAGE_END) {
            ((PageActivateListener) getPageViewByID(prev)).onPageDeactivate();
            ((View)getPageViewByID(prev)).setVisibility(View.INVISIBLE);
            try {
                panelCh.removeView((View) getPageViewByID(prev));
            }
            catch (Exception e) {
                Log.e(TAG, "doUIChangePage Remove ex:"+cur.name());
            }
            Log.v(TAG, "UiChange Deact:"+prev.name());
        }
        if ( cur != PageID.PAGE_END) {
            //setCurrentItem(cur.getID());
            try {
                panelCh.addView((View) getPageViewByID(cur));
            }catch (Exception e){
                Log.e(TAG, "doUIChangePage Add ex:"+cur.name());
            }
            ((View)getPageViewByID(cur)).setVisibility(View.VISIBLE);
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
            case PAGE_READY: v = getPageReadyView();
                break;
            case SELECT_SLOW: v = getSelectSlowView();
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
            case PAGE_ASK_STOP_CHG: v = getPageAskStopCharging();
                break;
            case FINISH_CHARGING: v = getFinishChargingView();
                break;
            case UNPLUG: v = getUnplugView();
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
                emergencyBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideEmergencyBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                emergencyBoxView.setVisibility(View.INVISIBLE);
            }
        });
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

    public void showFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showDspCommErrView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dspCommErrView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideDspCommErrView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dspCommErrView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showUnavailableConView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unavailableConView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideUnavailableConView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unavailableConView.setVisibility(View.INVISIBLE);
            }
        });
    }


}
