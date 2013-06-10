LOCAL_PATH := $(call my-dir)

#
# Gideros Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := gideros
LOCAL_SRC_FILES         := ../../libs/$(TARGET_ARCH_ABI)/libgideros.so

include $(PREBUILT_SHARED_LIBRARY)

#
# Plugin
#
include $(CLEAR_VARS)

LOCAL_MODULE           := heyzap
LOCAL_ARM_MODE         := arm
LOCAL_CFLAGS           := -O2
LOCAL_SRC_FILES        := heyzap.cpp heyzapbinder.cpp
LOCAL_LDLIBS           := -ldl -llog
LOCAL_SHARED_LIBRARIES := gideros

include $(BUILD_SHARED_LIBRARY)
