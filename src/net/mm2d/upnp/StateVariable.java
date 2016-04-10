/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StateVariable {
    private final Service mService;
    private final boolean mSendEvents;
    private final boolean mMulticast;
    private final String mName;
    private final String mDataType;
    private final List<String> mAllowedValueList;
    private final String mDefaultValue;
    private final String mMinimum;
    private final String mMaximum;
    private final String mStep;

    public static class Builder {
        private Service mService;
        private boolean mSendEvents = true;
        private boolean mMulticast = false;
        private String mName;
        private String mDataType;
        private final List<String> mAllowedValueList;
        private String mDefaultValue = null;
        private String mMinimum;
        private String mMaximum;
        private String mStep;

        public Builder() {
            mAllowedValueList = new ArrayList<>();
        }

        public void setService(Service service) {
            mService = service;
        }

        public void setSendEvents(String sendEvents) {
            mSendEvents = !"no".equalsIgnoreCase(sendEvents);
        }

        public void setMulticast(String multicast) {
            mMulticast = "yes".equalsIgnoreCase(multicast);
        }

        public void setName(String name) {
            mName = name;
        }

        public void setDataType(String dataType) {
            mDataType = dataType;
        }

        public void addAllowedValue(String value) {
            mAllowedValueList.add(value);
        }

        public void setDefaultValue(String defaultValue) {
            mDefaultValue = defaultValue;
        }

        public void setMinimum(String minimum) {
            mMinimum = minimum;
        }

        public void setMaximun(String maximun) {
            mMaximum = maximun;
        }

        public void setStep(String step) {
            mStep = step;
        }

        public StateVariable build() {
            return new StateVariable(this);
        }
    }

    public StateVariable(Builder builder) {
        mService = builder.mService;
        mSendEvents = builder.mSendEvents;
        mMulticast = builder.mMulticast;
        mName = builder.mName;
        mDataType = builder.mDataType;
        mAllowedValueList = builder.mAllowedValueList;
        mDefaultValue = builder.mDefaultValue;
        mMinimum = builder.mMinimum;
        mMaximum = builder.mMaximum;
        mStep = builder.mStep;
    }

    public Service getService() {
        return mService;
    }

    public boolean isSendEvents() {
        return mSendEvents;
    }

    public boolean isMulticast() {
        return mMulticast;
    }

    public String getName() {
        return mName;
    }

    public String getDataType() {
        return mDataType;
    }

    public List<String> getAllowedValueList() {
        return Collections.unmodifiableList(mAllowedValueList);
    }

    public String getDefaultValue() {
        return mDefaultValue;
    }

    public String getMinimum() {
        return mMinimum;
    }

    public String getMaximum() {
        return mMaximum;
    }

    public String getStep() {
        return mStep;
    }
}
