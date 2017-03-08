/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;

/**
 * HttpClientを作成するファクトリークラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class HttpClientFactory {
    /**
     * HttpClientを作成する。
     *
     * @param keepAlive keep-alive通信を行う場合true
     * @return HttpClientのインスタンス
     * @see HttpClient#HttpClient(boolean)
     */
    @Nonnull
    public HttpClient createHttpClient(final boolean keepAlive) {
        return new HttpClient(keepAlive);
    }
}
