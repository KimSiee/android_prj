/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:44
 */

package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.ksoap2.serialization.KvmSerializable;
import org.ksoap2.serialization.PropertyInfo;

import java.util.Hashtable;

/**
 * BootNotificationRequest
 * <p>
 * 
 * 
 */
public class BootNotification implements KvmSerializable {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargePointVendor")
    @Expose
    private String chargePointVendor;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargePointModel")
    @Expose
    private String chargePointModel;
    @SerializedName("chargePointSerialNumber")
    @Expose
    private String chargePointSerialNumber;
    @SerializedName("chargeBoxSerialNumber")
    @Expose
    private String chargeBoxSerialNumber;
    @SerializedName("firmwareVersion")
    @Expose
    private String firmwareVersion;
    @SerializedName("iccid")
    @Expose
    private String iccid;
    @SerializedName("imsi")
    @Expose
    private String imsi;
    @SerializedName("meterType")
    @Expose
    private String meterType;
    @SerializedName("meterSerialNumber")
    @Expose
    private String meterSerialNumber;

    /**
     * 
     * (Required)
     * 
     */
    public String getChargePointVendor() {
        return chargePointVendor;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargePointVendor(String chargePointVendor) {
        this.chargePointVendor = chargePointVendor;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getChargePointModel() {
        return chargePointModel;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargePointModel(String chargePointModel) {
        this.chargePointModel = chargePointModel;
    }

    public String getChargePointSerialNumber() {
        return chargePointSerialNumber;
    }

    public void setChargePointSerialNumber(String chargePointSerialNumber) {
        this.chargePointSerialNumber = chargePointSerialNumber;
    }

    public String getChargeBoxSerialNumber() {
        return chargeBoxSerialNumber;
    }

    public void setChargeBoxSerialNumber(String chargeBoxSerialNumber) {
        this.chargeBoxSerialNumber = chargeBoxSerialNumber;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getMeterSerialNumber() {
        return meterSerialNumber;
    }

    public void setMeterSerialNumber(String meterSerialNumber) {
        this.meterSerialNumber = meterSerialNumber;
    }

    @Override
    public Object getProperty(int i) {
        return null;
    }

    @Override
    public int getPropertyCount() {
        return 0;
    }

    @Override
    public void setProperty(int i, Object o) {

    }

    @Override
    public void getPropertyInfo(int i, Hashtable hashtable, PropertyInfo propertyInfo) {

    }
}
