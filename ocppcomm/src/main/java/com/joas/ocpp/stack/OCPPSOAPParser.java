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
 * Description of OCPPSOAPParser.
 * 
 * @author user
 */
public class OCPPSOAPParser extends OCPPMessageParser {
	// Start of user code (user defined attributes for OCPPSOAPParser)

	// End of user code

	/**
	 * The constructor.
	 */
	public OCPPSOAPParser() {
		// Start of user code constructor for OCPPSOAPParser)
		super();
		// End of user code
	}

	@Override
	public String Serialize(String action, Object msg) {
		return null;
	}

	@Override
	public Object DeSerialize(String action, String jsonstr) {
		return null;
	}

	// Start of user code (user defined methods for OCPPSOAPParser)

	// End of user code

}
