/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Text関係のユーティリティメソッドを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public final class TextUtils {
    /**
     * nullを許容し、Stringが空かどうかを検査する。
     *
     * @param string 検査する文字列
     * @return 引数がnullもしくは空の場合にtrue
     */
    public static boolean isEmpty(@Nullable final CharSequence string) {
        return string == null || string.length() == 0;
    }

    /**
     * nullを許容し、2つのStringが同一かを検査する。
     *
     * @param a 比較対象
     * @param b 比較対象
     * @return 2つの引数が等しいときtrue、2つともnullの場合もtrueを返す。
     */
    public static boolean equals(
            @Nullable final CharSequence a,
            @Nullable final CharSequence b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * nullを許容し、英語ロケール限定でLowerCaseに変換する。
     *
     * @param string 変換元のString
     * @return LowerCaseへ変換されたString
     */
    @Nullable
    public static String toLowerCase(@Nullable final String string) {
        return string == null ? null : string.toLowerCase(Locale.ENGLISH);
    }

    /**
     * nullを許容し、英語ロケール限定でUpperCaseに変換する。
     *
     * @param string 変換元のString
     * @return LowerCaseへ変換されたString
     */
    @Nullable
    public static String toUpperCase(@Nullable final String string) {
        return string == null ? null : string.toUpperCase(Locale.ENGLISH);
    }

    // インスタンス化禁止
    private TextUtils() {
    }
}
