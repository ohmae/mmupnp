/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;

import java.awt.Component;
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
    public String formatDescription() {
        return Formatter.format(getUserObject());
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
        invoke.addActionListener(e -> new ActionWindow(getUserObject()).show(frame.getX() + x, frame.getY() + y));
        menu.add(invoke);
        menu.show(invoker, x, y);
    }
}
