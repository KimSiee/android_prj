/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.ocpp.stack;

import android.database.Cursor;
import android.util.Log;

import com.joas.ocpp.msg.MeterValues;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.utils.LogWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Description of Transceiver.
 * 
 * @author user
 */
public class Transceiver extends Thread implements TransportListener {
	/**
	 * Description of the property transport.
	 */
	public static final String TAG = "Transceiver";
	public static final int PROTOCOL_TYPE_OCPP_J = 0x01;
	public static final int PROTOCOL_TYPE_OCPP_S = 0x02;

	public static final int RECONNECT_TIMEOUT = 15*1000; 	// 15초 // 서버 재접속 주기
	public static final int REQUEST_TIMEOUT = 30*1000; 	// 30 sec // Request에 대한 응답을 기다리는 시간

	public static final int RESPONSE_OK		= 0x00;
	public static final int RESPONSE_ERROR 	= 0x01;
	public static final int RESPONSE_TIMEOUT 	= 0x02;

	protected OCPPDbOpenHelper dbOpenHelper = null;
	OCPPConfiguration ocppConfiguration = null;

	protected Transport transport = null;

	protected OCPPMessageParser ocppMessageParser = null;
	protected OCPPMessage lastSentOCPPRequest = null;

	protected boolean isEndFlag = false;
	protected boolean isConnected = false;

	protected OCPPMessageQueue requestQueue;
	protected OCPPMessageQueue transactionQueue;

	protected long requestSeq = 0;

	private AtomicBoolean isMsgWait = new AtomicBoolean(false);

	protected boolean isSaveLostTransactionMessage = true;

	/**
	 * Description of the property listener.
	 */
	public TransceiverListener listener = null;

	private Thread sendMsgThread = null;
	private Object sendMsgSync = new Object();

	boolean isLockSendMsg = true;

	boolean isRequestReponseOk = false;

	// 현재 패킷 보내고 있는 상태가 Transaction인지 아닌지
	boolean isTransactionState = false;

	public String lastUniqeId_StopTransaction = "00";
	public String lastUniqeId_Metervalue = "00";
	public boolean isMonitorStat = false;
	/**
	 * The constructor.
	 */
	public Transceiver() {
		super();

		requestQueue = new OCPPMessageQueue();
		transactionQueue = new OCPPMessageQueue();

		sendMsgThread = new Thread(new Runnable() {
			@Override
			public void run() {
				sendMsgProcess();
			}
		});

		sendMsgThread.start();
	}

	void setLockSendMsg(boolean lock) {
		isLockSendMsg = lock;
	}

	public void setUseSaveLostTransactionMessage(boolean tf) { isSaveLostTransactionMessage = tf;}
    public void setTransactionState(boolean tf) { isTransactionState = true; }
	public boolean getTransactionState() { return isTransactionState; }
	public void setDbOpenHelper(OCPPDbOpenHelper dbHelper) { dbOpenHelper = dbHelper; }
	public OCPPMessageParser getOCPPMessageParser() { return ocppMessageParser; }

	public String getLastUniqeId_Stoptransaction(){return lastUniqeId_StopTransaction;}
	public void setLastUniqeId_StopTransaction(String uniqeId){ this.lastUniqeId_StopTransaction = uniqeId;}
	public String getLastUniqeId_Metervalue(){return lastUniqeId_Metervalue;}
	public void setLastUniqeId_Metervalue(String uniqeId){ this.lastUniqeId_Metervalue = uniqeId;}
	public void setTransactionMonitorStat(boolean stat){this.isMonitorStat = stat;}
	public boolean getTransactionMonitorStat(){return this.isMonitorStat;}

	public static Transceiver createTransceiver(int protoclType, OCPPDbOpenHelper dbHelper) {
		Transceiver transceiver;

		if ( protoclType == PROTOCOL_TYPE_OCPP_J ) {
			transceiver = new TransceiverWAMP();
		}
		else {
			transceiver = new TransceiverSOAP();
		}

		transceiver.setDbOpenHelper(dbHelper);

		return transceiver;
	}

	/**
	 * Returns transport.
	 * @return transport 
	 */
	public Transport getTransport() {
		return this.transport;
	}

	/**
	 * Sets a value to attribute transport. 
	 * @param newTransport 
	 */
	public void setTransport(Transport newTransport) {
		this.transport = newTransport;
		this.transport.setListener(this);
	}


	public void setOcppConfigration(OCPPConfiguration config) {
		ocppConfiguration = config;
	}

	/**
	 * Returns listener.
	 * @return listener 
	 */
	public TransceiverListener getListener() {
		return this.listener;
	}

	/**
	 * Sets a value to attribute listener. 
	 * @param newListener
	 */
	public void setListener(TransceiverListener newListener) {
		this.listener = newListener;
	}

	public void sendRequest(OCPPMessage message) {
		try {
			message.reqSeq = requestSeq;
			sendMessageToQueue(message);
			requestSeq++;
		} catch (Exception e) {
			LogWrapper.e(TAG, "sendRequest Ex:" + e.toString());
		}

	}

	public void sendResponse(OCPPMessage message) {
		try {
			//Encoding Message(WAMP/SOAP)
			processRawPayload(message, false);

			transport.sendMessage(message.rawPayload);

		} catch (Exception e) {
			LogWrapper.e(TAG, "sendResponse Ex:" + e.toString());
		}
	}

	public void sendResponseNotSupported(OCPPMessage message) {
		try {
			String msg = processResponseNotSupported(message);
			transport.sendMessage(msg);
		} catch (Exception e) {
			LogWrapper.e(TAG, "sendResponseNotSupported Ex:" + e.toString());
		}
	}

	public boolean getConnected() { return isConnected; }

	public void disconnect() {
		transport.disconnect();
		isConnected = false;
	}

	protected void sendMessageToQueue(OCPPMessage message) {
		// Transaction Message save to memory and Flash
		if ( message.isTransactionMessage() ) {

		    // 우선 저장한 다음 응답을 받으면 삭제한다.
		    saveTransactionMessage(message);

		    // 연결 타이밍. 만약 중간에 연결되고 StopTransaction이면 DB에 저장??
			// 다시 생각해서 정리가 필요함.
			/*
		    if ( message.action.equals("StopTransaction") ) {
                StopTransaction stopTransaction = (StopTransaction)message.payload;
                if ( stopTransaction.getTransactionId() == null ) return;;
            }
            */


		    if ( isConnected ) transactionQueue.add(message);
		}
		else if ( isConnected ) requestQueue.add(message);
	}

	@Override
	public void run() {
		// Connect Operation Thread
		while (!isEndFlag && !this.isInterrupted()) {
			if ( transport != null ) {
				if ( isConnected == false ) {
					if ( transport.connect() == true ) {
						isConnected = true;
						LogWrapper.v(TAG, "Transport Connect is Done.");

					}
					else {
						LogWrapper.v(TAG, "Try Connect to Server Failed.");
					}
				}
			}

			try {
				Thread.sleep(RECONNECT_TIMEOUT);
			}
			catch (InterruptedException e) {
				LogWrapper.e(TAG, e.toString());
			}
		}
	}

	public void stopTransceiver() {
		isEndFlag = true;
		transport.disconnect();
	}

	void responseAndErrorProcess(int respType) {
		if (lastSentOCPPRequest.isTransactionMessage()) {
			if ( respType == RESPONSE_TIMEOUT ) {

				lastSentOCPPRequest.retryCnt++;

				// Timeout but, Retry need. Maybe Connection Disconnct
				// 재시도 이후 재접속으로 수정..필요
				if ( lastSentOCPPRequest.retryCnt >= ocppConfiguration.TransactionMessageAttempts ) {
					if (listener != null) {
						listener.onRequestTimeout(lastSentOCPPRequest);
						if (transport != null) disconnect();
						LogWrapper.v(TAG, "Request Timeout Over Retry. Disconnect");
					}
				}
				LogWrapper.v(TAG, "Request Timeout:" + lastSentOCPPRequest.getAction());
			}
			else if ( respType == RESPONSE_ERROR ) {
				//TODO
				lastSentOCPPRequest.retryCnt++;
				// To DO..
				// RetryCnt 가 Configration 값보다 크다면 삭제...
				//if ( lastSentOCPPRequest.retryCnt >= 3 )
				LogWrapper.v(TAG, "Response Error:" + lastSentOCPPRequest.getAction());
			}
			else {
				isRequestReponseOk = true;
			}
		} else {
			if ( respType == RESPONSE_TIMEOUT ) {
				Log.e(TAG, "Resp Msg Timeout:"+lastSentOCPPRequest.action);
			}

			isRequestReponseOk = true;
		}

		//Log.v(TAG, "Recv Resp Msg:"+respType);
		isMsgWait.set(false);
	}

	void sendMsgProcess() {
		boolean isTransactionQueueEmpty = true;
		boolean isRequestQueueEmpty = true;

		while (!isEndFlag) {
			if ( isMsgWait.get() == false ) {
				OCPPMessage sendMsg = null;
				OCPPMessage sendMsgTransaction = null;
				OCPPMessage sendMsgRequest = null;

				try {
					synchronized (transactionQueue) {
						isTransactionQueueEmpty = transactionQueue.isEmpty();
					}
					synchronized (requestQueue) {
						isRequestQueueEmpty = requestQueue.isEmpty();
					}

					// 1. Transaction Queue에 데이터가 있는경우 우선 처리한다.
					// (lock걸려있는경우 전송하지 않는다.BootNotification에서 ACCEPT를 받아야 Lock가 풀림)
					if (isTransactionQueueEmpty == false && isLockSendMsg == false) {
						synchronized (transactionQueue) {
							sendMsgTransaction = transactionQueue.peek();
							sendMsg = sendMsgTransaction;
							//Log.v(TAG, "transQ peek:"+sendMsg.getAction());
						}
					}
					// 2. 일반 전송 Queue에 있는 데이터를 처리한다.
					if (isRequestQueueEmpty == false) {
						synchronized(requestQueue) {
							sendMsgRequest = requestQueue.peek();
							sendMsg = sendMsgRequest;
							//Log.v(TAG, "req peek:"+sendMsg.getAction());
						}
					}

					// 두개의 패킷이 모두 있으면 seq가 작은것은 항상 먼저 보낸다.(순서에 맞게 보내기 위함)
					if ( sendMsgTransaction != null && sendMsgRequest != null ) {
						if ( sendMsgTransaction.reqSeq > sendMsgRequest.reqSeq ) sendMsg = sendMsgRequest;
						else sendMsg = sendMsgTransaction;
					}

					if (sendMsg != null) {
						lastSentOCPPRequest = sendMsg;

						//Encoding Message(WAMP/SOAP)
						processRawPayload(sendMsg, true);

						isRequestReponseOk = false;

						transport.sendMessage(sendMsg.rawPayload);

						// 한 패킷의 Response(Confirm)이 오기 전이나 대기 시간이 끝날때까지 대기한다.
						// Response를 받으면 notify()가 불려짐
						// 계속해서 패킷을 연속으로 보내지 않기 위함
						isMsgWait.set(true);

						int waitCnt = REQUEST_TIMEOUT/10;
						if ( lastSentOCPPRequest.isTransactionMessage() ) waitCnt = (ocppConfiguration.TransactionMessageRetryInterval*1000)/10;

						boolean isTimeout = false;
						while ( isMsgWait.get() == true ) {
							Thread.sleep(10);
							if ( isEndFlag == true ) break;
							if ( --waitCnt <= 0 ) {
								responseAndErrorProcess(RESPONSE_TIMEOUT);
								isTimeout = true;
								break;
							}
						}

						if ( isTimeout == true && lastSentOCPPRequest.retryCnt < ocppConfiguration.TransactionMessageAttempts) continue;

						if (sendMsg == sendMsgTransaction) {
							if ( isRequestReponseOk ) {
                                removeSaveTransactionMessage(sendMsg.getId());
							}
							else {
								// Message Error
								removeSaveTransactionMessage(sendMsg.getId());

								LogWrapper.e(TAG, "transQ pop NAK: Response Not OK");
							}
                            synchronized (transactionQueue) {
                                //Log.v(TAG, "transQ pop:"+sendMsg.getAction());
                                if (!transactionQueue.isEmpty()) transactionQueue.pop();
                            }

						} else {
							synchronized (requestQueue) {
								if (!requestQueue.isEmpty()) requestQueue.pop();
								//Log.v(TAG, "req pop:"+sendMsg.getAction());
							}
						}
					}
				}catch (Exception e) {
					StringWriter stackTrace = new StringWriter();
					e.printStackTrace(new PrintWriter(stackTrace));
					LogWrapper.e(TAG, "Action:"+sendMsg.getAction()+" err:"+stackTrace.toString());
				}
			}

			try {
				Thread.sleep(10);
			} catch (Exception e) {
				LogWrapper.e(TAG, e.toString());
			}
		}
	}


    /**
     * 데이터베이스에 트렌젝션 메시지를 저장한다.
     * @param message 저장할 메시지
     */
    public void saveTransactionMessage(OCPPMessage message) {
		String payload = getOCPPMessageParser().Serialize(message.action, message.getPayload());

		String strQuery = String.format("REPLACE INTO %s(%s,%s,%s,%s,%s,%s) VALUES ('%s','%s','%s','%s','%d','%d')",
				OCPPDatabase.LostTransactionMessage.TABLENAME,
				OCPPDatabase.LostTransactionMessage.UNIQUEID,
				OCPPDatabase.LostTransactionMessage.ACTION,
				OCPPDatabase.LostTransactionMessage.PAYLOAD,
				OCPPDatabase.LostTransactionMessage.STARTTIME,
				OCPPDatabase.LostTransactionMessage.TID,
				OCPPDatabase.LostTransactionMessage.CONNECTOR_ID,
				message.id, message.action, payload, message.getTransactionStartTime(), -1, message.transactionConnectorId);
		if ( isSaveLostTransactionMessage ) dbOpenHelper.mDB.execSQL(strQuery);
    }

    /**
     * 저장되어 있는 트렉젠션 메시지를 지운다.
     * @param uniqueId 고유 아이디
     */
    public void removeSaveTransactionMessage(String uniqueId) {
        dbOpenHelper.mDB.delete(OCPPDatabase.LostTransactionMessage.TABLENAME, OCPPDatabase.LostTransactionMessage.UNIQUEID + "=?", new String[] { uniqueId });
    }

    /**
     * 연결이 시작되면 데이터베이스에 있는 트렌젝션 메시지들을 큐로 복구한다.
     */
    protected void restoreLostTransactionMessage() {
        String strQuery = String.format("SELECT %s, %s, %s, %s, %s, %s FROM %s order by id asc",
                OCPPDatabase.LostTransactionMessage.UNIQUEID,
                OCPPDatabase.LostTransactionMessage.ACTION,
                OCPPDatabase.LostTransactionMessage.PAYLOAD,
				OCPPDatabase.LostTransactionMessage.STARTTIME,
				OCPPDatabase.LostTransactionMessage.TID,
                OCPPDatabase.LostTransactionMessage.CONNECTOR_ID,
                OCPPDatabase.LostTransactionMessage.TABLENAME);

        String uniqueId = null;
        String action = null;
        String payload = null;
		String startTime = null;
		int tid = -1;
        int connectorId = -1;

        Cursor cursor = null;
        try {
            cursor = dbOpenHelper.mDB.rawQuery(strQuery, null);
            if (cursor != null && cursor.getCount() != 0){
                cursor.moveToFirst();

                do {
                    uniqueId = cursor.getString(0);
                    action = cursor.getString(1);
                    payload = cursor.getString(2);
                  	startTime =   cursor.getString(3);
					tid =   cursor.getInt(4);
                    connectorId = cursor.getInt(5);

                    Object objPayload = ocppMessageParser.DeSerialize(action, payload);
                    OCPPMessage ocppMessage = new OCPPMessage(uniqueId, action, objPayload);
                    ocppMessage.transactionConnectorId = connectorId;
                    ocppMessage.isRecoveryMsg = true;
                    ocppMessage.recoveryTid = tid;
					ocppMessage.setTransactionStartTime(startTime);

					if ( tid > 0 ) {
						if ( ocppMessage.action.equals("StopTransaction") ) {
							StopTransaction stopTransaction = (StopTransaction)ocppMessage.getPayload();
							stopTransaction.setTransactionId(tid);
						}
						else if ( ocppMessage.action.equals("MeterValues") ) {
							MeterValues meterValues = (MeterValues )ocppMessage.getPayload();
							meterValues.setTransactionId(tid);
						}
					}

                    transactionQueue.add(ocppMessage);
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            LogWrapper.e(TAG, "DB:"+OCPPDatabase.LostTransactionMessage.TABLENAME+" Version Query "+strQuery+" ERR!!:"+e.toString());
        }
        finally {
            if (cursor != null) cursor.close();
        }

    }

	public void findAndUpdateTransactionIDFromLostMsg(String startTime, int transactionId) {
    	// Find First Transaction Queue Msg and Fill
		transactionQueue.findAndUpdateTransactionID(startTime, transactionId);

		//DataBase Fill TID
		String strQuery = String.format("UPDATE %s SET %s = %d WHERE %s = '%s'",
				OCPPDatabase.LostTransactionMessage.TABLENAME,
				OCPPDatabase.LostTransactionMessage.TID,
				transactionId,
				OCPPDatabase.LostTransactionMessage.STARTTIME,
				startTime);

		try {
			dbOpenHelper.mDB.execSQL(strQuery);
		}catch (Exception e) {
			LogWrapper.e(TAG, "findAndUpdateTransactionIDFromLostMsg Query :"+strQuery+", Err:"+e.toString());
		}
	}

    @Override
	public void onRecvTransportMessage(String msg) {

	}

	@Override
	public void onConnectTransport() {
		if ( listener != null ) listener.onConnectTransceiver();

		restoreLostTransactionMessage();

		isMsgWait.set(false);

		LogWrapper.v(TAG, "Transport is Connected.");
	}

	@Override
	public void onDisconnectTransport() {
		isConnected = false;
		LogWrapper.v(TAG, "Transport is Disconnected.");

//		requestQueue.clear();
//        transactionQueue.clear();

		if ( listener != null ) listener.onDisconnectTransceiver();
		isMsgWait.set(true);
	}

	public void processRawPayload(OCPPMessage message, boolean isRequest) {
		message.setRawPayload("");
	}

	// Override need
	public String processResponseNotSupported(OCPPMessage message) {
		return "";
	}
}
