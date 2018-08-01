/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Argumentの実装
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class ArgumentImpl implements Argument {
    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser#loadDescription(HttpClient, DeviceImpl.Builder)
     * @see ServiceParser#loadDescription(HttpClient, DeviceImpl.Builder, ServiceImpl.Builder)
     * @see ActionImpl.Builder
     */
    static class Builder {
        private String mName;
        private boolean mInputDirection;
        private String mRelatedStateVariableName;
        private StateVariable mRelatedStateVariable;

        /**
         * インスタンス作成
         */
        public Builder() {
        }

        /**
         * Argument名を登録する。
         *
         * @param name Argument名
         * @return Builder
         */
        @Nonnull
        public Builder setName(@Nonnull final String name) {
            mName = name;
            return this;
        }

        /**
         * Directionの値を登録する
         *
         * <p>"in"の場合のみinput、それ以外をoutputと判定する。
         *
         * @param direction Directionの値
         * @return Builder
         */
        @Nonnull
        public Builder setDirection(@Nonnull final String direction) {
            mInputDirection = "in".equalsIgnoreCase(direction);
            return this;
        }

        /**
         * RelatedStateVariableの値を登録する。
         *
         * @param name RelatedStateVariableの値
         * @return Builder
         */
        @Nonnull
        public Builder setRelatedStateVariableName(@Nonnull final String name) {
            mRelatedStateVariableName = name;
            return this;
        }

        /**
         * RelatedStateVariableの値を返す。
         *
         * @return RelatedStateVariableの値
         */
        @Nullable
        public String getRelatedStateVariableName() {
            return mRelatedStateVariableName;
        }

        /**
         * RelatedStateVariableので指定されたStateVariableのインスタンスを登録する。
         *
         * @param variable StateVariableのインスタンス
         * @return Builder
         */
        @Nonnull
        public Builder setRelatedStateVariable(@Nonnull final StateVariable variable) {
            mRelatedStateVariable = variable;
            return this;
        }

        /**
         * Argumentのインスタンスを作成する。
         *
         * @return Argumentのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Nonnull
        public Argument build() throws IllegalStateException {
            if (mName == null) {
                throw new IllegalStateException("name must be set.");
            }
            if (mRelatedStateVariable == null) {
                throw new IllegalStateException("related state variable must be set.");
            }
            return new ArgumentImpl(this);
        }
    }

    @Nonnull
    private final String mName;
    private final boolean mInputDirection;
    @Nonnull
    private final StateVariable mRelatedStateVariable;

    private ArgumentImpl(@Nonnull final Builder builder) {
        mName = builder.mName;
        mInputDirection = builder.mInputDirection;
        mRelatedStateVariable = builder.mRelatedStateVariable;
    }

    @Override
    @Nonnull
    public String getName() {
        return mName;
    }

    @Override
    public boolean isInputDirection() {
        return mInputDirection;
    }

    @Override
    @Nonnull
    public StateVariable getRelatedStateVariable() {
        return mRelatedStateVariable;
    }
}
