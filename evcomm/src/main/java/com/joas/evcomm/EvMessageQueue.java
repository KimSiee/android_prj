/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 22 오전 11:57
 *
 */

package com.joas.evcomm;

import java.util.ArrayDeque;

public class EvMessageQueue {
    private ArrayDeque<EvPacket> queue;
    /**
     * The constructor.
     */
    public EvMessageQueue() {
        queue = new ArrayDeque<EvPacket>();
    }

    /**
     * Description of the method AddMessage.
     */
    public void add(EvPacket msg) {
        synchronized (queue) {
            if (msg != null) queue.add(msg);
        }
    }

    /**
     * Description of the method PopMessage.
     */
    public EvPacket pop() {
        synchronized (queue) {
            if (!queue.isEmpty()) return queue.pop();
        }
        return null;
    }

    /**
     * Description of the method TopMessage.
     */
    public EvPacket top() {
        synchronized (queue) {
            if (!queue.isEmpty()) return queue.peek();
        }
        return null;
    }

    public void clear() {
        synchronized (queue) {
            queue.clear();
        }
    }
}
