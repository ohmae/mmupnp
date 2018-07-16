/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

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
        return create(Collections.emptyList());
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
        return create(Protocol.DEFAULT, interfaces);
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
        return create(protocol, Collections.emptyList());
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
        return new ControlPointImpl(protocol, getDefaultInterfacesIfEmpty(protocol, interfaces), new DiFactory(protocol));
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
}
