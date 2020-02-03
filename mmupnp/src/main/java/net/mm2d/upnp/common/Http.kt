/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common

import java.net.*
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Provides various definitions and utilities required for HTTP.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object Http {
    /** HTTP/1.0 */
    const val HTTP_1_0 = "HTTP/1.0"
    /** HTTP/1.1 */
    const val HTTP_1_1 = "HTTP/1.1"
    /** default Http version */
    const val DEFAULT_HTTP_VERSION = HTTP_1_1
    /** SERVER */
    const val SERVER = "SERVER"
    /** HOST */
    const val HOST = "HOST"
    /** Transfer-Encoding */
    const val TRANSFER_ENCODING = "Transfer-Encoding"
    /** CALLBACK */
    const val CALLBACK = "CALLBACK"
    /** chunked */
    const val CHUNKED = "chunked"
    /** Connection */
    const val CONNECTION = "Connection"
    /** close */
    const val CLOSE = "close"
    /** keep-alive */
    const val KEEP_ALIVE = "keep-alive"
    /** Cache-Control */
    const val CACHE_CONTROL = "Cache-Control"
    /** Content-Length */
    const val CONTENT_LENGTH = "Content-Length"
    /** Content-Type */
    const val CONTENT_TYPE = "Content-Type"
    /** default Content-Type */
    const val CONTENT_TYPE_DEFAULT = "text/xml; charset=\"utf-8\""
    /** User-Agent */
    const val USER_AGENT = "User-Agent"
    /** Mandatory request */
    const val MAN = "MAN"
    /** Maximum wait time in seconds 1-5 */
    const val MX = "MX"
    /** Notification Type */
    const val NT = "NT"
    /** Notification Sub Type */
    const val NTS = "NTS"
    /** Subscription Identifier */
    const val SID = "SID"
    /** Sequence number 32-bit unsigned */
    const val SEQ = "SEQ"
    /** Search Target */
    const val ST = "ST"
    /** Used in SUBSCRIBE. CSV of StateVariables */
    const val STATEVAR = "STATEVAR"
    /** Used in SUBSCRIBE response. CSV of StateVariables */
    const val ACCEPTED_STATEVAR = "ACCEPTED-STATEVAR"
    /** Used in SUBSCRIBE/response. duration until subscription expires. */
    const val TIMEOUT = "TIMEOUT"
    /** ServiceID from the SCPD */
    const val SVCID = "SVCID"
    /** event importance */
    const val LVL = "LVL"
    /** Date */
    const val DATE = "DATE"
    /** Required for backwards compatibility with UPnP 1.0. (Header field name only; no field value.) */
    const val EXT = "EXT"
    /** URL to the UPnP description */
    const val LOCATION = "LOCATION"
    /** Unique Service Name */
    const val USN = "USN"
    /** number increased each time device sends an initial announce or update message */
    const val BOOTID_UPNP_ORG = "BOOTID.UPNP.ORG"
    /** number used for caching description information */
    const val CONFIGID_UPNP_ORG = "CONFIGID.UPNP.ORG"
    /** new BOOTID value that the device will use in subsequent announcements */
    const val NEXTBOOTID_UPNP_ORG = "NEXTBOOTID.UPNP.ORG"
    /** number identifies port on which device responds to unicast M-SEARCH */
    const val SEARCHPORT_UPNP_ORG = "SEARCHPORT.UPNP.ORG"
    /** a base URL with "https:" */
    const val SECURELOCATION_UPNP_ORG = "SECURELOCATION.UPNP.ORG"
    /** a device replies to a TCP port on the control point. */
    const val TCPPORT_UPNP_ORG = "TCPPORT.UPNP.ORG"
    /** friendly name of the control point */
    const val CPFN_UPNP_ORG = "CPFN.UPNP.ORG"
    /** uuid of the control point */
    const val CPUUID_UPNP_ORG = "CPUUID.UPNP.ORG"

    /** SOAPACTION */
    const val SOAPACTION = "SOAPACTION"
    /** Notification Type: upnp:event */
    const val UPNP_EVENT = "upnp:event"
    /** Notification Sub Type: upnp:propchange */
    const val UPNP_PROPCHANGE = "upnp:propchange"

    /** Method name of SUBSCRIBE */
    const val SUBSCRIBE = "SUBSCRIBE"
    /** Method name of UNSUBSCRIBE */
    const val UNSUBSCRIBE = "UNSUBSCRIBE"
    /** Method name of GET */
    const val GET = "GET"
    /** Method name of POST */
    const val POST = "POST"
    /** Method name of NOTIFY */
    const val NOTIFY = "NOTIFY"
    /** Method name of NOTIFY */
    const val M_SEARCH = "M-SEARCH"

    /** Default Http port */
    const val DEFAULT_PORT = 80

    /** http scheme */
    const val HTTP_SCHEME = "http://"
    private val formatLock = ReentrantLock()
    private val RFC_1123_FORMAT: DateFormat
    private val RFC_1036_FORMAT: DateFormat
    private val ASC_TIME_FORMAT: DateFormat

    init {
        val tz = TimeZone.getTimeZone("GMT")
        RFC_1123_FORMAT = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US).also { it.timeZone = tz }
        RFC_1036_FORMAT = SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.US).also { it.timeZone = tz }
        ASC_TIME_FORMAT = SimpleDateFormat("E MMM d HH:mm:ss yyyy", Locale.US).also { it.timeZone = tz }
    }

    /**
     * Enum for HTTP status
     */
    enum class Status(
        /**
         * HTTP status code
         */
        val code: Int,
        /**
         * HTTP status phrase
         */
        val phrase: String
    ) {
        /** Invalid status */
        HTTP_INVALID(0, "Invalid"),
        /** HTTP 100 */
        HTTP_CONTINUE(100, "Continue"),
        /** HTTP 101 */
        HTTP_SWITCH_PROTOCOL(101, "Switching Protocols"),
        /** HTTP 200 */
        HTTP_OK(200, "OK"),
        /** HTTP 201 */
        HTTP_CREATED(201, "Created"),
        /** HTTP 202 */
        HTTP_ACCEPTED(202, "Accepted"),
        /** HTTP 203 */
        HTTP_NOT_AUTHORITATIVE(203, "Non-Authoritative Information"),
        /** HTTP 204 */
        HTTP_NO_CONTENT(204, "No Content"),
        /** HTTP 205 */
        HTTP_RESET(205, "Reset Content"),
        /** HTTP 206 */
        HTTP_PARTIAL(206, "Partial Content"),
        /** HTTP 300 */
        HTTP_MULT_CHOICE(300, "Multiple Choices"),
        /** HTTP 301 */
        HTTP_MOVED_PERM(301, "Moved Permanently"),
        /** HTTP 302 */
        HTTP_FOUND(302, "Found"),
        /** HTTP 303 */
        HTTP_SEE_OTHER(303, "See Other"),
        /** HTTP 304 */
        HTTP_NOT_MODIFIED(304, "Not Modified"),
        /** HTTP 305 */
        HTTP_USE_PROXY(305, "Use Proxy"),
        /** HTTP 307 */
        HTTP_TEMP_REDIRECT(307, "Temporary Redirect"),
        /** HTTP 400 */
        HTTP_BAD_REQUEST(400, "Bad Request"),
        /** HTTP 401 */
        HTTP_UNAUTHORIZED(401, "Unauthorized"),
        /** HTTP 402 */
        HTTP_PAYMENT_REQUIRED(402, "Payment Required"),
        /** HTTP 403 */
        HTTP_FORBIDDEN(403, "Forbidden"),
        /** HTTP 404 */
        HTTP_NOT_FOUND(404, "Not Found"),
        /** HTTP 405 */
        HTTP_BAD_METHOD(405, "Method Not Allowed"),
        /** HTTP 406 */
        HTTP_NOT_ACCEPTABLE(406, "Not Acceptable"),
        /** HTTP 407 */
        HTTP_PROXY_AUTH(407, "Proxy Authentication Required"),
        /** HTTP 408 */
        HTTP_REQUEST_TIMEOUT(408, "Request Time-out"),
        /** HTTP 409 */
        HTTP_CONFLICT(409, "Conflict"),
        /** HTTP 410 */
        HTTP_GONE(410, "Gone"),
        /** HTTP 411 */
        HTTP_LENGTH_REQUIRED(411, "Length Required"),
        /** HTTP 412 */
        HTTP_PRECON_FAILED(412, "Precondition Failed"),
        /** HTTP 413 */
        HTTP_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
        /** HTTP 414 */
        HTTP_URI_TOO_LONG(414, "Request-URI Too Large"),
        /** HTTP 415 */
        HTTP_UNSUPPORTED_TYPE(415, "Unsupported Media Type"),
        /** HTTP 416 */
        HTTP_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
        /** HTTP 417 */
        HTTP_EXPECTATION_FAILED(417, "Expectation Failed"),
        /** HTTP 500 */
        HTTP_INTERNAL_ERROR(500, "Internal Server Error"),
        /** HTTP 501 */
        HTTP_NOT_IMPLEMENTED(501, "Not Implemented"),
        /** HTTP 502 */
        HTTP_BAD_GATEWAY(502, "Bad Gateway"),
        /** HTTP 503 */
        HTTP_UNAVAILABLE(503, "Service Unavailable"),
        /** HTTP 504 */
        HTTP_GATEWAY_TIMEOUT(504, "Gateway Time-out"),
        /** HTTP 505 */
        HTTP_VERSION(505, "HTTP Version not supported");

        companion object {
            /**
             * Returns Status from int value of status code.
             *
             * @param code status code
             * @return Status
             */
            @JvmStatic
            fun valueOf(code: Int): Status =
                values().find { it.code == code } ?: HTTP_INVALID
        }
    }

    /**
     * Parse the Date header.
     *
     * @param string Date header
     * @return result of parse, null, if fail
     */
    fun parseDate(string: String?): Date? {
        if (string.isNullOrEmpty()) return null
        return formatLock.withLock {
            RFC_1123_FORMAT.parseOrNull(string)
                ?: RFC_1036_FORMAT.parseOrNull(string)
                ?: ASC_TIME_FORMAT.parseOrNull(string)
        }
    }

    private fun DateFormat.parseOrNull(source: String): Date? = try {
        parse(source)
    } catch (ignored: ParseException) {
        null
    }

    /**
     * Creates a date string for the current time.
     *
     * @return Date string in RFC1123 format
     */
    fun getCurrentDate(): String = formatDate(System.currentTimeMillis())

    /**
     * Creates a date string.
     *
     * @param date date in Long
     * @return Date string in RFC1123 format
     */
    fun formatDate(date: Long): String = formatDate(Date(date))

    /**
     * Creates a date string.
     *
     * @param date date in Date
     * @return Date string in RFC1123 format
     */
    fun formatDate(date: Date): String = formatLock.withLock {
        RFC_1123_FORMAT.format(date)
    }

    /**
     * Determine HTTP URL or not.
     *
     * @param url URL
     * @return true: HTTP URL, false otherwise
     */
    fun isHttpUrl(url: String?): Boolean = url?.startsWith(HTTP_SCHEME, true) == true

    /**
     * Add ScopeID to URL.
     *
     * @param urlString String URL
     * @param scopeId ScopeID
     * @return URL with ScopeID added
     * @throws MalformedURLException Bad URL
     */
    @Throws(MalformedURLException::class)
    fun makeUrlWithScopeId(urlString: String, scopeId: Int): URL {
        val url = URL(urlString)
        if (scopeId == 0) {
            return url
        }
        val host = makeHostWithScopeId(url.host, scopeId)
        val port = url.port
        return if (url.defaultPort == port || port <= 0)
            URL("${url.protocol}://$host${url.file}")
        else
            URL("${url.protocol}://$host:${url.port}${url.file}")
    }

    private fun makeHostWithScopeId(host: String, scopeId: Int): String {
        if (!host.isInet6Host()) {
            return host
        }
        val length = host.length
        if (host[length - 1] != ']') {
            return host
        }
        val index = host.indexOf("%")
        return if (index < 0) {
            "${host.substring(0, length - 1)}%$scopeId]"
        } else "${host.substring(0, index)}%$scopeId]"
    }

    private fun String.isInet6Host(): Boolean = try {
        InetAddress.getByName(this) is Inet6Address
    } catch (ignored: UnknownHostException) {
        false
    }

    /**
     * Normalize URL information.
     *
     * URL has the following variations
     *
     * - Complete URL starting with "http://"
     * - Absolute path starting with "/"
     * - Relative path starting from other than "/"
     *
     * On the other hand, consider the following variations as baseUrl
     *
     * - "http://10.0.0.1:1000/" Host name only
     * - "http://10.0.0.1:1000" Host name only but without "/" at the end
     * - "http://10.0.0.1:1000/hoge/fuga" Ends with a file name
     * - "http://10.0.0.1:1000/hoge/fuga/" Ends with a directory name
     * - "http://10.0.0.1:1000/hoge/fuga?a=foo&amp;b=bar" With query above
     *
     * If the URL starts with "http: //", it will be used as it is.
     * If it starts with "/",
     * it will be combined with the first "/" after "://" of baseUrl as an absolute path of baseUrl host.
     * Otherwise, it is a relative path from Location,
     * and is combined with the directory name extracted from baseUrl.
     *
     * And this also gives scopeId.
     *
     * @param baseUrl baseUrl
     * @param url URL
     * @param scopeId ScopeID
     * @return Normalized URL
     * @throws MalformedURLException Bad URL
     */
    @Throws(MalformedURLException::class)
    fun makeAbsoluteUrl(baseUrl: String, url: String, scopeId: Int): URL =
        makeUrlWithScopeId(URL(URL(baseUrl), url).toString(), scopeId)
}

/**
 * Determine HTTP URL or not.
 *
 * @receiver target string
 * @return true: HTTP URL, false otherwise
 */
fun String?.isHttpUrl(): Boolean = Http.isHttpUrl(this)
