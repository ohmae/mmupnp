/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

/**
 * コールバック実行のためのExecutorの簡易インターフェース
 *
 * 登録されたListener等の実行が委譲されるため、適切なスレッドで実行する。
 * android.os.Handler#post をメソッドリファレンスで渡すことを想定しており、
 * {@link TaskExecutor}と比較して、terminateメソッドがないため、終了トリガを受け取ることはできない。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface CallbackHandler {
    /**
     * Runnableを実行する。
     *
     * @param callback 実行すべきcallback
     * @return 実行キューに積むことができたときtrue
     */
    boolean execute(Runnable callback);
}
