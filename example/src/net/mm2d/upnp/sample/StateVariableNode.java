/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.StateVariable;

import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StateVariableNode extends UpnpNode {
    public StateVariableNode(StateVariable variable) {
        super(variable);
        setAllowsChildren(false);
    }

    @Override
    public String getDetailText() {
        final StateVariable o = (StateVariable) getUserObject();
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(o.getName());
        sb.append('\n');
        sb.append("SendEvents: ");
        sb.append(o.isSendEvents() ? "yes" : "no");
        sb.append('\n');
        sb.append("Multicast: ");
        sb.append(o.isMulticast() ? "yes" : "no");
        sb.append('\n');
        sb.append("DataType: ");
        sb.append(o.getDataType());
        final List<String> list = o.getAllowedValueList();
        if (list.size() > 0) {
            sb.append('\n');
            sb.append("AllowedValue:");
            for (final String v : list) {
                sb.append('\n');
                sb.append('\t');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StateVariable o = (StateVariable) getUserObject();
        return o.getName();
    }
}
