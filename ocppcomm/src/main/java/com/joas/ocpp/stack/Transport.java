/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.ocpp.stack;

// Start of user code (user defined imports)

// End of user code

import com.joas.utils.LogWrapper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Description of Transport.
 * 
 * @author scchoi
 */

public class Transport {
	/**
	 * Description of the property listener.
	 */
	public static final String TAG = "Transport";
	public static final int TRANSPORT_CONNECT_TIMEOUT = 5000;
	private TransportListener listener = null;
	protected OCPPTransportMonitorListener monitorListener = null;

	String CPID = "";
	String basicAuthID = "";
	String basicAuthPassword ="";

	String connectURI = null;
	String certFile = null;
	boolean sslCertCheck = false;
	boolean useSSL = false;

	/**
	 * The constructor.
	 */
	public Transport() {
		// End of user code
	}

	public void sendMessage(String msg) {}

	public void onRecvMessage(String msg) {
		if ( listener != null ) listener.onRecvTransportMessage(msg);
	}

	public boolean connect() {
		return false;
	}
	public void disconnect() {	}

	public void onConnect() {
		if ( listener != null ) listener.onConnectTransport();
	}

	public void onDIsconnect() {
		if ( listener != null ) listener.onDisconnectTransport();
	}

	public void setCPID( String cpid ) {
		this.CPID = cpid;
	}

	public String getCPID() {
		return this.CPID;
	}

	public void setBasicAuthID( String id ) {
		this.basicAuthID = id;
	}

	public String getBasicAuthID() {
		return this.basicAuthID;
	}

	public void setBasicAuthPassword( String password ) {
		this.basicAuthPassword = password;
	}

	public String getBasicAuthPassword() {
		return this.basicAuthPassword;
	}

	public void setConnectURI( String uri ) {
		this.connectURI = uri;
	}

	public String getConnectURI() {
		return this.connectURI;
	}

	public void setSSLCertFile(String filename) { certFile = filename; }
	public String getSSLCertFile() { return certFile; }
	public void setSSLCertCheck(boolean tf) { sslCertCheck = tf;}
	public void setUseSSL(boolean tf) { useSSL = tf;}



	protected  SSLContext getTrustContext() {
		SSLContext context = null;
		try {
			File file = new File(certFile);

			if ( file.exists() ) {
				// Load CAs from an InputStream
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				//InputStream caInput = new BufferedInputStream(new FileInputStream(Environment.getExternalStorageDirectory() + File.separator+"server.crt"));
				//InputStream caInput =  getResources().openRawResource(R.raw.server);
				InputStream caInput = new BufferedInputStream(new FileInputStream(certFile));
				Certificate ca;
				try {
					ca = cf.generateCertificate(caInput);
					LogWrapper.v(TAG, "ca=" + ((X509Certificate) ca).getSubjectDN());
				} finally {
					caInput.close();
				}

// Create a KeyStore containing our trusted CAs
				String keyStoreType = KeyStore.getDefaultType();
				KeyStore keyStore = KeyStore.getInstance(keyStoreType);
				keyStore.load(null, null);
				keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
				String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
				tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager
				context = SSLContext.getInstance("SSLv3");
				context.init(null, tmf.getTrustManagers(), null);
			}
		}
		catch (Exception e)
		{
			LogWrapper.e(TAG, "SSL Exception:"+e.toString());
		}
		return context;
	}

	protected  SSLContext getAnyTrustContext() {
		SSLContext context = null;
		try {
			TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
						public void checkClientTrusted(X509Certificate[] certs, String authType) { }
						public void checkServerTrusted(X509Certificate[] certs, String authType) { }
					}
			};
			context = SSLContext.getInstance("SSLv3");
			context.init(null, trustAllCerts, new java.security.SecureRandom());
		}
		catch (Exception e)
		{
			LogWrapper.e(TAG, "SSL Exception:"+e.toString());
		}
		return context;
	}
	/**
	 * Returns listener.
	 * @return listener 
	 */
	public TransportListener getListener() {
		return this.listener;
	}

	public OCPPTransportMonitorListener getMonitorListener() {
		return this.monitorListener;
	}
	/**
	 * Sets a value to attribute listener. 
	 * @param newListener
	 */
	public void setListener(TransportListener newListener) {
		this.listener = newListener;
	}


	public void setMonitorListener(OCPPTransportMonitorListener newListener) {
		this.monitorListener = newListener;
	}

}
