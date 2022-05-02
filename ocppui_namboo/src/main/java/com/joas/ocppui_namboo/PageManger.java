/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 22. 5. 2. 오후 4:22
 *
 */

package com.joas.ocppui_namboo;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.joas.ocppui_namboo.R;
import com.joas.ocppui_namboo.page.AdminPasswordInputView;
import com.joas.ocppui_namboo.page.AuthWaitView;
import com.joas.ocppui_namboo.page.CardTagView;
import com.joas.ocppui_namboo.page.ChargingView;
import com.joas.ocppui_namboo.page.ConnectCarWaitView;
import com.joas.ocppui_namboo.page.ConnectorWaitView;
import com.joas.ocppui_namboo.page.DSPCommErrView;
import com.joas.ocppui_namboo.page.EmergencyBoxView;
import com.joas.ocppui_namboo.page.FaultBoxView;
import com.joas.ocppui_namboo.page.FinishChargingView;
import com.joas.ocppui_namboo.page.JoasCommMonitorView;
import com.joas.ocppui_namboo.page.JoasDSPMonitorView;
import com.joas.ocppui_namboo.page.JoasDebugMsgView;
import com.joas.ocppui_namboo.page.MessageBoxView;
import com.joas.ocppui_namboo.page.PageActivateListener;
import com.joas.ocppui_namboo.page.PageID;
import com.joas.ocppui_namboo.page.SelectFastView;
import com.joas.ocppui_namboo.page.SelectSlowView;
import com.joas.ocppui_namboo.page.SettingView;
import com.joas.ocppui_namboo.page.UnavailableConView;

public class PageManger {
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

    EmergencyBoxView emergencyBoxView;
    MessageBoxView messageBoxView;
    FaultBoxView faultBoxView;
    DSPCommErrView dspCommErrView;
    UnavailableConView unavailableConView;
    JoasCommMonitorView commMonitorView;
    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;
    AdminPasswordInputView adminPasswordInputView;

    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;
    ConstraintLayout mainLayout;
    FrameLayout frameViewSub;

    public PageManger(Context context) {
        baseContext = context;
    }


    public void init(UIFlowManager uiManager, Activity activity) {
        flowManager = uiManager;
        mainActivity = activity;
        initPages();
    }

    void initPages() {
        selectSlowView = new SelectSlowView(baseContext, flowManager, mainActivity);
        selectFastView = new SelectFastView(baseContext, flowManager, mainActivity);
        cardTagView = new CardTagView(baseContext, flowManager, mainActivity);
        authWaitView = new AuthWaitView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        connectCarWaitView = new ConnectCarWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);

        emergencyBoxView = new EmergencyBoxView(baseContext, flowManager, mainActivity);
        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        faultBoxView = new FaultBoxView(baseContext, flowManager, mainActivity);
        dspCommErrView = new DSPCommErrView(baseContext, flowManager, mainActivity);
        unavailableConView = new UnavailableConView(baseContext, flowManager, mainActivity);

        settingView = new SettingView(baseContext, flowManager, mainActivity);
        commMonitorView = new JoasCommMonitorView(baseContext, flowManager, mainActivity);
        joasDSPMonitorView = new JoasDSPMonitorView(baseContext, flowManager, mainActivity);
        joasDebugMsgView = new JoasDebugMsgView(baseContext, flowManager, mainActivity);

        adminPasswordInputView = new AdminPasswordInputView(mainActivity, flowManager, mainActivity);

        flowManager.getDspControl().setDspMonitorListener(joasDSPMonitorView);

        CoordinatorLayout MainLayout = (CoordinatorLayout)mainActivity.findViewById(R.id.layoutMain);
        frameViewSub = (FrameLayout) mainActivity.findViewById(R.id.viewsub_main);

        RelativeLayout layoutMsg = (RelativeLayout)mainActivity.findViewById(R.id.layoutMsg);

        layoutMsg.addView(messageBoxView);
        messageBoxView.setVisibility(View.INVISIBLE);

        layoutMsg.addView(faultBoxView);
        faultBoxView.setVisibility(View.INVISIBLE);

        layoutMsg.addView(emergencyBoxView);
        emergencyBoxView.setVisibility(View.INVISIBLE);

        layoutMsg.addView(dspCommErrView);
        dspCommErrView.setVisibility(View.INVISIBLE);

        layoutMsg.addView(unavailableConView);
        unavailableConView.setVisibility(View.INVISIBLE);

        MainLayout.addView(settingView);
        settingView.setVisibility(View.INVISIBLE);

        MainLayout.addView(commMonitorView);
        commMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDSPMonitorView);
        joasDSPMonitorView.setVisibility(View.INVISIBLE);

        MainLayout.addView(joasDebugMsgView);
        joasDebugMsgView.setVisibility(View.INVISIBLE);


        MainLayout.addView(adminPasswordInputView);
        adminPasswordInputView.setVisibility(View.INVISIBLE);


        if ( flowManager.getCpConfig().isFastCharger ) {
            changePage(PageID.SELECT_FAST);
        }
        else {
            changePage(PageID.SELECT_SLOW);
        }
    }

    public SelectSlowView getSelectSlowView() { return selectSlowView; }
    public SelectFastView getSelectFastView() { return selectFastView; }
    public CardTagView getCardTagView() { return cardTagView; }
    public AuthWaitView getAuthWaitView() { return authWaitView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ConnectCarWaitView getConnectCarWaitView() { return connectCarWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }
    public SettingView getSettingView() { return settingView; }
    public DSPCommErrView getDspCommErrView() { return dspCommErrView; }
    public JoasDebugMsgView getJoasDebugMsgView() { return joasDebugMsgView; }
    public JoasCommMonitorView getCommMonitorView() { return commMonitorView; }

    public synchronized void doUiChangePage(PageID prev, PageID cur) {
        if ( prev != PageID.PAGE_END) {
            ((PageActivateListener) getPageViewByID(prev)).onPageDeactivate();
            ((View)getPageViewByID(prev)).setVisibility(View.INVISIBLE);
            try {
                frameViewSub.removeView((View) getPageViewByID(prev));
            }
            catch (Exception e) {
                Log.e(TAG, "doUIChangePage Remove ex:"+cur.name());
            }
            Log.v(TAG, "UiChange Deact:"+prev.name());
        }
        if ( cur != PageID.PAGE_END) {
            //setCurrentItem(cur.getID());
            try {
                frameViewSub.addView((View) getPageViewByID(cur));
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

    public void showAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.VISIBLE);
            }
        });
    }

    public boolean isShowAdminPassswordInputView() {
        return adminPasswordInputView.getVisibility() == View.VISIBLE;
    }

    public void hideAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void showSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.onPageActivate();
                settingView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.setVisibility(View.INVISIBLE);
                settingView.onPageDeactivate();
            }
        });
    }
}
