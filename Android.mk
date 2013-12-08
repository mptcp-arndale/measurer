LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng
LOCAL_STATIC_JAVA_LIBRARIES := achartengine
LOCAL_SRC_FILES := $(call all-subdir-java-files) \
LOCAL_PACKAGE_NAME := measurer

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := achartengine:libs/achartengine-1.1.0.jar

include $(BUILD_MULTI_PREBUILT)

