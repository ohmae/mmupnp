/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.HttpRequest
import net.mm2d.upnp.SsdpMessage

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress

/**
 * SSDPリクエストメッセージを表現するクラス
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpRequest(
    val message: HttpRequest,
    private val delegate: SsdpMessageDelegate
) : SsdpMessage by delegate {

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    fun getMethod(): String = message.getMethod()

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    fun setMethod(method: String) {
        message.setMethod(method)
    }

    /**
     * URI（リクエストパス）を返す。
     *
     * @return URI
     */
    fun getUri(): String = message.getUri()

    /**
     * URI（リクエストパス）を設定する。
     *
     * 接続先の設定ではなくパスのみの設定
     *
     * @param uri URI
     */
    fun setUri(uri: String) {
        message.setUri(uri)
    }

    fun updateLocation() {
        delegate.updateLocation()
    }

    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        /**
         * インスタンス作成。
         */
        @JvmStatic
        fun create(): SsdpRequest {
            return HttpRequest.create().let {
                SsdpRequest(it, SsdpMessageDelegate(it))
            }
        }

        /**
         * 受信した情報からインスタンス作成。
         *
         * @param address 受信したインターフェースのアドレス
         * @param data    受信したデータ
         * @param length  受信したデータの長さ
         * @throws IOException 入出力エラー
         */
        @JvmStatic
        @Throws(IOException::class)
        fun create(address: InetAddress, data: ByteArray, length: Int): SsdpRequest {
            return HttpRequest.create().apply {
                readData(ByteArrayInputStream(data, 0, length))
            }.let { SsdpRequest(it, SsdpMessageDelegate(it, address)) }
        }
    }
}
