/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.net.*
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * HTTPに必要な各種定義とユーティリティを提供する。
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
    const val STATEVAR = "STATEVAR"
    const val ACCEPTED_STATEVAR = "ACCEPTED-STATEVAR"
    const val TIMEOUT = "TIMEOUT"
    const val DATE = "DATE"
    const val EXT = "EXT"
    const val LOCATION = "LOCATION"
    /**
     * Unique Service Name
     */
    const val USN = "USN"
    const val BOOTID_UPNP_ORG = "BOOTID.UPNP.ORG"
    const val CONFIGID_UPNP_ORG = "CONFIGID.UPNP.ORG"
    const val SEARCHPORT_UPNP_ORG = "SEARCHPORT.UPNP.ORG"
    const val SECURELOCATION_UPNP_ORG = "SECURELOCATION.UPNP.ORG"
    const val SOAPACTION = "SOAPACTION"
    const val UPNP_EVENT = "upnp:event"
    const val UPNP_PROPCHANGE = "upnp:propchange"

    const val SUBSCRIBE = "SUBSCRIBE"
    const val UNSUBSCRIBE = "UNSUBSCRIBE"
    const val GET = "GET"
    const val POST = "POST"
    const val NOTIFY = "NOTIFY"

    const val DEFAULT_PORT = 80

    const val HTTP_SCHEME = "http://"
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
     * 現在時刻の日付文字列を作成して返す。
     *
     * @return RFC1123形式の日付文字列
     */
    val currentDate: String
        @Synchronized get() = formatDate(System.currentTimeMillis())

    /**
     * HTTPのステータスコードを表現するEnum
     */
    enum class Status(
            /**
             * HTTPステータスコード
             */
            val code: Int,
            /**
             * HTTPステータスフレーズ
             */
            val phrase: String
    ) {
        /**
         * Invalid status
         */
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
             * ステータスコードのint値から該当するステータスコードを返す。
             *
             * @param code ステータスコード
             * @return 該当するStatus
             */
            fun valueOf(code: Int): Status {
                return values().find { it.code == code } ?: HTTP_INVALID
            }
        }
    }

    /**
     * Dateヘッダのパースを行う。
     *
     * @param string Dateヘッダ
     * @return パース結果、失敗した場合null
     */
    @Synchronized
    fun parseDate(string: String?): Date? {
        if (string.isNullOrEmpty()) return null
        return RFC_1123_FORMAT.parseOrNull(string)
                ?: RFC_1036_FORMAT.parseOrNull(string)
                ?: ASC_TIME_FORMAT.parseOrNull(string)
    }

    private fun DateFormat.parseOrNull(source: String): Date? {
        try {
            return parse(source)
        } catch (ignored: ParseException) {
        }
        return null
    }

    /**
     * 日付文字列を作成して返す。
     *
     * @param date 日付
     * @return RFC1123形式の日付文字列
     */
    @Synchronized
    fun formatDate(date: Long): String {
        return formatDate(Date(date))
    }

    /**
     * 日付文字列を作成して返す。
     *
     * @param date 日付
     * @return RFC1123形式の日付文字列
     */
    @Synchronized
    fun formatDate(date: Date): String {
        return RFC_1123_FORMAT.format(date)
    }

    /**
     * HTTPのURLか否かを返す。
     *
     * @param url URL
     * @return HTTPのURLのときtrue
     */
    fun isHttpUrl(url: String?): Boolean {
        return (url != null && url.length > HTTP_SCHEME.length
                && url.substring(0, HTTP_SCHEME.length).equals(HTTP_SCHEME, ignoreCase = true))
    }

    /**
     * URLについているqueryを削除して返す。
     *
     * @param url URL
     * @return queryを削除した
     */
    private fun removeQuery(url: String): String {
        val pos = url.indexOf('?')
        return if (pos > 0) url.substring(0, pos) else url
    }

    /**
     * BaseURLと絶対パスからURLを作成する。
     *
     * @param baseUrl BaseURL
     * @param path    絶対パス
     * @return 結合されたURL
     */
    private fun makeUrlWithAbsolutePath(baseUrl: String, path: String): String {
        val pos = baseUrl.indexOf('/', HTTP_SCHEME.length)
        return if (pos < 0) baseUrl + path else baseUrl.substring(0, pos) + path
    }

    /**
     * BaseURLと相対パスからURLを作成する。
     *
     * @param baseUrl BaseURL
     * @param path    相対パス
     * @return 結合されたURL
     */
    private fun makeUrlWithRelativePath(baseUrl: String, path: String): String {
        if (baseUrl.endsWith("/")) {
            return baseUrl + path
        }
        val pos = baseUrl.lastIndexOf('/')
        return if (pos > HTTP_SCHEME.length) baseUrl.substring(0, pos + 1) + path else "$baseUrl/$path"
    }

    /**
     * URLにScopeIDを追加する
     *
     * @param urlString URL文字列
     * @param scopeId   ScopeID
     * @return ScopeIDが付加されたURL
     * @throws MalformedURLException 不正なURL
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

    private fun String.isInet6Host(): Boolean {
        return try {
            InetAddress.getByName(this) is Inet6Address
        } catch (ignored: UnknownHostException) {
            false
        }
    }

    private fun makeAbsoluteUrl(baseUrl: String, url: String): String {
        if (url.isHttpUrl()) {
            return url
        }
        val base = removeQuery(baseUrl)
        return if (url.startsWith("/")) {
            makeUrlWithAbsolutePath(base, url)
        } else makeUrlWithRelativePath(base, url)
    }

    /**
     * URL情報を正規化して返す。
     *
     * URLとして渡される情報は以下のバリエーションがある
     *
     * - "http://"から始まる完全なURL
     * - "/"から始まる絶対パス
     * - "/"以外から始まる相対パス
     *
     * 一方、baseUrlの情報としては以下のバリエーションを考慮する
     *
     * - "http://10.0.0.1:1000/" ホスト名のみ
     * - "http://10.0.0.1:1000" ホスト名のみだが末尾の"/"がない。
     * - "http://10.0.0.1:1000/hoge/fuga" ファイル名で終わる
     * - "http://10.0.0.1:1000/hoge/fuga/" ディレクトリ名で終わる
     * - "http://10.0.0.1:1000/hoge/fuga?a=foo&amp;b=bar" 上記に対してクエリーが付いている
     *
     * URLが"http://"から始まっていればそのまま利用する。
     * "/"から始まっていればbaseUrlホストの絶対パスとして
     * baseUrlの"://"以降の最初の"/"までと結合する。
     * それ以外の場合はLocationからの相対パスであり、
     * baseUrlから抽出したディレクトリ名と結合する。
     *
     * またscopeIdの付与も行う。
     *
     * @param baseUrl URLパスのベースとなる値
     * @param url     URLパス情報
     * @param scopeId スコープID
     * @return 正規化したURL
     * @throws MalformedURLException 不正なURL
     */
    @Throws(MalformedURLException::class)
    fun makeAbsoluteUrl(baseUrl: String, url: String, scopeId: Int): URL {
        return makeUrlWithScopeId(makeAbsoluteUrl(baseUrl, url), scopeId)
    }
}

/**
 * HTTP URLかを判定する。
 *
 * @receiver 検査文字列
 * @return HTTP URLの場合true
 */
fun String?.isHttpUrl(): Boolean {
    return (this != null && this.startsWith(Http.HTTP_SCHEME, ignoreCase = true))
}
