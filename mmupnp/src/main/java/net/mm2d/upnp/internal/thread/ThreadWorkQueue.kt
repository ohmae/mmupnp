/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.log.Logger
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * ThreadPoolExecutorに渡すためのWorkQueue。
 *
 * 並列実行用に以下のような特徴のスレッドプールを作る
 *
 * - 必要な時には最大数までのスレッドを作成する
 * - 一定時間アイドル状態であればスレッド数を減らす
 * - ワーカースレッドを作る前にアイドルスレッドを優先的に使用する
 *
 * ThreadPoolExecutorはcorePoolSizeとmaximumPoolSizeの間のワーカースレッドについては、
 * execute時にworkQueue#offerをコールしfalseの場合にスレッドを作成する。
 * このときワーカースレッド数がmaximumPoolSizeを超える場合は、rejectされ、
 * RejectedExecutionHandlerがコールされる。
 *
 * そのため、上記特徴のスレッドプールを作成するには
 *
 * - offer()がコールされたとき、アイドルスレッド（takeもしくはpoll内でwait状態のスレッド）がいる場合、キューに積みtrueを返す
 * - 上記以外でfalseを返す
 * - RejectedExecutionHandlerを実装し、rejectedExecutionがコールされたとき、shutdown状態でなければキューに積む
 *
 * という特徴のQueueが必要となる
 */
internal class ThreadWorkQueue(
    private val delegate: BlockingQueue<Runnable> = LinkedBlockingQueue()
) : BlockingQueue<Runnable> by delegate, RejectedExecutionHandler {
    private val idleThreads = AtomicInteger(0)

    override fun offer(runnable: Runnable): Boolean {
        return if (idleThreads.get() == 0) {
            false
        } else delegate.offer(runnable)
    }

    @Throws(InterruptedException::class)
    override fun take(): Runnable {
        idleThreads.incrementAndGet()
        try {
            return delegate.take()
        } finally {
            idleThreads.decrementAndGet()
        }
    }

    @Throws(InterruptedException::class)
    override fun poll(timeout: Long, unit: TimeUnit): Runnable? {
        idleThreads.incrementAndGet()
        try {
            return delegate.poll(timeout, unit)
        } finally {
            idleThreads.decrementAndGet()
        }
    }

    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        if (executor.isShutdown) {
            Logger.e("already shutdown: task $r is rejected from $executor")
            return
        }
        if (!delegate.offer(r)) {
            Logger.e("Unexpected problem: task $r is rejected from $executor")
        }
    }
}
