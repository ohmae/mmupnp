/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate
import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate.StartLineDelegate
import java.io.InputStream

/**
 * HTTP response message.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class SingleHttpResponse
internal constructor(
    private val startLineDelegate: StartLine,
    private val delegate: SingleHttpMessageDelegate
) : SingleHttpMessage by delegate {

    internal data class StartLine(
        private var status: Status = Status.HTTP_INVALID,
        private var statusCode: Int = 0,
        var reasonPhrase: String = "",
        override var version: String = Http.DEFAULT_HTTP_VERSION
    ) : StartLineDelegate {
        override val shouldStriveToReadBody: Boolean
            get() = status == Status.HTTP_OK

        fun getStatusCode(): Int = statusCode

        fun setStatusCode(code: Int) {
            val status = Status.valueOf(code)
            require(status != Status.HTTP_INVALID) { "unexpected status code:$code" }
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
            require(params.size >= 3)
            version = params[0]
            val code = params[1].toIntOrNull() ?: throw IllegalArgumentException()
            setStatusCode(code)
            reasonPhrase = params[2]
        }
    }

    /**
     * Return the status code
     *
     * @return status code
     * @see getStatus
     */
    fun getStatusCode(): Int = startLineDelegate.getStatusCode()

    /**
     * Set the status code
     *
     * @param code status code
     * @see setStatus
     */
    fun setStatusCode(code: Int) {
        startLineDelegate.setStatusCode(code)
    }

    /**
     * Return the response phrase.
     *
     * @return response phrase
     * @see getStatus
     */
    fun getReasonPhrase(): String = startLineDelegate.reasonPhrase

    /**
     * Set the response phrase.
     *
     * @param reasonPhrase response phrase
     * @see setStatus
     */
    fun setReasonPhrase(reasonPhrase: String) {
        startLineDelegate.reasonPhrase = reasonPhrase
    }

    /**
     * Return the status.
     *
     * @return status
     */
    fun getStatus(): Status = startLineDelegate.getStatus()

    /**
     * Set the status.
     *
     * @param status status
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
         * Create a new instance.
         */
        @JvmStatic
        fun create(): SingleHttpResponse = StartLine().let {
            SingleHttpResponse(it, SingleHttpMessageDelegate(it))
        }

        /**
         * Create a new instance from InputStream
         */
        @JvmStatic
        fun create(input: InputStream): SingleHttpResponse = create().also { it.readData(input) }

        /**
         * Create a new instance with the same contents as the argument.
         *
         * @params original original message
         */
        @JvmStatic
        fun copy(original: SingleHttpResponse): SingleHttpResponse =
            original.startLineDelegate.copy().let {
                SingleHttpResponse(it, SingleHttpMessageDelegate(it, original.delegate))
            }
    }
}
