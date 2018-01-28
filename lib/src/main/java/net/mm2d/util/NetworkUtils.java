/*
 * Copyright(C) 2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ネットワーク関係のユーティリティメソッドを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class NetworkUtils {
    /**
     * 外部と通信可能なIPv4アドレスをもつインターフェースを返す。
     *
     * @return 外部と通信可能なIPv4アドレスを持つインターフェース。
     */
    @Nonnull
    public static List<NetworkInterface> getAvailableInet4Interfaces() {
        final List<NetworkInterface> resultList = new ArrayList<>();
        final List<NetworkInterface> allList = getNetworkInterfaceList();
        for (final NetworkInterface ni : allList) {
            if (isAvailableInet4Interface(new NetworkInterfaceWrapperImpl(ni))) {
                resultList.add(ni);
            }
        }
        return resultList;
    }

    /**
     * システムのすべてのネットワークインターフェースのリストを返す。
     *
     * <p>{@link java.net.NetworkInterface#getNetworkInterfaces()}
     * の戻り値を{@link java.util.Enumeration}ではなく{@link java.util.List}にしたもの。
     * インターフェースがない場合、及び、{@link java.net.SocketException}が発生するような場合は、
     * 空のListが返り、nullが返ることはない。
     *
     * @return システムのすべてのネットワークインターフェース
     * @see java.net.NetworkInterface#getNetworkInterfaces()
     */
    @Nonnull
    public static List<NetworkInterface> getNetworkInterfaceList() {
        final Enumeration<NetworkInterface> netIfs = getNetworkInterfaces();
        if (netIfs == null || !netIfs.hasMoreElements()) {
            return Collections.emptyList();
        }
        final List<NetworkInterface> list = new ArrayList<>();
        while (netIfs.hasMoreElements()) {
            list.add(netIfs.nextElement());
        }
        return list;
    }

    /**
     * システムのすべてのネットワークインターフェースを返す。
     *
     * <p>{@link java.net.NetworkInterface#getNetworkInterfaces()}
     * のコールにおいて{@link java.net.SocketException}が発生するような場合も
     * nullを返すようにしたもの。
     *
     * @return システムのすべてのネットワークインターフェースの列挙
     * @see java.net.NetworkInterface#getNetworkInterfaces()
     */
    @Nullable
    private static Enumeration<NetworkInterface> getNetworkInterfaces() {
        try {
            return sNetworkInterfaceEnumeration.get();
        } catch (final SocketException ignored) {
        }
        return null;
    }

    /**
     * 外部と通信可能なIPv4アドレスを持つか否かを返す。
     *
     * @param netIf 検査するNetworkInterface
     * @return true:外部と通信可能なIPv4アドレスを持つ。false:それ以外
     */
    // VisibleForTesting
    static boolean isAvailableInet4Interface(@Nonnull final NetworkInterfaceWrapper netIf) {
        return isConnectedToNetwork(netIf) && hasInet4Address(netIf);
    }

    /**
     * ネットワークに接続している状態か否かを返す。
     *
     * @param netIf 検査するNetworkInterface
     * @return true:ネットワークに接続している。false:それ以外
     */
    private static boolean isConnectedToNetwork(@Nonnull final NetworkInterfaceWrapper netIf) {
        try {
            return !netIf.isLoopback() && netIf.isUp() && netIf.supportsMulticast();
        } catch (final SocketException ignored) {
        }
        return false;
    }

    /**
     * IPv4のアドレスを持つか否かを返す。
     *
     * @param netIf 検査するNetworkInterface
     * @return true:IPv4アドレスを持つ。false:それ以外
     */
    private static boolean hasInet4Address(@Nonnull final NetworkInterfaceWrapper netIf) {
        final List<InterfaceAddress> addresses = netIf.getInterfaceAddresses();
        for (final InterfaceAddress address : addresses) {
            if (address.getAddress() instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }

    // VisibleForTesting
    private static NetworkInterfaceEnumeration sNetworkInterfaceEnumeration = new NetworkInterfaceEnumeration();

    // VisibleForTesting
    static class NetworkInterfaceEnumeration {
        Enumeration<NetworkInterface> get() throws SocketException {
            return NetworkInterface.getNetworkInterfaces();
        }
    }

    // VisibleForTesting
    interface NetworkInterfaceWrapper {
        List<InterfaceAddress> getInterfaceAddresses();

        boolean isLoopback() throws SocketException;

        boolean isUp() throws SocketException;

        boolean supportsMulticast() throws SocketException;
    }

    private static class NetworkInterfaceWrapperImpl implements NetworkInterfaceWrapper {
        private NetworkInterface mNetworkInterface;

        NetworkInterfaceWrapperImpl(@Nonnull final NetworkInterface networkInterface) {
            mNetworkInterface = networkInterface;
        }

        @Override
        public List<InterfaceAddress> getInterfaceAddresses() {
            return mNetworkInterface.getInterfaceAddresses();
        }

        @Override
        public boolean isLoopback() throws SocketException {
            return mNetworkInterface.isLoopback();
        }

        @Override
        public boolean isUp() throws SocketException {
            return mNetworkInterface.isUp();
        }

        @Override
        public boolean supportsMulticast() throws SocketException {
            return mNetworkInterface.supportsMulticast();
        }
    }

    // インスタンス化禁止
    private NetworkUtils() {
        throw new AssertionError();
    }
}
