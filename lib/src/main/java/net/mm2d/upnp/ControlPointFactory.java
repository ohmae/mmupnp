/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.internal.impl.ControlPointImpl;
import net.mm2d.upnp.internal.impl.DiFactory;

import java.net.NetworkInterface;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ControlPointのインスタンスを作成するFactory。
 *
 * <p>将来的にControlPointもインターフェースに変更するため、
 * ControlPointのコンストラクタはDeprecatedとしている。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public final class ControlPointFactory {
    /**
     * ControlPointの初期化パラメータ
     */
    public static class Params {
        @Nonnull
        private Protocol mProtocol = Protocol.DEFAULT;
        @Nullable
        private Collection<NetworkInterface> mInterfaces;
        @Nullable
        private TaskExecutor mCallbackExecutor;
        @Nullable
        private TaskExecutor mIoExecutor;

        private boolean mNotifySegmentCheckEnabled;

        /**
         * 使用するプロトコルスタックを指定する。
         *
         * <p>未指定の場合デフォルトのプロトコルスタックが使用される。
         *
         * @param protocol 使用するプロトコルスタック
         * @return このインスタンス
         * @see Protocol
         */
        @Nonnull
        public Params setProtocol(@Nonnull final Protocol protocol) {
            mProtocol = protocol;
            return this;
        }

        @Nonnull
        Protocol getProtocol() {
            return mProtocol;
        }

        /**
         * 使用するインターフェースを指定する。
         *
         * <p>未指定の場合、プロトコルスタックから自動選択される。
         *
         * @param interfaces 使用するインターフェース
         * @return このインスタンス
         */
        @Nonnull
        public Params setInterfaces(@Nullable final Collection<NetworkInterface> interfaces) {
            mInterfaces = interfaces;
            return this;
        }

        @Nullable
        Collection<NetworkInterface> getInterfaces() {
            return mInterfaces;
        }

        /**
         * SSDP Notifyパケットを受け取った時にセグメントチェックを行う設定を行う
         *
         * @param enabled セグメントチェックを有効にするときtrue
         * @return このインスタンス
         */
        @Nonnull
        public Params setNotifySegmentCheckEnabled(final boolean enabled) {
            mNotifySegmentCheckEnabled = enabled;
            return this;
        }

        boolean isNotifySegmentCheckEnabled() {
            return mNotifySegmentCheckEnabled;
        }

        /**
         * コールバックを実行する{@link TaskExecutor}を指定する。
         *
         * コールバックのスレッドを指定したい場合に指定する。
         * 未指定の場合singleThreadのExecutorが使用される。
         *
         * @param executor コールバックを実行するTaskExecutor
         * @return このインスタンス
         */
        @Nonnull
        public Params setCallbackExecutor(@Nullable final TaskExecutor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        @Nullable
        TaskExecutor getCallbackExecutor() {
            return mCallbackExecutor;
        }

        /**
         * IO処理を実行する{@link TaskExecutor}を指定する。
         *
         * IO処理のスレッドを指定したい場合に指定する。
         * 未指定の場合、CPU数に基づくExecutorが利用される。
         *
         * @param executor IO処理を実行するTaskExecutor
         * @return このインスタンス
         */
        @Nonnull
        public Params setIoExecutor(@Nullable final TaskExecutor executor) {
            mIoExecutor = executor;
            return this;
        }

        @Nullable
        TaskExecutor getIoExecutor() {
            return mIoExecutor;
        }
    }

    /**
     * ControlPointのインスタンスを作成する。
     *
     * <p>引数のインターフェースを利用するように初期化される。
     * 使用するインターフェースは自動的に選定される。
     *
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @Nonnull
    public static ControlPoint create()
            throws IllegalStateException {
        return create(new Params());
    }

    /**
     * ControlPointのインスタンスを作成する。
     *
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @Nonnull
    public static ControlPoint create(@Nullable final Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        return create(new Params().setInterfaces(interfaces));
    }

    /**
     * ControlPointのインスタンスを作成する。
     *
     * <p>プロトコルスタックのみ指定して初期化を行う。
     * 使用するインターフェースは自動的に選定される。
     *
     * @param protocol 使用するプロトコルスタック
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @Nonnull
    public static ControlPoint create(@Nonnull final Protocol protocol)
            throws IllegalStateException {
        return create(new Params().setProtocol(protocol));
    }

    /**
     * ControlPointのインスタンスを作成する。
     *
     * @param protocol   使用するプロトコルスタック
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @Nonnull
    public static ControlPoint create(
            @Nonnull final Protocol protocol,
            @Nullable final Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        return create(new Params().setProtocol(protocol).setInterfaces(interfaces));
    }

    /**
     * ControlPointのインスタンスを作成する。
     *
     * @param params 初期化パラメータ
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @Nonnull
    public static ControlPoint create(@Nonnull final Params params) {
        final Protocol protocol = params.getProtocol();
        return new ControlPointImpl(
                protocol,
                getDefaultInterfacesIfEmpty(protocol, params.getInterfaces()),
                params.isNotifySegmentCheckEnabled(),
                new DiFactory(protocol, params.getCallbackExecutor(), params.getIoExecutor()));
    }

    @Nonnull
    private static Collection<NetworkInterface> getDefaultInterfacesIfEmpty(
            @Nonnull final Protocol protocol,
            @Nullable final Collection<NetworkInterface> interfaces) {
        if (interfaces == null || interfaces.isEmpty()) {
            return protocol.getAvailableInterfaces();
        }
        return interfaces;
    }

    // インスタンス化禁止
    private ControlPointFactory() {
    }
}
