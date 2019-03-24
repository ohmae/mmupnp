/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * UPnP Deviceを表現するインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Device {
    /**
     * 紐付いたControlPointを返す。
     *
     * @return 紐付いたControlPoint
     */
    val controlPoint: ControlPoint

    /**
     * 最新のSSDPパケットを返す。
     *
     * @return 最新のSSDPパケット
     */
    val ssdpMessage: SsdpMessage

    /**
     * 更新がなければ無効となる時間(ms)を返す。
     *
     * @return 更新がなければ無効となる時間(ms)
     */
    val expireTime: Long

    /**
     * DeviceDescriptionのXML文字列を返す。
     *
     * @return DeviceDescriptionのXML文字列
     */
    val description: String

    /**
     * 受信したインターフェースのScopeIDを返す。
     *
     * @return ScopeID、設定されていない場合(IPv4含む)は0
     */
    val scopeId: Int

    /**
     * SSDPパケットに記述されているLocationヘッダの値を返す。
     *
     * @return Locationヘッダの値
     */
    val location: String

    /**
     * URLのベースとして使用する値を返す。
     *
     *
     * URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
     *
     * @return URLのベースとして使用する値
     */
    val baseUrl: String

    /**
     * Locationに記述のIPアドレスを返す。
     *
     * 記述に異常がある場合は空文字が返る。
     *
     * @return IPアドレス
     */
    val ipAddress: String

    /**
     * UDNタグの値を返す。
     *
     * Required. Unique Device Name. Universally-unique identifier for the device, whether root or embedded.
     * shall be the same over time for a specific device instance (i.e., shall survive reboots).
     * shall match the field value of the NT header field in device discovery messages. shall match the prefix of
     * the USN header field in all discovery messages. (Clause 1, "Discovery" explains the NT and USN header fields.)
     * shall begin with "uuid:" followed by a UUID suffix specified by a UPnP vendor. See clause 1.1.4,
     * "UUID format and recommended generation algorithms" for the MANDATORY UUID format.
     *
     * @return UDNタグの値
     */
    val udn: String

    /**
     * UPCタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Allowed. Universal Product Code. 12-digit, all-numeric code that identifies the consumer package.
     * Managed by the Uniform Code Council. Specified by UPnP vendor. Single UPC.
     *
     * @return UPCタグの値
     */
    val upc: String?

    /**
     * deviceTypeタグの値を返す。
     *
     * Required. UPnP device type. Single URI.
     *
     *  * For standard devices defined by a UPnP Forum working committee, shall begin with "urn:schemas-upnp-org:device:"
     * followed by the standardized device type suffix, a colon, and an integer device version i.e.
     * urn:schemas-upnp-org:device:deviceType:ver.
     * The highest supported version of the device type shall be specified.
     *  * For non-standard devices specified by UPnP vendors, shall begin with "urn:", followed by a Vendor Domain Name,
     * followed by ":device:", followed by a device type suffix, colon, and an integer version, i.e.,
     * "urn:domain-name:device:deviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the device type shall be specified.
     *
     * The device type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * chars, not counting the version suffix and separating colon.
     *
     * @return deviceTypeタグの値
     */
    val deviceType: String

    /**
     * friendlyNameタグの値を返す。
     *
     * Required. Short description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return friendlyNameタグの値
     */
    val friendlyName: String

    /**
     * manufacturerタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Required. Manufacturer's name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header
     * fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return manufacturerタグの値
     */
    val manufacture: String?

    /**
     * manufacturerURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Allowed. Web site for Manufacturer. Is allowed to have a different value depending on language requested
     * (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return manufacturerURLタグの値
     */
    val manufactureUrl: String?

    /**
     * modelNameタグの値を返す。
     *
     * Required. Model name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT- LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNameタグの値
     */
    val modelName: String

    /**
     * modelURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Allowed. Web site for model. Is allowed to have a different value depending on language requested (see
     * ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return modelURLタグの値
     */
    val modelUrl: String?

    /**
     * modelDescriptionタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Recommended. Long description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 128 characters.
     *
     * @return modelDescriptionタグの値
     */
    val modelDescription: String?

    /**
     * modelNumberタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Recommended. Model number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNumberタグの値
     */
    val modelNumber: String?

    /**
     * serialNumberタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Recommended. Serial number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return serialNumberタグの値
     */
    val serialNumber: String?

    /**
     * presentationURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * Recommended. URL to presentation for device (see clause 5, “Presentation”). shall be relative to the URL at
     * which the device description is located in accordance with the rules specified in clause 5 of RFC 3986.
     * Specified by UPnP vendor. Single URL.
     *
     * @return presentationURLタグの値
     */
    val presentationUrl: String?

    /**
     * Iconのリストを返す。
     *
     * @return Iconのリスト
     * @see Icon
     */
    val iconList: List<Icon>

    /**
     * Serviceのリストを返す。
     *
     * @return Serviceのリスト
     * @see Service
     */
    val serviceList: List<Service>

    /**
     * Embedded Deviceであることを返す。
     *
     * @return Embedded Deviceの時true
     */
    val isEmbeddedDevice: Boolean

    /**
     * Embedded Deviceの場合に親のDeviceを返す。
     *
     * @return 親のDevice、root deviceの時null
     */
    val parent: Device?

    /**
     * Embedded Deviceのリストを返す。
     *
     * @return Embedded Deviceのリスト
     */
    val deviceList: List<Device>

    /**
     * 固定デバイスか否かを返す。
     *
     * @return 固定デバイスの時true
     */
    val isPinned: Boolean

    /**
     * Iconのバイナリーデータを読み込む
     *
     * @param client 通信に使用するHttpClient
     * @param filter 読み込むIconを選別するFilter
     */
    fun loadIconBinary(client: HttpClient, filter: (List<Icon>) -> List<Icon>)

    /**
     * SSDPパケットを設定する。
     *
     * 同一性はSSDPパケットのUUIDで判断し
     * SSDPパケット受信ごとに更新される
     *
     * @param message SSDPパケット
     */
    fun updateSsdpMessage(message: SsdpMessage)

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * 個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない。
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * タグ名にネームスペースプレフィックスは含まない。
     * 複数のネームスペースがある場合は最初に見つかったネームスペースのタグが返される。
     * 値が存在しない場合nullが返る。
     *
     * @param name タグ名
     * @return タグの値
     */
    fun getValue(name: String): String?

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * 個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない、
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * ネームスペースはプレフィックスではなく、URIを指定する。
     * 値が存在しない場合nullが返る。
     *
     * @param namespace ネームスペース（URI）
     * @param name      タグ名
     * @return タグの値
     */
    fun getValueWithNamespace(
        namespace: String,
        name: String
    ): String?

    /**
     * 指定文字列とServiceIDが合致するサービスを返す。
     *
     * 見つからない場合はnullを返す。
     *
     * @param id サーチするID
     * @return 見つかったService
     * @see Service
     */
    fun findServiceById(id: String): Service?

    /**
     * 指定文字列とServiceTypeが合致するサービスを返す。
     *
     * 見つからない場合はnullを返す。
     *
     * @param type サーチするType
     * @return 見つかったService
     * @see Service
     */
    fun findServiceByType(type: String): Service?

    /**
     * 指定文字列の名前を持つActionを返す。
     *
     * 全サービスに対して検索をかけ、最初に見つかったものを返す。
     * 見つからない場合はnullを返す。
     *
     * 特定のサービスのActionを取得したい場合は、
     * [findServiceById] もしくは
     * [findServiceByType] を使用してServiceを取得した後、
     * [Service.findAction] を使用する。
     *
     * @param name Action名
     * @return 見つかったAction
     * @see Service
     *
     * @see Action
     */
    fun findAction(name: String): Action?

    /**
     * 指定DeviceTypeのEmbedded Deviceを探す。
     *
     * 見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    fun findDeviceByType(deviceType: String): Device?

    /**
     * 指定DeviceTypeのEmbedded Deviceを再帰的に探す。
     *
     * 孫要素以降についても再帰的に探索する。
     * 同一のDeviceTypeが存在する場合は記述順が先にあるものが優先される。
     *
     * 見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    fun findDeviceByTypeRecursively(deviceType: String): Device?
}
