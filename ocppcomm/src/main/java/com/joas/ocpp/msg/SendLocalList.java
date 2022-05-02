
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * SendLocalListRequest
 * <p>
 * 
 * 
 */
public class SendLocalList {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("listVersion")
    @Expose
    private Integer listVersion;
    @SerializedName("localAuthorizationList")
    @Expose
    private List<LocalAuthorizationList> localAuthorizationList = new ArrayList<LocalAuthorizationList>();
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("updateType")
    @Expose
    private SendLocalList.UpdateType updateType;

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

    public List<LocalAuthorizationList> getLocalAuthorizationList() {
        return localAuthorizationList;
    }

    public void setLocalAuthorizationList(List<LocalAuthorizationList> localAuthorizationList) {
        this.localAuthorizationList = localAuthorizationList;
    }

    /**
     * 
     * (Required)
     * 
     */
    public SendLocalList.UpdateType getUpdateType() {
        return updateType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setUpdateType(SendLocalList.UpdateType updateType) {
        this.updateType = updateType;
    }

    public enum UpdateType {

        @SerializedName("Differential")
        DIFFERENTIAL("Differential"),
        @SerializedName("Full")
        FULL("Full");
        private final String value;
        private final static Map<String, SendLocalList.UpdateType> CONSTANTS = new HashMap<String, SendLocalList.UpdateType>();

        static {
            for (SendLocalList.UpdateType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private UpdateType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SendLocalList.UpdateType fromValue(String value) {
            SendLocalList.UpdateType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
