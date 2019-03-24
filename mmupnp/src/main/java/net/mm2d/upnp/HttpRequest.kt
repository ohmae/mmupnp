/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.internal.message.HttpMessageDelegate
import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate
import net.mm2d.upnp.util.toAddressString
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URL

/**
 * HTTPリクエストメッセージを表現するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HttpRequest internal constructor(
    private val startLineDelegate: StartLine,
    private val delegate: HttpMessageDelegate
) : HttpMessage by delegate {

    internal data class StartLine(
        var method: String = Http.GET,
        var uri: String = "",
        override var version: String = Http.DEFAULT_HTTP_VERSION
    ) : StartLineDelegate {
        override var startLine: String
            get() = "$method $uri $version"
            set(line) {
                val params = line.split(" ", limit = 3)
                if (params.size < 3) {
                    throw IllegalArgumentException()
                }
                method = params[0]
                uri = params[1]
                version = params[2]
            }
    }

    /**
     * 宛先アドレス情報。
     */
    var address: InetAddress? = null

    /**
     * 宛先ポート番号。
     */
    var port: Int = 0

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    val method: String
        get() = startLineDelegate.method

    /**
     * URI（リクエストパス）を返す。
     *
     * @return URI
     */
    val uri: String
        get() = startLineDelegate.uri

    /**
     * アドレスとポート番号の組み合わせ文字列。
     */
    @Throws(IllegalStateException::class)
    fun getAddressString(): String {
        address?.let {
            return it.toAddressString(port)
        } ?: throw IllegalStateException("address must be set")
    }

    /**
     * 送信先URLを設定する。
     *
     * @param url 接続先URL
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    @Throws(IOException::class)
    fun setUrl(url: URL) {
        setUrl(url, false)
    }

    /**
     * 接続先URLを設定する。
     *
     * @param url            接続先URL
     * @param withHostHeader trueを指定するとURLにもとづいてHOSTヘッダの設定も行う
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    @Throws(IOException::class)
    fun setUrl(url: URL, withHostHeader: Boolean) {
        if (url.protocol != "http") {
            throw IOException("unsupported protocol." + url.protocol)
        }
        address = InetAddress.getByName(url.host)
        port = when {
            url.port > 65535 ->
                throw IOException("port number is too large. port=${url.port}")
            url.port < 0 ->
                Http.DEFAULT_PORT
            else ->
                url.port
        }
        setUri(url.file)
        if (withHostHeader) {
            setHeader(Http.HOST, getAddressString())
        }
    }

    /**
     * 宛先SocketAddressを返す
     *
     * @return 宛先SocketAddress
     */
    @Throws(IllegalStateException::class)
    fun getSocketAddress(): SocketAddress {
        address?.let {
            return InetSocketAddress(it, port)
        } ?: throw IllegalStateException("address must be set")
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    fun setMethod(method: String) {
        startLineDelegate.method = method
    }

    /**
     * URI（リクエストパス）を設定する。
     *
     * 接続先の設定ではなくパスのみの設定
     *
     * @param uri URI
     * @see setUrl
     */
    fun setUri(uri: String) {
        startLineDelegate.uri = uri
    }

    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        /**
         * インスタンス作成。
         */
        @JvmStatic
        fun create(): HttpRequest {
            val startLine = StartLine()
            val delegate = HttpMessageDelegate(startLine)
            return HttpRequest(startLine, delegate)
        }

        /**
         * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
         *
         * @param original コピー元
         */
        @JvmStatic
        fun copy(original: HttpRequest): HttpRequest {
            val startLine = original.startLineDelegate.copy()
            val delegate = HttpMessageDelegate(startLine, original.delegate)
            return HttpRequest(startLine, delegate).also {
                it.address = original.address
                it.port = original.port
            }
        }
    }
}

