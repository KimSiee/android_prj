/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of TransportListener.
 * 
 * @author user
 */
public interface TransportListener {
	// Start of user code (user defined attributes for TransportListener)

	// End of user code

	/**
	 * Description of the method OnRecvTransportMessage.
	 */
	public void onRecvTransportMessage(String msg);

	/**
	 * Description of the method OnConnectTransport.
	 */
	public void onConnectTransport();

	/**
	 * Description of the method OnDisconnectTransport.
	 */
	public void onDisconnectTransport();

	// Start of user code (user defined methods for TransportListener)

	// End of user code

}
