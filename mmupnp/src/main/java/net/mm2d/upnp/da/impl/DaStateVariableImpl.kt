/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.impl

import net.mm2d.upnp.da.DaStateVariable
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

class DaStateVariableImpl(
    override val isSendEvents: Boolean,
    override val isMulticast: Boolean,
    override val name: String,
    override val dataType: String,
    override val allowedValueList: List<String>,
    override val defaultValue: String?,
    override val minimum: String?,
    override val maximum: String?,
    override val step: String?
) : DaStateVariable {
    fun toElement(document: Document): Element =
        document.createElement("stateVariable").also { element ->
            element.setAttribute("sendEvents", if (isSendEvents) "yes" else "no")
            if (isMulticast) {
                element.setAttribute("multicast", "yes")
            }
            element.appendChildNode("name", name)
            element.appendChildNode("dataType", dataType)
        }

    internal class Builder {
        private var sendEvents = true
        private var multicast = false
        private var name: String? = null
        private var dataType: String? = null
        private val allowedValueList = mutableListOf<String>()
        private var defaultValue: String? = null
        private var minimum: String? = null
        private var maximum: String? = null
        private var step: String? = null

        fun build(): DaStateVariableImpl {
            val name = name
                ?: throw IllegalStateException("name must be set.")
            val dataType = dataType
                ?: throw IllegalStateException("dataType must be set.")

            return DaStateVariableImpl(
                isSendEvents = sendEvents,
                isMulticast = multicast,
                name = name,
                dataType = dataType,
                allowedValueList = allowedValueList,
                defaultValue = defaultValue,
                minimum = minimum,
                maximum = maximum,
                step = step
            )
        }

        fun setSendEvents(sendEvents: String): Builder = apply {
            this.sendEvents = !sendEvents.equals("no", ignoreCase = true)
        }

        fun setMulticast(multicast: String): Builder = apply {
            this.multicast = multicast.equals("yes", ignoreCase = true)
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        fun setDataType(dataType: String): Builder = apply {
            this.dataType = dataType
        }

        fun addAllowedValue(value: String): Builder = apply {
            allowedValueList.add(value)
        }

        fun setDefaultValue(defaultValue: String): Builder = apply {
            this.defaultValue = defaultValue
        }

        fun setMinimum(minimum: String): Builder = apply {
            this.minimum = minimum
        }

        fun setMaximum(maximum: String): Builder = apply {
            this.maximum = maximum
        }

        fun setStep(step: String): Builder = apply {
            this.step = step
        }
    }
}

fun Node.createElement(tagName: String): Element =
    ownerDocument.createElement(tagName)

fun Node.appendChildNode(tagName: String, textContent: String): Node {
    appendChild(
        createElement(tagName).also {
            it.textContent = textContent
        }
    )
    return this
}
