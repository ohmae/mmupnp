/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.concurrent.TimeUnit;

/**
 * ライブラリのプロパティ情報を管理するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public final class Property {
    /**
     * ライブラリバージョン
     */
    public static final String LIB_VERSION = "mmupnp/1.0";
    /**
     * OSバージョン
     */
    public static final String OS_VERSION;
    /**
     * UPnPバージョン
     */
    public static final String UPNP_VERSION = "UPnP/1.0";
    /**
     * デフォルトタイムアウト値[ms]（30秒）
     *
     * <p>{@link java.net.Socket#setSoTimeout(int)}に渡すためint値で定義
     */
    public static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    // SystemプロパティからOS名とバージョン番号を取得する
    static {
        final String os = System.getProperty("os.name").split(" ")[0];
        final String ver = System.getProperty("os.version");
        OS_VERSION = os + "/" + ver;
    }
}
