/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * UPnP Deviceを表現するインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface Device {
    /**
     * Iconのバイナリーデータを読み込む
     *
     * @param client 通信に使用するHttpClient
     * @param filter 読み込むIconを選別するFilter
     */
    void loadIconBinary(
            @Nonnull HttpClient client,
            @Nonnull IconFilter filter);

    /**
     * 紐付いたControlPointを返す。
     *
     * @return 紐付いたControlPoint
     */
    @Nonnull
    ControlPoint getControlPoint();

    /**
     * SSDPパケットを設定する。
     *
     * <p>同一性はSSDPパケットのUUIDで判断し
     * SSDPパケット受信ごとに更新される
     *
     * @param message SSDPパケット
     */
    void updateSsdpMessage(@Nonnull SsdpMessage message);

    /**
     * 最新のSSDPパケットを返す。
     *
     * @return 最新のSSDPパケット
     */
    @Nonnull
    SsdpMessage getSsdpMessage();

    /**
     * 更新がなければ無効となる時間[ms]を返す。
     *
     * @return 更新がなければ無効となる時間[ms]
     */
    long getExpireTime();

    /**
     * DeviceDescriptionのXML文字列を返す。
     *
     * @return DeviceDescriptionのXML文字列
     */
    @Nonnull
    String getDescription();

    /**
     * 受信したインターフェースのScopeIDを返す。
     *
     * @return ScopeID、設定されていない場合(IPv4含む)は0
     */
    int getScopeId();

    /**
     * 必要に応じてURLにこのデバイスのScopeIdを付加する。
     *
     * <p>SSDPメッセージがIPv6インターフェース経由で通知されており、
     * そのインターフェースのScopeIDが設定されている場合、
     * かつURLのhost部がIPv6リテラルである場合にのみ付加する。
     *
     * @param url 元となるURL
     * @return ScopeIDが付加されたURL
     * @throws MalformedURLException 不正なURL
     */
    URL appendScopeIdIfNeed(@Nonnull String url) throws MalformedURLException;


    /**
     * URL情報を正規化して返す。
     *
     * @param url URLパス情報
     * @return 正規化したURL
     * @throws MalformedURLException 不正なURL
     * @see Http#getAbsoluteUrl(String, String)
     */
    @Nonnull
    URL getAbsoluteUrl(@Nonnull String url) throws MalformedURLException;

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * <p>個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない。
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * タグ名にネームスペースプレフィックスは含まない。
     * 複数のネームスペースがある場合は最初に見つかったネームスペースのタグが返される。
     * 値が存在しない場合nullが返る。
     *
     * @param name タグ名
     * @return タグの値
     */
    @Nullable
    String getValue(@Nonnull String name);

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * <p>個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない、
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * ネームスペースはプレフィックスではなく、URIを指定する。
     * 値が存在しない場合nullが返る。
     *
     * @param namespace ネームスペース（URI）
     * @param name      タグ名
     * @return タグの値
     */
    @Nullable
    String getValueWithNamespace(
            @Nonnull String namespace,
            @Nonnull String name);

    /**
     * SSDPパケットに記述されているLocationヘッダの値を返す。
     *
     * @return Locationヘッダの値
     */
    @Nonnull
    String getLocation();

    /**
     * URLのベースとして使用する値を返す。
     *
     * URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
     *
     * @return URLのベースとして使用する値
     */
    @Nonnull
    String getBaseUrl();

    /**
     * Locationに記述のIPアドレスを返す。
     *
     * <p>記述に異常がある場合は空文字が返る。
     *
     * @return IPアドレス
     */
    @Nonnull
    String getIpAddress();

    /**
     * UDNタグの値を返す。
     *
     * <p>Required. Unique Device Name. Universally-unique identifier for the device, whether root or embedded.
     * shall be the same over time for a specific device instance (i.e., shall survive reboots).
     * shall match the field value of the NT header field in device discovery messages. shall match the prefix of
     * the USN header field in all discovery messages. (Clause 1, "Discovery" explains the NT and USN header fields.)
     * shall begin with "uuid:" followed by a UUID suffix specified by a UPnP vendor. See clause 1.1.4,
     * "UUID format and recommended generation algorithms" for the MANDATORY UUID format.
     *
     * @return UDNタグの値
     */
    @Nonnull
    String getUdn();

    /**
     * UPCタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Universal Product Code. 12-digit, all-numeric code that identifies the consumer package.
     * Managed by the Uniform Code Council. Specified by UPnP vendor. Single UPC.
     *
     * @return UPCタグの値
     */
    @Nullable
    String getUpc();

    /**
     * deviceTypeタグの値を返す。
     *
     * <p>Required. UPnP device type. Single URI.
     * <ul>
     * <li>For standard devices defined by a UPnP Forum working committee, shall begin with "urn:schemas-upnp-org:device:"
     * followed by the standardized device type suffix, a colon, and an integer device version i.e.
     * urn:schemas-upnp-org:device:deviceType:ver.
     * The highest supported version of the device type shall be specified.
     * <li>For non-standard devices specified by UPnP vendors, shall begin with "urn:", followed by a Vendor Domain Name,
     * followed by ":device:", followed by a device type suffix, colon, and an integer version, i.e.,
     * "urn:domain-name:device:deviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the device type shall be specified.
     * </ul>
     * <p>The device type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * chars, not counting the version suffix and separating colon.
     *
     * @return deviceTypeタグの値
     */
    @Nonnull
    String getDeviceType();

    /**
     * friendlyNameタグの値を返す。
     *
     * <p>Required. Short description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return friendlyNameタグの値
     */
    @Nonnull
    String getFriendlyName();

    /**
     * manufacturerタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Required. Manufacturer's name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header
     * fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return manufacturerタグの値
     */
    @Nullable
    String getManufacture();

    /**
     * manufacturerURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Web site for Manufacturer. Is allowed to have a different value depending on language requested
     * (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return manufacturerURLタグの値
     */
    @Nullable
    String getManufactureUrl();

    /**
     * modelNameタグの値を返す。
     *
     * <p>Required. Model name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT- LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNameタグの値
     */
    @Nonnull
    String getModelName();

    /**
     * modelURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Web site for model. Is allowed to have a different value depending on language requested (see
     * ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return modelURLタグの値
     */
    @Nullable
    String getModelUrl();

    /**
     * modelDescriptionタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Long description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 128 characters.
     *
     * @return modelDescriptionタグの値
     */
    @Nullable
    String getModelDescription();

    /**
     * modelNumberタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Model number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNumberタグの値
     */
    @Nullable
    String getModelNumber();

    /**
     * serialNumberタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Serial number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return serialNumberタグの値
     */
    @Nullable
    String getSerialNumber();

    /**
     * presentationURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. URL to presentation for device (see clause 5, “Presentation”). shall be relative to the URL at
     * which the device description is located in accordance with the rules specified in clause 5 of RFC 3986.
     * Specified by UPnP vendor. Single URL.
     *
     * @return presentationURLタグの値
     */
    @Nullable
    String getPresentationUrl();

    /**
     * Iconのリストを返す。
     *
     * @return Iconのリスト
     * @see Icon
     */
    @Nonnull
    List<Icon> getIconList();

    /**
     * Serviceのリストを返す。
     *
     * @return Serviceのリスト
     * @see Service
     */
    @Nonnull
    List<Service> getServiceList();

    /**
     * 指定文字列とServiceIDが合致するサービスを返す。
     *
     * <p>見つからない場合はnullを返す。
     *
     * @param id サーチするID
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    Service findServiceById(@Nonnull String id);

    /**
     * 指定文字列とServiceTypeが合致するサービスを返す。
     *
     * <p>見つからない場合はnullを返す。
     *
     * @param type サーチするType
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    Service findServiceByType(@Nonnull String type);

    /**
     * 指定文字列の名前を持つActionを返す。
     *
     * <p>全サービスに対して検索をかけ、最初に見つかったものを返す。
     * 見つからない場合はnullを返す。
     *
     * <p>特定のサービスのActionを取得したい場合は、
     * {@link #findServiceById(String)} もしくは
     * {@link #findServiceByType(String)} を使用してServiceを取得した後、
     * {@link Service#findAction(String)} を使用する。
     *
     * @param name Action名
     * @return 見つかったAction
     * @see Service
     * @see Action
     */
    @Nullable
    Action findAction(@Nonnull String name);

    /**
     * Embedded Deviceであることを返す。
     *
     * @return Embedded Deviceの時true
     */
    boolean isEmbeddedDevice();

    /**
     * Embedded Deviceの場合に親のDeviceを返す。
     *
     * @return 親のDevice、root deviceの時null
     */
    @Nullable
    Device getParent();

    /**
     * Embedded Deviceのリストを返す。
     *
     * @return Embedded Deviceのリスト
     */
    @Nonnull
    List<Device> getDeviceList();

    /**
     * 指定DeviceTypeのEmbedded Deviceを探す。
     *
     * <p>見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    @Nullable
    Device findDeviceByType(@Nonnull String deviceType);

    /**
     * 指定DeviceTypeのEmbedded Deviceを再帰的に探す。
     *
     * <p>孫要素以降についても再帰的に探索する。
     * 同一のDeviceTypeが存在する場合は記述順が先にあるものが優先される。
     * <p>見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    @Nullable
    Device findDeviceByTypeRecursively(@Nonnull String deviceType);

    @Nonnull
    Set<String> getEmbeddedDeviceUdnSet();
}
