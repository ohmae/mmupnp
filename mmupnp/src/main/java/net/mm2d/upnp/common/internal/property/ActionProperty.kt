/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

/**
 * Property of UPnP Action.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ActionProperty(
    /**
     * Return the Action name.
     *
     * @return Action name
     */
    val name: String,

    /**
     * Return the Argument list.
     *
     * List is the immutable.
     * When the modification method is called, UnsupportedOperationException will be thrown.
     *
     * @return Argument list
     */
    val argumentList: List<ArgumentProperty> = emptyList()
) {
    class Builder {
        var name: String? = null
        val argumentBuilderList: MutableList<ArgumentProperty.Builder> = mutableListOf()

        fun build(stateVariableList: List<StateVariableProperty>): ActionProperty {
            val name = checkNotNull(name) { "name must be set." }
            return ActionProperty(
                name = name,
                argumentList = argumentBuilderList.map { it.build(stateVariableList) }.toList()
            )
        }
    }
}
