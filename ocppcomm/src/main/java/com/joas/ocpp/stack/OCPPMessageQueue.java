/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:40
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

import android.util.Log;

import com.joas.ocpp.msg.MeterValues;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.utils.LogWrapper;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Description of OCPPMessageQueue.
 * 
 * @author user
 */
public class OCPPMessageQueue {
	private ArrayDeque<OCPPMessage> queue;
	/**
	 * The constructor.
	 */
	public OCPPMessageQueue() {
		queue = new ArrayDeque<OCPPMessage>();
	}

	/**
	 * Description of the method AddMessage.
	 */
	public void add(OCPPMessage msg) {
		synchronized (queue) {
			if (msg != null) queue.add(msg);
		}
	}

	/**
	 * Description of the method PopMessage.
	 */
	public OCPPMessage pop() {
		synchronized (queue) {
			if (!queue.isEmpty()) return queue.pop();
		}
		return null;
	}

	/**
	 * Description of the method TopMessage.
	 */
	public OCPPMessage peek() {
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

	public boolean isEmpty() {
		boolean ret = true;
		synchronized (queue) {
			ret = queue.isEmpty();
		}
		return ret;
	}

	public void findAndUpdateTransactionID(String startTime, int transactionID) {
		synchronized (queue) {

			for (OCPPMessage ocppMessage : queue) {
				if ( ocppMessage.action.equals("StartTransaction") == false ) {
					if ( ocppMessage.getTransactionStartTime().equals(startTime) ) {
						if ( ocppMessage.action.equals("StopTransaction") ) {
							StopTransaction stopTransaction = (StopTransaction)ocppMessage.getPayload();
							stopTransaction.setTransactionId(transactionID);
						}
						else if ( ocppMessage.action.equals("MeterValues") ) {
							MeterValues meterValues = (MeterValues )ocppMessage.getPayload();
							meterValues.setTransactionId(transactionID);
						}
					}
				}
			}
		}
	}
}
