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
        return "ServiceType: " +
                service.getServiceType() +
                '\n' +
                "ServiceId: " +
                service.getServiceId() +
                '\n' +
                "ScpdUrl: " +
                service.getScpdUrl() +
                '\n' +
                "ControlUrl: " +
                service.getControlUrl() +
                '\n' +
                "EventSubUrl: " +
                service.getEventSubUrl();
    }

    @Override
    public String getDetailXml() {
        final Service service = (Service) getUserObject();
        return formatXml(service.getDescription());
    }

    @Override
    public String toString() {
        final Service o = (Service) getUserObject();
        return o.getServiceType();
    }
}
