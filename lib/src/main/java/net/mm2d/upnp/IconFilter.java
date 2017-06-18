/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Deviceのロード時にIconのバイナリデータをダウンロードするIconを指定するためのフィルター。
 *
 * <p>デフォルトでは何もダウンロードしない{@link #NONE}が設定されている。
 * すべてをダウンロードする場合は{@link #ALL}が定義されているためそれを利用する。
 * 特定のIconだけをダウンロードしたり、特定の条件でのみダウンロードする場合は、
 * このインターフェースを実装し、{@link ControlPoint#setIconFilter(IconFilter)}に渡す。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public interface IconFilter {
    /**
     * ダウンロードしない場合に指定する。デフォルト値。
     */
    IconFilter NONE = new IconFilter() {
        @Override
        @Nonnull
        public List<Icon> filter(@Nonnull final List<Icon> list) {
            return Collections.emptyList();
        }
    };
    /**
     * すべてをダウンロードする場合に指定する。
     */
    IconFilter ALL = new IconFilter() {
        @Override
        @Nonnull
        public List<Icon> filter(@Nonnull final List<Icon> list) {
            return list;
        }
    };

    /**
     * ダウンロード時にバイナリデータをダウンロードするIconを選択する。
     *
     * <p>戻り値で返したListに含まれるIconがダウンロードされる。
     * Iconの情報がない場合はコールされない。
     *
     * @param list Deviceに記述されたIconのリスト、nullやemptyでコールされることはない。
     * @return ダウンロードするIconのリスト、nullを返してはならない。
     * 取得すべきiconがない場合はemptyListを返す必要がある。
     */
    @Nonnull
    List<Icon> filter(@Nonnull List<Icon> list);
}
