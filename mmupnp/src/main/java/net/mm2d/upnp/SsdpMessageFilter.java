/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;

/**
 * SsdpMessage受信時にそのメッセージを受け入れるか判定するインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface SsdpMessageFilter {
    /**
     * デフォルトのフィルター。すべてを受け付ける。
     */
    SsdpMessageFilter DEFAULT = message -> true;

    /**
     * SsdpMessageの受信時にコールされる。
     *
     * <p>引数のSsdpMessageを受け入れる場合にtrueを返す。
     * falseを返すとそれ以上の処理は行われない。
     *
     * @param message 受信したSsdpMessage
     * @return 受け入れる場合true、falseを返すと移行の処理は行われない。
     */
    boolean accept(@Nonnull SsdpMessage message);
}
