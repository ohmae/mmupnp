/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Argumentを表現するインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Argument {
    /**
     * Argument名を返す。
     *
     * @return Argument名
     */
    val name: String

    /**
     * Input方向か否かを返す。
     *
     * @return Inputの場合true
     */
    val isInputDirection: Boolean

    /**
     * RelatedStateVariableで指定されたStateVariableのインスタンスを返す。
     *
     * @return StateVariableのインスタンス
     */
    val relatedStateVariable: StateVariable
}
