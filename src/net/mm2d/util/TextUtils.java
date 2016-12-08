/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import javax.annotation.Nullable;

/**
 * Text関係のよく使う機能を実装する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class TextUtils {
    /**
     * 文字列が空かどうかを検査する。
     *
     * @param string 検査する文字列
     * @return 引数がnullもしくは空の場合にtrue
     */
    public static boolean isEmpty(@Nullable String string) {
        return string == null || string.length() == 0;
    }
}
