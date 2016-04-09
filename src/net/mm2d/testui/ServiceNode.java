/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.testui;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.StateVariable;

import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ServiceNode extends UpnpNode {
    public ServiceNode(Service service) {
        super(service);
        final List<Action> actions = service.getActionList();
        for (final Action action : actions) {
            add(new ActionNode(action));
        }
        final UpnpNode node = new UpnpNode("StateVariable");
        add(node);
        final List<StateVariable> variables = service.getStateVariableList();
        for (final StateVariable variable : variables) {
            node.add(new StateVariableNode(variable));
        }
    }

    @Override
    public String getDetailText() {
        final Service service = (Service) getUserObject();
        final StringBuilder sb = new StringBuilder();
        sb.append("ServiceType: ");
        sb.append(service.getServiceType());
        sb.append('\n');
        sb.append("ServiceId: ");
        sb.append(service.getServiceId());
        sb.append('\n');
        sb.append("ScpdUrl: ");
        sb.append(service.getScpdUrl());
        sb.append('\n');
        sb.append("ControlUrl: ");
        sb.append(service.getControlUrl());
        sb.append('\n');
        sb.append("EventSubUrl: ");
        sb.append(service.getEventSubUrl());
        return sb.toString();
    }

    @Override
    public String getDetailXml() {
        final Service service = (Service) getUserObject();
        return formatXml(service.getServiceXml());
    }

    @Override
    public String toString() {
        final Service o = (Service) getUserObject();
        return o.getServiceType();
    }
}
