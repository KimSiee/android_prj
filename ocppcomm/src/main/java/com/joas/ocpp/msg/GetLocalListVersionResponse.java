
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetLocalListVersionResponse
 * <p>
 * 
 * 
 */
public class GetLocalListVersionResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("listVersion")
    @Expose
    private Integer listVersion;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getListVersion() {
        return listVersion;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setListVersion(Integer listVersion) {
        this.listVersion = listVersion;
    }

}
