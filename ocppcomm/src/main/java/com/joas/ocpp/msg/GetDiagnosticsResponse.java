
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetDiagnosticsResponse
 * <p>
 * 
 * 
 */
public class GetDiagnosticsResponse {

    @SerializedName("fileName")
    @Expose
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
