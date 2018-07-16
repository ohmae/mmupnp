/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;

/**
 * Argumentを表現するインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface Argument {
    /**
     * このArgumentを保持するActionを返す。
     *
     * @return このArgumentを保持するAction
     */
    @Nonnull
    Action getAction();

    /**
     * Argument名を返す。
     *
     * @return Argument名
     */
    @Nonnull
    String getName();

    /**
     * Input方向か否かを返す。
     *
     * @return Inputの場合true
     */
    boolean isInputDirection();

    /**
     * RelatedStateVariableで指定されたStateVariableのインスタンスを返す。
     *
     * @return StateVariableのインスタンス
     */
    @Nonnull
    StateVariable getRelatedStateVariable();
}
