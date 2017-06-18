/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;

import java.net.NetworkInterface;
import java.util.Collection;

import javax.annotation.Nonnull;

/**
 * ControlPointのテストを用意にするためのDependency injection
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class ControlPointFactory {
    @Nonnull
    SsdpSearchServerList createSsdpSearchServerList(
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final ResponseListener listener) {
        return new SsdpSearchServerList(interfaces, listener);
    }

    @Nonnull
    SsdpNotifyReceiverList createSsdpNotifyReceiverList(
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        return new SsdpNotifyReceiverList(interfaces, listener);
    }

    @Nonnull
    EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
        return new EventReceiver(listener);
    }
}
