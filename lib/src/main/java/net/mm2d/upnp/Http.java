/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TextUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPに必要な各種定義とユーティリティを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public final class Http {
    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";
    public static final String DEFAULT_HTTP_VERSION = HTTP_1_1;
    public static final String SERVER = "SERVER";
    public static final String HOST = "HOST";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CALLBACK = "CALLBACK";
    public static final String CHUNKED = "chunked";
    public static final String CONNECTION = "Connection";
    public static final String CLOSE = "close";
    public static final String KEEP_ALIVE = "keep-alive";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_DEFAULT = "text/xml; charset=\"utf-8\"";
    public static final String USER_AGENT = "User-Agent";
    /**
     * Mandatory request
     */
    public static final String MAN = "MAN";
    /**
     * Maximum wait time in seconds 1-5
     */
    public static final String MX = "MX";
    /**
     * Notification Type
     */
    public static final String NT = "NT";
    /**
     * Notification Sub Type
     */
    // ...ならNSTでは？
    public static final String NTS = "NTS";
    /**
     * Subscription Identifier
     */
    public static final String SID = "SID";
    /**
     * Sequence number 32-bit unsigned
     */
    public static final String SEQ = "SEQ";
    /**
     * Search Target
     */
    public static final String ST = "ST";
    public static final String STATEVAR = "STATEVAR";
    public static final String ACCEPTED_STATEVAR = "ACCEPTED-STATEVAR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String DATE = "DATE";
    public static final String EXT = "EXT";
    public static final String LOCATION = "LOCATION";
    /**
     * Unique Service Name
     */
    public static final String USN = "USN";
    public static final String BOOTID_UPNP_ORG = "BOOTID.UPNP.ORG";
    public static final String CONFIGID_UPNP_ORG = "CONFIGID.UPNP.ORG";
    public static final String SEARCHPORT_UPNP_ORG = "SEARCHPORT.UPNP.ORG";
    public static final String SECURELOCATION_UPNP_ORG = "SECURELOCATION.UPNP.ORG";
    public static final String SOAPACTION = "SOAPACTION";
    public static final String UPNP_EVENT = "upnp:event";
    public static final String UPNP_PROPCHANGE = "upnp:propchange";

    public static final String SUBSCRIBE = "SUBSCRIBE";
    public static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String NOTIFY = "NOTIFY";

    public static final int DEFAULT_PORT = 80;

    /**
     * HTTPのステータスコードを表現するEnum
     */
    public enum Status {
        HTTP_INVALID(0, "Invalid"),
        HTTP_CONTINUE(100, "Continue"),
        HTTP_SWITCH_PROTOCOL(101, "Switching Protocols"),
        HTTP_OK(200, "OK"),
        HTTP_CREATED(201, "Created"),
        HTTP_ACCEPTED(202, "Accepted"),
        HTTP_NOT_AUTHORITATIVE(203, "Non-Authoritative Information"),
        HTTP_NO_CONTENT(204, "No Content"),
        HTTP_RESET(205, "Reset Content"),
        HTTP_PARTIAL(206, "Partial Content"),
        HTTP_MULT_CHOICE(300, "Multiple Choices"),
        HTTP_MOVED_PERM(301, "Moved Permanently"),
        HTTP_FOUND(302, "Found"),
        HTTP_SEE_OTHER(303, "See Other"),
        HTTP_NOT_MODIFIED(304, "Not Modified"),
        HTTP_USE_PROXY(305, "Use Proxy"),
        HTTP_TEMP_REDIRECT(307, "Temporary Redirect"),
        HTTP_BAD_REQUEST(400, "Bad Request"),
        HTTP_UNAUTHORIZED(401, "Unauthorized"),
        HTTP_PAYMENT_REQUIRED(402, "Payment Required"),
        HTTP_FORBIDDEN(403, "Forbidden"),
        HTTP_NOT_FOUND(404, "Not Found"),
        HTTP_BAD_METHOD(405, "Method Not Allowed"),
        HTTP_NOT_ACCEPTABLE(406, "Not Acceptable"),
        HTTP_PROXY_AUTH(407, "Proxy Authentication Required"),
        HTTP_REQUEST_TIMEOUT(408, "Request Time-out"),
        HTTP_CONFLICT(409, "Conflict"),
        HTTP_GONE(410, "Gone"),
        HTTP_LENGTH_REQUIRED(411, "Length Required"),
        HTTP_PRECON_FAILED(412, "Precondition Failed"),
        HTTP_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
        HTTP_URI_TOO_LONG(414, "Request-URI Too Large"),
        HTTP_UNSUPPORTED_TYPE(415, "Unsupported Media Type"),
        HTTP_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
        HTTP_EXPECTATION_FAILED(417, "Expectation Failed"),
        HTTP_INTERNAL_ERROR(500, "Internal Server Error"),
        HTTP_NOT_IMPLEMENTED(501, "Not Implemented"),
        HTTP_BAD_GATEWAY(502, "Bad Gateway"),
        HTTP_UNAVAILABLE(503, "Service Unavailable"),
        HTTP_GATEWAY_TIMEOUT(504, "Gateway Time-out"),
        HTTP_VERSION(505, "HTTP Version not supported"),
        ;
        private final int mCode;
        @Nonnull
        private final String mPhrase;

        Status(
                final int code,
                @Nonnull final String phrase) {
            mCode = code;
            mPhrase = phrase;
        }

        /**
         * ステータスコードを返す。
         *
         * @return ステータスコード
         */
        public int getCode() {
            return mCode;
        }

        /**
         * レスポンスフレーズを返す。
         *
         * @return レスポンスフレーズ
         */
        @Nonnull
        public String getPhrase() {
            return mPhrase;
        }

        /**
         * ステータスコードのint値から該当するステータスコードを返す。
         *
         * @param code ステータスコード
         * @return 該当するStatus
         */
        @Nonnull
        public static Status valueOf(final int code) {
            for (final Status c : values()) {
                if (c.getCode() == code) {
                    return c;
                }
            }
            return HTTP_INVALID;
        }
    }

    private static final String HTTP_SCHEME = "http://";
    private static final DateFormat RFC_1123_FORMAT;
    private static final DateFormat RFC_1036_FORMAT;
    private static final DateFormat ASC_TIME_FORMAT;

    static {
        final Locale locale = Locale.US;
        final TimeZone tz = TimeZone.getTimeZone("GMT");
        RFC_1123_FORMAT = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", locale);
        RFC_1123_FORMAT.setTimeZone(tz);
        RFC_1036_FORMAT = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", locale);
        RFC_1036_FORMAT.setTimeZone(tz);
        ASC_TIME_FORMAT = new SimpleDateFormat("E MMM d HH:mm:ss yyyy", locale);
        ASC_TIME_FORMAT.setTimeZone(tz);
    }

    /**
     * Dateヘッダのパースを行う。
     *
     * @param string Dateヘッダ
     * @return パース結果、失敗した場合null
     */
    @Nullable
    public static synchronized Date parseDate(@Nullable final String string) {
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        try {
            return RFC_1123_FORMAT.parse(string);
        } catch (final ParseException ignored) {
        }
        try {
            return RFC_1036_FORMAT.parse(string);
        } catch (final ParseException ignored) {
        }
        try {
            return ASC_TIME_FORMAT.parse(string);
        } catch (final ParseException ignored) {
        }
        return null;
    }

    /**
     * 日付文字列を作成して返す。
     *
     * @param date 日付
     * @return RFC1123形式の日付文字列
     */
    @Nonnull
    public static synchronized String formatDate(final long date) {
        return formatDate(new Date(date));
    }

    /**
     * 日付文字列を作成して返す。
     *
     * @param date 日付
     * @return RFC1123形式の日付文字列
     */
    @SuppressWarnings("WeakerAccess")
    @Nonnull
    public static synchronized String formatDate(@Nonnull final Date date) {
        return RFC_1123_FORMAT.format(date);
    }

    /**
     * 現在時刻の日付文字列を作成して返す。
     *
     * @return RFC1123形式の日付文字列
     */
    @Nonnull
    public static synchronized String getCurrentDate() {
        return formatDate(System.currentTimeMillis());
    }

    /**
     * HTTPのURLか否かを返す。
     *
     * @param url URL
     * @return HTTPのURLのときtrue
     */
    public static boolean isHttpUrl(@Nullable final String url) {
        return url != null && url.length() > HTTP_SCHEME.length()
                && url.substring(0, HTTP_SCHEME.length()).equalsIgnoreCase(HTTP_SCHEME);
    }

    /**
     * URLについているqueryを削除して返す。
     *
     * @param url URL
     * @return queryを削除した
     */
    @Nonnull
    private static String removeQuery(@Nonnull final String url) {
        final int pos = url.indexOf('?');
        if (pos > 0) {
            return url.substring(0, pos);
        }
        return url;
    }

    /**
     * BaseURLと絶対パスからURLを作成する。
     *
     * @param baseUrl BaseURL
     * @param path    絶対パス
     * @return 結合されたURL
     */
    @Nonnull
    private static String makeUrlWithAbsolutePath(
            @Nonnull final String baseUrl,
            @Nonnull final String path) {
        final int pos = baseUrl.indexOf('/', HTTP_SCHEME.length());
        if (pos < 0) {
            return baseUrl + path;
        }
        return baseUrl.substring(0, pos) + path;
    }

    /**
     * BaseURLと相対パスからURLを作成する。
     *
     * @param baseUrl BaseURL
     * @param path    相対パス
     * @return 結合されたURL
     */
    @Nonnull
    private static String makeUrlWithRelativePath(
            @Nonnull final String baseUrl,
            @Nonnull final String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + path;
        }
        final int pos = baseUrl.lastIndexOf('/');
        if (pos > HTTP_SCHEME.length()) {
            return baseUrl.substring(0, pos + 1) + path;
        }
        return baseUrl + "/" + path;
    }

    /**
     * URLにScopeIDを追加する
     *
     * @param urlString URL文字列
     * @param scopeId   ScopeID
     * @return ScopeIDが付加されたURL
     * @throws MalformedURLException 不正なURL
     */
    @Nonnull
    public static URL makeUrlWithScopeId(
            @Nonnull final String urlString,
            final int scopeId) throws MalformedURLException {
        final URL url = new URL(urlString);
        if (scopeId == 0) {
            return url;
        }
        final String host = makeHostWithScopeId(url.getHost(), scopeId);
        final int port = url.getPort();
        if (url.getDefaultPort() == port || port <= 0) {
            return new URL(url.getProtocol() + "://" + host + url.getFile());
        }
        return new URL(url.getProtocol() + "://" + host + ":" + url.getPort() + url.getFile());
    }

    @Nonnull
    private static String makeHostWithScopeId(
            @Nonnull final String host,
            final int scopeId) {
        if (!isInet6Host(host)) {
            return host;
        }
        final int length = host.length();
        if (host.charAt(length - 1) != ']') {
            return host;
        }
        final int index = host.indexOf("%");
        if (index < 0) {
            return host.substring(0, length - 1) + "%" + scopeId + "]";
        }
        return host.substring(0, index) + "%" + scopeId + "]";
    }

    private static boolean isInet6Host(@Nonnull final String host) {
        try {
            return InetAddress.getByName(host) instanceof Inet6Address;
        } catch (final UnknownHostException ignored) {
            return false;
        }
    }

    @Nonnull
    private static String makeAbsoluteUrl(
            @Nonnull final String baseUrl,
            @Nonnull final String url) {
        if (isHttpUrl(url)) {
            return url;
        }
        final String base = removeQuery(baseUrl);
        if (url.startsWith("/")) {
            return makeUrlWithAbsolutePath(base, url);
        }
        return makeUrlWithRelativePath(base, url);
    }

    /**
     * URL情報を正規化して返す。
     *
     * <p>URLとして渡される情報は以下のバリエーションがある
     * <ul>
     * <li>"http://"から始まる完全なURL
     * <li>"/"から始まる絶対パス
     * <li>"/"以外から始まる相対パス
     * </ul>
     *
     * <p>一方、baseUrlの情報としては以下のバリエーションを考慮する
     * <ul>
     * <li>"http://10.0.0.1:1000/" ホスト名のみ
     * <li>"http://10.0.0.1:1000" ホスト名のみだが末尾の"/"がない。
     * <li>"http://10.0.0.1:1000/hoge/fuga" ファイル名で終わる
     * <li>"http://10.0.0.1:1000/hoge/fuga/" ディレクトリ名で終わる
     * <li>"http://10.0.0.1:1000/hoge/fuga?a=foo&amp;b=bar" 上記に対してクエリーが付いている
     * </ul>
     *
     * <p>URLが"http://"から始まっていればそのまま利用する。
     * "/"から始まっていればbaseUrlホストの絶対パスとして
     * baseUrlの"://"以降の最初の"/"までと結合する。
     * それ以外の場合はLocationからの相対パスであり、
     * baseUrlから抽出したディレクトリ名と結合する。
     *
     * <p>またscopeIdの付与も行う。
     *
     * @param baseUrl URLパスのベースとなる値
     * @param url     URLパス情報
     * @param scopeId スコープID
     * @return 正規化したURL
     * @throws MalformedURLException 不正なURL
     */
    @Nonnull
    public static URL makeAbsoluteUrl(
            @Nonnull final String baseUrl,
            @Nonnull final String url,
            final int scopeId)
            throws MalformedURLException {
        return makeUrlWithScopeId(makeAbsoluteUrl(baseUrl, url), scopeId);
    }

    /**
     * インスタンス化禁止
     */
    private Http() {
    }
}
