/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Argument;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ArgumentNode extends UpnpNode {
    public ArgumentNode(final Argument argument) {
        super(argument);
        setAllowsChildren(false);
    }

    @Override
    public Argument getUserObject() {
        return (Argument) super.getUserObject();
    }

    @Override
    public String formatDescription() {
        return Formatter.format(getUserObject());
    }

    @Override
    public String toString() {
        return getUserObject().getName();
    }
}
