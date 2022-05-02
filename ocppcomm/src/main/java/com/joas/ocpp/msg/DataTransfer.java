
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * DataTransferRequest
 * <p>
 * 
 * 
 */
public class DataTransfer {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("vendorId")
    @Expose
    private String vendorId;
    @SerializedName("messageId")
    @Expose
    private String messageId;
    @SerializedName("data")
    @Expose
    private String data;

    /**
     * 
     * (Required)
     * 
     */
    public String getVendorId() {
        return vendorId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
