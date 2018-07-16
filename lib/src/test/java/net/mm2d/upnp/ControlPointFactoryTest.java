/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.NetworkUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class ControlPointFactoryTest {
    @Test
    public void constructor_引数無しでコール() throws Exception {
        ControlPointFactory.create();
    }

    @Test
    public void constructor_インターフェース指定() throws Exception {
        ControlPointFactory.create(NetworkUtils.getAvailableInet4Interfaces());
    }

    @Test
    public void constructor_インターフェース選別() throws Exception {
        ControlPointFactory.create(Protocol.IP_V4_ONLY, NetworkUtils.getNetworkInterfaceList());
        ControlPointFactory.create(Protocol.IP_V6_ONLY, NetworkUtils.getNetworkInterfaceList());
        ControlPointFactory.create(Protocol.DUAL_STACK, NetworkUtils.getNetworkInterfaceList());
    }
}
