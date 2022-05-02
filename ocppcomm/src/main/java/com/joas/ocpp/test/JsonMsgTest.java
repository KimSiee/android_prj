package com.joas.ocpp.test;

import com.google.gson.Gson;
import com.joas.ocpp.msg.*;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import android.util.Log;

/**
 * Created by scchoi on 2017-05-24.
 */

public class JsonMsgTest {
    private static final String TAG = "JsonMsgTest";
    Gson gson;
    public JsonMsgTest() {
        gson = new Gson();
    }

    public boolean testBootNotificationDeserialize() {
        String jsonmsg = "{\"chargeBoxSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointModel\":\"NQC-ACDC\","+
                "\"chargePointSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointVendor\":\"DBT\","+
                "\"firmwareVersion\":\"1.0.49\",\"meterSerialNumber\":\"gir.vat.mx.000e48\",\"meterType\":\"DBT NQC-ACDC\"}";


        BootNotification bootNotification = gson.fromJson(jsonmsg, BootNotification.class);

        if (jsonmsg != null) {
            if (!bootNotification.getChargePointVendor().equals("DBT")) return false;
            if (!bootNotification.getChargePointModel().equals("NQC-ACDC")) return false;
            if (!bootNotification.getChargePointSerialNumber().equals("gir.vat.mx.000e48")) return false;
            if (!bootNotification.getChargeBoxSerialNumber().equals("gir.vat.mx.000e48")) return false;
            if (!bootNotification.getFirmwareVersion().equals("1.0.49")) return false;
            if (!bootNotification.getMeterType().equals("DBT NQC-ACDC")) return false;
            if (!bootNotification.getMeterSerialNumber().equals("gir.vat.mx.000e48")) return false;

            Log.v(TAG, "getChargePointVendor:" + bootNotification.getChargePointVendor());
            Log.v(TAG, "getChargePointModel:" + bootNotification.getChargePointModel());
            Log.v(TAG, "getChargePointSerialNumber:" + bootNotification.getChargePointSerialNumber());
            Log.v(TAG, "getChargeBoxSerialNumber:" + bootNotification.getChargeBoxSerialNumber());
            Log.v(TAG, "getFirmwareVersion:" + bootNotification.getFirmwareVersion());
            Log.v(TAG, "getMeterType:" + bootNotification.getMeterType());
            Log.v(TAG, "getMeterSerialNumber:" + bootNotification.getMeterSerialNumber());
        }
        return true;
    }

    public boolean testBootNotificationSerialize() {
        String jsonmsg = "{\"chargeBoxSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointModel\":\"NQC-ACDC\","+
                        "\"chargePointSerialNumber\":\"gir.vat.mx.000e48\",\"chargePointVendor\":\"DBT\","+
                        "\"firmwareVersion\":\"1.0.49\",\"meterSerialNumber\":\"gir.vat.mx.000e48\",\"meterType\":\"DBT NQC-ACDC\"}";

        BootNotification bootNotification = new BootNotification();
        bootNotification.setChargePointVendor("DBT");
        bootNotification.setChargePointModel("NQC-ACDC");
        bootNotification.setChargePointSerialNumber("gir.vat.mx.000e48");
        bootNotification.setChargeBoxSerialNumber("gir.vat.mx.000e48");
        bootNotification.setFirmwareVersion("1.0.49");
        bootNotification.setMeterType("DBT NQC-ACDC");
        bootNotification.setMeterSerialNumber("gir.vat.mx.000e48");

        String str = gson.toJson(bootNotification, BootNotification.class);

        Log.v(TAG, "jsonEncode:"+str);
        return  str.equals(jsonmsg);
    }

}
