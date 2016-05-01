/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * HTTPに必要な各種定義とユーティリティを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
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
    public static final String MAN = "MAN";
    public static final String MX = "MX";
    public static final String NT = "NT";
    public static final String NTS = "NTS";
    public static final String SID = "SID";
    public static final String SEQ = "SEQ";
    public static final String ST = "ST";
    public static final String STATEVAR = "STATEVAR";
    public static final String ACCEPTED_STATEVAR = "ACCEPTED-STATEVAR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String DATE = "DATE";
    public static final String EXT = "EXT";
    public static final String LOCATION = "LOCATION";
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

    public static final String USER_AGENT_VALUE = Property.OS_VERSION
            + " " + Property.UPNP_VERSION
            + " " + Property.LIB_VERSION;
    public static final String SERVER_VALUE = Property.OS_VERSION
            + " " + Property.UPNP_VERSION
            + " " + Property.LIB_VERSION;

    /**
     * HTTPのステータスコードを表現するEnum
     */
    public enum Status {
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
        HTTP_EXPECTION_FAILED(417, "Expectation Failed"),
        HTTP_INTERNAL_ERROR(500, "Internal Server Error"),
        HTTP_NOT_IMPLEMENTED(501, "Not Implemented"),
        HTTP_BAD_GATEWAY(502, "Bad Gateway"),
        HTTP_UNAVAILABLE(503, "Service Unavailable"),
        HTTP_GATEWAY_TIMEOUT(504, "Gateway Time-out"),
        HTTP_VERSION(505, "HTTP Version not supported"),;
        private final int mCode;
        private final String mPhrase;

        Status(int code, String phrase) {
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
        public String getPhrase() {
            return mPhrase;
        }

        /**
         * ステータスコードのint値から該当するステータスコードを返す。
         * 
         * @param code ステータスコード
         * @return 該当するStatus
         */
        public static Status valueOf(int code) {
            for (final Status c : values()) {
                if (c.getCode() == code) {
                    return c;
                }
            }
            return null;
        }
    }

    private static final DateFormat sRfc1123Format;
    private static final DateFormat sRfc1036Format;
    private static final DateFormat sAsctimeFormat;

    static {
        final Locale locale = Locale.US;
        final TimeZone tz = TimeZone.getTimeZone("GMT");
        sRfc1123Format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", locale);
        sRfc1123Format.setTimeZone(tz);
        sRfc1036Format = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", locale);
        sRfc1036Format.setTimeZone(tz);
        sAsctimeFormat = new SimpleDateFormat("E MMM d HH:mm:ss yyyy", locale);
        sAsctimeFormat.setTimeZone(tz);
    }

    /**
     * Dateヘッダのパースを行う。
     *
     * @param string Dateヘッダ
     * @return パース結果、失敗した場合null
     */
    public synchronized static Date parseDate(String string) {
        try {
            return sRfc1123Format.parse(string);
        } catch (final ParseException ignored) {
        }
        try {
            return sRfc1036Format.parse(string);
        } catch (final ParseException ignored) {
        }
        try {
            return sAsctimeFormat.parse(string);
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
    public synchronized static String formatDate(long date) {
        return formatDate(new Date(date));
    }

    /**
     * 日付文字列を作成して返す。
     *
     * @param date 日付
     * @return RFC1123形式の日付文字列
     */
    public synchronized static String formatDate(Date date) {
        return sRfc1123Format.format(date);
    }

    /**
     * 現在時刻の日付文字列を作成して返す。
     *
     * @return RFC1123形式の日付文字列
     */
    public synchronized static String getCurrentData() {
        return formatDate(System.currentTimeMillis());
    }
}
