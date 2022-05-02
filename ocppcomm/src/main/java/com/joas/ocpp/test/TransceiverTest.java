package com.joas.ocpp.test;

import com.joas.ocpp.msg.BootNotification;
import com.joas.ocpp.stack.OCPPMessage;
import com.joas.ocpp.stack.Transceiver;
import com.joas.ocpp.stack.TransceiverListener;
import com.joas.ocpp.stack.TransceiverWAMP;
import com.joas.ocpp.stack.Transport;
import com.joas.ocpp.stack.TransportWebSocket;

/**
 * Created by user on 2017-06-12.
 */

public class TransceiverTest implements TransceiverListener {
    private static final String TAG = "TransceiverTest";
    Transceiver testTrans;

    public void testTransceiverWAMP() {
        String jsonmsg = "{\"chargeBoxSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointModel\":\"NQC-ACDC\","+
                "\"chargePointSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointVendor\":\"DBT\","+
                "\"firmwareVersion\":\"1.0.49\",\"meterSerialNumber\":\"gir.vat.mx.000e48\",\"meterType\":\"DBT NQC-ACDC\"}";
        String callMsg = "[2, \"19223201\",\"BootNotification\", "+jsonmsg+"]";

        Transport testTS = new TransportWebSocket();


        testTrans = new TransceiverWAMP();
        testTrans.setTransport(testTS);
        testTrans.setListener(this);

        // CPID 테스트(URI뒷부분에)
        testTS.setCPID("CP12");

        // Basic Auth Test
        testTS.setBasicAuthID("joas");
        testTS.setBasicAuthPassword("j1234");

        // SSL Test!!
        //testTS.setSSLCertFile(Environment.getExternalStorageDirectory() + File.separator+"server.crt");

        //WebSocket 연결 테스트
        testTS.setConnectURI("ws://192.168.0.48:9000/ocpp");

        //testTS.connect();

        testTrans.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }


        //testTS.sendMessage(callMsg);

        //testTrans.
    }

    @Override
    public void onRecvRequest(OCPPMessage msg) {

    }

    @Override
    public void onRecvResponse(OCPPMessage msg) {

    }

    @Override
    public void onRecvError(OCPPMessage msg) {

    }

    @Override
    public void onRequestTimeout(OCPPMessage msg) {

    }

    @Override
    public void onConnectTransceiver() {
        BootNotification bootNotification = new BootNotification();
        bootNotification.setChargePointVendor("JOAS");
        bootNotification.setChargePointModel("NQC-ACDC");
        bootNotification.setChargePointSerialNumber("gir.vat.mx.000e48");
        bootNotification.setChargeBoxSerialNumber("gir.vat.mx.000e48");
        bootNotification.setFirmwareVersion("1.0.49");
        bootNotification.setMeterType("DBT NQC-ACDC");
        bootNotification.setMeterSerialNumber("gir.vat.mx.000e48");

        OCPPMessage msg = new OCPPMessage("1234", "BootNotification", bootNotification);

        testTrans.sendRequest(msg);
    }

    @Override
    public void onDisconnectTransceiver() {

    }

}
