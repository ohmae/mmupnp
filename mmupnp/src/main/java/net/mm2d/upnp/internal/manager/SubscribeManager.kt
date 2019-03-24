/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.log.Logger
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.impl.DiFactory
import net.mm2d.upnp.internal.server.EventReceiver
import net.mm2d.upnp.internal.thread.TaskExecutors

internal class SubscribeManager(
    private val taskExecutors: TaskExecutors,
    private val listeners: Set<NotifyEventListener>,
    factory: DiFactory
) {
    private val subscribeHolder: SubscribeHolder = factory.createSubscribeHolder(taskExecutors)
    private val eventReceiver: EventReceiver = factory.createEventReceiver(
        taskExecutors, this::onEventReceived
    )

    /**
     * イベント通知を受け取るポートを返す。
     *
     * @return イベント通知受信用ポート番号
     * @see EventReceiver
     */
    fun getEventPort(): Int = eventReceiver.getLocalPort()

    fun onEventReceived(sid: String, seq: Long, properties: List<Pair<String, String>>): Boolean {
        Logger.d { "$sid $seq $properties" }
        val service = subscribeHolder.getService(sid)
        if (service == null) {
            Logger.e("service is null")
            return false
        }
        return taskExecutors.callback {
            properties.forEach {
                notifyEvent(service, seq, it.first, it.second)
            }
        }
    }

    private fun notifyEvent(service: Service, seq: Long, name: String?, value: String?) {
        val variable = service.findStateVariable(name)
        if (variable?.isSendEvents != true || value == null) {
            Logger.w { "illegal notify argument: $name $value" }
            return
        }
        listeners.forEach {
            it.onNotifyEvent(service, seq, variable.name, value)
        }
    }

    fun initialize() {
        subscribeHolder.start()
    }

    fun start() {
        eventReceiver.start()
    }

    fun stop() {
        subscribeHolder.getServiceList().forEach {
            taskExecutors.io { it.unsubscribeSync() }
        }
        subscribeHolder.clear()
        eventReceiver.stop()
    }

    fun terminate() {
        subscribeHolder.stop()
    }

    /**
     * SubscriptionIDに合致する[Service]を返す。
     *
     * 合致するServiceがない場合null
     *
     * @param subscriptionId SubscriptionID
     * @return 該当Service
     * @see Service
     */
    fun getSubscribeService(subscriptionId: String): Service? {
        return subscribeHolder.getService(subscriptionId)
    }

    /**
     * SubscriptionIDが確定した[Service]を購読リストに登録する
     *
     * [Service]のsubscribeが実行された後に[Service]からコールされる。
     *
     * @param service 登録するService
     * @param timeout タイムアウトするまでの時間
     * @param keep    keep-aliveを行う場合true
     * @see Service
     * @see Service.subscribeSync
     */
    fun register(service: Service, timeout: Long, keep: Boolean) {
        subscribeHolder.add(service, timeout, keep)
    }

    fun renew(service: Service, timeout: Long) {
        subscribeHolder.renew(service, timeout)
    }

    fun setKeepRenew(service: Service, keep: Boolean) {
        subscribeHolder.setKeepRenew(service, keep)
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除する[Service]
     * @see Service
     * @see Service.unsubscribeSync
     */
    fun unregister(service: Service) {
        subscribeHolder.remove(service)
    }
}
