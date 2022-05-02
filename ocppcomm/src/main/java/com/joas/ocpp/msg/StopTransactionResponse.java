
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * StopTransactionResponse
 * <p>
 * 
 * 
 */
public class StopTransactionResponse {

    @SerializedName("idTagInfo")
    @Expose
    private IdTagInfo idTagInfo;

    public IdTagInfo getIdTagInfo() {
        return idTagInfo;
    }

    public void setIdTagInfo(IdTagInfo idTagInfo) {
        this.idTagInfo = idTagInfo;
    }

}
