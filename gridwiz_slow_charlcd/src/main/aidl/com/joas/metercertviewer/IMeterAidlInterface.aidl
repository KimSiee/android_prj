// IMeterAidlInterface.aidl
package com.joas.metercertviewer;

// Declare any non-default types here with import statements

interface IMeterAidlInterface {
    long readMeter();
    double readMeterVoltage();
    double readMeterCurrent();
    void startApp(int uiVer);
    void startAppNewPos(int uiVer, int x, int y, int w, int h, float fontSize, int backColor, int foreColor);
    void stopApp();

    void setCharLCDRotatePeriod(int periodSec);
    void setCharLCDBacklight(boolean tf);
    void setCharLCDDisp(int dispCnt, String dispStr1, String dispStr2, String dispStr3, String dispStr4);
}
