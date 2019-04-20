/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Deviceのロード時にIconのバイナリデータをダウンロードするIconを指定するためのフィルター。
 *
 * デフォルトでは何もダウンロードしない[.NONE]が設定されている。
 * すべてをダウンロードする場合は[.ALL]が定義されているためそれを利用する。
 * 特定のIconだけをダウンロードしたり、特定の条件でのみダウンロードする場合は、
 * このインターフェースを実装し、[ControlPoint.setIconFilter]に渡す。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface IconFilter {
    /**
     * ダウンロード時にバイナリデータをダウンロードするIconを選択する。
     *
     * 戻り値で返したListに含まれるIconがダウンロードされる。
     * Iconの情報がない場合はコールされない。
     *
     * @param list Deviceに記述されたIconのリスト、nullやemptyでコールされることはない。
     * @return ダウンロードするIconのリスト、nullを返してはならない。
     * 取得すべきiconがない場合はemptyListを返す必要がある。
     */
    operator fun invoke(list: List<Icon>): List<Icon>
}
