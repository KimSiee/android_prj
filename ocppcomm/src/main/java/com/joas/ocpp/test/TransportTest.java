package com.joas.ocpp.test;

import com.joas.ocpp.stack.Transport;
import com.joas.ocpp.stack.TransportListener;
import com.joas.ocpp.stack.TransportWebSocket;
import com.joas.utils.LogWrapper;

/**
 * Created by user on 2017-05-31.
 */

public class TransportTest implements TransportListener {
    private static final String TAG = "TransportTest";

    public TransportTest() {
    }

    public void testTransportWebsocket() {
        String jsonmsg = "{\"chargeBoxSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointModel\":\"NQC-ACDC\","+
                "\"chargePointSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointVendor\":\"DBT\","+
                "\"firmwareVersion\":\"1.0.49\",\"meterSerialNumber\":\"gir.vat.mx.000e48\",\"meterType\":\"DBT NQC-ACDC\"}";
        String callMsg = "[2, \"19223201\",\"BootNotification\", "+jsonmsg+"]";

        Transport testTS = new TransportWebSocket();
        testTS.setListener(this);

        // CPID 테스트(URI뒷부분에)
        testTS.setCPID("CP12");

        // Basic Auth Test
        testTS.setBasicAuthID("joas");
        testTS.setBasicAuthPassword("j1234");

        // SSL Test!!
        //testTS.setSSLCertFile(Environment.getExternalStorageDirectory() + File.separator+"server.crt");

        //WebSocket 연결 테스트
        testTS.setConnectURI("ws://192.168.0.48:9000/ocpp");

        testTS.connect();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        testTS.sendMessage(callMsg);
    }

    @Override
    public void onRecvTransportMessage(String msg) {
        LogWrapper.v(TAG, "Recv:"+msg);
    }

    @Override
    public void onConnectTransport() {
        LogWrapper.v(TAG, "OnConnectTransport");
    }

    @Override
    public void onDisconnectTransport() {
        LogWrapper.v(TAG, "OnDisconnectTransport");
    }
}
