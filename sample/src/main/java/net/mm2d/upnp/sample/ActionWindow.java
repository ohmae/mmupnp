/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.text.JTextComponent;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
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
        invoke.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    updateResult(mAction.invoke(makeArgument()));
                } catch (final IOException e1) {
                    e1.printStackTrace();
                    mErrorMessage.setText(e1.getMessage());
                }
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

    private void setResult(final String name, final String value) {
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
            if (mMap.size() != 0) {
                panel.add(new JSeparator(SwingConstants.HORIZONTAL));
            }
            final StateVariable variable = argument.getRelatedStateVariable();
            panel.add(new JLabel("(" + variable.getDataType() + ") " + argument.getName()));
            final List<String> allowedValueList = variable.getAllowedValueList();
            if (!allowedValueList.isEmpty()) {
                final JComboBox<String> comboBox = new JComboBox<>(allowedValueList.toArray(new String[allowedValueList.size()]));
                mMap.put(argument, new ComboBoxContainer(comboBox));
                panel.add(comboBox);
                if (variable.getDefaultValue() != null) {
                    comboBox.setSelectedItem(variable.getDefaultValue());
                }
                continue;
            }
            final JTextArea area = new JTextArea();
            area.setBorder(new BevelBorder(BevelBorder.LOWERED));
            area.setLineWrap(true);
            if (!argument.isInputDirection()) {
                area.setBackground(new Color(0xeeeeee));
            }
            if (variable.getDefaultValue() != null) {
                area.setText(variable.getDefaultValue());
            }
            mMap.put(argument, new TextContainer(area));
            panel.add(area);
        }
    }

    public void show(final int x, final int y) {
        setSize(400, 800);
        setLocation(x, y);
        setVisible(true);
    }
}
