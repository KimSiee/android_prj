
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LocalAuthorizationList {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("idTag")
    @Expose
    private String idTag;
    @SerializedName("idTagInfo")
    @Expose
    private IdTagInfo idTagInfo;

    /**
     * 
     * (Required)
     * 
     */
    public String getIdTag() {
        return idTag;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public IdTagInfo getIdTagInfo() {
        return idTagInfo;
    }

    public void setIdTagInfo(IdTagInfo idTagInfo) {
        this.idTagInfo = idTagInfo;
    }

}
