/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 8. 28 오전 11:50
 *
 */

package com.joas.utils;

public interface SimpleFTPUploadListener {
    void onSimpleFTPUploadFileError(String err);
    void onSimpleFTPUploadFinished();
}
