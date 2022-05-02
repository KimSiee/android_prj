/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.joas.utils.LogWrapper;

/**
 * Description of TransceiverWAMP.
 * 
 * @author user
 */
public class TransceiverWAMP extends Transceiver {
	private static final String TAG = "TransceiverWAMP";
	private static final int INDEX_MESSAGEID = 0;
	private static final int INDEX_CALL_ACTION = 2;
	private static final int INDEX_CALL_PAYLOAD = 3;
	private static final int INDEX_CALLRESULT_PAYLOAD = 2;


	private static final int INDEX_CALLERROR_ERRORCODE = 2;
	private static final int INDEX_CALLERROR_DESCRIPTION = 3;
	private static final int INDEX_CALLERROR_PAYLOAD = 4;

	private static final int INDEX_UNIQUEID = 1;
	private static final String CALL_FORMAT = "[2,\"%s\",\"%s\",%s]";
	private static final String CALLRESULT_FORMAT = "[3,\"%s\",%s]";
	private static final String CALLERROR_FORMAT = "[4,\"%s\",\"%s\",\"%s\",%s]";

	/**
	 * The constructor.
	 */
	public TransceiverWAMP() {
		super();
		ocppMessageParser = new OCPPJSONParser();
	}

	@Override
	public void onRecvTransportMessage(String msg) {
		OCPPMessage message = parse(msg);

		if ( message != null ) {
			switch (message.getType()) {
				case OCPPMessage.TYPENUMBER_CALL:
					onCall(message);
					break;
				case OCPPMessage.TYPENUMBER_CALLRESULT:
					onCallResult(message);
					break;
				case OCPPMessage.TYPENUMBER_CALLERROR:
					onCallError(message);
					break;
			}
		}
	}

	public void onCall(OCPPMessage message) {
		if ( listener != null ) listener.onRecvRequest(message);
	}

	public void onCallResult(OCPPMessage message) {
		if ( listener != null ) listener.onRecvResponse(message);
	}

	public void onCallError(OCPPMessage message) {
		if ( listener != null ) listener.onRecvError(message);
	}

	protected Object makeCallResult(String uniqueId, String action, Object payload) {
		return String.format(CALLRESULT_FORMAT, uniqueId, payload);
	}

	protected Object makeCall(String uniqueId, String action, Object payload) {
		return String.format(CALL_FORMAT, uniqueId, action, payload);
	}

	protected Object makeCallError(String uniqueId, String action, String errorCode, String errorDescription) {
		return String.format(CALLERROR_FORMAT, uniqueId, errorCode, errorDescription, "{}");
	}

	protected OCPPMessage parse(Object json) {
		OCPPMessage message = null;
		try {
			JsonParser parser = new JsonParser();
			JsonArray array = parser.parse(json.toString()).getAsJsonArray();

			message = new OCPPMessage();
			message.setType(array.get(INDEX_MESSAGEID).getAsInt());
			message.setId(array.get(INDEX_UNIQUEID).getAsString());

			if (array.get(INDEX_MESSAGEID).getAsInt() == OCPPMessage.TYPENUMBER_CALL) {
				message.setAction(array.get(INDEX_CALL_ACTION).getAsString());
				Object payload = ocppMessageParser.DeSerialize(message.getAction(), array.get(INDEX_CALL_PAYLOAD).toString());
				message.setPayload(payload);

				// Call Error 처리
				if ( payload == null ) {
					String errMsg = (String)makeCallError(message.getId(), message.getAction(), "NotImplemented", "NotImplemented");
					this.transport.sendMessage(errMsg);
					return null;
				}
			} else if (array.get(INDEX_MESSAGEID).getAsInt() == OCPPMessage.TYPENUMBER_CALLRESULT) {
				message.setType(OCPPMessage.TYPENUMBER_CALLRESULT);
				Object payload = null;
				String action = null;
				if ( lastSentOCPPRequest != null ) {
					action = lastSentOCPPRequest.getAction() + "Response";
					if (message.getId().equals(lastSentOCPPRequest.getId()) == true) {
						payload = ocppMessageParser.DeSerialize(action, array.get(INDEX_CALLRESULT_PAYLOAD).toString());

						responseAndErrorProcess(RESPONSE_OK);
					} else {
						LogWrapper.v(TAG, "CallResult ID is Wrong:" + lastSentOCPPRequest.getId() + "!=" + message.getId());
					}
				}

				if ( payload == null ) {
					return null;
				}
				else {
					message.setAction(action);
					message.setPayload(payload);
					message.setRequestMsg(lastSentOCPPRequest);
				}
			} else if (array.get(INDEX_MESSAGEID).getAsInt() == OCPPMessage.TYPENUMBER_CALLERROR) {
				message.setErrorCode(array.get(INDEX_CALLERROR_ERRORCODE).getAsString());
				message.setErrorDescription(array.get(INDEX_CALLERROR_DESCRIPTION).getAsString());
				message.setRawPayload(array.get(INDEX_CALLERROR_PAYLOAD).toString());

				responseAndErrorProcess(RESPONSE_ERROR);
			}
		} catch (Exception e) {
			LogWrapper.e(TAG, "parse Ex:" + Log.getStackTraceString(e) );
		}

		return message;
	}

	@Override
	public void processRawPayload(OCPPMessage message, boolean isRequest) {
		String jsonMsg = ocppMessageParser.Serialize(message.getAction(), message.getPayload());
		String msg = "";
		if ( isRequest == true ) msg = (String) makeCall(message.getId(), message.getAction(), jsonMsg);
		else msg = (String) makeCallResult(message.getId(), message.getAction(), jsonMsg);
		message.setRawPayload(msg);
	}

	@Override
	public String processResponseNotSupported(OCPPMessage message) {
		String msg = "";
		msg = (String)makeCallError(message.getId(), message.getAction(),  "NotSupported",  "Requested Action is not supported." );
		return msg;
	}
}
