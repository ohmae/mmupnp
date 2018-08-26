/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

class ThreadPool {
    @Nonnull
    private final ExecutorService mSequentialExecutor;
    @Nonnull
    private final ExecutorService mParallelExecutor;

    ThreadPool() {
        this(createSequentialExecutor(), createParallelExecutor());
    }

    private static ExecutorService createSequentialExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    private static ExecutorService createParallelExecutor() {
        // TODO: スレッド数を範囲内で動的に増減させつつ、アイドルスレッドがあればそのスレッドに実行させるThreadPoolを作る
        // - ThreadPoolExecutorはworkQueueにofferを実行しfalseの時にスレッドを増加させるため
        //    - SynchronousQueueではスレッド上限に来たときにrejectされてしまう
        //    - LinkedBlockingQueueでは常にキューイングを行うためスレッド数が変化しない
        // workQueueで細工をするならofferをコールされた際
        // - 読み出し待ちのスレッドがあればキューに積み、trueを返す
        // - 読み出し待ちのスレッドはなく、スレッド数に余裕があればfalse
        // - 読み出し待ちのスレッドはなく、スレッド数に余裕がなければキューに積みtrueを返す
        // スレッド数はExecutor側の情報なので可能なら避けたい
        // - 読み出し待ちのスレッドがあればキューに積み、trueを返す
        // - 読み出し待ちのスレッドがなければfalse
        // というQueueを作り、RejectedExecutionHandler経由でキューに積むほうがよいか？
        return Executors.newFixedThreadPool(calculateMaximumPoolSize());
    }

    private static int calculateMaximumPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    ThreadPool(
            @Nonnull final ExecutorService sequential,
            @Nonnull final ExecutorService parallel) {
        mSequentialExecutor = sequential;
        mParallelExecutor = parallel;
    }

    boolean executeInSequential(@Nonnull final Runnable command) {
        if (mSequentialExecutor.isShutdown()) {
            return false;
        }
        try {
            mSequentialExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    boolean executeInParallel(@Nonnull final Runnable command) {
        if (mParallelExecutor.isShutdown()) {
            return false;
        }
        try {
            mParallelExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    void terminate() {
        mSequentialExecutor.shutdownNow();
        mParallelExecutor.shutdown();
        try {
            if (!mParallelExecutor.awaitTermination(
                    Property.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                mParallelExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Log.w(e);
        }
    }
}
