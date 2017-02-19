/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

/**
 * StringのKey/Valueのペアを保持するImmutableなクラス。
 *
 * <p>{@code Pair<String, String>}のエイリアス
 *
 * @see Pair
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StringPair extends Pair<String, String> {
    /**
     * 初期値を指定してインスタンス化する。
     *
     * @param key   Keyの値
     * @param value Valueの値
     */
    public StringPair(String key, String value) {
        super(key, value);
    }
}
