/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\project\\SmartCharger\\android\\SmartCharger\\posco_slow_charlcd\\src\\main\\aidl\\com\\joas\\metercertviewer\\IMeterAidlInterface.aidl
 */
package com.joas.metercertviewer;
// Declare any non-default types here with import statements

public interface IMeterAidlInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.joas.metercertviewer.IMeterAidlInterface
{
private static final java.lang.String DESCRIPTOR = "com.joas.metercertviewer.IMeterAidlInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.joas.metercertviewer.IMeterAidlInterface interface,
 * generating a proxy if needed.
 */
public static com.joas.metercertviewer.IMeterAidlInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.joas.metercertviewer.IMeterAidlInterface))) {
return ((com.joas.metercertviewer.IMeterAidlInterface)iin);
}
return new com.joas.metercertviewer.IMeterAidlInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
java.lang.String descriptor = DESCRIPTOR;
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(descriptor);
return true;
}
case TRANSACTION_readMeter:
{
data.enforceInterface(descriptor);
long _result = this.readMeter();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_readMeterVoltage:
{
data.enforceInterface(descriptor);
double _result = this.readMeterVoltage();
reply.writeNoException();
reply.writeDouble(_result);
return true;
}
case TRANSACTION_readMeterCurrent:
{
data.enforceInterface(descriptor);
double _result = this.readMeterCurrent();
reply.writeNoException();
reply.writeDouble(_result);
return true;
}
case TRANSACTION_readMeterCh:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
long _result = this.readMeterCh(_arg0);
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_readMeterVoltageCh:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
double _result = this.readMeterVoltageCh(_arg0);
reply.writeNoException();
reply.writeDouble(_result);
return true;
}
case TRANSACTION_readMeterCurrentCh:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
double _result = this.readMeterCurrentCh(_arg0);
reply.writeNoException();
reply.writeDouble(_result);
return true;
}
case TRANSACTION_setMaxChannel:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
this.setMaxChannel(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_startApp:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
int _result = this.startApp(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_startAppNewPos:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
int _arg3;
_arg3 = data.readInt();
int _arg4;
_arg4 = data.readInt();
float _arg5;
_arg5 = data.readFloat();
int _arg6;
_arg6 = data.readInt();
int _arg7;
_arg7 = data.readInt();
int _result = this.startAppNewPos(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_stopApp:
{
data.enforceInterface(descriptor);
this.stopApp();
reply.writeNoException();
return true;
}
case TRANSACTION_setCharLCDRotatePeriod:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
this.setCharLCDRotatePeriod(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setCharLCDBacklight:
{
data.enforceInterface(descriptor);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setCharLCDBacklight(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setCharLCDDisp:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _arg3;
_arg3 = data.readString();
java.lang.String _arg4;
_arg4 = data.readString();
this.setCharLCDDisp(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_readSeqNumber:
{
data.enforceInterface(descriptor);
int _result = this.readSeqNumber();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_readMeterVersion:
{
data.enforceInterface(descriptor);
java.lang.String _result = this.readMeterVersion();
reply.writeNoException();
reply.writeString(_result);
return true;
}
default:
{
return super.onTransact(code, data, reply, flags);
}
}
}
private static class Proxy implements com.joas.metercertviewer.IMeterAidlInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public long readMeter() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_readMeter, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public double readMeterVoltage() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
double _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_readMeterVoltage, _data, _reply, 0);
_reply.readException();
_result = _reply.readDouble();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public double readMeterCurrent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
double _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_readMeterCurrent, _data, _reply, 0);
_reply.readException();
_result = _reply.readDouble();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public long readMeterCh(int ch) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(ch);
mRemote.transact(Stub.TRANSACTION_readMeterCh, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public double readMeterVoltageCh(int ch) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
double _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(ch);
mRemote.transact(Stub.TRANSACTION_readMeterVoltageCh, _data, _reply, 0);
_reply.readException();
_result = _reply.readDouble();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public double readMeterCurrentCh(int ch) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
double _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(ch);
mRemote.transact(Stub.TRANSACTION_readMeterCurrentCh, _data, _reply, 0);
_reply.readException();
_result = _reply.readDouble();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setMaxChannel(int count) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(count);
mRemote.transact(Stub.TRANSACTION_setMaxChannel, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int startApp(int uiVer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uiVer);
mRemote.transact(Stub.TRANSACTION_startApp, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int startAppNewPos(int uiVer, int x, int y, int w, int h, float fontSize, int backColor, int foreColor) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uiVer);
_data.writeInt(x);
_data.writeInt(y);
_data.writeInt(w);
_data.writeInt(h);
_data.writeFloat(fontSize);
_data.writeInt(backColor);
_data.writeInt(foreColor);
mRemote.transact(Stub.TRANSACTION_startAppNewPos, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void stopApp() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopApp, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setCharLCDRotatePeriod(int periodSec) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(periodSec);
mRemote.transact(Stub.TRANSACTION_setCharLCDRotatePeriod, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setCharLCDBacklight(boolean tf) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((tf)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setCharLCDBacklight, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setCharLCDDisp(int dispCnt, java.lang.String dispStr1, java.lang.String dispStr2, java.lang.String dispStr3, java.lang.String dispStr4) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(dispCnt);
_data.writeString(dispStr1);
_data.writeString(dispStr2);
_data.writeString(dispStr3);
_data.writeString(dispStr4);
mRemote.transact(Stub.TRANSACTION_setCharLCDDisp, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int readSeqNumber() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_readSeqNumber, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String readMeterVersion() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_readMeterVersion, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_readMeter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_readMeterVoltage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_readMeterCurrent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_readMeterCh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_readMeterVoltageCh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_readMeterCurrentCh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_setMaxChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_startApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_startAppNewPos = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_stopApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_setCharLCDRotatePeriod = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_setCharLCDBacklight = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_setCharLCDDisp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_readSeqNumber = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_readMeterVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
}
public long readMeter() throws android.os.RemoteException;
public double readMeterVoltage() throws android.os.RemoteException;
public double readMeterCurrent() throws android.os.RemoteException;
public long readMeterCh(int ch) throws android.os.RemoteException;
public double readMeterVoltageCh(int ch) throws android.os.RemoteException;
public double readMeterCurrentCh(int ch) throws android.os.RemoteException;
public void setMaxChannel(int count) throws android.os.RemoteException;
public int startApp(int uiVer) throws android.os.RemoteException;
public int startAppNewPos(int uiVer, int x, int y, int w, int h, float fontSize, int backColor, int foreColor) throws android.os.RemoteException;
public void stopApp() throws android.os.RemoteException;
public void setCharLCDRotatePeriod(int periodSec) throws android.os.RemoteException;
public void setCharLCDBacklight(boolean tf) throws android.os.RemoteException;
public void setCharLCDDisp(int dispCnt, java.lang.String dispStr1, java.lang.String dispStr2, java.lang.String dispStr3, java.lang.String dispStr4) throws android.os.RemoteException;
public int readSeqNumber() throws android.os.RemoteException;
public java.lang.String readMeterVersion() throws android.os.RemoteException;
}
