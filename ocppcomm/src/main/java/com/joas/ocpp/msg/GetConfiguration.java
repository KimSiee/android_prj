
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetConfigurationRequest
 * <p>
 * 
 * 
 */
public class GetConfiguration {

    @SerializedName("key")
    @Expose
    private List<String> key = new ArrayList<String>();

    public List<String> getKey() {
        return key;
    }

    public void setKey(List<String> key) {
        this.key = key;
    }

}
