/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * UPnP ControlPointのインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface ControlPoint {
    /**
     * 機器発見イベント通知用リスナー。
     *
     * <p>
     * {@link #onDiscover(Device)}
     * {@link #onLost(Device)}
     * 及び、
     * {@link NotifyEventListener#onNotifyEvent(Service, long, String, String)}
     * は、いずれも同一のスレッドからコールされる。
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
        void onDiscover(@Nonnull Device device);

        /**
         * 機器喪失時にコールされる。
         *
         * <p>有効期限切れ、SSDP byebye受信、ControlPointの停止によって発生する
         *
         * @param device 喪失したDevice
         * @see Device
         */
        void onLost(@Nonnull Device device);
    }

    /**
     * NotifyEvent通知を受け取るリスナー。
     *
     * <p>
     * {@link #onNotifyEvent(Service, long, String, String)}
     * 及び、
     * {@link DiscoveryListener#onDiscover(Device)}
     * {@link DiscoveryListener#onLost(Device)}
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
        void onNotifyEvent(
                @Nonnull Service service,
                long seq,
                @Nonnull String variable,
                @Nonnull String value);
    }

    /**
     * 初期化を行う。
     *
     * <p>利用前にかならず実行する。
     * 一度初期化を行うと再初期化は不可能。
     * インターフェースの変更など、再初期化が必要な場合はインスタンスの生成からやり直すこと。
     * また、終了する際は必ず{@link #terminate()}をコールすること。
     *
     * @see #initialize()
     */
    void initialize();

    /**
     * 終了処理を行う。
     *
     * <p>動作中の場合、停止処理を行う。
     * 一度終了処理を行ったあとは再初期化は不可能。
     * インスタンス参照を破棄すること。
     *
     * @see #stop()
     * @see #initialize()
     */
    void terminate();

    /**
     * 処理を開始する。
     *
     * <p>本メソッドのコール前はネットワークに関連する処理を実行することはできない。
     * 既に開始状態の場合は何も行われない。
     * 一度開始したあとであっても、停止処理後であれば再度開始可能。
     *
     * @see #initialize()
     */
    void start();

    /**
     * 処理を停止する。
     *
     * <p>開始していない状態、既に停止済みの状態の場合なにも行われない。
     * 停止に伴い発見済みDeviceはLost扱いとなる。
     * 停止後は発見済みDeviceのインスタンスを保持していても正常に動作しない。
     *
     * @see #start()
     */
    void stop();

    /**
     * 保持している発見済みのデバイスリストをクリアする。
     *
     * <p>コール時点で保持されているデバイスはlost扱いとして通知される。
     */
    void clearDeviceList();

    /**
     * Searchパケットを送出する。
     *
     * <p>{@link #search(String)}を引数nullでコールするのと等価。
     */
    void search();

    /**
     * Searchパケットを送出する。
     *
     * <p>stがnullの場合、"ssdp:all"として動作する。
     *
     * @param st SearchパケットのSTフィールド
     */
    void search(@Nullable String st);

    /**
     * ダウンロードするIconを選択するフィルタを設定する。
     *
     * @param filter 設定するフィルタ、nullは指定できない。
     * @see IconFilter#NONE
     * @see IconFilter#ALL
     */
    void setIconFilter(@Nonnull IconFilter filter);

    /**
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    void addDiscoveryListener(@Nonnull DiscoveryListener listener);

    /**
     * 機器発見リスナーを削除する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    void removeDiscoveryListener(@Nonnull DiscoveryListener listener);

    /**
     * NotifyEvent受信リスナーを登録する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    void addNotifyEventListener(@Nonnull NotifyEventListener listener);

    /**
     * NotifyEvent受信リスナーを削除する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    void removeNotifyEventListener(@Nonnull NotifyEventListener listener);


    /**
     * 発見したデバイスの数を返す。
     *
     * @return デバイスの数
     */
    int getDeviceListSize();

    /**
     * 発見したデバイスのリストを返す。
     *
     * <p>内部で保持するリストのコピーが返される。
     *
     * @return デバイスのリスト
     * @see Device
     */
    @Nonnull
    List<Device> getDeviceList();

    /**
     * 指定UDNのデバイスを返す。
     *
     * <p>見つからない場合nullが返る。
     *
     * @param udn UDN
     * @return 指定UDNのデバイス
     * @see Device
     */
    @Nullable
    Device getDevice(@Nonnull String udn);

    /**
     * 固定デバイスを設定する。
     *
     * <p>設定したデバイスの取得ができた後は時間経過やByeByeで削除されることはない。
     *
     * @param location locationのURL。正確な値である必要がある。
     */
    void addPinnedDevice(@Nonnull String location);

    /**
     * 固定デバイスを削除する。
     *
     * <p>固定デバイスを削除する。
     * <p>該当するlocationを持つデバイスがあったとしても固定デバイスでない場合は削除されない。
     *
     * @param location locationのURL
     */
    void removePinnedDevice(@Nonnull String location);
}
