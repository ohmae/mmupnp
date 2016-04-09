/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.testui;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.StateVariable;

import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ActionNode extends UpnpNode {
    public ActionNode(Action action) {
        super(action);
        final List<Argument> arguments = action.getArgumentList();
        for (final Argument argument : arguments) {
            add(new ArgumentNode(argument));
        }
    }

    @Override
    public String getDetailText() {
        final Action action = (Action) getUserObject();
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
        final Action o = (Action) getUserObject();
        return o.getName();
    }
}
