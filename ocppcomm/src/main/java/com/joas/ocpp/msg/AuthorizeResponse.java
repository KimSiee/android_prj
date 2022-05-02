/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:43
 */

package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * AuthorizeResponse
 * <p>
 * 
 * 
 */
public class AuthorizeResponse {

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

}
