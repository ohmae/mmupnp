/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * UPnP ControlPointのインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface ControlPoint {
    /**
     * 機器発見イベント通知用リスナー。
     *
     * [onDiscover] [onLost]及び、
     * [NotifyEventListener.onNotifyEvent]は、いずれも同一のスレッドからコールされる。
     *
     * @see NotifyEventListener
     */
    interface DiscoveryListener {
        /**
         * 機器発見時にコールされる。
         *
         * @param device 発見したDevice
         * @see Device
         */
        fun onDiscover(device: Device)

        /**
         * 機器喪失時にコールされる。
         *
         * 有効期限切れ、SSDP byebye受信、ControlPointの停止によって発生する
         *
         * @param device 喪失したDevice
         * @see Device
         */
        fun onLost(device: Device)
    }

    /**
     * NotifyEvent通知を受け取るリスナー。
     *
     * [onNotifyEvent]及び、
     * [DiscoveryListener.onDiscover]
     * [DiscoveryListener.onLost]
     * は、いずれも同一のスレッドからコールされる。
     *
     * @see DiscoveryListener
     */
    interface NotifyEventListener {
        /**
         * NotifyEvent受信時にコールされる。
         *
         * @param service  対応するService
         * @param seq      シーケンス番号
         * @param variable 変数名
         * @param value    値
         * @see Service
         */
        fun onNotifyEvent(service: Service, seq: Long, variable: String, value: String)
    }

    /**
     * 発見したデバイスの数
     */
    val deviceListSize: Int

    /**
     * 発見したデバイスのリストを返す。
     *
     * @see Device
     */
    val deviceList: List<Device>

    /**
     * 初期化を行う。
     *
     * 利用前にかならず実行する。
     * 一度初期化を行うと再初期化は不可能。
     * インターフェースの変更など、再初期化が必要な場合はインスタンスの生成からやり直すこと。
     * また、終了する際は必ず[terminate]をコールすること。
     *
     * @see terminate
     */
    fun initialize()

    /**
     * 終了処理を行う。
     *
     * 動作中の場合、停止処理を行う。
     * 一度終了処理を行ったあとは再初期化は不可能。
     * インスタンス参照を破棄すること。
     *
     * @see stop
     * @see initialize
     */
    fun terminate()

    /**
     * 処理を開始する。
     *
     * 本メソッドのコール前はネットワークに関連する処理を実行することはできない。
     * 既に開始状態の場合は何も行われない。
     * 一度開始したあとであっても、停止処理後であれば再度開始可能。
     *
     * @see initialize
     */
    fun start()

    /**
     * 処理を停止する。
     *
     * 開始していない状態、既に停止済みの状態の場合なにも行われない。
     * 停止に伴い発見済みDeviceはLost扱いとなる。
     * 停止後は発見済みDeviceのインスタンスを保持していても正常に動作しない。
     *
     * @see start
     */
    fun stop()

    /**
     * 保持している発見済みのデバイスリストをクリアする。
     *
     * コール時点で保持されているデバイスはlost扱いとして通知される。
     */
    fun clearDeviceList()

    /**
     * Searchパケットを送出する。
     *
     * [search]を引数nullでコールするのと等価。
     */
    fun search()

    /**
     * Searchパケットを送出する。
     *
     * stがnullの場合、"ssdp:all"として動作する。
     *
     * @param st SearchパケットのSTフィールド
     */
    fun search(st: String?)

    /**
     * SsdpMessageを受け入れるかどうかの判定メソッドを設定する。
     *
     * @param filter 判定メソッド、nullはすべて受け付け。
     */
    fun setSsdpMessageFilter(filter: ((SsdpMessage) -> Boolean)?)

    /**
     * ダウンロードするIconを選択するフィルタを設定する。
     *
     * 未指定の場合は何も取得しない。
     *
     * @param filter 設定するフィルタ、nullを指定すると何も取得しない。
     */
    fun setIconFilter(filter: ((List<Icon>) -> List<Icon>)?)

    /**
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    fun addDiscoveryListener(listener: DiscoveryListener)

    /**
     * 機器発見リスナーを削除する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    fun removeDiscoveryListener(listener: DiscoveryListener)

    /**
     * NotifyEvent受信リスナーを登録する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    fun addNotifyEventListener(listener: NotifyEventListener)

    /**
     * NotifyEvent受信リスナーを削除する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    fun removeNotifyEventListener(listener: NotifyEventListener)

    /**
     * 指定UDNのデバイスを返す。
     *
     * 見つからない場合nullが返る。
     *
     * @param udn UDN
     * @return 指定UDNのデバイス
     * @see Device
     */
    fun getDevice(udn: String): Device?

    /**
     * デバイスの追加を試みる。
     *
     * キャッシュしておいた情報を元に読み込ませるなどに利用。
     *
     * @param uuid     UDN
     * @param location location
     */
    fun tryAddDevice(uuid: String, location: String)

    /**
     * 固定デバイスを設定する。
     *
     * 設定したデバイスの取得ができた後は時間経過やByeByeで削除されることはない。
     *
     * @param location locationのURL。正確な値である必要がある。
     */
    fun tryAddPinnedDevice(location: String)

    /**
     * 固定デバイスを削除する。
     *
     * 固定デバイスを削除する。
     * 該当するlocationを持つデバイスがあったとしても固定デバイスでない場合は削除されない。
     *
     * @param location locationのURL
     */
    fun removePinnedDevice(location: String)
}
