/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:39
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

import com.joas.ocpp.msg.MeterValues;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * Description of OCPPMessage.
 * 
 * @author user
 */
public class OCPPMessage {
	public static final int TYPENUMBER_CALL = 2;
	public static final int TYPENUMBER_CALLRESULT = 3;
	public static final int TYPENUMBER_CALLERROR = 4;

	int type = 0;

	String errCode;
	String errDesc;
	String rawPayload;

	protected OCPPMessage requestMsg = null;

	// Transaction등 메시지들의 Transcation을 connectorid를 통해서 얻어오기 위해사용.
	public int transactionConnectorId = -1;
	/**
	 * Description of the property id.
	 */
	protected String id = "";

	/**
	 * Description of the property action.
	 */
	public String action = "";

	/**
	 * Description of the property payload.
	 */
	protected Object payload = null;

	public int retryCnt = 0;

	public long reqSeq = 0;

	public boolean isRecoveryMsg = false;
	public int recoveryTid = -1;

	String transactionStartTime = null;

	private static final SimpleDateFormat transactionStartTimeFormatter =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

	// Start of user code (user defined attributes for OCPPMessage)

	// End of user code

	/**
	 * The constructor.
	 */
	public OCPPMessage() {
	}

	public OCPPMessage(String _action, Object _payload) {
		this.id = UUID.randomUUID().toString();
		this.action = _action;
		this.payload = _payload;
	}

	public OCPPMessage(String _id, String _action, Object _payload) {
		this.id = _id;
		this.action = _action;
		this.payload = _payload;
	}

	public void setType(int type) { this.type = type; }

	public int getType() { return this.type; }

	/**
	 * Returns id.
	 * @return id 
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets a value to attribute id. 
	 * @param newId 
	 */
	public void setId(String newId) {
		this.id = newId;
	}

	/**
	 * Returns action.
	 * @return action 
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * Sets a value to attribute action. 
	 * @param newAction 
	 */
	public void setAction(String newAction) {
		this.action = newAction;
	}

	/**
	 * Returns payload.
	 * @return payload 
	 */
	public Object getPayload() {
		return this.payload;
	}

	/**
	 * Sets a value to attribute payload. 
	 * @param newPayload 
	 */
	public void setPayload(Object newPayload) {
		this.payload = newPayload;
	}

	public String setErrorCode() { return this.errCode; }
	public void setErrorCode(String code) { this.errCode = code; }

	public String getErrorDescription() { return this.errDesc; }
	public void setErrorDescription(String desc) { this.errDesc = desc; }

	public String getRawPayload() { return this.rawPayload; }
	public void setRawPayload(String raw) { this.rawPayload = raw; }

	public void setRequestMsg(OCPPMessage request) { requestMsg = request; }
	public OCPPMessage getRequestMsg() { return requestMsg; }

	public void setTransactionStartTime(Calendar time) {
		transactionStartTime = transactionStartTimeFormatter.format(time.getTime());
	}
	public void setTransactionStartTime(String time) {
		transactionStartTime = time;
	}

	public String getTransactionStartTime() { return transactionStartTime; }

	/**
	 * (OCPP 1.6)의 3.6 Transaction-Rrelated Message를 구별한다.
	 * StartTransaction 과 StopTransaction, 그리고 MeterValues에서 transactionId를 가지고 있으면
	 * Transaction 메시지이다.
	 * @return Transcation 메시지인지 아닌지를 리턴
	 */
	public boolean isTransactionMessage() {
		boolean ret = false;
		if ( action == null ) return false;

		if ( action.equals("StartTransaction") || action.equals("StopTransaction") ) {
			ret = true;
		}
		else if ( action.equals("MeterValues")  ) {
			MeterValues meterValues = (MeterValues)payload;
			if ( meterValues != null ) {
				if ( meterValues.getTransactionId() != null ) ret = true;
			}
		}
		return ret;
	}
}
