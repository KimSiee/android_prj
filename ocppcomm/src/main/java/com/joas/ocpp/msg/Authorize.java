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
 * AuthorizeRequest
 * <p>
 * 
 * 
 */
public class Authorize {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("idTag")
    @Expose
    private String idTag;

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

}
