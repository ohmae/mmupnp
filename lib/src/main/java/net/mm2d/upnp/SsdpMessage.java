/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP(Simple Service Discovery Protocol)メッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public abstract class SsdpMessage {
    /**
     * M-SEARCHのリスエストメソッド
     */
    public static final String M_SEARCH = "M-SEARCH";
    /**
     * NOTIFYのリクエストメソッド
     */
    public static final String NOTIFY = "NOTIFY";
    /**
     * NTSの値：ssdp:alive
     */
    public static final String SSDP_ALIVE = "ssdp:alive";
    /**
     * NTSの値：ssdp:byebye
     */
    public static final String SSDP_BYEBYE = "ssdp:byebye";
    /**
     * NTSの値：ssdp:update
     */
    public static final String SSDP_UPDATE = "ssdp:update";
    /**
     * MANの値：ssdp:discover
     */
    public static final String SSDP_DISCOVER = "\"ssdp:discover\"";

    @Nonnull
    private final HttpMessage mMessage;
    private static final int DEFAULT_MAX_AGE = 1800;
    private final int mMaxAge;
    private final long mExpireTime;
    @Nonnull
    private final String mUuid;
    @Nonnull
    private final String mType;
    @Nullable
    private String mNts;
    @Nullable
    private String mLocation;
    @Nullable
    private final InterfaceAddress mInterfaceAddress;

    /**
     * 内部表現としての{@link HttpMessage}のインスタンスを作成する。
     *
     * <p>{@link HttpRequest}か{@link HttpResponse}のインスタンスを返すように小クラスで実装する。
     *
     * @return {@link HttpMessage}のインスタンス
     */
    @Nonnull
    protected abstract HttpMessage newMessage();

    /**
     * 内部表現としての{@link HttpMessage}を返す。
     *
     * @return 内部表現としての{@link HttpMessage}
     */
    @Nonnull
    protected HttpMessage getMessage() {
        return mMessage;
    }

    /**
     * インスタンス作成。
     */
    public SsdpMessage() {
        mMessage = newMessage();
        mMaxAge = 0;
        mExpireTime = 0;
        mUuid = "";
        mType = "";
        mNts = "";
        mLocation = null;
        mInterfaceAddress = null;
    }

    /**
     * 受信した情報からインスタンス作成
     *
     * @param address 受信したInterfaceAddress
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @throws IOException 入出力エラー
     */
    public SsdpMessage(final @Nonnull InterfaceAddress address,
                       final @Nonnull byte[] data, final int length)
            throws IOException {
        mMessage = newMessage();
        mInterfaceAddress = address;
        mMessage.readData(new ByteArrayInputStream(data, 0, length));
        mMaxAge = parseCacheControl(mMessage);
        final String[] result = parseUsn(mMessage);
        mUuid = result[0];
        mType = result[1];
        mExpireTime = TimeUnit.SECONDS.toMillis(mMaxAge) + System.currentTimeMillis();
        updateHeader();
    }

    void updateHeader() {
        mLocation = mMessage.getHeader(Http.LOCATION);
        mNts = mMessage.getHeader(Http.NTS);
    }

    private static int parseCacheControl(final @Nonnull HttpMessage message) {
        final String age = TextUtils.toLowerCase(message.getHeader(Http.CACHE_CONTROL));
        if (TextUtils.isEmpty(age) || !age.startsWith("max-age")) {
            return DEFAULT_MAX_AGE;
        }
        final int pos = age.indexOf('=');
        if (pos < 0 || pos + 1 == age.length()) {
            return DEFAULT_MAX_AGE;
        }
        try {
            return Integer.parseInt(age.substring(pos + 1));
        } catch (final NumberFormatException ignored) {
        }
        return DEFAULT_MAX_AGE;
    }

    @Nonnull
    private static String[] parseUsn(final @Nonnull HttpMessage message) {
        final String usn = message.getHeader(Http.USN);
        if (TextUtils.isEmpty(usn) || !usn.startsWith("uuid")) {
            return new String[]{"", ""};
        }
        final int pos = usn.indexOf("::");
        if (pos < 0) {
            return new String[]{usn, ""};
        }
        return new String[]{usn.substring(0, pos), usn.substring(pos + 2)};
    }

    /**
     * Locationに正常なURLが記述されており、記述のアドレスとパケットの送信元アドレスに不一致がないか検査する。
     *
     * @param sourceAddress 送信元アドレス
     * @return true:送信元との不一致を含めてLocationに不正がある場合。false:それ以外
     */
    public boolean hasInvalidLocation(final @Nonnull InetAddress sourceAddress) {
        if (!isHttpUrl(mLocation)) {
            return true;
        }
        try {
            final InetAddress locationAddress = InetAddress.getByName(new URL(mLocation).getHost());
            return !sourceAddress.equals(locationAddress);
        } catch (MalformedURLException | UnknownHostException ignored) {
        }
        return true;
    }

    private static boolean isHttpUrl(String url) {
        return url != null && url.length() > 6
                && url.substring(0, 7).equalsIgnoreCase("http://");
    }

    /**
     * このパケットを受信したInterfaceAddressを返す。
     *
     * @return このパケットを受信したInterfaceAddress
     */
    @Nullable
    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return 値
     */
    @Nullable
    public String getHeader(final @Nonnull String name) {
        return mMessage.getHeader(name);
    }

    /**
     * ヘッダの値を設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    public void setHeader(final @Nonnull String name, final @Nonnull String value) {
        mMessage.setHeader(name, value);
    }

    /**
     * USNに記述されたUUIDを返す。
     *
     * @return UUID
     */
    @Nonnull
    public String getUuid() {
        return mUuid;
    }

    /**
     * USNに記述されたTypeを返す。
     *
     * @return Type
     */
    @Nonnull
    public String getType() {
        return mType;
    }

    /**
     * NTSフィールドの値を返す。
     *
     * @return NTSフィールドの値
     */
    @Nullable
    public String getNts() {
        return mNts;
    }

    /**
     * max-ageの値を返す。
     *
     * @return max-ageの値
     */
    public int getMaxAge() {
        return mMaxAge;
    }

    /**
     * 有効期限が切れる時刻を返す。
     *
     * <p>受信時刻からmax-ageを加算した時刻
     *
     * @return 有効期限が切れる時刻
     */
    public long getExpireTime() {
        return mExpireTime;
    }

    /**
     * Locationの値を返す。
     *
     * @return Locationの値
     */
    @Nullable
    public String getLocation() {
        return mLocation;
    }

    @Override
    @Nonnull
    public String toString() {
        return mMessage.toString();
    }
}
