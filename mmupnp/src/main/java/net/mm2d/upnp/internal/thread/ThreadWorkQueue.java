/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.log.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

/**
 * ThreadPoolExecutorに渡すためのWorkQueue。
 *
 * <p>並列実行用に以下のような特徴のスレッドプールを作る</p>
 * <ul>
 * <li>必要な時には最大数までのスレッドを作成する</li>
 * <li>一定時間アイドル状態であればスレッド数を減らす</li>
 * <li>ワーカースレッドを作る前にアイドルスレッドを優先的に使用する</li>
 * </ul>
 *
 * <p>ThreadPoolExecutorはcorePoolSizeとmaximumPoolSizeの間のワーカースレッドについては、
 * execute時にworkQueue#offerをコールしfalseの場合にスレッドを作成する。
 * このときワーカースレッド数がmaximumPoolSizeを超える場合は、rejectされ、
 * RejectedExecutionHandlerがコールされる。</p>
 *
 * <p>そのため、上記特徴のスレッドプールを作成するには</p>
 *
 * <ul>
 * <li>offer()がコールされたとき、アイドルスレッド
 * （takeもしくはpoll内でwait状態のスレッド）がいる場合、キューに積みtrueを返す</li>
 * <li>上記以外でfalseを返す</li>
 * <li>RejectedExecutionHandlerを実装し、rejectedExecutionがコールされたとき、
 * shutdown状態でなければキューに積む</li>
 * </ul>
 *
 * <p>という特徴のQueueが必要となる</p>
 */
class ThreadWorkQueue implements BlockingQueue<Runnable>, RejectedExecutionHandler {
    @Nonnull
    private final BlockingQueue<Runnable> mDelegate;
    @Nonnull
    private final AtomicInteger mIdleThreads = new AtomicInteger(0);

    ThreadWorkQueue() {
        this(new LinkedBlockingQueue<>());
    }

    // VisibleForTesting
    ThreadWorkQueue(@Nonnull final BlockingQueue<Runnable> delegate) {
        mDelegate = delegate;
    }

    @Override
    public boolean offer(@Nonnull final Runnable runnable) {
        if (mIdleThreads.get() == 0) {
            return false;
        }
        return mDelegate.offer(runnable);
    }

    @Nonnull
    @Override
    public Runnable take() throws InterruptedException {
        mIdleThreads.incrementAndGet();
        try {
            return mDelegate.take();
        } finally {
            mIdleThreads.decrementAndGet();
        }
    }

    @Override
    public Runnable poll(
            final long timeout,
            @Nonnull final TimeUnit unit) throws InterruptedException {
        mIdleThreads.incrementAndGet();
        try {
            return mDelegate.poll(timeout, unit);
        } finally {
            mIdleThreads.decrementAndGet();
        }
    }

    @Override
    public void rejectedExecution(
            final Runnable r,
            final ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            Logger.e("already shutdown: task " + r + " is rejected from " + executor);
            return;
        }
        if (!mDelegate.offer(r)) {
            Logger.e("Unexpected problem: task " + r + " is rejected from " + executor);
        }
    }

    // 以下、デリゲートのためのボイラープレートコード
    @Override
    public boolean add(@Nonnull final Runnable runnable) {
        return mDelegate.add(runnable);
    }

    @Override
    public Runnable remove() {
        return mDelegate.remove();
    }

    @Override
    public Runnable poll() {
        return mDelegate.poll();
    }

    @Override
    public Runnable element() {
        return mDelegate.element();
    }

    @Override
    public Runnable peek() {
        return mDelegate.peek();
    }

    @Override
    public void put(@Nonnull final Runnable runnable) throws InterruptedException {
        mDelegate.put(runnable);
    }

    @Override
    public boolean offer(
            final Runnable runnable,
            final long timeout,
            @Nonnull final TimeUnit unit) throws InterruptedException {
        return mDelegate.offer(runnable, timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return mDelegate.remainingCapacity();
    }

    @Override
    public boolean remove(final Object o) {
        return mDelegate.remove(o);
    }

    @Override
    public boolean containsAll(@Nonnull final Collection<?> c) {
        return mDelegate.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull final Collection<? extends Runnable> c) {
        return mDelegate.addAll(c);
    }

    @Override
    public boolean removeAll(@Nonnull final Collection<?> c) {
        return mDelegate.removeAll(c);
    }

    @Override
    public boolean retainAll(@Nonnull final Collection<?> c) {
        return mDelegate.retainAll(c);
    }

    @Override
    public void clear() {
        mDelegate.clear();
    }

    @Override
    public int size() {
        return mDelegate.size();
    }

    @Override
    public boolean isEmpty() {
        return mDelegate.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return mDelegate.contains(o);
    }

    @Nonnull
    @Override
    public Iterator<Runnable> iterator() {
        return mDelegate.iterator();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return mDelegate.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull final T[] a) {
        return mDelegate.toArray(a);
    }

    @Override
    public int drainTo(@Nonnull final Collection<? super Runnable> c) {
        return mDelegate.drainTo(c);
    }

    @Override
    public int drainTo(
            @Nonnull final Collection<? super Runnable> c,
            final int maxElements) {
        return mDelegate.drainTo(c, maxElements);
    }
}
