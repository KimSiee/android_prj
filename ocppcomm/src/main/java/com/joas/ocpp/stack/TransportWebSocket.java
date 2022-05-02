/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.ocpp.stack;

import android.util.Log;

import com.joas.utils.LogWrapper;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import java.util.List;
import java.util.Map;

/**
 * Description of TransportWebSocket.
 * 
 * @author scchoi
 */
public class TransportWebSocket extends Transport implements WebSocketListener {
	public static final String TAG = "TPWebSocket";
	WebSocket ws = null;
	WebSocketFactory wsFactory = null;

	/**
	 * The constructor.
	 */
	public TransportWebSocket() {
		super();

		wsFactory = new WebSocketFactory();
	}

	public void setPingInterval(int intervalSec) {
		if (ws !=null) {
			ws.setPingInterval((long)intervalSec*1000L);
		}
	}

	/**
	 * WebSocket 서버로 연결을 시도한다.
	 * @return 성공시 true, 연결 실패시 false를 리턴한다.
	 */
	@Override
	public boolean connect() {
		try {
			if ( getConnectURI() != null ) {

				// SSL 사용시
				if ( useSSL ) {
					if (sslCertCheck) {
						if (certFile != null) {
							wsFactory.setSSLContext(getTrustContext());
						}
					} else {
						wsFactory.setSSLContext(getAnyTrustContext());
					}
				}

				// 해당 URI에 CPID(고유 충전기 ID)를 조합하여 접속한다.
				ws = wsFactory.createSocket(getConnectURI()+"/"+getCPID(), TRANSPORT_CONNECT_TIMEOUT);
				ws.addListener(this);

				// Basic Authentication 을 사용할 경우 ID 및 Password를 설정한다.
				if (getBasicAuthID() != null && getBasicAuthID().length() > 0) {
					ws.setUserInfo(getBasicAuthID(), getBasicAuthPassword());
				}

				// Protocol을 설정한다.(OCPP 버전)
				ws.addProtocol("ocpp1.6");
			}
		}
		catch (Exception e) {
			LogWrapper.e(TAG, "Connect Exception 1:"+e.toString());
			return false;
		}

		// Websocket 연결을 시도한다.
		try {
			ws.connect();
		}
		catch (Exception e) {
			LogWrapper.e(TAG, "Connect Exception 2:"+e.toString());
			return false;
		}

		return true;
	}

	/**
	 * 연결을 끊는다.
	 */
	@Override
	public void disconnect() {
		if ( ws != null ) ws.disconnect();
		ws = null;
	}

	/**
	 * Websocket응 통해서 String 메시지를 보낸다.
	 * @param msg 보낼 메시지(String)
	 */
	@Override
	public void sendMessage(String msg) {
		if ( ws != null ) {
			if ( monitorListener != null) monitorListener.onOCPPTransportSendRaw(msg);
			ws.sendText(msg);
			Log.v(TAG, "Send Json:"+msg);
		}
	}

	@Override
	public void onTextMessage(WebSocket websocket, String text) throws Exception {
		if ( monitorListener != null) monitorListener.onOCPPTransportRecvRaw(text);
		Log.v(TAG, "Recv Json:" + text);
		onRecvMessage(text);
	}

	@Override
	public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {

	}

	@Override
	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
		onConnect();
		if ( monitorListener != null) monitorListener.onOCPPTransportConnected();
		LogWrapper.v(TAG, "onConnected");
	}

	@Override
	public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {

	}

	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
		onDIsconnect();
		if ( monitorListener != null) monitorListener.onOCPPTransportDisconnected();
	}

	@Override
	public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}


	@Override
	public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

	}

	@Override
	public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

	}

	@Override
	public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

	}

	@Override
	public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

	}

	@Override
	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {

	}

	@Override
	public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {

	}

	@Override
	public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {

	}

	@Override
	public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {

	}

	@Override
	public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

	}

	@Override
	public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {

	}

	@Override
	public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {

	}

	@Override
	public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

	}
}
