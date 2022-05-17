/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:40
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of OCPPStackProperty.
 * 
 * @author user
 */
public class OCPPStackProperty {
	// Connection Infos
	public String cpid 			= "01";
	public String serverUri 		= "ws://127.0.0.1:9000/ocpp";
	public boolean useBasicAuth = false;
	public String authID			= "user";
	public String authPassword	= "password";
	public boolean useSSL 		= false;
	public boolean useSSLCheckCert = false;
	public String sslKeyFile		= "server.crt";

	// Boot Notification Info
	public String chargePointVender = "JOAS";
	public String chargePointModel	= "JC-92B1-11-01";
	public String chargePointSerialNumber = "0000001";
	public String chargeBoxSerialNumber = "0000001";
	public String firmwareVersion 	= "0.0.1";
	public String meterType		  	= "AC";
	public String meterSerialNumber = "0000001";

	/**
	 * The constructor.
	 */
	public OCPPStackProperty() {
		// Start of user code constructor for OCPPStackProperty)
		super();
		// End of user code
	}
}
