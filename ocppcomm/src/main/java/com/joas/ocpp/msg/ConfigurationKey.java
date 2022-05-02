
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ConfigurationKey {

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
    @SerializedName("readonly")
    @Expose
    private Boolean readonly;
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
    public Boolean getReadonly() {
        return readonly;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
