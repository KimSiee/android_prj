package com.joas.ocpp.test;

import com.joas.ocpp.stack.OCPPStack;
import com.joas.ocpp.stack.OCPPStackProperty;
import com.joas.ocpp.stack.Transceiver;

/**
 * Created by user on 2017-06-14.
 */

public class OCPPStackTest {
    private static final String TAG = "OCPPStackTest";
    OCPPStack stack;

    public void testOCPPStack() {
        OCPPStackProperty newOcppProperty = new OCPPStackProperty();
        newOcppProperty.cpid = "1234";
        newOcppProperty.useBasicAuth = true;
        newOcppProperty.authID = "joas";
        newOcppProperty.authPassword = "j1234";
        newOcppProperty.serverUri = "ws://192.168.0.48:9000/ocpp";

        stack = new OCPPStack(null, null, "", false);
        stack.init("ocpp-j", newOcppProperty);
        stack.startOcpp();
    }

    public void testAuth() {
        stack.sendAuthorizeRequest("1234567890123456");
    }
}
