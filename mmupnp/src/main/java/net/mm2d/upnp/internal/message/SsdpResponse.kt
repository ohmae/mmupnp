/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.HttpResponse
import net.mm2d.upnp.SsdpMessage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress

/**
 * SSDPレスポンスメッセージを表現するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpResponse(
// VisibleForTesting
    val message: HttpResponse,
    private val delegate: SsdpMessageDelegate
) : SsdpMessage by delegate {
    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see getStatus
     */
    fun getStatusCode(): Int = message.getStatusCode()

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see setStatus
     */
    fun setStatusCode(code: Int) {
        message.setStatusCode(code)
    }

    /**
     * レスポンスフレーズを取得する
     *
     * @return レスポンスフレーズ
     * @see getStatus
     */
    fun getReasonPhrase(): String = message.getReasonPhrase()

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see setStatus
     */
    fun setReasonPhrase(reasonPhrase: String) {
        message.setReasonPhrase(reasonPhrase)
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    fun getStatus(): Status = message.getStatus()

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     * @return HttpResponse
     */
    fun setStatus(status: Status) {
        message.setStatus(status)
    }

    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        /**
         * 受信した情報からインスタンス作成
         *
         * @param address 受信したインターフェースのアドレス
         * @param data    受信したデータ
         * @param length  受信したデータの長さ
         * @throws IOException 入出力エラー
         */
        @JvmStatic
        @Throws(IOException::class)
        fun create(address: InetAddress, data: ByteArray, length: Int): SsdpResponse {
            return HttpResponse.create().apply {
                readData(ByteArrayInputStream(data, 0, length))
            }.let { SsdpResponse(it, SsdpMessageDelegate(it, address)) }
        }
    }
}
