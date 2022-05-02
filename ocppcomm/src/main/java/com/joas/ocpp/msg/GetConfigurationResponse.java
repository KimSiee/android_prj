
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetConfigurationResponse
 * <p>
 * 
 * 
 */
public class GetConfigurationResponse {

    @SerializedName("configurationKey")
    @Expose
    private List<ConfigurationKey> configurationKey = new ArrayList<ConfigurationKey>();
    @SerializedName("unknownKey")
    @Expose
    private List<String> unknownKey = new ArrayList<String>();

    public List<ConfigurationKey> getConfigurationKey() {
        return configurationKey;
    }

    public void setConfigurationKey(List<ConfigurationKey> configurationKey) {
        this.configurationKey = configurationKey;
    }

    public List<String> getUnknownKey() {
        return unknownKey;
    }

    public void setUnknownKey(List<String> unknownKey) {
        this.unknownKey = unknownKey;
    }

}
