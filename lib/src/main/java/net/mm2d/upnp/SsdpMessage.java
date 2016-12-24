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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDPメッセージを表現するクラス。
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
    private final String mNts;
    @Nullable
    private final String mLocation;
    @Nullable
    private final InetAddress mPacketAddress;
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
        mLocation = "";
        mPacketAddress = null;
        mInterfaceAddress = null;
    }

    /**
     * 受信した情報からインスタンス作成
     *
     * @param ifa 受信したInterfaceAddress
     * @param dp  受信したDatagramPacket
     * @throws IOException 入出力エラー
     */
    public SsdpMessage(@Nonnull InterfaceAddress ifa, @Nonnull DatagramPacket dp)
            throws IOException {
        mMessage = newMessage();
        mInterfaceAddress = ifa;
        mPacketAddress = dp.getAddress();
        mMessage.readData(new ByteArrayInputStream(dp.getData(), 0, dp.getLength()));
        mMaxAge = parseCacheControl(mMessage);
        final String[] result = parseUsn(mMessage);
        mUuid = result[0];
        mType = result[1];
        mExpireTime = TimeUnit.SECONDS.toMillis(mMaxAge) + System.currentTimeMillis();
        mLocation = mMessage.getHeader(Http.LOCATION);
        mNts = mMessage.getHeader(Http.NTS);
    }

    private static int parseCacheControl(@Nonnull HttpMessage message) {
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
    private static String[] parseUsn(@Nonnull HttpMessage message) throws IOException {
        final String usn = message.getHeader(Http.USN);
        if (TextUtils.isEmpty(usn) || !usn.startsWith("uuid")) {
            throw new IOException("");
        }
        final int pos = usn.indexOf("::");
        if (pos < 0) {
            return new String[]{
                    usn, ""
            };
        }
        return new String[]{
                usn.substring(0, pos), usn.substring(pos + 2)
        };
    }

    /**
     * Locationに記述のアドレスとパケットの送信元アドレスが一致しているかを返す。
     *
     * @return Locationに問題がある場合true
     */
    public boolean hasInvalidLocation() {
        if (TextUtils.isEmpty(mLocation)) {
            return true;
        }
        final String packetAddress = mPacketAddress.getHostAddress();
        try {
            final String locationAddress = new URL(mLocation).getHost();
            return !packetAddress.equals(locationAddress);
        } catch (final MalformedURLException ignored) {
        }
        return true;
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
    public String getHeader(@Nonnull String name) {
        return mMessage.getHeader(name);
    }

    /**
     * ヘッダの値を設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    public void setHeader(@Nonnull String name, @Nonnull String value) {
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
