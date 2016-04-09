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
    private boolean mSendEvents = true;
    private boolean mMulticast = false;
    private String mName;
    private String mDataType;
    private final List<String> mAllowedValueList;
    private String mDefaultValue = null;
    private String mMinimum;
    private String mMaximun;
    private String mStep;

    public StateVariable(Service service) {
        mService = service;
        mAllowedValueList = new ArrayList<>();
    }

    public boolean isSendEvents() {
        return mSendEvents;
    }

    public void setSendEvents(String sendEvents) {
        mSendEvents = !"no".equalsIgnoreCase(sendEvents);
    }

    public boolean isMulticast() {
        return mMulticast;
    }

    public void setMulticast(String multicast) {
        mMulticast = "yes".equalsIgnoreCase(multicast);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDataType() {
        return mDataType;
    }

    public void setDataType(String dataType) {
        mDataType = dataType;
    }

    public List<String> getAllowedValueList() {
        return Collections.unmodifiableList(mAllowedValueList);
    }

    public void addAllowedValue(String value) {
        mAllowedValueList.add(value);
    }

    public String getDefaultValue() {
        return mDefaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        mDefaultValue = defaultValue;
    }

    public String getMinimum() {
        return mMinimum;
    }

    public void setMinimum(String minimum) {
        mMinimum = minimum;
    }

    public String getMaximun() {
        return mMaximun;
    }

    public void setMaximun(String maximun) {
        mMaximun = maximun;
    }

    public String getStep() {
        return mStep;
    }

    public void setStep(String step) {
        mStep = step;
    }

    public Service getService() {
        return mService;
    }
}
