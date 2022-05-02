/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of TransceiverListener.
 * 
 * @author user
 */
public interface 	TransceiverListener {

	public void onRecvRequest(OCPPMessage message);
	public void onRecvResponse(OCPPMessage message);
	public void onRecvError(OCPPMessage message);
	public void onRequestTimeout(OCPPMessage message);

	/**
	 * Description of the method OnConnectTransport.
	 */
	public void onConnectTransceiver();

	/**
	 * Description of the method OnDisconnectTransport().
	 */
	public void onDisconnectTransceiver();
}
