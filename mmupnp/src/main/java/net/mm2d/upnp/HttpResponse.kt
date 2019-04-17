/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.internal.message.HttpMessageDelegate
import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HttpResponse
internal constructor(
    private val startLineDelegate: StartLine,
    private val delegate: HttpMessageDelegate
) : HttpMessage by delegate {

    internal data class StartLine(
        private var status: Status = Status.HTTP_INVALID,
        private var statusCode: Int = 0,
        var reasonPhrase: String = "",
        override var version: String = Http.DEFAULT_HTTP_VERSION
    ) : StartLineDelegate {
        fun getStatusCode(): Int = statusCode

        fun setStatusCode(code: Int) {
            val status = Status.valueOf(code)
            if (status == Status.HTTP_INVALID) {
                throw IllegalArgumentException("unexpected status code:$code")
            }
            setStatus(status)
        }

        fun getStatus(): Status = status

        fun setStatus(status: Status) {
            this.status = status
            statusCode = status.code
            reasonPhrase = status.phrase
        }

        override fun getStartLine(): String = "$version $statusCode $reasonPhrase"

        override fun setStartLine(startLine: String) {
            val params = startLine.split(" ", limit = 3)
            if (params.size < 3) {
                throw IllegalArgumentException()
            }
            version = params[0]
            val code = params[1].toIntOrNull() ?: throw IllegalArgumentException()
            setStatusCode(code)
            reasonPhrase = params[2]
        }
    }

    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see getStatus
     */
    fun getStatusCode(): Int = startLineDelegate.getStatusCode()

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see setStatus
     */
    fun setStatusCode(code: Int) {
        startLineDelegate.setStatusCode(code)
    }

    /**
     * レスポンスフレーズを取得する
     *
     * @return レスポンスフレーズ
     * @see getStatus
     */
    fun getReasonPhrase(): String = startLineDelegate.reasonPhrase

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see setStatus
     */
    fun setReasonPhrase(reasonPhrase: String) {
        startLineDelegate.reasonPhrase = reasonPhrase
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    fun getStatus(): Status = startLineDelegate.getStatus()

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     * @return HttpResponse
     */
    fun setStatus(status: Status) {
        startLineDelegate.setStatus(status)
    }

    /**
     * Message as String
     */
    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        /**
         * インスタンスを作成。
         */
        @JvmStatic
        fun create(): HttpResponse {
            return StartLine().let {
                HttpResponse(it, HttpMessageDelegate(it))
            }
        }

        /**
         * 同一内容のインスタンスを作成する。
         */
        @JvmStatic
        fun copy(original: HttpResponse): HttpResponse {
            return original.startLineDelegate.copy().let {
                HttpResponse(it, HttpMessageDelegate(it, original.delegate))
            }
        }
    }
}
