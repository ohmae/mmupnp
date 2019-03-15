/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;

import javax.annotation.Nullable;

/**
 * IO関係のユーティリティメソッドを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public final class IoUtils {
    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * <p>nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param closeable close処理をするCloseable
     */
    public static void closeQuietly(@Nullable final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final IOException ignored) {
        }
    }

    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * <p>nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param socket close処理をするSocket
     */
    public static void closeQuietly(@Nullable final Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (final IOException ignored) {
        }
    }

    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * <p>nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param datagramSocket close処理をするDatagramSocket
     */
    public static void closeQuietly(@Nullable final DatagramSocket datagramSocket) {
        if (datagramSocket == null) {
            return;
        }
        datagramSocket.close();
    }

    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * <p>nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param serverSocket close処理をするServerSocket
     */
    public static void closeQuietly(@Nullable final ServerSocket serverSocket) {
        if (serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (final IOException ignored) {
        }
    }

    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * <p>nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param selector close処理をするSelector
     */
    public static void closeQuietly(@Nullable final Selector selector) {
        if (selector == null) {
            return;
        }
        try {
            selector.close();
        } catch (final IOException ignored) {
        }
    }

    // インスタンス化禁止
    private IoUtils() {
    }
}
