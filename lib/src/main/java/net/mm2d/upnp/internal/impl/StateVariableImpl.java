/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.StateVariable;
import net.mm2d.upnp.internal.parser.DeviceParser;
import net.mm2d.upnp.internal.parser.ServiceParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * StateVariableの実装
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StateVariableImpl implements StateVariable {
    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser#loadDescription(HttpClient, DeviceImpl.Builder)
     * @see ServiceParser#loadDescription(HttpClient, DeviceImpl.Builder, ServiceImpl.Builder)
     */
    public static class Builder {
        private boolean mSendEvents = true;
        private boolean mMulticast = false;
        private String mName;
        private String mDataType;
        private final List<String> mAllowedValueList = new ArrayList<>();
        private String mDefaultValue = null;
        private String mMinimum;
        private String mMaximum;
        private String mStep;

        /**
         * インスタンス作成
         */
        public Builder() {
        }

        /**
         * SendEventsの値を登録する。
         *
         * <p>値が"no"でなければyesであると判定する。
         *
         * @param sendEvents SendEventsの値
         * @return Builder
         */
        @Nonnull
        public Builder setSendEvents(@Nonnull final String sendEvents) {
            mSendEvents = !"no".equalsIgnoreCase(sendEvents);
            return this;
        }

        /**
         * Multicastの値を登録する
         *
         * <p>値が"yes"でなければnoであると判定する。
         * なお、Multicastの受信には非対応である。
         *
         * @param multicast Multicastの値
         * @return Builder
         */
        @Nonnull
        public Builder setMulticast(@Nonnull final String multicast) {
            mMulticast = "yes".equalsIgnoreCase(multicast);
            return this;
        }

        /**
         * StateVariable名を登録する。
         *
         * @param name StateVariable名
         * @return Builder
         */
        @Nonnull
        public Builder setName(@Nonnull final String name) {
            mName = name;
            return this;
        }

        /**
         * DataTypeを登録する。
         *
         * @param dataType DataType
         * @return Builder
         */
        @Nonnull
        public Builder setDataType(@Nonnull final String dataType) {
            mDataType = dataType;
            return this;
        }

        /**
         * AllowedValueを登録する。
         *
         * @param value AllowedValue
         * @return Builder
         */
        @Nonnull
        public Builder addAllowedValue(@Nonnull final String value) {
            mAllowedValueList.add(value);
            return this;
        }

        /**
         * DefaultValueを登録する。
         *
         * @param defaultValue DefaultValue
         * @return Builder
         */
        @Nonnull
        public Builder setDefaultValue(@Nonnull final String defaultValue) {
            mDefaultValue = defaultValue;
            return this;
        }

        /**
         * Minimumを登録する。
         *
         * @param minimum Minimum
         * @return Builder
         */
        @Nonnull
        public Builder setMinimum(@Nonnull final String minimum) {
            mMinimum = minimum;
            return this;
        }

        /**
         * Maximumを登録する。
         *
         * @param maximum Maximum
         * @return Builder
         */
        @Nonnull
        public Builder setMaximum(@Nonnull final String maximum) {
            mMaximum = maximum;
            return this;
        }

        /**
         * Stepを登録する。
         *
         * @param step Step
         * @return Builder
         */
        @Nonnull
        public Builder setStep(@Nonnull final String step) {
            mStep = step;
            return this;
        }

        /**
         * StateVariableのインスタンスを作成する。
         *
         * @return StateVariableのインスタンス
         */
        @Nonnull
        public StateVariable build() {
            if (mName == null) {
                throw new IllegalStateException("name must be set.");
            }
            if (mDataType == null) {
                throw new IllegalStateException("dataType must be set.");
            }
            return new StateVariableImpl(this);
        }
    }

    private final boolean mSendEvents;
    private final boolean mMulticast;
    @Nonnull
    private final String mName;
    @Nonnull
    private final String mDataType;
    @Nonnull
    private final List<String> mAllowedValueList;
    @Nullable
    private final String mDefaultValue;
    @Nullable
    private final String mMinimum;
    @Nullable
    private final String mMaximum;
    @Nullable
    private final String mStep;

    private StateVariableImpl(@Nonnull final Builder builder) {
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

    @Override
    public boolean isSendEvents() {
        return mSendEvents;
    }

    @Override
    public boolean isMulticast() {
        return mMulticast;
    }

    @Override
    @Nonnull
    public String getName() {
        return mName;
    }

    @Override
    @Nonnull
    public String getDataType() {
        return mDataType;
    }

    @Override
    @Nonnull
    public List<String> getAllowedValueList() {
        return Collections.unmodifiableList(mAllowedValueList);
    }

    @Override
    @Nullable
    public String getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    @Nullable
    public String getMinimum() {
        return mMinimum;
    }

    @Override
    @Nullable
    public String getMaximum() {
        return mMaximum;
    }

    @Override
    @Nullable
    public String getStep() {
        return mStep;
    }
}
