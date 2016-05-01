/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * StateVariableを表現するクラス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StateVariable {
    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see Device#loadDescription()
     * @see Service#loadDescription(HttpClient)
     */
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

        /**
         * インスタンス作成
         */
        public Builder() {
            mAllowedValueList = new ArrayList<>();
        }

        /**
         * このStateVariableを保持するServiceを登録する。
         *
         * @param service このStateVariableを保持するService
         */
        public void setService(Service service) {
            mService = service;
        }

        /**
         * SendEventsの値を登録する。
         *
         * 値が"no"でなければyesであると判定する。
         *
         * @param sendEvents SendEventsの値
         */
        public void setSendEvents(String sendEvents) {
            mSendEvents = !"no".equalsIgnoreCase(sendEvents);
        }

        /**
         * Multicastの値を登録する
         *
         * 値が"yes"でなければnoであると判定する。
         * なお、Multicastの受信には非対応である。
         *
         * @param multicast Multicastの値
         */
        public void setMulticast(String multicast) {
            mMulticast = "yes".equalsIgnoreCase(multicast);
        }

        /**
         * StateVariable名を登録する。
         *
         * @param name StateVariable名
         */
        public void setName(String name) {
            mName = name;
        }

        /**
         * DataTypeを登録する。
         *
         * @param dataType DataType
         */
        public void setDataType(String dataType) {
            mDataType = dataType;
        }

        /**
         * AllowedValueを登録する。
         *
         * @param value AllowedValue
         */
        public void addAllowedValue(String value) {
            mAllowedValueList.add(value);
        }

        /**
         * DefaultValueを登録する。
         *
         * @param defaultValue DefaultValue
         */
        public void setDefaultValue(String defaultValue) {
            mDefaultValue = defaultValue;
        }

        /**
         * Minimumを登録する。
         *
         * @param minimum Minimum
         */
        public void setMinimum(String minimum) {
            mMinimum = minimum;
        }

        /**
         * Maximumを登録する。
         *
         * @param maximun Maximum
         */
        public void setMaximun(String maximun) {
            mMaximum = maximun;
        }

        /**
         * Stepを登録する。
         *
         * @param step Step
         */
        public void setStep(String step) {
            mStep = step;
        }

        /**
         * StateVariableのインスタンスを作成する。
         *
         * @return StateVariableのインスタンス
         */
        public StateVariable build() {
            return new StateVariable(this);
        }
    }

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

    /**
     * このStateVariableを保持するServiceを返す
     *
     * @return このStateVariableを保持するService
     */
    public Service getService() {
        return mService;
    }

    /**
     * SendEventsの状態を返す。
     *
     * @return SendEventsがyesの場合true
     */
    public boolean isSendEvents() {
        return mSendEvents;
    }

    /**
     * Multicastの状態を返す。
     *
     * @return Multicastがyesの場合true
     */
    public boolean isMulticast() {
        return mMulticast;
    }

    /**
     * StateVariable名を返す。
     *
     * @return StateVariable名
     */
    public String getName() {
        return mName;
    }

    /**
     * DataTypeを返す。
     *
     * @return DataType
     */
    public String getDataType() {
        return mDataType;
    }

    /**
     * AllowedValueListを返す。
     *
     * リストは変更不可。
     *
     * @return AllowedValueList
     */
    public List<String> getAllowedValueList() {
        return Collections.unmodifiableList(mAllowedValueList);
    }

    /**
     * DefaultValueの値を返す。
     *
     * @return DefaultValue
     */
    public String getDefaultValue() {
        return mDefaultValue;
    }

    /**
     * Minimumの値を返す。
     *
     * @return Minimum
     */
    public String getMinimum() {
        return mMinimum;
    }

    /**
     * Maximumの値を返す。
     *
     * @return Maximum
     */
    public String getMaximum() {
        return mMaximum;
    }

    /**
     * Stepの値を返す。
     *
     * @return Stepの値
     */
    public String getStep() {
        return mStep;
    }
}
