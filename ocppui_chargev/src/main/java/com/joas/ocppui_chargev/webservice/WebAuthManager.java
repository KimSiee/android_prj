/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 31 오후 2:53
 *
 */

package com.joas.ocppui_chargev.webservice;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;


public class WebAuthManager {
    static Key key = null;

    public WebAuthManager() {
        if (WebAuthManager.key == null) WebAuthManager.generateKey();
    }

    public static void generateKey() {
        key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public static String getAuthToken(String userid) {
        if (WebAuthManager.key == null) WebAuthManager.generateKey();

        Calendar curTime = Calendar.getInstance();
        curTime.add(Calendar.DAY_OF_MONTH, 1); // 1일
        //curTime.add(Calendar.SECOND, 30); // Test 30초

        Date expiredDate = curTime.getTime();

        String jws = Jwts.builder()
                .setSubject("auth")
                .setHeaderParam("userid", userid)
                .setExpiration(expiredDate)
                .signWith(key)
                .compact();
        return jws;
    }

    public static boolean verifyAuthToken(String token) {
        boolean ret = false;
        try {

            Jwts.parser().requireSubject("auth").setSigningKey(key).parseClaimsJws(token);

            //OK, we can trust this JWT
            ret = true;

        } catch (JwtException e) {

            //don't trust the JWT!
            ret = false;
        }
        return ret;
    }
}
