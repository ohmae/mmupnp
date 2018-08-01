/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.StateVariable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ActionWindow extends JFrame {
    private final Action mAction;
    private final Map<Argument, Container> mMap = new HashMap<>();
    private final JLabel mErrorMessage;

    private interface Container {
        String getValue();

        void setValue(String string);
    }

    private static class TextContainer implements Container {
        private final JTextComponent mJTextComponent;

        TextContainer(final JTextComponent component) {
            mJTextComponent = component;
        }

        @Override
        public String getValue() {
            return mJTextComponent.getText();
        }

        @Override
        public void setValue(final String string) {
            mJTextComponent.setText(string);
        }
    }

    private static class ComboBoxContainer implements Container {
        private final JComboBox<String> mJComboBox;

        ComboBoxContainer(final JComboBox<String> component) {
            mJComboBox = component;
        }

        @Override
        public String getValue() {
            return (String) mJComboBox.getSelectedItem();
        }

        @Override
        public void setValue(final String string) {
            mJComboBox.setSelectedItem(string);
        }
    }

    private JPanel makeControlPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        final JButton invoke = new JButton("Invoke");
        invoke.addActionListener(e -> {
            try {
                final Map<String, String> result = mAction.invoke(makeArgument(), true);
                if (result.containsKey(Action.ERROR_CODE_KEY)) {
                    if (result.containsKey(Action.ERROR_DESCRIPTION_KEY)) {
                        mErrorMessage.setText("error:" + result.get(Action.ERROR_CODE_KEY)
                                + " " + result.get(Action.ERROR_DESCRIPTION_KEY));
                    } else {
                        mErrorMessage.setText("error:" + result.get(Action.ERROR_CODE_KEY));
                    }
                    return;
                }
                updateResult(mAction.invoke(makeArgument()));
            } catch (final IOException e1) {
                e1.printStackTrace();
                mErrorMessage.setText(e1.getMessage());
            }
        });
        panel.add(invoke);
        return panel;
    }

    private Map<String, String> makeArgument() {
        final Map<String, String> argument = new HashMap<>();
        for (final Map.Entry<Argument, Container> entry : mMap.entrySet()) {
            if (entry.getKey().isInputDirection()) {
                argument.put(entry.getKey().getName(), entry.getValue().getValue());
            }
        }
        return argument;
    }

    private void updateResult(final Map<String, String> result) {
        for (final Entry<String, String> entry : result.entrySet()) {
            setResult(entry.getKey(), entry.getValue());
        }
    }

    private void setResult(
            final String name,
            final String value) {
        for (final Map.Entry<Argument, Container> entry : mMap.entrySet()) {
            if (entry.getKey().isInputDirection()) {
                continue;
            }
            if (name.equals(entry.getKey().getName())) {
                entry.getValue().setValue(value);
                break;
            }
        }
    }

    public ActionWindow(final Action action) {
        super(action.getName());
        getContentPane().add(makeControlPanel(), BorderLayout.NORTH);
        mErrorMessage = new JLabel();
        getContentPane().add(mErrorMessage, BorderLayout.SOUTH);
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
        mAction = action;
        for (final Argument argument : action.getArgumentList()) {
            panel.add(new JSeparator(SwingConstants.HORIZONTAL));
            panel.add(makeArgumentPanel(argument));
        }
    }

    private JPanel makeArgumentPanel(final Argument argument) {
        final StateVariable variable = argument.getRelatedStateVariable();
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("(" + variable.getDataType() + ") " + argument.getName()), BorderLayout.NORTH);

        if (!variable.getAllowedValueList().isEmpty()) {
            panel.add(makeComboBox(argument), BorderLayout.CENTER);
            return panel;
        }

        final DataType type = DataType.of(variable.getDataType());
        if (type.isMultiLine()) {
            panel.add(makeTextArea(argument), BorderLayout.CENTER);
            return panel;
        }

        panel.add(makeTextField(argument), BorderLayout.CENTER);
        return panel;
    }

    @Nonnull
    private JComponent makeComboBox(final Argument argument) {
        final StateVariable variable = argument.getRelatedStateVariable();
        final List<String> allowedValueList = variable.getAllowedValueList();
        final JComboBox<String> comboBox = new JComboBox<>(allowedValueList.toArray(new String[0]));
        if (variable.getDefaultValue() != null) {
            comboBox.setSelectedItem(variable.getDefaultValue());
        }
        mMap.put(argument, new ComboBoxContainer(comboBox));
        return comboBox;
    }

    @Nonnull
    private JComponent makeTextArea(final Argument argument) {
        final StateVariable variable = argument.getRelatedStateVariable();
        final DataType type = DataType.of(variable.getDataType());

        final JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        if (!argument.isInputDirection()) {
            area.setBackground(new Color(0xeeeeee));
        }
        if (variable.getDefaultValue() != null) {
            area.setText(variable.getDefaultValue());
        } else if (argument.isInputDirection()) {
            area.setText(type.getDefault());
        }
        mMap.put(argument, new TextContainer(area));
        return new JScrollPane(area);
    }

    @Nonnull
    private JComponent makeTextField(final Argument argument) {
        final StateVariable variable = argument.getRelatedStateVariable();
        final DataType type = DataType.of(variable.getDataType());

        final JTextField field = new JTextField();
        if (!argument.isInputDirection()) {
            field.setBackground(new Color(0xeeeeee));
        }
        if (variable.getDefaultValue() != null) {
            field.setText(variable.getDefaultValue());
        } else if (argument.isInputDirection()) {
            field.setText(type.getDefault());
        }
        mMap.put(argument, new TextContainer(field));
        return field;
    }


    public void show(
            final int x,
            final int y) {
        setSize(400, 800);
        setLocation(x, y);
        setVisible(true);
    }
}
