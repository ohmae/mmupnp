/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import javax.annotation.Nullable;

/**
 * StringのKey/Valueのペアを保持するImmutableなクラス。
 *
 * <p>{@code Pair<String, String>}のエイリアス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 * @see Pair
 */
public class StringPair extends Pair<String, String> {
    /**
     * 初期値を指定してインスタンス化する。
     *
     * @param key   Keyの値
     * @param value Valueの値
     */
    public StringPair(final @Nullable String key, final @Nullable String value) {
        super(key, value);
    }
}
