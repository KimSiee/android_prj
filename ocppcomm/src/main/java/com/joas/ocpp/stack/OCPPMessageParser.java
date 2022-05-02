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
 * Description of OCPPMessageParser.
 * 
 * @author user
 */
public abstract class OCPPMessageParser {
	// Start of user code (user defined attributes for OCPPMessageParser)

	// End of user code

	/**
	 * The constructor.
	 */
	public OCPPMessageParser() {
	}

	/**
	 * Description of the method Serialize.
	 */
	public abstract String Serialize(String action, Object msg);

	/**
	 * Description of the method DeSerialize.
	 */
	public abstract Object DeSerialize(String action, String jsonstr);

}
