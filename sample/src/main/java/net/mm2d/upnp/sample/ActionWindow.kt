/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.io.IOException
import javax.swing.*
import javax.swing.text.JTextComponent

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ActionWindow(private val action: Action) : JFrame(action.name) {
    private val map = mutableMapOf<Argument, Container>()
    private val errorMessage: JLabel

    private interface Container {
        var value: String
    }

    private class TextContainer(
        private val component: JTextComponent
    ) : Container {
        override var value: String
            get() = component.text
            set(string) {
                component.text = string
            }
    }

    private class ComboBoxContainer(
        private val box: JComboBox<String>
    ) : Container {
        override var value: String
            get() = box.selectedItem as String
            set(string) {
                box.selectedItem = string
            }
    }

    init {
        contentPane.add(makeControlPanel(), BorderLayout.NORTH)
        errorMessage = JLabel()
        contentPane.add(errorMessage, BorderLayout.SOUTH)
        val panel = JPanel().also {
            it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
            action.argumentList.forEach { argument ->
                it.add(JSeparator(SwingConstants.HORIZONTAL))
                it.add(makeArgumentPanel(argument))
            }
        }
        contentPane.add(JScrollPane(panel), BorderLayout.CENTER)
    }

    private fun makeControlPanel(): JPanel = JPanel().also {
        it.layout = FlowLayout()
        it.add(JButton("Invoke").also { button ->
            button.addActionListener { invokeAction() }
        })
    }

    private fun invokeAction() {
        try {
            setResult(action.invokeSync(makeArgument(), true))
        } catch (e: IOException) {
            e.printStackTrace()
            errorMessage.text = e.message
        }
    }

    private fun setResult(result: Map<String, String>) {
        if (result.containsKey(Action.ERROR_CODE_KEY)) {
            if (result.containsKey(Action.ERROR_DESCRIPTION_KEY)) {
                errorMessage.text = "error:${result[Action.ERROR_CODE_KEY]} ${result[Action.ERROR_DESCRIPTION_KEY]}"
            } else {
                errorMessage.text = "error:${result[Action.ERROR_CODE_KEY]}"
            }
        } else {
            updateResult(result)
        }
    }

    private fun makeArgument(): Map<String, String> {
        return map.filterKeys { it.isInputDirection }
            .map { it.key.name to it.value.value }
            .toMap()
    }

    private fun updateResult(result: Map<String, String>) {
        result.forEach { (key, value) ->
            setResult(key, value)
        }
    }

    private fun setResult(name: String, value: String) {
        for ((key, value1) in map) {
            if (key.isInputDirection) {
                continue
            }
            if (name == key.name) {
                value1.value = value
                break
            }
        }
    }

    private fun makeArgumentPanel(argument: Argument): JPanel {
        return JPanel().also {
            it.layout = BorderLayout()
            it.add(JLabel("(${argument.relatedStateVariable.dataType}) ${argument.name}"), BorderLayout.NORTH)
            it.add(makeComponent(argument), BorderLayout.CENTER)
        }
    }

    private fun makeComponent(argument: Argument): JComponent {
        val variable = argument.relatedStateVariable
        return when {
            variable.allowedValueList.isNotEmpty() -> makeComboBox(argument)
            DataType.of(variable.dataType).isMultiLine -> makeTextArea(argument)
            else -> makeTextField(argument)
        }
    }

    private fun makeComboBox(argument: Argument): JComponent {
        val variable = argument.relatedStateVariable
        return JComboBox(variable.allowedValueList.toTypedArray()).also {
            if (variable.defaultValue != null) {
                it.selectedItem = variable.defaultValue
            }
            map[argument] = ComboBoxContainer(it)
        }
    }

    private fun makeTextArea(argument: Argument): JComponent = JScrollPane(JTextArea().also {
        it.lineWrap = true
        if (!argument.isInputDirection) {
            it.background = Color(0xeeeeee)
        }
        val variable = argument.relatedStateVariable
        if (variable.defaultValue != null) {
            it.text = variable.defaultValue
        } else if (argument.isInputDirection) {
            it.text = DataType.of(variable.dataType).default
        }
        map[argument] = TextContainer(it)
    })

    private fun makeTextField(argument: Argument): JComponent = JTextField().also {
        if (!argument.isInputDirection) {
            it.background = Color(0xeeeeee)
        }
        val variable = argument.relatedStateVariable
        if (variable.defaultValue != null) {
            it.text = variable.defaultValue
        } else if (argument.isInputDirection) {
            it.text = DataType.of(variable.dataType).default
        }
        map[argument] = TextContainer(it)
    }

    fun show(x: Int, y: Int) {
        setSize(400, 800)
        setLocation(x, y)
        isVisible = true
    }
}
