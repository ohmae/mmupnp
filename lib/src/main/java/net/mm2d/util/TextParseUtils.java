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
     * 与えられた文字列を10進数としてint値にパースする。
     *
     * <p>パースできなかった場合はデフォルト値を返す。
     *
     * @param value        パースする文字列
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static int parseIntSafely(@Nullable final String value, final int defaultValue) {
        return parseIntSafely(value, 10, defaultValue);
    }

    /**
     * 与えられた文字列をradix進数としてint値にパースする。
     *
     * <p>パースできなかった場合はデフォルト値を返す。
     *
     * @param value        パースする文字列
     * @param radix        パースする文字列の基数
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static int parseIntSafely(@Nullable final String value, final int radix, final int defaultValue) {
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value, radix);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 与えられた文字列を10進数としてlong値にパースする。
     *
     * <p>パースできなかった場合はデフォルト値を返す。
     *
     * @param value        パースする文字列
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static long parseLongSafely(@Nullable final String value, final long defaultValue) {
        return parseLongSafely(value, 10, defaultValue);
    }

    /**
     * 与えられた文字列をradix進数としてint値にパースする。
     *
     * <p>パースできなかった場合はデフォルト値を返す。
     *
     * @param value        パースする文字列
     * @param radix        パースする文字列の基数
     * @param defaultValue デフォルト値
     * @return パース結果
     */
    public static long parseLongSafely(@Nullable final String value, final int radix, final long defaultValue) {
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value, radix);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }
}
