
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ChangeConfigurationRequest
 * <p>
 * 
 * 
 */
public class ChangeConfiguration {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("key")
    @Expose
    private String key;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("value")
    @Expose
    private String value;

    /**
     * 
     * (Required)
     * 
     */
    public String getKey() {
        return key;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

}
