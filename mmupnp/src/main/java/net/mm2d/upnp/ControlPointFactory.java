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
import java.util.Collections;

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

        /**
         * 使用するインターフェースを指定する。
         *
         * <p>未指定の場合、プロトコルスタックから自動選択される。
         *
         * @param nif 使用するインターフェース
         * @return このインスタンス
         */
        @Nonnull
        public Params setInterface(@Nullable final NetworkInterface nif) {
            mInterfaces = Collections.singletonList(nif);
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

        /**
         * コールバックを実行する{@link CallbackHandler}を指定する。
         *
         * コールバックのスレッドを指定したい場合に指定する。
         * 未指定の場合singleThreadのExecutorが使用される。
         *
         * {@link #setCallbackExecutor(TaskExecutor)}と排他利用であり、後で設定したものが優先される。
         * {@link ControlPoint#terminate()}を実行しても何ら終了処理は行われないため、必要であれば利用側で後始末を実行する。
         *
         * Androidプラットホームであれば、android.os.Handlerを使うことで簡単にUIスレッドでコールバックが実行できる。
         * <pre>
         * param.setCallbackHandler(handler::post)
         * </pre>
         *
         * @param handler コールバックを実行するCallbackHandler
         * @return このインスタンス
         * @see #setCallbackExecutor(TaskExecutor)
         */
        @Nonnull
        public Params setCallbackHandler(@Nonnull final CallbackHandler handler) {
            mCallbackExecutor = new TaskExecutorWrapper(handler);
            return this;
        }

        @Nullable
        TaskExecutor getCallbackExecutor() {
            return mCallbackExecutor;
        }
    }

    private static class TaskExecutorWrapper implements TaskExecutor {
        private final CallbackHandler mHandler;

        TaskExecutorWrapper(@Nonnull final CallbackHandler handler) {
            mHandler = handler;
        }

        @Override
        public boolean execute(@Nonnull final Runnable task) {
            return mHandler.execute(task);
        }

        @Override
        public void terminate() {
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
                new DiFactory(protocol, params.getCallbackExecutor()));
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
