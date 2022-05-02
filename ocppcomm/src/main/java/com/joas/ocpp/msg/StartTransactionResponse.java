
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * StartTransactionResponse
 * <p>
 * 
 * 
 */
public class StartTransactionResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("idTagInfo")
    @Expose
    private IdTagInfo idTagInfo;
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
    public IdTagInfo getIdTagInfo() {
        return idTagInfo;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setIdTagInfo(IdTagInfo idTagInfo) {
        this.idTagInfo = idTagInfo;
    }

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
