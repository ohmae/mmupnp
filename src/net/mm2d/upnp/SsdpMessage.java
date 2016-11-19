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

    private final HttpMessage mMessage;
    private static final int DEFAULT_MAX_AGE = 1800;
    private int mMaxAge;
    private long mExpireTime;
    private String mUuid;
    private String mType;
    private String mNts;
    private String mLocation;
    private InetAddress mPacketAddress;
    private InterfaceAddress mInterfaceAddress;

    /**
     * 内部表現としての{@link HttpMessage}のインスタンスを作成する。
     *
     * {@link HttpRequest}か{@link HttpResponse}のインスタンスを返すように小クラスで実装する。
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
    }

    /**
     * 受信した情報からインスタンス作成
     *
     * @param ifa 受信したInterfaceAddress
     * @param dp 受信したDatagramPacket
     * @throws IOException 入出力エラー
     */
    public SsdpMessage(@Nonnull InterfaceAddress ifa, @Nonnull DatagramPacket dp)
            throws IOException {
        mMessage = newMessage();
        mInterfaceAddress = ifa;
        mMessage.readData(new ByteArrayInputStream(dp.getData(), 0, dp.getLength()));
        parseMessage();
        mPacketAddress = dp.getAddress();
    }

    /**
     * Locationに記述のアドレスとパケットの送信元アドレスが一致しているかを返す。
     *
     * @return 一致している場合true
     */
    public boolean hasValidLocation() {
        if (TextUtils.isEmpty(mLocation)) {
            return false;
        }
        final String packetAddress = mPacketAddress.getHostAddress();
        try {
            final String locationAddress = new URL(mLocation).getHost();
            return packetAddress.equals(locationAddress);
        } catch (final MalformedURLException ignored) {
        }
        return false;
    }

    private void parseMessage() {
        parseCacheControl();
        parseUsn();
        mExpireTime = mMaxAge * 1000 + System.currentTimeMillis();
        mLocation = mMessage.getHeader(Http.LOCATION);
        mNts = mMessage.getHeader(Http.NTS);
    }

    /**
     * このパケットを受信したInterfaceAddressを返す。
     *
     * @return このパケットを受信したInterfaceAddress
     */
    @Nonnull
    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    private void parseCacheControl() {
        mMaxAge = DEFAULT_MAX_AGE;
        final String age = mMessage.getHeader(Http.CACHE_CONTROL);
        if (TextUtils.isEmpty(age) || !age.toLowerCase().startsWith("max-age")) {
            return;
        }
        final int pos = age.indexOf('=');
        if (pos < 0 || pos + 1 == age.length()) {
            return;
        }
        try {
            mMaxAge = Integer.parseInt(age.substring(pos + 1));
        } catch (final NumberFormatException ignored) {
        }
    }

    private void parseUsn() {
        final String usn = mMessage.getHeader(Http.USN);
        if (TextUtils.isEmpty(usn) || !usn.startsWith("uuid")) {
            return;
        }
        final int pos = usn.indexOf("::");
        if (pos < 0) {
            mUuid = usn;
            return;
        }
        mUuid = usn.substring(0, pos);
        if (pos + 2 < usn.length()) {
            mType = usn.substring(pos + 2);
        }
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
     * @param name ヘッダ名
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
    @Nullable
    public String getUuid() {
        return mUuid;
    }

    /**
     * USNに記述されたTypeを返す。
     *
     * @return Type
     */
    @Nullable
    public String getType() {
        return mType;
    }

    /**
     * NTSフィールドの値を返す。
     *
     * @return NSTフィールドの値
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
     * 受信時刻からmax-ageを加算した時刻
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
