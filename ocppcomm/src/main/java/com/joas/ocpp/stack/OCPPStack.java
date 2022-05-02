/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:40
 */

package com.joas.ocpp.stack;

import android.content.Context;

import com.joas.ocpp.msg.Authorize;
import com.joas.ocpp.msg.AuthorizeResponse;
import com.joas.ocpp.msg.BootNotification;
import com.joas.ocpp.msg.BootNotificationResponse;
import com.joas.ocpp.msg.CancelReservation;
import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChangeAvailabilityResponse;
import com.joas.ocpp.msg.ChangeConfiguration;
import com.joas.ocpp.msg.ChangeConfigurationResponse;
import com.joas.ocpp.msg.ChargingSchedule;
import com.joas.ocpp.msg.ClearCacheResponse;
import com.joas.ocpp.msg.ClearChargingProfile;
import com.joas.ocpp.msg.ClearChargingProfileResponse;
import com.joas.ocpp.msg.DataTransfer;
import com.joas.ocpp.msg.DataTransferResponse;
import com.joas.ocpp.msg.DiagnosticsStatusNotification;
import com.joas.ocpp.msg.FirmwareStatusNotification;
import com.joas.ocpp.msg.GetCompositeSchedule;
import com.joas.ocpp.msg.GetCompositeScheduleResponse;
import com.joas.ocpp.msg.GetConfiguration;
import com.joas.ocpp.msg.GetConfigurationResponse;
import com.joas.ocpp.msg.GetDiagnostics;
import com.joas.ocpp.msg.GetDiagnosticsResponse;
import com.joas.ocpp.msg.GetLocalListVersionResponse;
import com.joas.ocpp.msg.Heartbeat;
import com.joas.ocpp.msg.HeartbeatResponse;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.MeterValue;
import com.joas.ocpp.msg.MeterValues;
import com.joas.ocpp.msg.RemoteStartTransaction;
import com.joas.ocpp.msg.RemoteStartTransactionResponse;
import com.joas.ocpp.msg.RemoteStopTransaction;
import com.joas.ocpp.msg.RemoteStopTransactionResponse;
import com.joas.ocpp.msg.ReserveNow;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.Reset;
import com.joas.ocpp.msg.ResetResponse;
import com.joas.ocpp.msg.SampledValue;
import com.joas.ocpp.msg.SendLocalList;
import com.joas.ocpp.msg.SendLocalListResponse;
import com.joas.ocpp.msg.SetChargingProfile;
import com.joas.ocpp.msg.SetChargingProfileResponse;
import com.joas.ocpp.msg.StartTransaction;
import com.joas.ocpp.msg.StartTransactionResponse;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.ocpp.msg.TriggerMessage;
import com.joas.ocpp.msg.TriggerMessageResponse;
import com.joas.ocpp.msg.UnlockConnector;
import com.joas.ocpp.msg.UnlockConnectorResponse;
import com.joas.ocpp.msg.UpdateFirmware;
import com.joas.ocpp.msg.UpdateFirmwareResponse;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Description of OCPPStack.
 * 
 * @author user
 */
public class OCPPStack implements TransceiverListener{

	private static class MeterValueList extends ArrayList<MeterValue> {}

	public static final String TAG = "OCPPStack";
	public static final int TIME_SYNC_GAP_MS	= 10*1000;

	/**
	 * Description of the property transceiver.
	 */
	public Transceiver transceiver = null;

	/**
	 * Description of the property listener.
	 */
	public OCPPStackListener listener = null;

	/**
	 * Description of the property ocppProperty.
	 */
	public OCPPStackProperty ocppProperty = null;
	public OCPPStackStatus ocppStatus = null;
	protected Transport transport;
	protected TimeoutTimer heartbeatTimer;
	protected TimeoutTimer bootNotificationTimer;
	protected TimeoutTimer authLocalRespTimer;

	protected Context androidContext = null;
	protected OCPPDbOpenHelper dbOpenHelper = null;
	protected AuthorizeCache authorizeCache = null;
	protected LocalAuthList localAuthList  = null;
	protected OCPPMessage lateAuthMessage = null;
	protected OCPPConfiguration ocppConfiguration = null;
	protected LocalConfig localConfig = null;
	protected OCPPDiagnosticManager ocppDiagnosticManager = null;

	protected boolean[] connectorAvailableList = null;

	boolean isBootNorificationSent = false;
	/**
	 * The constructor.
	 */
	public OCPPStack(Context context, OCPPConfiguration config, String basePath, boolean isSoftReset) {
		ocppConfiguration = config;
		//isBootNorificationSent = isSoftReset; // SoftReset인 경우에는 BootNotification을 보내지 않는다.

		// Database, 시각설정 등 안드로이드 시스템관련 API를 사용하기 위해서 Context를 저장한다.
		androidContext = context;

		// 진단메시지 매니져 초기화
		ocppDiagnosticManager = new OCPPDiagnosticManager(androidContext, basePath, this);
		ocppDiagnosticManager.open();

		//Using Database(SQLite) for Android System
		dbOpenHelper = new OCPPDbOpenHelper(androidContext, basePath);
		dbOpenHelper.open();

		// 인증 캐쉬 생성
		authorizeCache = new AuthorizeCache(dbOpenHelper);

		// 로컬인증 리스트 클래스 생성
		localAuthList = new LocalAuthList(dbOpenHelper);

		// 내부 저장 설정 클래스 생성
		localConfig = new LocalConfig(dbOpenHelper);

		// localConfig에서 ocppConfiguration값을 불러옴.
		localConfig.loadOcppConfiguration(ocppConfiguration);

		// 스택 상태들 생성
		ocppStatus = new OCPPStackStatus();

		//커넥터 사용가능 세팅
		connectorAvailableList = new boolean[ocppConfiguration.NumberOfConnectors+1];

		localConfig.getConnectorAvalList(connectorAvailableList);

		// 주기적인 전송 메시지인 HeartBeat 타이머 설정(주기)
		heartbeatTimer = new TimeoutTimer(ocppConfiguration.HeartbeatInterval*1000, new TimeoutHandler() {
			public void run() {
			onHeartheatTimeout();
			}
		});

		// BootNotification 타이머 설정
		bootNotificationTimer = new TimeoutTimer(1000, new TimeoutHandler() {
			public void run() {
				sendBootNotificationRequest();
			}
		});

		authLocalRespTimer = new TimeoutTimer(10, new TimeoutHandler() {
			public void run() {
				if ( lateAuthMessage != null )  onAuthorizeResponse(lateAuthMessage, true);
			}
		});
	}

	/**
	 * Description of the method init.
	 */
	public void init(String protocol, OCPPStackProperty newOcppProperty) {
		if ( protocol.equals("ocpp-j") == true ) {
			transceiver = Transceiver.createTransceiver(Transceiver.PROTOCOL_TYPE_OCPP_J, dbOpenHelper);
			transport = new TransportWebSocket();
			((TransportWebSocket) transport).setPingInterval(ocppConfiguration.WebSocketPingInterval);
		}
		else {
			transceiver = Transceiver.createTransceiver(Transceiver.PROTOCOL_TYPE_OCPP_S, dbOpenHelper);
			transport = new TransportHTTP();
		}
		transceiver.setTransport(transport);
		transceiver.setOcppConfigration(ocppConfiguration);
		transceiver.setListener(this);
		setOcppProperty(newOcppProperty);
	}

	/**
	 * Description of the method startStack.
	 */
	public void startOcpp() {
		transceiver.start();
	}

	public void stopOcpp() {
		transceiver.stopTransceiver();
	}

	public void closeOcpp() {
		if (transceiver != null) {
			transceiver.stopTransceiver();
			transceiver.interrupt();
		}

		try {
			if (heartbeatTimer != null) heartbeatTimer.end();
			if (bootNotificationTimer != null) bootNotificationTimer.end();
			if (authLocalRespTimer != null) authLocalRespTimer.end();
		}
		catch(Exception e){}
	}

	public OCPPDiagnosticManager getOcppDiagnosticManager() { return ocppDiagnosticManager; }

	public LocalConfig getLocalConfig() { return localConfig; }

	public OCPPDbOpenHelper getDbOpenHelper() { return dbOpenHelper; }
	/**
	 * Returns transceiver.
	 * @return transceiver 
	 */
	public Transceiver getTransceiver() {
		return this.transceiver;
	}

	/**
	 * Sets a value to attribute transceiver. 
	 * @param newTransceiver 
	 */
	public void setTransceiver(Transceiver newTransceiver) {
		this.transceiver = newTransceiver;
	}

	public AuthorizeCache getAuthorizeCache() { return authorizeCache; }
	public LocalAuthList getLocalAuthList() { return localAuthList; }

	/**
	 * Returns listener.
	 * @return listener 
	 */
	public OCPPStackListener getListener() {
		return this.listener;
	}

	public void setTransportMonitorListener(OCPPTransportMonitorListener listener) {
		transport.setMonitorListener(listener);
	}

	/**
	 * Sets a value to attribute listener. 
	 * @param newListener
	 */
	public void setListener(OCPPStackListener newListener) {
		this.listener = newListener;
	}

	/**
	 * Returns ocppProperty.
	 * @return ocppProperty 
	 */
	public OCPPStackProperty getOcppProperty() {
		return this.ocppProperty;
	}

	/**
	 * Sets a value to attribute ocppProperty. 
	 * @param newOcppProperty 
	 */
	public void setOcppProperty(OCPPStackProperty newOcppProperty) {
		this.ocppProperty = newOcppProperty;

		if ( transport != null ) {
			transport.setCPID(newOcppProperty.cpid);

			// Basic Auth Test
			if (newOcppProperty.useBasicAuth == true) {
				transport.setBasicAuthID(newOcppProperty.authID);
				transport.setBasicAuthPassword(newOcppProperty.authPassword);
			}

			transport.setUseSSL(newOcppProperty.useSSL);

			if (newOcppProperty.useSSL == true) {
				transport.setSSLCertFile(newOcppProperty.sslKeyFile);
			}

			transport.setSSLCertCheck(newOcppProperty.useSSLCheckCert);

			transport.setConnectURI(newOcppProperty.serverUri);
		}
	}

	public boolean getConnectorAvailable(int connectorId) {
		if ( connectorId > ocppConfiguration.NumberOfConnectors) return false;
		return connectorAvailableList[connectorId];
	}

	public void setConnectorAvailable(int connectorId, boolean tf) {
		if ( connectorId > ocppConfiguration.NumberOfConnectors) return;
		connectorAvailableList[connectorId] = tf;
		localConfig.setConnectorAvalList(connectorAvailableList);
	}


	public void sendRequest(OCPPMessage message) {
		// BootNotification 받은값이 Accepted이여야 메시지 전송함.
		if ( message.isTransactionMessage() || isRegAccepted() ) {
			transceiver.sendRequest(message);
		}
	}


	public void sendStatusNotificationRequest(int connectorId, StatusNotification.Status status, StatusNotification.ErrorCode error) {
		StatusNotification statusNotification = new StatusNotification();
		statusNotification.setConnectorId(connectorId);
		statusNotification.setStatus(status);
		statusNotification.setErrorCode(error);
		statusNotification.setTimestamp(Calendar.getInstance());


		OCPPMessage message = new OCPPMessage("StatusNotification", statusNotification);

		sendRequest(message);
	}

	public void sendDataTransferRequest(String data){
		DataTransfer dataTransfer = new DataTransfer();
		dataTransfer.setVendorId("joas");
		dataTransfer.setData(data);

		OCPPMessage message = new OCPPMessage("DataTransfer",dataTransfer);

		sendRequest(message);
	}

	void sendHeartBeatRequest() {
		Heartbeat heartbeat = new Heartbeat();
		OCPPMessage msg = new OCPPMessage("Heartbeat", heartbeat);
		transceiver.sendRequest(msg);
	}

	void sendBootNotificationRequest() {
		BootNotification bootNotification = new BootNotification();
		bootNotification.setChargePointVendor(ocppProperty.chargePointVender);
		bootNotification.setChargePointModel(ocppProperty.chargePointModel);
		bootNotification.setChargePointSerialNumber(ocppProperty.chargePointSerialNumber);
		bootNotification.setChargeBoxSerialNumber(ocppProperty.chargeBoxSerialNumber);
		bootNotification.setFirmwareVersion(ocppProperty.firmwareVersion);
		bootNotification.setMeterType(ocppProperty.meterType);
		bootNotification.setMeterSerialNumber(ocppProperty.meterSerialNumber);

		OCPPMessage message = new OCPPMessage("BootNotification", bootNotification);

		transceiver.sendRequest(message);
	}

	public void doLocalAuth(OCPPMessage message, IdTagInfo idTagInfo) {
		AuthorizeResponse authorizeResponse = new AuthorizeResponse();
		authorizeResponse.setIdTagInfo(idTagInfo);
		lateAuthMessage = new OCPPMessage("AuthorizeResponse", authorizeResponse);
		lateAuthMessage.requestMsg = message;
		authLocalRespTimer.begin();
	}

	public void sendAuthorizeRequest(String idTag) {
		boolean isAuthLocalFound = false;

		Authorize authorize = new Authorize();
		authorize.setIdTag(idTag);
		OCPPMessage message = new OCPPMessage("Authorize", authorize);

		IdTagInfo idTagInfo = null;

		boolean isLocalAuth = true;

		//통신 연결이 안되어 있을때 LocalAuthorizeOffline 가 false 이면 인증 진행 안함
		if ( ocppConfiguration.LocalAuthorizeOffline == false && transceiver.isConnected == false ) {
			isLocalAuth = false;
			LogWrapper.v(TAG, "LocalAuthorizeOffline false and offline");
		}

		// LocalPreAuthorize 가 false이면 로컬 인증 진행 안함
		if (ocppConfiguration.LocalPreAuthorize == false ) isLocalAuth = false;

		if ( isLocalAuth ) {
			// 인증 Cache 모드를 사용하면 사용자 인증을 체크한다.
			if (ocppConfiguration.AuthorizationCacheEnabled == true) {
//				idTagInfo = authorizeCache.searchCacheAuthInfo(idTag);
				idTagInfo = localAuthList.searchLocalAuthInfo(idTag);

				if (idTagInfo != null) {
					doLocalAuth(message, idTagInfo);
					LogWrapper.v(TAG, "AuthorizeCache found");
					isAuthLocalFound = true;
				} else {
					LogWrapper.v(TAG, "AuthorizeCache not found");
				}
			}

			// Local 사용자 인증 모드를 사용하면 로컬인증을 체크한다.
			if (idTagInfo == null && ocppConfiguration.LocalAuthListEnabled == true) {


				idTagInfo = localAuthList.searchLocalAuthInfo(idTag);

				if (idTagInfo != null) {
					doLocalAuth(message, idTagInfo);
					LogWrapper.v(TAG, "AuthorizeLocalList found");
					isAuthLocalFound = true;
				} else {
					LogWrapper.v(TAG, "AuthorizeLocalList not found");
				}

			}
		}

		if ( isAuthLocalFound == false ){
		    // 통신 연결이 되어 있지 않을 때 처리
		    if ( transceiver.isConnected == false ) {

		    	// 만약 AllowOfflineTxForUnknownId이 true이면 인증 진행함
		    	if ( ocppConfiguration.AllowOfflineTxForUnknownId == true) {
					IdTagInfo retIdTag = new IdTagInfo();
					retIdTag.setStatus(IdTagInfo.Status.ACCEPTED);
					doLocalAuth(message, retIdTag);
				}
				else {
					idTagInfo = new IdTagInfo();
					idTagInfo.setStatus(IdTagInfo.Status.BLOCKED);
					doLocalAuth(message, idTagInfo);
				}
            }
			else {
			    sendRequest(message);
            }
		}
	}

	protected void setContextSampleValue(SampledValue value, SampledValue.Context context) {
		if ( context != SampledValue.Context.SAMPLE_PERIODIC ) value.setContext(context);
	}


	public void sendMeterValueRequest(int connectorId, long meterVal, int meterValInterval, int current, int curPower, int curOffered, int powerOffered, int soc, SampledValue.Context context, String measurand, boolean isInTransaction, Calendar startTime, int transactionId) {
		List<MeterValue> listValue = new ArrayList<MeterValue>();

		MeterValue meterValue = new MeterValue();
		meterValue.setTimestamp(Calendar.getInstance());

		List<SampledValue> listSample = new ArrayList<SampledValue>();
		SampledValue sampledValue = new SampledValue();
		sampledValue.setValue(""+meterVal);
		sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_REGISTER);
		sampledValue.setUnit(SampledValue.Unit.W);
		setContextSampleValue(sampledValue, context);
		listSample.add(sampledValue);

		if ( meterValInterval >= 0 && measurand.contains("Energy.Active.Import.Interval") ) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + meterValInterval);
			sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_INTERVAL);
			sampledValue.setUnit(SampledValue.Unit.W);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		if ( soc >= 0 && measurand.contains("SoC")) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + soc);
			sampledValue.setMeasurand(SampledValue.Measurand.SO_C);
			sampledValue.setUnit(SampledValue.Unit.PERCENT);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		if ( current >= 0 && measurand.contains("Current.Import") ) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + current);
			sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_IMPORT);
			sampledValue.setUnit(SampledValue.Unit.A);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		if ( curPower >= 0 && measurand.contains("Power.Active.Export")) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + curPower);
			sampledValue.setMeasurand(SampledValue.Measurand.POWER_ACTIVE_EXPORT);
			sampledValue.setUnit(SampledValue.Unit.W);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		if ( curOffered >= 0 && measurand.contains("Current.Offered")) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + curOffered);
			sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_OFFERED);
			sampledValue.setUnit(SampledValue.Unit.A);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		if ( powerOffered >= 0 && measurand.contains("Power.Offered")) {
			sampledValue = new SampledValue();
			sampledValue.setValue("" + powerOffered);
			sampledValue.setMeasurand(SampledValue.Measurand.POWER_OFFERED);
			sampledValue.setUnit(SampledValue.Unit.W);
			setContextSampleValue(sampledValue, context);
			listSample.add(sampledValue);
		}

		meterValue.setSampledValue(listSample);
		listValue.add(meterValue);

		MeterValues meterValues = new MeterValues();
		meterValues.setConnectorId(connectorId);
		meterValues.setMeterValue(listValue);

		if ( isInTransaction ) meterValues.setTransactionId(transactionId);

		OCPPMessage message = new OCPPMessage("MeterValues", meterValues);

		// 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
		message.transactionConnectorId = connectorId;

		if(startTime!=null) message.setTransactionStartTime(startTime);

		sendRequest(message);
	}


	public void sendStartTransactionRequest(int connectorId, String idTag, int meterStart, int reservationId, Calendar startTime) {
		StartTransaction startTransaction = new StartTransaction();
		startTransaction.setConnectorId(connectorId);
		startTransaction.setIdTag(idTag);
		startTransaction.setMeterStart(meterStart);
		if ( reservationId > 0 ) startTransaction.setReservationId(reservationId);
		startTransaction.setTimestamp(startTime);

		OCPPMessage message = new OCPPMessage("StartTransaction", startTransaction);

		// 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
		message.transactionConnectorId = connectorId;

		message.setTransactionStartTime(startTime);

		sendRequest(message);

	}

	public void sendStoptTransactionRequest(int connectorId, String idTag, int meterStop, StopTransaction.Reason reason, Calendar startTime, int transactionId) {
		StopTransaction stopTransaction = new StopTransaction();
        stopTransaction.setIdTag(idTag);
		stopTransaction.setMeterStop(meterStop);
		stopTransaction.setTimestamp(Calendar.getInstance());
		stopTransaction.setReason(reason);
        stopTransaction.setTransactionId(transactionId);

		OCPPMessage message = new OCPPMessage("StopTransaction", stopTransaction);

		// 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
		message.transactionConnectorId = connectorId;
		message.setTransactionStartTime(startTime);

		sendRequest(message);
	}

	boolean isRegAccepted() {
		return (ocppStatus.regStatus == OCPPStackStatus.REG_STATUS_ACCEPTED);
	}

	public void sendFirmwareStatusNotification(FirmwareStatusNotification.Status status) {
		FirmwareStatusNotification statusNotification = new FirmwareStatusNotification();
		statusNotification.setStatus(status);

		OCPPMessage message = new OCPPMessage("FirmwareStatusNotification", statusNotification);

		sendRequest(message);
	}

	public void sendDiagnosticsStatusNotification(DiagnosticsStatusNotification.Status status) {
		DiagnosticsStatusNotification diagnosticsStatusNotification = new DiagnosticsStatusNotification();

		diagnosticsStatusNotification.setStatus(status);
		OCPPMessage message = new OCPPMessage("DiagnosticsStatusNotification", diagnosticsStatusNotification);

		sendRequest(message);
	}


   /**
	* BootNotification 응답을 처리한다.
	* 해당 메시지는 RegistrationStatus, 현재 시간, Heartbeat해야될 주기정보(interval)을 가진다.
	* @param message	 */

	void onBootNotificationResponse(OCPPMessage message) {
		BootNotificationResponse bootNotificationResponse;

		bootNotificationResponse = (BootNotificationResponse) message.getPayload();

		// 만약 헤더에 Status가 없으면 Accepted로 처리함.(차지인!!)
		if ( bootNotificationResponse.getStatus() == null ) bootNotificationResponse.setStatus(BootNotificationResponse.Status.ACCEPTED);

		switch ( bootNotificationResponse.getStatus() ) {
			case ACCEPTED:
				ocppStatus.regStatus = OCPPStackStatus.REG_STATUS_ACCEPTED;
				break;
			case REJECTED:
				ocppStatus.regStatus = OCPPStackStatus.REG_STATUS_REJECTED;
				break;
			case PENDING:
				ocppStatus.regStatus = OCPPStackStatus.REG_STATUS_PENDING;
				break;
		}

		if ( isRegAccepted() ) {
			// HeartBeat 주기를 지정한다.
			changeHeartbeatInterval(bootNotificationResponse.getInterval().intValue());

			// 시간을 설정하는 함수를 호출한다.
			onRecvCurrentTime(bootNotificationResponse.getCurrentTime());

			// Lock를 풀어서 Transaction 메시지를 전송할 수 있도록 한다.
			transceiver.setLockSendMsg(false);

			// 성공적으로 전송되었으면 그 이후에는 다시 전송하지 않는다.(Hard Reset, Power Reset 전까지)
			isBootNorificationSent = true;
		}
		else {
			bootNotificationTimer.setTimeout(bootNotificationResponse.getInterval().intValue()*1000);
			bootNotificationTimer.begin();
		}


		// 서버 접속 성공을 하게 되면 Connection이벤트를 보낸다.
		if ( listener != null ) listener.onBootNotificationResponse(isRegAccepted());

		// To Do.. When RegiseterStatus is Rejected or Pending, Retry??, Reconnect??
	}

	void changeHeartbeatInterval(int interval) {
		ocppConfiguration.HeartbeatInterval = interval;

		// 해당 타이머를 재시작한다.
		heartbeatTimer.end();
		// 다음 불릴 시간을 설정
		heartbeatTimer.setTimeout(interval*1000);
		// 타이머를 해당 주기로 부른다.
		heartbeatTimer.beginPeriod();
	}

	//==================================================================================
	// OCPP Request Message 처리
	//==================================================================================

	void sendResponse(OCPPMessage message, Object response) {
		OCPPMessage respMsg = new OCPPMessage(message.getId(), message.getAction()+"Response", response);
		transceiver.sendResponse(respMsg);
	}

	void sendResponseNotSupported(OCPPMessage message) {
		transceiver.sendResponseNotSupported(message);
	}

	/**
	 * GetConfiguration 메시지를 받으면 현재 가지고 있는 ConfigrationKey값을 찾아서 보낸다.
	 * @param message
	 */
	void onGetConfigurationRequest(OCPPMessage message) {
		GetConfiguration getConfiguration = (GetConfiguration)message.getPayload();
		GetConfigurationResponse response;
		if ( getConfiguration.getKey().size() == 0 ) response = ocppConfiguration.getConfigurationsAll();
		else response = ocppConfiguration.getConfigurationsResponse(getConfiguration.getKey());
		sendResponse(message, response);
	}

	void onChangeAvailability(OCPPMessage message) {
		ChangeAvailability changeAvailability = (ChangeAvailability)message.getPayload();
		setConnectorAvailable(changeAvailability.getConnectorId(), changeAvailability.getType() == ChangeAvailability.Type.OPERATIVE);

		ChangeAvailabilityResponse response = new ChangeAvailabilityResponse();
		response.setStatus(ChangeAvailabilityResponse.Status.ACCEPTED);
		sendResponse(message, response);

		// listener로 이벤트 발생
		if ( listener != null ) {
			listener.onChangeAvailability(changeAvailability.getConnectorId(), changeAvailability.getType());
		}
	}

	/**
	 * 설정을 변경하기 위한 Request를 받을때 처리
	 * @param message
	 */
	void onChangeConfigurationRequest(OCPPMessage message) {
		ChangeConfiguration changeConfiguration = (ChangeConfiguration)message.getPayload();

		boolean ret = onChangeConfiguration(changeConfiguration);

		ChangeConfigurationResponse response;

		if ( ret == true ) {
			response = ocppConfiguration.changeConfigurationResponse(changeConfiguration.getKey(), changeConfiguration.getValue());

			// 성공적으로 변경시 내부 Config에 저장함
			if (response.getStatus() == ChangeConfigurationResponse.Status.ACCEPTED) {
				localConfig.saveOcppConfiguration(changeConfiguration.getKey(), changeConfiguration.getValue());
			}
		} else {
			response = new ChangeConfigurationResponse();
			response.setStatus(ChangeConfigurationResponse.Status.REJECTED);
		}

		sendResponse(message, response);
	}

	boolean onChangeConfiguration(ChangeConfiguration changeConfiguration) {
		boolean ret = true;

		if ( changeConfiguration.getKey().equals("HeartbeatInterval") ) {
			changeHeartbeatInterval(Integer.parseInt(changeConfiguration.getValue()));
		}
		else if ( changeConfiguration.getKey().equals("HeartbeatInterval") ) {
			int interval = Integer.parseInt(changeConfiguration.getValue());
			if ( interval < 0 ) ret = false;
			else {
				if ( transport instanceof TransportWebSocket ) {
					((TransportWebSocket) transport).setPingInterval(interval);
				}
			}
		}
		return ret;
	}

	void onClearCacheRequest(OCPPMessage message) {
		ClearCacheResponse response = new ClearCacheResponse();
		authorizeCache.clearAuthInfo();
		response.setStatus(ClearCacheResponse.Status.ACCEPTED);
		sendResponse(message, response);
	}

	void onClearChargingProfile(OCPPMessage message) {
		ClearChargingProfile clearChargingProfile = (ClearChargingProfile)message.getPayload();
		ClearChargingProfileResponse response = new ClearChargingProfileResponse();
		ClearChargingProfileResponse.Status status = ClearChargingProfileResponse.Status.UNKNOWN;
		if (listener != null) {
			if ( listener.onClearChargingProfile(clearChargingProfile) ) status = ClearChargingProfileResponse.Status.ACCEPTED;
		}
		response.setStatus(status);
		sendResponse(message, response);
	}

	void onCancelReservation(OCPPMessage message) {
		CancelReservation cancelReservation = (CancelReservation)message.getPayload();
		CancelReservationResponse response = new CancelReservationResponse();
		CancelReservationResponse.Status ret = CancelReservationResponse.Status.REJECTED;

		try {
			if (listener != null) {
				ret = listener.onCancelReservation(cancelReservation.getReservationId());
			}
		}catch (Exception e) {
			LogWrapper.e(TAG, "onCancelReservation:"+e.toString());
		}

		response.setStatus(ret);
		sendResponse(message, response);
	}

	void onDataTransferRequest(OCPPMessage message) {
		DataTransferResponse response = new DataTransferResponse();
		response.setStatus(DataTransferResponse.Status.ACCEPTED);
		sendResponse(message, response);
	}

	void onGetLocalListVersionRequest(OCPPMessage message) {
		GetLocalListVersionResponse response = new GetLocalListVersionResponse();
		response.setListVersion(localConfig.getLocalAuthVersion());
		sendResponse(message, response);
	}

	/**
	 * 로컬 인증에 대한 리스트를 받으면 수행한다.	 *
	 * @param message
	 */
	void onSendLocalListRequest(OCPPMessage message) {
		SendLocalListResponse response = new SendLocalListResponse();
		response.setStatus(SendLocalListResponse.Status.ACCEPTED);
		SendLocalList sendLocalList = (SendLocalList)message.getPayload();

		// 만약 타입이 DIFFERENTIAL이면 버전 검사를 하여 현재보다 높을때만 수행하며 같거나 낮을때는 MISMATCH를 돌려준다.
		if ( sendLocalList.getUpdateType() == SendLocalList.UpdateType.DIFFERENTIAL ) {
			int curVersion = localConfig.getLocalAuthVersion();
			if ( curVersion >= sendLocalList.getListVersion() ) response.setStatus(SendLocalListResponse.Status.VERSION_MISMATCH);
			else {
				localAuthList.updateList(sendLocalList.getLocalAuthorizationList(), false);
				localConfig.setLocalAuthVersion(sendLocalList.getListVersion());
			}
		} else if ( sendLocalList.getUpdateType() == SendLocalList.UpdateType.FULL ) {
			localAuthList.updateList(sendLocalList.getLocalAuthorizationList(), true);
			localConfig.setLocalAuthVersion(sendLocalList.getListVersion());
		}
		sendResponse(message, response);
	}

	void onSetChargingProfile(OCPPMessage message) {
		SetChargingProfile setChargingProfile = (SetChargingProfile)message.getPayload();
		SetChargingProfileResponse response = new SetChargingProfileResponse();
		SetChargingProfileResponse.Status ret = SetChargingProfileResponse.Status.REJECTED;

		// listener로 이벤트 발생
		if ( listener != null ) {
			if ( listener.onSetChargingProfile(setChargingProfile.getConnectorId(), setChargingProfile.getCsChargingProfiles()) == true ) {
				ret = SetChargingProfileResponse.Status.ACCEPTED;
			}
		}
		response.setStatus(ret);
		sendResponse(message, response);
	}
	void onGetCompositeSchedule(OCPPMessage message){
		GetCompositeSchedule getCompositeSchedule = (GetCompositeSchedule) message.getPayload();

		GetCompositeScheduleResponse response = new GetCompositeScheduleResponse();
		GetCompositeScheduleResponse.Status ret = GetCompositeScheduleResponse.Status.ACCEPTED;
		String ru = getCompositeSchedule.getChargingRateUnit().toString();

		ChargingSchedule chargingSchedule = new ChargingSchedule();
		chargingSchedule.setDuration(getCompositeSchedule.getDuration());
		chargingSchedule.setChargingRateUnit(ChargingSchedule.ChargingRateUnit.valueOf(ru));

		response.setStatus(ret);
		response.setConnectorId(getCompositeSchedule.getConnectorId());
		response.setChargingSchedule(chargingSchedule);
		sendResponse(message,response);

	}

	void onRemoteStartTransactionRequest(OCPPMessage message) {
		RemoteStartTransaction remoteStartTransaction = (RemoteStartTransaction)message.getPayload();
		RemoteStartTransactionResponse response = new RemoteStartTransactionResponse();

		boolean ret = false;

		// listener로 이벤트 발생
		if ( listener != null ) {
			int connectionId = 0;
			if ( remoteStartTransaction.getConnectorId() != null ) connectionId = remoteStartTransaction.getConnectorId();
			ret = listener.onRemoteStartTransaction(connectionId, remoteStartTransaction.getIdTag(), remoteStartTransaction.getChargingProfile());
		}

		response.setStatus(ret ? RemoteStartTransactionResponse.Status.ACCEPTED : RemoteStartTransactionResponse.Status.REJECTED);

		sendResponse(message, response);

		if(ocppConfiguration.AuthorizeRemoteTxRequests){
			//authorizeRequest 전송
			Authorize authorize = new Authorize();
			authorize.setIdTag(remoteStartTransaction.getIdTag());
			OCPPMessage msg = new OCPPMessage("Authorize", authorize);

//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			sendRequest(msg);
		}
	}

	void onRemoteStopTransactionRequest(OCPPMessage message) {
		RemoteStopTransaction remoteStopTransaction = (RemoteStopTransaction)message.getPayload();
		RemoteStopTransactionResponse response = new RemoteStopTransactionResponse();

		int tid = -1;
		if ( remoteStopTransaction.getTransactionId() != null ) tid = remoteStopTransaction.getTransactionId().intValue();

		boolean ret = false;
		if ( listener != null ) ret = listener.onRemoteStopTransaction(tid);

		response.setStatus(ret ? RemoteStopTransactionResponse.Status.ACCEPTED : RemoteStopTransactionResponse.Status.REJECTED);
		sendResponse(message, response);
	}

	void onResetRequeset(OCPPMessage message) {
		Reset reset = (Reset)message.getPayload();
		ResetResponse response = new ResetResponse();
		response.setStatus(ResetResponse.Status.ACCEPTED);
		sendResponse(message, response);

		// listener으로 이벤트 전달
		if ( listener != null ) listener.onResetRequest(reset.getType() == Reset.Type.HARD );
	}

	void onReserveNow(OCPPMessage message) {
		ReserveNow reserveNow = (ReserveNow)message.getPayload();
		ReserveNowResponse response = new ReserveNowResponse();
		ReserveNowResponse.Status ret = ReserveNowResponse.Status.REJECTED;

		try {
			if (listener != null) {
				ret = listener.onReserveNow(reserveNow.getConnectorId(), reserveNow.getExpiryDate(), reserveNow.getIdTag(), reserveNow.getParentIdTag(), reserveNow.getReservationId());
			}
		}catch (Exception e) {
			LogWrapper.e(TAG, "onReserveNow:"+e.toString());
		}
		response.setStatus(ret);

		sendResponse(message, response);
	}

	void onTriggerMessageRequest(OCPPMessage message) {
		TriggerMessage triggerMessage  = (TriggerMessage)message.getPayload();
		TriggerMessageResponse response = new TriggerMessageResponse();

		boolean isInternalSent = false;
		switch( triggerMessage.getRequestedMessage().toString() ) {
			case "BootNotification":
				sendBootNotificationRequest();
				isInternalSent = true;
				break;
			case "Heartbeat":
				sendHeartBeatRequest();
				isInternalSent = true;
				break;
			case "DiagnosticsStatusNotification":
				sendDiagnosticsStatusNotification(ocppDiagnosticManager.getDiagnosticStatus());
				isInternalSent = true;
				break;
		}

		if ( isRegAccepted() ) {
			try {
				if (listener != null && isInternalSent == false) {
					listener.onTriggerMessage(triggerMessage);
				}
			} catch (Exception e) {
				LogWrapper.e(TAG, "onTriggerMessageRequest:" + e.toString());
			}
			response.setStatus(TriggerMessageResponse.Status.ACCEPTED);
		}
		else {
			response.setStatus(TriggerMessageResponse.Status.REJECTED);
		}

		sendResponse(message, response);
	}



	void onUnlockConnector(OCPPMessage message) {
		UnlockConnector unlockConnector = (UnlockConnector)message.getPayload();
		UnlockConnectorResponse response = new UnlockConnectorResponse();

		// Unlock에 대해서는 서포트 하지 않음(충전완료시에 자동으로 Unlock가 됨)
		response.setStatus(UnlockConnectorResponse.Status.NOT_SUPPORTED);
		sendResponse(message, response);
	}

	void onUpdateFirmwareRequest(OCPPMessage message) {
		UpdateFirmware updateFirmware = (UpdateFirmware)message.getPayload();
		UpdateFirmwareResponse response = new UpdateFirmwareResponse();
		sendResponse(message, response);

		// listener으로 이벤트 전달
		int retries = 0;
		int retryInterval = 0;
		try {
			if (updateFirmware.getRetries() != null)
				retries = updateFirmware.getRetries().intValue();
			if (updateFirmware.getRetryInterval() != null)
				retryInterval = updateFirmware.getRetryInterval().intValue();
			if (listener != null)
				listener.onUpdateFirmwareRequest(updateFirmware.getLocation(), retries, updateFirmware.getRetrieveDate(), retryInterval);
		} catch(Exception e){
			LogWrapper.e(TAG, "onUpdateFirmwareRequest:"+e.toString());
		}
	}

	void onGetDiagnosticsRequest(OCPPMessage message) {
		GetDiagnostics getDiagnostics = (GetDiagnostics)message.getPayload();
		GetDiagnosticsResponse response = new GetDiagnosticsResponse();

		String fileName = ocppDiagnosticManager.onRecvGetDiagnostics(getDiagnostics);
		response.setFileName(fileName);

		sendResponse(message, response);
	}


	// TODO Not supported Command Default
	void onNotSupportedRequest(OCPPMessage message) {
		LogWrapper.d(TAG, "Not Supported Reqeust:"+message.getAction());
		sendResponseNotSupported(message);
	}

	//==================================================================================
	// OCPP Response Message 처리
	//==================================================================================
	/**
	 * Heartbeat의 응답을 처리한다.
	 * 해당 메시지는 현재 시간을 포함한다. 현재시간이 일정시간이상 틀리다면 설정해야한다.
	 * @param message
	 */
	void onHeartbeatResponse(OCPPMessage message) {
		HeartbeatResponse heartbeatResponse = (HeartbeatResponse)message.getPayload();

		// 시간을 설정하는 함수를 호출한다.
		onRecvCurrentTime(heartbeatResponse.getCurrentTime());
	}

	void onStartTransactionResponse(OCPPMessage message) {
		StartTransactionResponse startTransactionResponse = (StartTransactionResponse)message.getPayload();

		transceiver.setTransactionState(true); // Transaceiver를 Transaction 상태로 만든다.

		if ( listener != null ) {
			listener.onStartTransactionResult(
					message.getRequestMsg().transactionConnectorId,
					startTransactionResponse.getIdTagInfo(),
					message.getRequestMsg().getTransactionStartTime(),
					startTransactionResponse.getTransactionId() );
		}

		// StartTransaction이면 이후 해당 메시지의 Transaction ID를 모두 찾아서 채워준다.
		transceiver.findAndUpdateTransactionIDFromLostMsg(message.getRequestMsg().getTransactionStartTime(), startTransactionResponse.getTransactionId());
	}

	void onStopTransactionResponse(OCPPMessage message) {
		transceiver.setTransactionState(false); // Transaceiver를 Transaction 상태를 해제한다.
	}

	void onHeartheatTimeout() {
		if ( isRegAccepted() ) {
			sendHeartBeatRequest();
		}
	}

	void onAuthorizeResponse(OCPPMessage message, boolean isLocalResp) {
		AuthorizeResponse authorizeResponse = (AuthorizeResponse)message.getPayload();
		Authorize authorizeRequest = (Authorize)message.getRequestMsg().getPayload();

		if ( isLocalResp == false ) {
			// 인증에 성공을 하면 해당 인증정보를 Cache에 저장한다.
			if (authorizeResponse.getIdTagInfo().getStatus() == IdTagInfo.Status.ACCEPTED && ocppConfiguration.AuthorizationCacheEnabled) {
				authorizeCache.saveAuthInfo(authorizeRequest.getIdTag(), authorizeResponse.getIdTagInfo());
			}
		}

		if ( listener != null ) listener.onAuthorizeResponse(authorizeResponse);
	}

	/**
	 * 현재 시간을 서버시간과 동기를 한다.
	 * 안드로이드에서 시간을 설정하려면 android.permission.SET_TIME 이 필요한데 이는 signature 된 App에서만 가능
	 * 에물레이터를 사용할때는 에물레이터용 Platform Key를 사용하면 되지만 추후 build된 platform에서는
	 * 해당 빌드 Playform Key를 사용해야 한다.
	 * @param syncTime
	 */

	public void onRecvCurrentTime(Calendar syncTime) {
		if ( listener != null ) listener.onTimeUpdate(syncTime);
	}



	//======================================================================
	// Override 메시지, 이벤트 처리
	//======================================================================

	/**
	 * Tranceiver에서 Requeset를 받았을때 처리하는 함수
	 * @param message 받은 메시지
	 */
	@Override
	public void onRecvRequest(OCPPMessage message) {
		switch ( message.getAction() ) {
			case "ChangeAvailability":
				onChangeAvailability(message);
				break;
			case "ChangeConfiguration":
				onChangeConfigurationRequest(message);
				break;
			case "ClearCache":
				onClearCacheRequest(message);
				break;
			case "ClearChargingProfile":
				onClearChargingProfile(message);
				break;
			case "CancelReservation":
				onCancelReservation(message);
				break;
			case "DataTransfer":
				onDataTransferRequest(message);
				break;
			case "GetConfiguration":
				onGetConfigurationRequest(message);
				break;
			case "GetLocalListVersion":
				onGetLocalListVersionRequest(message);
				break;
			case "RemoteStartTransaction":
				onRemoteStartTransactionRequest(message);
				break;
			case "RemoteStopTransaction":
				onRemoteStopTransactionRequest(message);
				break;
			case "SendLocalList":
				onSendLocalListRequest(message);
				break;
			case "SetChargingProfile":
				onSetChargingProfile(message);
				break;
			case "Reset":
				onResetRequeset(message);
				break;
			case "ReserveNow":
				onReserveNow(message);
				break;
			case "UnlockConnector":
				onUnlockConnector(message);
				break;
			case "UpdateFirmware":
				onUpdateFirmwareRequest(message);
				break;
			case "TriggerMessage":
				onTriggerMessageRequest(message);
				break;
			case "GetDiagnostics":
				onGetDiagnosticsRequest(message);
				break;
			case "GetCompositeSchedule":
				onGetCompositeSchedule(message);
				break;
			default:
				onNotSupportedRequest(message);
				break;
		}
	}

	/**
	 * Tranceiver에서 Requeset에 대한 Response를 받았을때 처리하는 함수
	 * @param message 받은 메시지
	 */
	@Override
	public void onRecvResponse(OCPPMessage message) {
		switch ( message.getAction() ) {
			case "BootNotificationResponse":
				onBootNotificationResponse(message);
				break;
			case "HeartbeatResponse":
				onHeartbeatResponse(message);
				break;
			case "AuthorizeResponse":
				onAuthorizeResponse(message, false);
				break;
			case "StartTransactionResponse":
				onStartTransactionResponse(message);
				break;
			case "StopTransactionResponse":
				onStopTransactionResponse(message);
				break;
		}
	}

	@Override
	public void onRecvError(OCPPMessage message) {

	}

	@Override
	public void onRequestTimeout(OCPPMessage message) {

	}

	@Override
	public void onConnectTransceiver() {
		// 서버와 처음 통신 연결이 되었을 때 메시지를 보낸다.
		// Spec에서는 첫 부팅 이외에는 연결될때마다 굳이 보내지 않아도 된다고 나와 있음
		// 처음 프로그램실행시 1번만 보냄
		if (isBootNorificationSent == false ) {
			bootNotificationTimer.begin();
		}

		LogWrapper.d(TAG, "onConnectTransceiver: Connected to Server");

	}

	@Override
	public void onDisconnectTransceiver() {
		System.gc();
	}

}