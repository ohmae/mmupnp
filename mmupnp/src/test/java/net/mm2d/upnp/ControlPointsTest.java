/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.empty.EmptyAction;
import net.mm2d.upnp.empty.EmptyControlPoint;
import net.mm2d.upnp.empty.EmptyDevice;
import net.mm2d.upnp.empty.EmptyService;
import net.mm2d.upnp.empty.EmptySsdpMessage;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ControlPointsTest {

    @Test
    public void emptyControlPoint() {
        assertThat(ControlPoints.emptyControlPoint(), is(instanceOf(EmptyControlPoint.class)));
    }

    @Test
    public void emptyDevice() {
        assertThat(ControlPoints.emptyDevice(), is(instanceOf(EmptyDevice.class)));
    }

    @Test
    public void emptyService() {
        assertThat(ControlPoints.emptyService(), is(instanceOf(EmptyService.class)));
    }

    @Test
    public void emptyAction() {
        assertThat(ControlPoints.emptyAction(), is(instanceOf(EmptyAction.class)));
    }

    @Test
    public void emptySsdpMessage() {
        assertThat(ControlPoints.emptySsdpMessage(), is(instanceOf(EmptySsdpMessage.class)));
    }
}
