/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 8. 17 오후 4:59
 *
 */

package com.joas.kepcocomm;

import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by user on 2018-08-17.
 */

public class KepcoCostManager {
    public static final int APLY_MODE_START = 1; // 시작시각 기준
    public static final int APLY_MODE_INTERVAL = 2; // 구간 기준

    public static final int TAX_MODE_VAT = 1; // 부가세 적용
    public static final int TAX_MODE_NOVAT = 2; // 부가세 미적용
    public static final int TAX_MODE_INCLUDE = 3; // 부가세 단가포함

    public static final int IDX_SUM_TOTAL = 0;
    public static final int IDX_SUM_MIN = 1;
    public static final int IDX_SUM_MID = 2;
    public static final int IDX_SUM_MAX = 3;
    public static final int IDX_SUM_CNT = 4;

    private KepcoCostInfo currentCostInfo;

    private double[] sumLoad = new double[IDX_SUM_CNT];
    private double[] sumCost = new double[IDX_SUM_CNT];

    private double oldMeter = 0.0;

    int aplyMode = 1;
    int taxMode = 1;
    double startTimeCost = 0.0;
    double startKwhCost = 0.0;
    double startInfraCost = 0.0;
    double startServiceCost = 0.0;

    int startTimeLoadVal = 0;

    CostTable costTable = new CostTable();
    String costFilePath = "/Cost";
    String costFileName = "CostTable.txt";

    double sum_chargecost = 0;
    double sum_chargecostNoVAT = 0;

    double sumKwhCost = 0;
    double sumInfraCost = 0;
    double sumServiceCost = 0;

    ChargingPeriodInfo periodInfo = new ChargingPeriodInfo();
    Object periodInfoLock = new Object();

    class ChargingPeriodInfo {
        public double periodSumCost = 0;
        public double periodSumKwh = 0;
        public double periodUCost = 0;
        public int periodLoadCL = 0;

        public ChargingPeriodInfo() {
            initVal();
        }

        public void initVal() {
            periodSumCost = 0;
            periodSumKwh = 0;
            periodUCost = 0;
            periodLoadCL = 0;
        }
    }

    public KepcoCostManager(String filePath) {
        costFilePath = filePath;

        costFileRead();
        initCostSumValues();
    }

    public void costFileRead() {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( costFilePath + "/" + costFileName);
        } catch (Exception ex) {
            loadString = null;
        }

        if ( loadString != null ) {
            try {
                JSONObject obj = new JSONObject(loadString);
                costTable.parseJsonObject(obj);
            } catch (Exception ex) {
            }
        }

        updateCurrentCostTable();
    }

    // 합계 게산 값들 초기화
    public void initCostSumValues() {
        for (int i = 0; i < IDX_SUM_CNT; i++) {
            sumLoad[i] = 0.0;
            sumCost[i] = 0.0;
        }

        oldMeter = 0.0;
        startTimeCost = 0.0;
        startTimeLoadVal = 1;
        startKwhCost = 0.0;
        startInfraCost = 0.0;
        startServiceCost = 0.0;

        sum_chargecost = 0;
        sum_chargecostNoVAT = 0;
        sumKwhCost = 0;
        sumInfraCost = 0;
        sumServiceCost = 0;

        periodInfo.initVal();
    }

    // 단가정보중 마지막 버전을 가져온다.
    public String getLastVersion() {
        String ver = "";
        for (int i = 0; i < CostTable.KEPCO_COST_MAX_CNT; i++) {
            if (costTable.list[i].version.length() > 0) {
                ver = costTable.list[i].version;
            }
        }
        return ver;
    }

    public void updateCurrentCostTable() {
        int findIdx = 0; // 0번째 테이블 디폴트

        try {
            //오늘 날짜            
            String nowDate = TimeUtil.getCurrentTimeAsString("yyyyMMdd");

            for (int i = 0; i < CostTable.KEPCO_COST_MAX_CNT; i++) {
                if (costTable.list[i].version.length() > 0) {
                    // 단가의 날짜가 오늘보다 작거나 같을때(이전일때) 선택
                    if (costTable.list[i].version.compareTo(nowDate) <= 0) findIdx = i;
                }
            }

            currentCostInfo = costTable.list[findIdx];
        }
        catch (Exception e){
        }
    }

    public KepcoCostInfo getCostInfoDate(String date) {
        KepcoCostInfo cInfo = null;

        int findIdx = 0; // 0번째 테이블 디폴트

        try {
            for (int i = 0; i < CostTable.KEPCO_COST_MAX_CNT; i++) {
                if (costTable.list[i].version.length() > 0) {
                    // 단가의 날짜가 오늘보다 작거나 같을때(이전일때) 선택
                    if (costTable.list[i].version.compareTo(date) <= 0) findIdx = i;
                }
            }

            cInfo = costTable.list[findIdx];
        }
        catch (Exception e){

        }
        return cInfo;
    }

    public KepcoCostInfo getCurrentCostInfo() {
        return currentCostInfo;
    }

    // 단가를 저장한다.
    // 단가는 현재 최대 3개까지 저장이 가능하며, 마지막 단가 날짜 이후로 새로운 단가정보가 수신된다.(이때 마지막 날짜랑 동일한경우 해당 날짜 단가 변경)
    // 배열에 3개의 단가를 하나씩 저장하며 만약 모든 단가가 다 차있는경우에는 첫번째 배열의 값을 지우고
    // 그다음 값을 첫번째로 당긴다. 즉 1,2,3 을 2,3,3 으로 만든 다음 마지막 배열값을 업데이트한다.
    public void saveKepcoCostTable(String version, double[] kwhbill_ucost, double[] infrabill_ucost, double[] svc_bill_ucost, int[] load_cl) {
        // 1, 비어있는 슬롯이 있거나 날짜가 같은 슬롯에 채움
        int findIdx = -1;
        for (int i = 0; i < CostTable.KEPCO_COST_MAX_CNT; i++)
        {
            if (costTable.list[i].version.length() == 0 || costTable.list[i].version.equals(version))
            {
                findIdx = i;
                break;
            }
        }

        // 2. 만약 비어있는 슬롯이 없으면 마지막을 선택, 앞에 하나씩 당김.
        if (findIdx == -1)
        {
            for (int i = 0; i < CostTable.KEPCO_COST_MAX_CNT - 1; i++)
            {
                costTable.list[i] = costTable.list[i + 1];
            }
            findIdx = CostTable.KEPCO_COST_MAX_CNT - 1;

            // 비어있는 슬롯 새로 생성
            costTable.list[findIdx] = new KepcoCostInfo();
        }

        //3. 값을 채운다.
        costTable.list[findIdx].version = version;
        System.arraycopy(kwhbill_ucost, 0, costTable.list[findIdx].KWHBILL_UCOST, 0, KepcoCostInfo.KEPCO_COST_TIME_INFO_CNT);
        System.arraycopy(infrabill_ucost, 0, costTable.list[findIdx].INFRABILL_UCOST, 0, KepcoCostInfo.KEPCO_COST_TIME_INFO_CNT);
        System.arraycopy(svc_bill_ucost, 0, costTable.list[findIdx].SVC_BILL_UCOST, 0, KepcoCostInfo.KEPCO_COST_TIME_INFO_CNT);
        System.arraycopy(load_cl, 0, costTable.list[findIdx].LOAD_CL, 0, KepcoCostInfo.KEPCO_COST_TIME_INFO_CNT);

        // 4. 저장
        FileUtil.stringToFile(costFilePath, costFileName, costTable.getJsonObject().toString(), false);
    }

    // 충전 시작시 불려짐
    public void startCostCalc(double meter_val, int ucost_aply_tp, int tax_tp, int time_cost)
    {
        initCostSumValues();
        updateCurrentCostTable();

        oldMeter = meter_val;

        aplyMode = ucost_aply_tp;
        taxMode = tax_tp;

        //시작 시간 기준 Cost 계산, 및 모드 저장
        if (aplyMode == APLY_MODE_START)
        {
            startTimeCost = getCurTimeCostVal(time_cost);
            startTimeLoadVal = currentCostInfo.LOAD_CL[time_cost];
            if (startTimeLoadVal >= IDX_SUM_CNT) startTimeLoadVal = 1;
            //SharedData.ipcm[0].starttime_ver = currentCostInfo.version;
            //SharedData.ipcm[0].starttime_time_cost = time_cost;

            startKwhCost = currentCostInfo.KWHBILL_UCOST[time_cost];
            startInfraCost = currentCostInfo.INFRABILL_UCOST[time_cost];
            startServiceCost = currentCostInfo.SVC_BILL_UCOST[time_cost];

        }
        sum_chargecost = 0;
        sum_chargecostNoVAT = 0;
        sumKwhCost = 0;
        sumInfraCost = 0;
        sumServiceCost = 0;
    }

    public double getCurTimeCostVal(int time_cost)
    {
        return currentCostInfo.INFRABILL_UCOST[time_cost] + currentCostInfo.KWHBILL_UCOST[time_cost] + currentCostInfo.SVC_BILL_UCOST[time_cost];
    }

    public void processCostCalc(double meter_val, int time_cost)
    {
        // 차이 계산
        double gapMeter = meter_val - oldMeter;

        if (gapMeter < 0) gapMeter = 0;
        else if (gapMeter > 1.0) gapMeter = 0.01; // 초당 1kW 이상인 경우에 0.01만 증가하도록 수정// 계량기 갑 튀는 문제 대처

        oldMeter = meter_val;

        double curCost = 0.0;
        int loadVal = 1;

        double curKwhCost = 0.0;
        double curInfraCost = 0.0;
        double curSvcCost = 0.0;

        // 시작시간 기준인경우 시작시간 단가를 적용한다.
        if (aplyMode == APLY_MODE_START)
        {
            curCost = startTimeCost;
            curKwhCost = startKwhCost;
            curInfraCost = startInfraCost;
            curSvcCost = startServiceCost;

        }
        else // 구간단가 계산
        {
            curCost = getCurTimeCostVal(time_cost);
            curKwhCost = currentCostInfo.KWHBILL_UCOST[time_cost];
            curInfraCost = currentCostInfo.INFRABILL_UCOST[time_cost];
            curSvcCost = currentCostInfo.SVC_BILL_UCOST[time_cost];

        }
        loadVal = currentCostInfo.LOAD_CL[time_cost];

        // 최종 값 누적, 구간별 누적
        sumCost[IDX_SUM_TOTAL] += gapMeter * curCost;
        sumCost[loadVal] += gapMeter * curCost;

        sumKwhCost += gapMeter * curKwhCost;
        sumInfraCost += gapMeter * curInfraCost;
        sumServiceCost += gapMeter * curSvcCost;

        // 총 충전량 계산, 구간별 누적
        sumLoad[IDX_SUM_TOTAL] += gapMeter;
        sumLoad[loadVal] += gapMeter;

        // UI 표시 데이터(VAT 미포함)
        if (taxMode == TAX_MODE_INCLUDE)
        {
            sum_chargecostNoVAT = sumCost[IDX_SUM_TOTAL] / 1.1;
            sum_chargecost = sumCost[IDX_SUM_TOTAL];
        }
        else
        {
            sum_chargecostNoVAT = sumCost[IDX_SUM_TOTAL];

            if (taxMode == TAX_MODE_VAT) sum_chargecost = sumCost[IDX_SUM_TOTAL] * 1.1;
            else sum_chargecost = sum_chargecostNoVAT;
        }

        synchronized (periodInfoLock) {
            periodInfo.periodSumCost += gapMeter * curCost;
            periodInfo.periodSumKwh += gapMeter;
        }
    }

    public ChargingPeriodInfo getAndResetPeriodData(int time_cost) {
        ChargingPeriodInfo info = periodInfo;
        synchronized (periodInfoLock) {
            periodInfo = new ChargingPeriodInfo();
            if (aplyMode == APLY_MODE_START) {
                periodInfo.periodUCost = startTimeCost;
            }
            else {
                periodInfo.periodUCost = getCurTimeCostVal(time_cost);
            }
            periodInfo.periodLoadCL = currentCostInfo.LOAD_CL[time_cost];
        }
        return info;
    }

    public double getTaxCost() { return (sum_chargecostNoVAT * 0.1);}

    public double getSumChargeCostVAT() {
        return sum_chargecost;
    }

    public double getSum_chargecostNoVAT() {
        return sum_chargecostNoVAT;
    }

    public double getSumChargeCost(int idx) {
        return sumCost[idx];
    }
    public double getSumChargeKwh(int idx) {
        return sumLoad[idx];
    }

    public double getSumKwhCost() { return sumKwhCost; }
    public double getSumInfraCost() { return sumInfraCost; }
    public double getSumServiceCost() { return sumServiceCost; }

    public class CostTable
    {
        public static final int KEPCO_COST_MAX_CNT = 3;

        public KepcoCostInfo[] list = new KepcoCostInfo[KEPCO_COST_MAX_CNT];

        public CostTable() {
            for (int i = 0; i < KEPCO_COST_MAX_CNT; i++)
                list[i] = new KepcoCostInfo();
        }

        public JSONObject getJsonObject() {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (int i = 0; i < KEPCO_COST_MAX_CNT; i++) {
                array.put(list[i].getJsonObject());
            }
            try {
                json.put("cost_table", array);
            }
            catch (Exception e) {

            }

            return json;
        }

        public void parseJsonObject(JSONObject json) {
            try {
                JSONArray array = json.getJSONArray("cost_table");

                for (int i = 0; i < KEPCO_COST_MAX_CNT; i++) {
                    list[i].parseJsonObject(array.getJSONObject(i));
                }
            }catch (Exception e) {

            }
        }
    }
}
