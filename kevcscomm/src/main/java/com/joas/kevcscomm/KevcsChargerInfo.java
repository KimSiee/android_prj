/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 8. 13 오후 2:09
 *
 */

package com.joas.kevcscomm;


/**
 * 통신에서 사용되는 충전기 정보에 대한 정의
 */
public class KevcsChargerInfo {
    public KevcsProtocol.CPStat cp_stat = KevcsProtocol.CPStat.NONE;
    public KevcsProtocol.OutletType outlet_type = KevcsProtocol.OutletType.NONE;

    public int outlet_id = 1;
    public String card_date = "";
    public String mng_pass = "1234";

    // 0x72 GLOB_CP_CONFIG
    public int charge_tp = 1; // 1 완속, 2:급속, 4:중속
    public int cp_tp = 3; // 충전기 유형, 3:bc타입, 7:콤보
    public String manufr = "10"; // 제조사 코드(중앙제어)
    public String model_nm = "JC6111KE"; // 모델명
    public String serial_no = "";
    public int trnas_tp = 2; // 1:statelss, 2:statefull
    public int send_port = 8482;
    public int recv_port = 0;
    public int net_type = 1; // 1:유선, 2:문선
    public int charge_ability = 7; // 충전기 용량
    public String cp_firmware_ver = "V0"; // 펌웨어 버전
    public String cp_ip = "127.0.0.1";
    public String paym_manufr = "01"; //결제단말기 제조사코드(세한 01)
    public String board_ver = "0"; //제어보드 버전

    //0x31 인증
    public int cert_tp = 1;
    public String cert_pass = "";

    //0x34
    public String member_card_no = "";
    public int charge_req_cfm_mthd = KevcsProtocol.CHARGE_REQ_CFM_METHOD_FULL; // 1:full, 2:전력량, 3:금액
    public double charge_req_kwh = 0;
    public int charge_req_amt = 0;
    public int pay_mthd = 3;
    public String charge_st_datetime = "";
    public double current_kwh = 0;
    public double current_amt = 0;
    public double current_ucost = 0;
    public String charge_accum_time = "";
    public int car_soc = 0;
    public String ucost_ymd = "";
    public int load_cl = 0;

    //0x35
    public int charge_end_stat = KevcsProtocol.CHARGE_END_FULL; // 충전완료상태 (1:full, 2:사용자 종료, 3:고장, 오류)

    //0x62 결제
    public boolean isPaymentCharging = false;
    public String paym_aprv_type = "";
    public String paym_charge_kwh = "0.00";
    public String paym_charge_amt = "0";

    public KevcsProtocol.LocalMode localMode = KevcsProtocol.LocalMode.CARD_TAG_AUTO;
    public KevcsChargerRxInfo rxInfo = new KevcsChargerRxInfo();

    public KevcsChargeCtl chargeCtl = new KevcsChargeCtl();
    public KevcsPayRetInfo payRetInfo = new KevcsPayRetInfo();
    public KevcsPaymRxInfo payRxInfo = new KevcsPaymRxInfo();

    public class KevcsChargerRxInfo {
        public String ucost_ymd = "";
        public int ucost_aply_tp = 1;
        public int tax_tp = 3;
        public String card_date = "";
        public String firmVersion = "";

        public String paym_model_no ="";
        public String paym_fw_ver = "";
        public String paym_ant_stat = "";
        public String pam_ip = "";
        public String pam_gw = "";
        public String van_ip = "";
        public String van_port = "";
        public String m2m_fw_ver = "";
        public String paym_tid = "";
        public String svr_ip = "";
        public String svr_port = "";
        public String paym_setup_yn = "";

    }

    public class KevcsPaymRxInfo {
        public String paym_model_no ="";
        public String paym_fw_ver = "";
        public String paym_ant_stat = "";
        public String pam_ip = "";
        public String pam_gw = "";
        public String van_ip = "";
        public String van_port = "";
        public String m2m_fw_ver = "";
        public String paym_stat = "";
        public String paym_update_stat = "";
        public String paym_tid = "";
        public String svr_ip = "";
        public String svr_port = "";
        public String base_paym_amt = "5000";

        //무결성 검사
        public String[] paym_inty_chk_ret = new String[5];
        public String[] paym_inty_chk_date = new String[5];
        public String[] paym_inty_chk_type = new String[5];
        public String paym_reg_model = "";
    }

    public class KevcsPayRetInfo {
        public String aprv_type = "";
        public String charge_kwh = "";
        public String charge_amt = "";
        public String pay_card_no = "";
        public String card_name = "";
        public String card_aprv_no = "";
        public String aprv_amt = "";
        public String pay_stat = "";
        public String pay_datetime = "";
        public String cncl_datetime = "";
        public String pay_ret_code = "";
        public String pay_ret_msg1 = "";
        public String pay_ret_msg2 = "";
        public String pay_ret_text = "";
        public String trade_id = "";
    }

    public void initKevcsPayRet() {
        payRetInfo = new KevcsPayRetInfo();
    }
}
