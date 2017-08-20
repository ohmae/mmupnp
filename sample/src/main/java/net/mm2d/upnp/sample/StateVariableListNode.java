/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.StateVariable;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class StateVariableListNode extends UpnpNode {
    public StateVariableListNode(@Nonnull final List<StateVariable> variables) {
        super("StateVariable");
        for (final StateVariable variable : variables) {
            add(new StateVariableNode(variable));
        }
    }
}
