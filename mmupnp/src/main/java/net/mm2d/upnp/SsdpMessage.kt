/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress

/**
 * SSDP(Simple Service Discovery Protocol)メッセージを表現するインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface SsdpMessage {

    /**
     * 固定用メッセージかを返す。
     *
     * @return 固定用メッセージの時true
     */
    val isPinned: Boolean

    /**
     * 受信したインターフェースのScopeIDを返す。
     *
     * @return ScopeID、設定されていない場合(IPv4含む)は0
     */
    val scopeId: Int

    /**
     * このパケットを受信したインターフェースのアドレスを返す。
     *
     * @return このパケットを受信したインターフェースのアドレス
     */
    val localAddress: InetAddress?

    /**
     * USNに記述されたUUIDを返す。
     *
     * @return UUID
     */
    val uuid: String

    /**
     * USNに記述されたTypeを返す。
     *
     * @return Type
     */
    val type: String

    /**
     * NTSフィールドの値を返す。
     *
     * @return NTSフィールドの値
     */
    val nts: String?

    /**
     * max-ageの値を返す。
     *
     * @return max-ageの値
     */
    val maxAge: Int

    /**
     * 有効期限が切れる時刻を返す。
     *
     *
     * 受信時刻からmax-ageを加算した時刻
     *
     * @return 有効期限が切れる時刻
     */
    val expireTime: Long

    /**
     * Locationの値を返す。
     *
     * @return Locationの値
     */
    val location: String?

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return 値
     */
    fun getHeader(name: String): String?

    /**
     * ヘッダの値を設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    fun setHeader(name: String, value: String)

    /**
     * 指定されたOutputStreamにメッセージの内容を書き出す。
     *
     * @param os 出力先
     * @throws IOException 入出力エラー
     */
    @Throws(IOException::class)
    fun writeData(os: OutputStream)

    companion object {
        /**
         * M-SEARCHのリスエストメソッド
         */
        const val M_SEARCH = "M-SEARCH"
        /**
         * NOTIFYのリクエストメソッド
         */
        const val NOTIFY = "NOTIFY"
        /**
         * NTSの値：ssdp:alive
         */
        const val SSDP_ALIVE = "ssdp:alive"
        /**
         * NTSの値：ssdp:byebye
         */
        const val SSDP_BYEBYE = "ssdp:byebye"
        /**
         * NTSの値：ssdp:update
         */
        const val SSDP_UPDATE = "ssdp:update"
        /**
         * MANの値：ssdp:discover
         */
        const val SSDP_DISCOVER = "\"ssdp:discover\""
    }
}
