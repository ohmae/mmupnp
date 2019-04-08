/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.log.Logger
import net.mm2d.upnp.internal.util.closeQuietly
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.net.URL

/**
 * HTTP通信を行うクライアントソケット。
 *
 * UPnPの通信でよく利用される小さなデータのやり取りに特化したもの。
 * 長大なデータのやり取りは想定していない。
 * 手軽に利用できることを優先し、効率などはあまり考慮されていない。
 * 原則同一のサーバに対する一連の通信ごとにインスタンスを作成する想定。
 *
 * 相手の応答がkeep-alive可能な応答であった場合はコネクションを切断せず、
 * 継続して利用するという、消極的なkeep-alive機能も提供する。
 *
 * keep-alive状態であっても、post時に維持したコネクションと
 * 同一のホスト・ポートでない場合は切断、再接続を行う。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HttpClient(keepAlive: Boolean = true) {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    /**
     * ソケットが使用したローカルアドレスを返す。
     *
     * connectの後保存し、次の接続まで保持される。
     *
     * @return ソケットが使用したローカルアドレス
     */
    var localAddress: InetAddress? = null
        private set
    /**
     * keep-alive設定。
     *
     * デフォルトはtrue。
     * trueを指定した場合、応答がkeep-alive可能なものであればコネクションを継続する。
     * trueを指定しても、応答がkeep-alive可能でない場合はコネクションを切断する。
     * falseを指定した場合、応答の内容によらずコネクションは切断する。
     *
     * また、true/falseどちらを指定した場合であっても、
     * postの引数で渡された[HttpRequest]の内容を変更することはない。
     * ヘッダへkeep-aliveの記述が必要な場合はpostをコールする前に、
     * [HttpRequest]へ設定しておく必要がある。
     */
    var isKeepAlive: Boolean = keepAlive

    /**
     * ソケットが閉じている場合true
     */
    val isClosed: Boolean
        get() = socket == null

    /**
     * リクエストを送信し、レスポンスを受信する。
     *
     * 利用するHTTPメソッドは引数に依存する。
     *
     * @param request 送信するリクエスト
     * @return 受信したレスポンス
     * @throws IOException 通信エラー
     */
    @Throws(IOException::class)
    fun post(request: HttpRequest): HttpResponse {
        return post(request, 0)
    }

    /**
     * リクエストを送信し、レスポンスを受信する。
     *
     * 利用するHTTPメソッドは引数に依存する。
     *
     * @param request       送信するリクエスト
     * @param redirectDepth リダイレクトの深さ
     * @return 受信したレスポンス
     * @throws IOException 通信エラー
     */
    @Throws(IOException::class)
    private fun post(request: HttpRequest, redirectDepth: Int): HttpResponse {
        confirmReuseSocket(request)
        val response: HttpResponse = try {
            doRequest(request)
        } catch (e: IOException) {
            closeSocket()
            throw e
        }
        if (!isKeepAlive || !response.isKeepAlive()) {
            closeSocket()
        }
        return redirectIfNeeded(request, response, redirectDepth)
    }

    private fun confirmReuseSocket(request: HttpRequest) {
        if (!canReuse(request)) {
            closeSocket()
        }
    }

    @Throws(IOException::class)
    private fun doRequest(request: HttpRequest): HttpResponse {
        if (isClosed) {
            openSocket(request)
            return writeAndRead(request)
        } else {
            try {
                return writeAndRead(request)
            } catch (e: IOException) {
                // コネクションを再利用した場合はpeerから既に切断されていた可能性がある。
                // KeepAliveできないサーバである可能性があるのでKeepAliveを無効にしてリトライ
                Logger.w { "retry:\n" + e.message }
                isKeepAlive = false
                closeSocket()
                openSocket(request)
                return writeAndRead(request)
            }
        }
    }

    @Throws(IOException::class)
    // open状態でのみコールする
    private fun writeAndRead(request: HttpRequest): HttpResponse {
        request.writeData(outputStream!!)
        return HttpResponse.create().also { it.readData(inputStream!!) }
    }

    @Throws(IOException::class)
    private fun redirectIfNeeded(request: HttpRequest, response: HttpResponse, redirectDepth: Int): HttpResponse {
        if (needToRedirect(response) && redirectDepth < REDIRECT_MAX) {
            val location = response.getHeader(Http.LOCATION)
            if (!location.isNullOrEmpty()) {
                return redirect(request, location, redirectDepth)
            }
        }
        return response
    }

    private fun needToRedirect(response: HttpResponse): Boolean {
        return when (response.getStatus()) {
            Http.Status.HTTP_MOVED_PERM,
            Http.Status.HTTP_FOUND,
            Http.Status.HTTP_SEE_OTHER,
            Http.Status.HTTP_TEMP_REDIRECT -> true
            else -> false
        }
    }

    @Throws(IOException::class)
    private fun redirect(request: HttpRequest, location: String, redirectDepth: Int): HttpResponse {
        val newRequest = HttpRequest.copy(request).apply {
            setUrl(URL(location), true)
            setHeader(Http.CONNECTION, Http.CLOSE)
        }
        return HttpClient(false)
            .post(newRequest, redirectDepth + 1)
    }

    // VisibleForTesting
    internal fun canReuse(request: HttpRequest): Boolean {
        val socket = socket ?: return false
        return socket.isConnected &&
                socket.inetAddress == request.address &&
                socket.port == request.port
    }

    @Throws(IOException::class)
    private fun openSocket(request: HttpRequest) {
        val socket = Socket().also { socket = it }
        socket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT)
        socket.soTimeout = Property.DEFAULT_TIMEOUT
        inputStream = BufferedInputStream(socket.getInputStream())
        outputStream = BufferedOutputStream(socket.getOutputStream())
        localAddress = socket.localAddress
    }

    private fun closeSocket() {
        inputStream.closeQuietly()
        outputStream.closeQuietly()
        socket.closeQuietly()
        inputStream = null
        outputStream = null
        socket = null
    }

    /**
     * ソケットのクローズを行う。
     */
    fun close() {
        closeSocket()
    }

    /**
     * 単純なHTTP GETにより文字列を取得する。
     *
     * @param url 取得先URL
     * @return 取得できた文字列
     * @throws IOException 取得に問題があった場合
     */
    @Throws(IOException::class)
    fun downloadString(url: URL): String {
        // download()の中でgetBody()がnullで無いことはチェック済み
        return download(url).getBody()!!
    }

    /**
     * 単純なHTTP GETによりバイナリを取得する。
     *
     * @param url 取得先URL
     * @return 取得できたバイナリ
     * @throws IOException 取得に問題があった場合
     */
    @Throws(IOException::class)
    fun downloadBinary(url: URL): ByteArray {
        // download()の中でgetBody()がnullで無いことはチェック済み
        return download(url).getBodyBinary()!!
    }

    /**
     * 単純なHTTP GETを実行する。
     *
     * @param url 取得先URL
     * @return HTTPレスポンス
     * @throws IOException 取得に問題があった場合
     */
    @Throws(IOException::class)
    fun download(url: URL): HttpResponse {
        val request = makeHttpRequest(url)
        val response = post(request)
        // response bodyがemptyであることは正常
        if (response.getStatus() !== Http.Status.HTTP_OK || response.getBody() == null) {
            Logger.i { "request:\n$request\nresponse:\n$response" }
            throw IOException(response.startLine)
        }
        return response
    }

    @Throws(IOException::class)
    private fun makeHttpRequest(url: URL): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.GET)
            setUrl(url, true)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, if (isKeepAlive) Http.KEEP_ALIVE else Http.CLOSE)
        }
    }

    companion object {
        private const val REDIRECT_MAX = 2
    }
}
