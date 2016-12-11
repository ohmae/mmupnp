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
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public interface IconFilter {
    /**
     * ダウンロードしない場合に指定する。デフォルト値。
     */
    IconFilter NONE = new IconFilter() {
        @Override
        @Nonnull
        public List<Icon> filter(@Nonnull List<Icon> list) {
            return Collections.emptyList();
        }
    };
    /**
     * すべてをダウンロードする場合に指定する。
     */
    IconFilter ALL = new IconFilter() {
        @Override
        @Nonnull
        public List<Icon> filter(@Nonnull List<Icon> list) {
            return list;
        }
    };

    /**
     * ダウンロード時にバイナリデータをダウンロードするIconを選択する。
     *
     * 戻り値で返したListに含まれるIconがダウンロードされる。
     *
     * @param list Deviceに記述されたIconのリスト
     * @return ダウンロードするIconのリスト、nullを返してはならない
     */
    @Nonnull
    List<Icon> filter(@Nonnull List<Icon> list);
}
