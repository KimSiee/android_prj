/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 19 오전 11:00
 *
 */

package com.joas.kevcscomm;

public class KevcsProtocol {
    public static final byte[] HEADER_MAGIC = {(byte) 0x00, (byte) 0x00, (byte) 0xDB, (byte) 0x09};

    public static final int HEADER_SIZE = 28;
    public static final int HEADER_LENGTH_FIELD = 24;
    public static final int HEADER_DSTIP_FIELD = 8;
    public static final int HEADER_SRCIP_FIELD = 12;
    public static final int HEADER_MSGID_FIELD = 16;

    public static final int DATA_CMD_FIELD = HEADER_SIZE + 0;
    public static final int DATA_RET_FIELD = HEADER_SIZE + 1;
    public static final int DATA_CPID_FIELD = HEADER_SIZE + 2;
    public static final int DATA_VDATA_FIELD = HEADER_SIZE + 6;

    public static final int PING_MSG_PERIOD = 300; // 5 * 60 (5분)
    public static final int PAYM_INTY_PERIOD = 3600; // 1시간

    public static final int CERT_TP_RFCARD = 1;
    public static final int CERT_TP_MEMBER_NUM = 2;

    public static final int CHARGE_END_FULL = 1;
    public static final int CHARGE_END_USER = 2;
    public static final int CHARGE_END_FAULT = 3;

    public static final int CHARGER_TYPE_SLOW = 1;
    public static final int CHARGER_TYPE_FAST = 2;

    public static final int APRV_TYPE_PREPAY = 1;
    public static final int APRV_TYPE_REALPAY = 2;
    public static final int APRV_TYPE_CANCEL_PREPAY = 9;

    public static final int CHARGE_REQ_CFM_METHOD_FULL = 1;
    public static final int CHARGE_REQ_CFM_METHOD_KWH = 2;
    public static final int CHARGE_REQ_CFM_METHOD_COST = 3;

    public static final String PAY_STAT_SUCCESS = "01";
    public static final String PAY_STAT_ERROR = "02";
    public static final String PAY_STAT_CANCEL = "99";

    public static final int PAY_METHOD_CREDIT = 1;
    public static final int PAY_METHOD_MEMBER = 3;


    public enum KevcsCmd {
        GLOB_STAT_PING_10(0x10), // 충전기 상태 확인 Event/5분 5분주기
        GLOB_INIT_START_11(0x11), // 충전기 시작 Event
        GLOB_INIT_END_12(0x12), //충전기 종료 Event
        PAYM_STAT_INFO_13(0x13),// 결제단말기 상태정보 Event 추가
        GLOB_TIME_SYNC_14(0x14), //충전기 시각 동기화 Event
        GLOB_CHARGE_CTL_15(0x15), //충전기 제어(예약) Event
        GLOB_FAULT_EVENT_16(0x16),// 충전기 고장(오류)정보 Event 추가
        INFO_CHARGEUCOST_REQ_22(0x22),// 충전단가 요청 Event
        EV_MEMB_CERT_31(0x31),//회원정보 확인 Event
        GLOB_QRCODE_CERT_33(0x33),//QR결제 인증 요청(원격인증)
        EV_CHARGE_STAT_34(0x34),//충전진행 상태 Event/5분 정각5분
        EV_CHARGE_END_35(0x35),//충전진행 종료 Event
        EV_MEMB_CARD_36(0x36),//결제단말기-회원카드 번호 Event 추가
        GLOB_CARD_INFO_37(0x37),//회원카드 정보 요청 Event 추가
        GLOB_CARD_APRV_38(0x38),//충전요금 카드승인정보 Event 추가
        EV_CHARGE_STOP_39(0x39),    //QR인증을 통해 충전중인 사용자 원격 충전 종료 Event 추가 - add by si.
        GLOB_PAYM_STAT_61(0x61),//결제단말기 상태확인 Event 변경
        GLOB_CHARGE_AMT_62(0x62),//충전요금 결제정보 Event 추가
        GLOB_PAYM_COMM_64(0x64),//결제단말기 통신정보확인 Event 추가
        GLOB_PAYM_ANT_65(0x65),//결제단말기 안테나 상태 Event 추가
        GLOB_PAYM_INTY_66(0x66),//결제단말기 무결성 확인 Event 추가
        GLOB_PAYM_STAT_SEND_67(0x67), //결제단말기 상태정보 전송 Event 추가
        GLOB_PAYM_CTRL_68(0x68),
        GLOB_FIRM_UPDATE_71(0x71),//충전기 펌웨어 업데이트 Event
        GLOB_CP_CONFIG_72(0x72),//충전기 설정 정보 전송 Event 추가
        //GLOB_QRIMG_DOWN_73(0x73), //충전기 QR이미지 다운로드
        KEVCS_CMD_END(0xFF);

        int id;

        private KevcsCmd(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static KevcsCmd getValue(int _id) {
            KevcsCmd[] As = KevcsCmd.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return KevcsCmd.KEVCS_CMD_END;
        }

        public static KevcsCmd getValue(String name) {
            KevcsCmd[] As = KevcsCmd.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].name().equals(name))
                    return As[i];
            }
            return KevcsCmd.KEVCS_CMD_END;
        }

        public static String[] names() {
            String[] names = new String[values().length];
            int index = 0;

            for (KevcsCmd cmd : values()) {
                names[index++] = cmd.name();
            }

            return names;
        }
    }

    public enum CPStat {
        NONE(0),
        READY(1),
        START(2),
        CONNECT(3),
        OPTION_SELECT(4),
        CARD_PRE_PAY(5),
        CANCEL_CHARGE(6),
        CHARGING(7),
        FINISH_CHARGE(8),
        FINISH_CONNECTING(9),
        CARD_PAY(10),
        SEPERATE_CONNECTOR(11),
        V2G_ING(12),
        V2G_FINISH(13),
        V2G_STOP_TEMP(14),
        V2G_SEPERATE_CONNECTOR(15),
        TESTING(16),
        FIXING(17),
        LIMIT_LOAD(18),
        FAULT(19),
        DELAY_20(20),
        DELAY_21(21),
        DELAY_22(22),
        PAY_ERROR(23),
        ERROR_24(24),
        ERROR_25(25),
        CPSTAT_END(26);

        int id;

        private CPStat(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static CPStat getValue(int _id) {
            CPStat[] As = CPStat.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return CPStat.CPSTAT_END;
        }
    }

    public enum KevcsRet {
        PKT_ACK(0x41), // 전문확인(단순ACK)
        PKT_BEGIN(0x42),  // 작업시작
        PKT_COMPT(0x43), //작업완료
        PKT_DATA(0x44), //데이터
        PKT_ERROR(0x45), // 에러 (데이터적인 오류)
        PKT_FAIL(0x46), // 실패 (시스템적인 오류)
        PKT_MSG(0x4D), // 메시지
        PKT_NEXT(0x4E), // 연속된 다음데이터 요청
        PKT_PUSH(0x50), // 일방적 데이터 전달
        PKT_REQUEST(0x52), // 데이터요청
        PKT_NOT(0x58), //  존재하지 않음
        PKT_YES(0x59), //  성공 (OK)
        KEVCS_CMDACK_END(0xFF);

        int id;

        private KevcsRet(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static KevcsRet getValue(int _id) {
            KevcsRet[] As = KevcsRet.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return KevcsRet.KEVCS_CMDACK_END;
        }
    }

    public enum OutletType {
        NONE(0),
        AC_SLOW_5PIN(1),
        AC_SLOW_7PIN(2),
        DC_CHADEMO(3),
        AC3(4),
        DC_COMBO(5),
        CPSTAT_END(6);

        int id;

        private OutletType(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static OutletType getValue(int _id) {
            OutletType[] As = OutletType.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return OutletType.CPSTAT_END;
        }
    }

    public enum LocalMode {
        NONE(0),
        CARDNUM_SEARCH(1),
        CARD_TAG_AUTO(2);

        int id;

        private LocalMode(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public boolean Compare(int i) {
            return id == i;
        }

        public static LocalMode getValue(int _id) {
            LocalMode[] As = LocalMode.values();
            for (int i = 0; i < As.length; i++) {
                if (As[i].Compare(_id))
                    return As[i];
            }
            return LocalMode.CARD_TAG_AUTO;
        }
    }

    static public boolean isCmdForPayTerminal(int cmd) {
        boolean ret = false;

        switch ( KevcsCmd.getValue(cmd) ) {
            case EV_MEMB_CARD_36:
            case GLOB_PAYM_STAT_61:
            case GLOB_CHARGE_AMT_62:
            case GLOB_PAYM_COMM_64:
            case GLOB_PAYM_ANT_65:
            case GLOB_PAYM_INTY_66:
            case GLOB_PAYM_STAT_SEND_67:
            case GLOB_PAYM_CTRL_68:
                ret = true;
                break;
            default: ret = false;
                break;
        }
        return ret;
    }
}