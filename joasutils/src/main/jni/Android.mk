TOP_LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

include $(TOP_LOCAL_PATH)/serialport/Android.mk
include $(TOP_LOCAL_PATH)/watchdog/Android.mk
