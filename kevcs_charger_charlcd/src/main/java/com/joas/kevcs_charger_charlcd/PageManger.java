/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kevcs_charger_charlcd;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.joas.kevcs_charger_charlcd.page.ConnectorWaitView;
import com.joas.kevcs_charger_charlcd.page.CreditCardPayView;
import com.joas.kevcs_charger_charlcd.page.EmergencyBoxView;
import com.joas.kevcs_charger_charlcd.page.FaultBoxView;
import com.joas.kevcs_charger_charlcd.page.FinishChargingErrorView;
import com.joas.kevcs_charger_charlcd.page.FinishChargingView;
import com.joas.kevcs_charger_charlcd.page.InputCostView;
import com.joas.kevcs_charger_charlcd.page.InputKwhView;
import com.joas.kevcs_charger_charlcd.page.InputNumberView;
import com.joas.kevcs_charger_charlcd.page.InputPasswordView;
import com.joas.kevcs_charger_charlcd.page.JoasDSPMonitorView;
import com.joas.kevcs_charger_charlcd.page.JoasDebugMsgView;
import com.joas.kevcs_charger_charlcd.page.MessageBoxView;
import com.joas.kevcs_charger_charlcd.page.PageActivateListener;
import com.joas.kevcs_charger_charlcd.page.PageID;
import com.joas.kevcs_charger_charlcd.page.SelectAmountTypeView;
import com.joas.kevcs_charger_charlcd.page.SelectAuthView;
import com.joas.kevcs_charger_charlcd.page.SelectFastView;
import com.joas.kevcs_charger_charlcd.page.SelectSlowView;
import com.joas.kevcs_charger_charlcd.page.SettingView;
import com.joas.kevcs_charger_charlcd.page.StartView;
import com.joas.kevcs_charger_charlcd.page.UnPlugView;
import com.joas.kevcs.ui.R;
import com.joas.kevcs_charger_charlcd.page.AdminPasswordInputView;
import com.joas.kevcs_charger_charlcd.page.AuthMemberCardView;
import com.joas.kevcs_charger_charlcd.page.AuthPayWaitView;
import com.joas.kevcs_charger_charlcd.page.ChargingStopAskView;
import com.joas.kevcs_charger_charlcd.page.ChargingView;
import com.joas.kevcs_charger_charlcd.page.ConnectCarWaitView;

public class PageManger {
    public static final String TAG = "PageManager";

    Context baseContext;
    UIFlowManager flowManager;

    StartView startView;
    SelectFastView selectFastView;
    SelectSlowView selectSlowView;
    SelectAuthView selectAuthView;
    AuthMemberCardView authMemberCardView;
    InputNumberView inputNumberView;
    InputPasswordView inputPasswordView;
    SelectAmountTypeView selectAmountTypeView;
    InputCostView inputCostView;
    InputKwhView inputKwhView;
    CreditCardPayView creditCardPayView;
    AuthPayWaitView authPayWaitView;
    ConnectorWaitView connectorWaitView;
    ConnectCarWaitView connectCarWaitView;
    ChargingView chargingView;
    FinishChargingView finishChargingView;
    FinishChargingErrorView finishChargingErrorView;
    ChargingStopAskView chargingStopAskView;
    UnPlugView unPlugView;

    SettingView settingView;

    EmergencyBoxView emergencyBoxView;
    MessageBoxView messageBoxView;
    FaultBoxView faultBoxView;

    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;

    AdminPasswordInputView adminPasswordInputView;

    ConstraintLayout mainLayout;
    FrameLayout frameViewSub;
    RelativeLayout frameMsg;

    PageID prevPageID = PageID.PAGE_END;
    PageID curPageID = PageID.PAGE_END;

    Activity mainActivity;

    public PageManger(Context context) {
        baseContext = context;
    }

    public void init(UIFlowManager uiManager, Activity activity) {
        flowManager = uiManager;
        mainActivity = activity;
        initPages();
    }

    void initPages() {
        startView = new StartView(baseContext, flowManager, mainActivity);
        selectFastView = new SelectFastView(baseContext, flowManager, mainActivity);
        selectSlowView = new SelectSlowView(baseContext, flowManager, mainActivity);
        selectAuthView = new SelectAuthView(baseContext, flowManager, mainActivity);
        authMemberCardView = new AuthMemberCardView(baseContext, flowManager, mainActivity);
        inputNumberView = new InputNumberView(baseContext, flowManager, mainActivity);
        inputPasswordView = new InputPasswordView(baseContext, flowManager, mainActivity);
        selectAmountTypeView = new SelectAmountTypeView(baseContext, flowManager, mainActivity);
        inputCostView = new InputCostView(baseContext, flowManager, mainActivity);
        inputKwhView = new InputKwhView(baseContext, flowManager, mainActivity);
        creditCardPayView = new CreditCardPayView(baseContext, flowManager, mainActivity);
        authPayWaitView = new AuthPayWaitView(baseContext, flowManager, mainActivity);
        connectorWaitView = new ConnectorWaitView(baseContext, flowManager, mainActivity);
        connectCarWaitView = new ConnectCarWaitView(baseContext, flowManager, mainActivity);
        chargingView = new ChargingView(baseContext, flowManager, mainActivity);
        finishChargingView = new FinishChargingView(baseContext, flowManager, mainActivity);
        finishChargingErrorView = new FinishChargingErrorView(baseContext, flowManager, mainActivity);
        unPlugView = new UnPlugView(baseContext, flowManager, mainActivity);

        settingView = new SettingView(baseContext, flowManager, mainActivity);
        chargingStopAskView = new ChargingStopAskView(baseContext, flowManager, mainActivity);
        emergencyBoxView = new EmergencyBoxView(baseContext, flowManager, mainActivity);
        messageBoxView = new MessageBoxView(baseContext, flowManager, mainActivity);
        faultBoxView = new FaultBoxView(baseContext, flowManager, mainActivity);

        joasDSPMonitorView = new JoasDSPMonitorView(baseContext, flowManager, mainActivity);
        joasDebugMsgView = new JoasDebugMsgView(baseContext, flowManager, mainActivity);
        adminPasswordInputView = new AdminPasswordInputView(mainActivity, flowManager, mainActivity);

        flowManager.getDspControl().setDspMonitorListener(joasDSPMonitorView);

        frameViewSub = (FrameLayout) mainActivity.findViewById(R.id.viewsub_main);
        frameMsg = mainActivity.findViewById(R.id.frameMsg);

        mainLayout = (ConstraintLayout)mainActivity.findViewById(R.id.layoutMain);
        mainLayout.addView(messageBoxView);

        mainLayout.addView(chargingStopAskView);
        chargingStopAskView.setVisibility(View.INVISIBLE);

        frameMsg.addView(faultBoxView);
        frameMsg.addView(emergencyBoxView);
        mainLayout.addView(settingView);
        settingView.setVisibility(View.INVISIBLE);

        mainLayout.addView(joasDSPMonitorView);
        joasDSPMonitorView.setVisibility(View.INVISIBLE);

        mainLayout.addView(adminPasswordInputView);
        adminPasswordInputView.setVisibility(View.INVISIBLE);

        messageBoxView.setVisibility(View.INVISIBLE);
        faultBoxView.setVisibility(View.INVISIBLE);

        emergencyBoxView.setVisibility(View.INVISIBLE);

        mainLayout.addView(joasDebugMsgView);
        joasDebugMsgView.setVisibility(View.INVISIBLE);

        changePage(PageID.START);
    }

    public StartView getStartView() { return startView; }
    public SelectFastView getSelectFastView() { return selectFastView; }
    public SelectSlowView getSelectSlowView() { return selectSlowView; }
    public SelectAuthView getSelectAuthView() { return selectAuthView; }
    public AuthMemberCardView getAuthMemberCardView() { return authMemberCardView; }
    public InputNumberView getInputNumberView() { return inputNumberView; }
    public InputPasswordView getInputPasswordView() { return inputPasswordView; }
    public SelectAmountTypeView getSelectAmountTypeView() {  return selectAmountTypeView; }
    public InputCostView getInputCostView() { return inputCostView; }
    public InputKwhView getInputKwhView() { return inputKwhView; }
    public CreditCardPayView getCreditCardPayView() { return creditCardPayView; }
    public AuthPayWaitView getAuthPayWaitView() { return authPayWaitView; }
    public ConnectorWaitView getConnectorWaitView() { return connectorWaitView; }
    public ConnectCarWaitView getConnectCarWaitView() { return connectCarWaitView; }
    public ChargingView getChargingView() { return chargingView; }
    public FinishChargingView getFinishChargingView() { return finishChargingView; }
    public FinishChargingErrorView getFinishChargingErrorView() { return finishChargingErrorView; }
    public ChargingStopAskView getChargingStopAskView() { return chargingStopAskView; }
    public UnPlugView getUnPlugView() { return unPlugView; }

    public SettingView getSettingView() { return settingView; }
    public JoasDebugMsgView getJoasDebugMsgView() { return joasDebugMsgView; }

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
                Log.e(TAG, "doUIChangePage Add cur:"+cur.name()+", e:"+e.toString());
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
            case START: v = getStartView();
                break;
            case SELECT_FAST: v = getSelectFastView();
                break;
            case SELECT_SLOW: v = getSelectSlowView();
                break;
            case SELECT_AUTH: v = getSelectAuthView();
                break;
            case AUTH_MEMBER_CARD: v = getAuthMemberCardView();
                break;
            case INPUT_NUMBER: v = getInputNumberView();
                break;
            case INPUT_PASSWORD: v = getInputPasswordView();
                break;
            case SELECT_AMOUNT_TYPE: v = getSelectAmountTypeView();
                break;
            case INPUT_AMOUNT_COST: v = getInputCostView();
                break;
            case INPUT_AMOUNT_KWH: v = getInputKwhView();
                break;
            case CREDIT_CARD_PAY: v = getCreditCardPayView();
                break;
            case AUTH_PAY_WAIT: v = getAuthPayWaitView();
                break;
            case CONNECTOR_WAIT: v = getConnectorWaitView();
                break;
            case CONNECT_CAR_WAIT: v = getConnectCarWaitView();
                break;
            case CHARGING: v = getChargingView();
                break;
            case FINISH_CHARGING: v = getFinishChargingView();
                break;
            case FINISH_CHARGING_ERRROR: v = getFinishChargingErrorView();
                break;
            case UNPLUG: v = getUnPlugView();
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
                messageBoxView.onPageActivate();
            }
        });
    }

    public void hideMessageBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageBoxView.setVisibility(View.INVISIBLE);
                messageBoxView.onPageDeactivate();
            }
        });
    }

    public void showFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.onPageActivate();
                faultBoxView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideFaultBox() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faultBoxView.setVisibility(View.INVISIBLE);
                faultBoxView.onPageDeactivate();
            }
        });
    }

    public boolean isShowFaultBox() {
        if ( faultBoxView == null ) return false;
        return faultBoxView.getVisibility() == View.VISIBLE;
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
                adminPasswordInputView.onPageActivate();
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
                adminPasswordInputView.onPageDeactivate();
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

    public void showChargingStopAskView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chargingStopAskView.onPageActivate();
                chargingStopAskView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideChargingStopAskView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chargingStopAskView.onPageDeactivate();
                chargingStopAskView.setVisibility(View.INVISIBLE);
            }
        });
    }
}
