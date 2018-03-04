/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.StateVariable;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ActionNode extends UpnpNode {
    public ActionNode(final Action action) {
        super(action);
        final List<Argument> arguments = action.getArgumentList();
        for (final Argument argument : arguments) {
            add(new ArgumentNode(argument));
        }
    }

    @Override
    public Action getUserObject() {
        return (Action) super.getUserObject();
    }

    @Override
    public String getDetailText() {
        final Action action = getUserObject();
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(action.getName());
        sb.append('\n');
        final List<Argument> args = action.getArgumentList();
        for (final Argument arg : args) {
            final StateVariable v = arg.getRelatedStateVariable();
            sb.append(arg.isInputDirection() ? "in:" : "out:");
            sb.append("(");
            sb.append(v.getDataType());
            sb.append(")");
            sb.append(arg.getName());
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getUserObject().getName();
    }

    @Override
    public void showContextMenu(
            final JFrame frame,
            final Component invoker,
            final int x,
            final int y) {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem invoke = new JMenuItem("Invoke Action");
        invoke.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                new ActionWindow(getUserObject()).show(frame.getX() + x, frame.getY() + y);
            }
        });
        menu.add(invoke);
        menu.show(invoker, x, y);
    }
}
