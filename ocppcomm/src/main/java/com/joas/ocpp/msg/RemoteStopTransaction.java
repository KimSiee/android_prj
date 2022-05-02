
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * RemoteStopTransactionRequest
 * <p>
 * 
 * 
 */
public class RemoteStopTransaction {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("transactionId")
    @Expose
    private Integer transactionId;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getTransactionId() {
        return transactionId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

}
