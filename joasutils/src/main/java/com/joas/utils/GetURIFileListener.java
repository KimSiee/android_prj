/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 1. 16 오후 2:14
 *
 */

package com.joas.utils;

public interface GetURIFileListener {
    void onGetURIFileError(String err);
    void onGetURIFileFinished();
}
