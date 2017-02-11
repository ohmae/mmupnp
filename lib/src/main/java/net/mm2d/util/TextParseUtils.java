/*
 * Copyright(C) 2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import javax.annotation.Nullable;

/**
 * テキストパース関係のユーティリティメソッドを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class TextParseUtils {
    /**
     * 文字列をint値としてパースする。パースできなかった場合はデフォルト値を返す。
     *
     * @param string       パースする文字列
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static int parseIntSafely(@Nullable String string, int defaultValue) {
        if (TextUtils.isEmpty(string)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(string);
        } catch (final NumberFormatException ignored) {
        }
        return defaultValue;
    }

    /**
     * 文字列をlong値としてパースする。パースできなかった場合はデフォルト値を返す。
     *
     * @param string       パースする文字列
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static long parseLongSafely(@Nullable String string, long defaultValue) {
        if (TextUtils.isEmpty(string)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(string);
        } catch (final NumberFormatException ignored) {
        }
        return defaultValue;
    }
}
