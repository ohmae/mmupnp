/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * タスクを指定スレッドで実行するExecutorのインターフェース
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface TaskExecutor {
    /**
     * タスクを特定のスレッドで実行する。
     *
     * @param task 実行するタスク
     * @return 実行をできた場合、もしくは実行待ちキューに積むことができた場合true
     */
    fun execute(task: Runnable): Boolean

    /**
     * ControlPointの終了処理中にコールされる。
     *
     * スレッドの停止などが必要な場合はここで実装する。
     */
    fun terminate() {}
}
