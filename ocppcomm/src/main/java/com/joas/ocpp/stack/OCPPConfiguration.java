/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 31 오후 4:03
 *
 */

package com.joas.ocpp.stack;

import com.joas.ocpp.msg.ChangeConfigurationResponse;
import com.joas.ocpp.msg.ConfigurationKey;
import com.joas.ocpp.msg.GetConfigurationResponse;
import com.joas.utils.LogWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * OCPP의 Configuration Key를 세팅한다.
 * 해당 Key의 변수(Attribute) 선언은 반듯이 프로토콜 명세서에 있는 이름과 동일하게 한다.
 * 이를 Reflection을 이용하여 변수 이름을 Key 값으로 치환하여 그대로 사용한다.
 */

public class OCPPConfiguration {
    /**
     * Optional. If this key exists, the Charge Point supports Unknown Offline Authorization.
     * If this key reports a value of true, Unknown Offline Authorization is enabled.
     */
    public boolean AllowOfflineTxForUnknownId = false;

    /**
     * optional. If this key exists, the Charge Point supports an Authorization Cache.
     * If this key reports a value of true, the Authorization Cache is enabled.
     */
    public boolean AuthorizationCacheEnabled = false;

    /**
     *  required. Whether a remote request to start a transaction in the form of a RemoteStartTransaction.req message
     *  should be authorized beforehand like a local action to start a transaction.
     */
    public boolean AuthorizeRemoteTxRequests = false;

    /**
     * optional. Number of times to blink Charge Point lighting when signalling
     */
    //public int BlinkRepeat = 0;

    /**
     *  required. Size (in seconds) of the clock-aligned data interval.
     *  This is the size (in seconds) of the set of evenly spaced aggregation intervals per day,
     *  starting at 00:00:00 (midnight)
     */
    public int ClockAlignedDataInterval = 0;

    /**
     * required. Interval (from successful authorization) until
     * incipient charging session is automatically canceled due to
     * failure of EV user to (correctly) insert the charging cable connector(s) into the appropriate connector(s).
     */
    public int ConnectionTimeOut = 60;

    /**
     * required
     * Maximum number of requested configuration keys in a GetConfiguration.req PDU.
     */
//    public int GetConfigurationMaxKeys = 50;
    public int GetConfigurationMaxKeys = 11;

    public int HeartbeatInterval = 60;

    /**
     * optional
     */
    //public int LightIntensity = 100;

    /**
     * required. whether the Charge Point, when offline, will start a transaction for locallyauthorized identifiers.
     */
    public boolean LocalAuthorizeOffline = true;

    /**
     * required. whether the Charge Point, when online, will start a transaction for locallyauthorized identifiers
     * without waiting for or requesting an Authorize.conf from the Central System
     */
    public boolean LocalPreAuthorize = true;

    /**
     * optional. (Wh) Maximum energy in Wh delivered when an identifier is invalidated by the Central System after start of a transaction.
     */
    //public int MaxEnergyOnInvalidId = 0;

    /**
     * required. Clock-aligned measurand(s) to be included in a MeterValues.req PDU, every ClockAlignedDataInterval seconds
     */
    public String MeterValuesAlignedData = "Current.Import, Current.Offered, Energy.Active.Import.Register, Energy.Active.Import.Interval, Power.Active.Import, Power.Offered, SoC, Voltage";;

    /**
     * optional. Maximum number of items in a MeterValuesAlignedData Configuration Key.
     */
    public int MeterValuesAlignedDataMaxLength = 8;

    /**
     * required. Sampled measurands to be included in a MeterValues.req PDU,
     * every MeterValueSampleInterval seconds. Where applicable,
     * the Measurand is combined with the optional phase; for instance: Voltage.L1 Default: "Energy.Active.Import.Register"
     */
    public String MeterValuesSampledData = "Current.Import, Current.Offered, Energy.Active.Import.Register, Power.Active.Import, Power.Offered, SoC, Voltage";

    public int MeterValuesSampledDataMaxLength = 8;



    /**
     * required. Interval between sampling of metering (or other) data, intended to be
     * transmitted by "MeterValues" PDUs. For charging session data
     * (ConnectorId>0), samples are acquired and transmitted periodically at this
     * interval from the start of the charging transaction.
     * A value of "0" (numeric zero), by convention, is to be interpreted to mean
     * that no sampled data should be transmitted.
     */
    public int MeterValueSampleInterval = 300; // 5min

    /**
     * optional. The minimum duration that a Charge Point or Connector status is stable before a StatusNotification.req PDU is sent to the Central System.
     */
    //public int MinimumStatusDuration = 600; // 10min

    /**
     * required. The number of physical charging connectors of this Charge Point
     */
    public int NumberOfConnectors = 1;

    /**
     * required. Number of times to retry an unsuccessful reset of the Charge Point.
     */
    public int ResetRetries = 1;

    /**
     * required.  The phase rotation per connector in respect to the connector’s energy meter (or if absent, the grid connection).
     */
    public String ConnectorPhaseRotation = "RST";

    /**
     * optional. Maximum number of items in a ConnectorPhaseRotation Configuration Key.
     */
    //public int ConnectorPhaseRotationMaxLength = 10;

    /**
     * required. When set to true, the Charge Point SHALL administratively stop the transaction when the cable is unplugged from the EV.
     */
    public boolean StopTransactionOnEVSideDisconnect = true;

    /**
     * required. whether the Charge Point will stop an ongoing transaction
     * when it receives a non- Accepted authorization status in a StartTransaction.conf for thistransaction
     */
    public boolean StopTransactionOnInvalidId = true;

    /**
     * Clock-aligned periodic measurand(s) to be included in the TransactionData element of StopTransaction.req
     * MeterValues.req PDU for every ClockAlignedDataInterval of the charging session
     */
    public String StopTxnAlignedData = "Current.Import, Current.Offered, Energy.Active.Import.Register, Energy.Active.Import.Interval, Power.Active.Import, Power.Offered, SoC, Voltage";

    /**
     * optional. Maximum number of items in a StopTxnAlignedData Configuration Key.
     */
    public int StopTxnAlignedDataMaxLength = 8;

    public String StopTxnSampledData = "Current.Import, Current.Offered, Energy.Active.Import.Register, Power.Active.Import, Power.Offered, SoC, Voltage";
    public int StopTxnSampledDataMaxLength = 8;

    /**
     * required. A list of supported Feature Profiles. Possible profile identifiers: Core,
     FirmwareManagement, LocalAuthListManagement, Reservation,
     SmartCharging and RemoteTrigger.
     */
    public String SupportedFeatureProfiles = "Core,FirmwareManagement,LocalAuthListManagement,Reservation,SmartCharging,RemoteTrigger";
    public int SupportedFeatureProfilesMaxLength = 6;

    /**
     * required. How often the Charge Point should try to submit a transaction-related message when the Central System fails to process it.
     */
    public int TransactionMessageAttempts = 3;

    /**
     * required. How long the Charge Point should wait before resubmitting a transactionrelated message that the Central System failed to process.
     */
    public int TransactionMessageRetryInterval = 30;

    /**
     * required. When set to true, the Charge Point SHALL unlock the cable on Charge Point side when the cable is unplugged at the EV.
     */
    public boolean UnlockConnectorOnEVSideDisconnect = true;

    /**
     * optional
     */
    public int WebSocketPingInterval = 0;

    /**
     * required. whether the Local Authorization List is enabled
     */
    public boolean LocalAuthListEnabled = false;

    /**
     * required. Maximum number of identifications that can be stored in the Local Authorization List
     */
    public int LocalAuthListMaxLength = 100000;

    /**
     * required. Maximum number of identifications that can be send in a single SendLocalList.req
     */
    public int SendLocalListMaxLength = 100;

    /**
     * optional. If this configuration key is present and set to true: Charge Point support reservations on connector 0.
     */
    //public boolean ReserveConnectorZeroSupported = false;

    /**
     * required. Max StackLevel of a ChargingProfile. The number defined also indicates
     * the max allowed number of installed charging schedules per Charging Profile Purposes.
     */
    public int ChargeProfileMaxStackLevel = 5;

    /**
     * required. A list of supported quantities for use in a ChargingSchedule. Allowed values: 'Current' and 'Power'
     */
    public String ChargingScheduleAllowedChargingRateUnit = "Power";

    /**
     * required. Maximum number of periods that may be defined per ChargingSchedule.
     */
    public int ChargingScheduleMaxPeriods = 24;

    /**
     * optional. If defined and true, this Charge Point support switching from 3 to 1 phase during a charging session.
     */
    //Npublic boolean ConnectorSwitch3to1PhaseSupported = false;

    /**
     * required. Maximum number of Charging profiles installed at a time
     * ChargeProfileMaxStackLevel + txProfile + maxPorfile
     */
    public int MaxChargingProfilesInstalled = 7;

    //======== OCPP 시뮬레이터 사용?? Spec에서는 없음 Unknown 처리

    public boolean LocalAuthorizationListEnabled = false; // LocalAuthListEnabled와 동일 하게 처리함

    // Firmware Update시.. Configuration 규약에 있으나. Configuration리스트에는 빠져있음. 현재 가능한 프로토콜 명시
    public String SupportedFileTransferProtocols = "FTP, HTTP";

    /*
    public boolean AllowOfflineTxForUnknownIdAvailable = false;
    public boolean AllowOfflineTxForUnknownIdEnabled = false;
    public boolean AuthorizationCacheAvailable = false;
    public int HeartBeatInterval = 10;

    public boolean SupportedCompliancyProfiles = false;
    public String ChargingScheduleAllowedSchedulingUnit = "Power";
    public int ProximityLockRetries = 1;
    public int ProximityContactRetries = 1;
    public String ChargePointId = "01";
    public int LocalAuthListSize = 0;
    public int LocalAuthMaxElementsOnce = 100;
    public String MeterValuesTriggeredData = "";
    public boolean Testing = false;
    */

    //  JOAS 회사 고유값
    public int JoasDiagnosticsLogDays = 60; // 최대 진단 로그 저장 기간

    /**
     * 해당 Key가 ReadOnly인지 아닌지 판별한다.(Spec 9.)
     * @param key 키값
     * @return ReadOnly일때 True
     */
    public boolean isKeyReadOnly(String key) {
        boolean ret = false;
        if ( key == null ) return false;

        switch ( key ) {
            case "GetConfigurationMaxKeys":
            case "NumberOfConnectors":
            case "MeterValuesSampledDataMaxLength":
            case "MeterValuesAlignedDataMaxLength":
            case "ConnectorPhaseRotationMaxLength":
            case "StopTxnAlignedDataMaxLength":
            case "StopTxnSampledDataMaxLength":
            case "SupportedFeatureProfiles":
            case "SupportedFeatureProfilesMaxLength":
            case "LocalAuthListMaxLength":
            case "SendLocalListMaxLength":
            case "ReserveConnectorZeroSupported":
            case "ChargeProfileMaxStackLevel":
            case "ChargingScheduleAllowedChargingRateUnit":
            case "ChargingScheduleMaxPeriods":
            case "ConnectorSwitch3to1PhaseSupported":
            case "MaxChargingProfilesInstalled":
            case "StopTransactionOnEVSideDisconnect":
//            case "AuthorizeRemoteTxRequests":
                ret = true;
                break;
        }

        return ret;
    }

    /**
     * Reflaction 사용하여 현재 변수이름에 해당하는 값을 돌려준다.
     * @param listKey GetConfigration 패킷에 있는 key List
     * @return 해당 List에 맞는 KeyValue List
     */

    public GetConfigurationResponse getConfigurationsResponse(List<String> listKey) {
        GetConfigurationResponse response = new GetConfigurationResponse();
        List<ConfigurationKey> listConfig = new ArrayList<ConfigurationKey>();
        List<String> listUnknown = new ArrayList<String>();
        for (String key : listKey) {
            key = key.trim();
            try {
                Field field = this.getClass().getField(key);
                ConfigurationKey configKey = new ConfigurationKey();
                configKey.setKey(key);

                if ( isKeyReadOnly(key) ) configKey.setReadonly( true );
                else configKey.setReadonly( false );

                String value = field.get(this).toString();
                configKey.setValue(value);

                listConfig.add(configKey);
            } catch (NoSuchFieldException e) {
                LogWrapper.d("OCPPConfig", "getConfig key:"+key+" unknown");
                listUnknown.add(key);
            } catch (Exception e) {
                LogWrapper.d("OCPPConfig", "getConfig key:"+key+" Ex:"+e.toString());
            }
        }
        response.setConfigurationKey(listConfig);
        response.setUnknownKey(listUnknown);
        return response;
    }

    public GetConfigurationResponse getConfigurationsAll() {
        GetConfigurationResponse response = new GetConfigurationResponse();
        List<ConfigurationKey> listConfig = new ArrayList<ConfigurationKey>();
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            String key = field.getName();
            try {
                ConfigurationKey configKey = new ConfigurationKey();
                configKey.setKey(key);

                if ( isKeyReadOnly(key) ) configKey.setReadonly( true );
                else configKey.setReadonly( false );

                String value = field.get(this).toString();
                configKey.setValue(value);

                listConfig.add(configKey);
            }
            catch (Exception e) {
                LogWrapper.d("OCPPConfig", "getConfig key:"+key+" Ex:"+e.toString());
            }
        }
        response.setConfigurationKey(listConfig);
        return response;
    }

    public ChangeConfigurationResponse changeConfigurationResponse(String key, String value) {
        ChangeConfigurationResponse response = new ChangeConfigurationResponse();
        key = key.trim();

        if ( isKeyReadOnly(key) == true ) {
            response.setStatus(ChangeConfigurationResponse.Status.REJECTED);
        }
        else {
            try {
                Field field = this.getClass().getField(key);
                if ( field.getType() == Integer.TYPE ) field.setInt(this, Integer.parseInt(value));
                else if ( field.getType() == Boolean.TYPE ) field.setBoolean(this, Boolean.parseBoolean(value));
                else {
                    field.set(this, value);
                }

                // Custom Key from OCPP Simulator
                if ( key.equals("LocalAuthorizationListEnabled") ) {
                    LocalAuthListEnabled = Boolean.parseBoolean(value);
                }

                response.setStatus(ChangeConfigurationResponse.Status.ACCEPTED);
                // To. Do!!
                // Setting 갑 바뀐것 적용!! 알림

            } catch (NoSuchFieldException e) {
                response.setStatus(ChangeConfigurationResponse.Status.NOT_SUPPORTED);
            } catch (IllegalAccessException e) {
                LogWrapper.e("OCPPConfig","parse Error:"+e.toString());
                response.setStatus(ChangeConfigurationResponse.Status.NOT_SUPPORTED);
            }
        }

        return response;
    }

    public void setConfigration(String key, String value) {
        try {
            Field field = this.getClass().getField(key);
            if ( field.getType() == Integer.TYPE ) field.setInt(this, Integer.parseInt(value));
            else if ( field.getType() == Boolean.TYPE ) field.setBoolean(this, Boolean.parseBoolean(value));
            else {
                field.set(this, value);
            }
        } catch (Exception e) {
            LogWrapper.e("setConfigration","key:"+key+", value:"+value+", Error:"+e.toString());
        }
    }

    public String getListAsString() {
        String ret = "";
        try {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields) {
                ret += field.getName() + " : " + field.get(this).toString() + "\n";
            }
        }catch (Exception e) {
            LogWrapper.e("getListAsString","e:"+e.toString());
        }
        return ret;
    }
}
