/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class EmptyServiceTest {
    @Test
    public void getDevice() {
        final Service service = new EmptyService();
        assertThat(service.getDevice(), is(not(nullValue())));
    }

    @Test
    public void getServiceType() {
        final Service service = new EmptyService();
        assertThat(service.getServiceType(), is(not(nullValue())));
    }

    @Test
    public void getServiceId() {
        final Service service = new EmptyService();
        assertThat(service.getServiceId(), is(not(nullValue())));
    }

    @Test
    public void getScpdUrl() {
        final Service service = new EmptyService();
        assertThat(service.getScpdUrl(), is(not(nullValue())));
    }

    @Test
    public void getControlUrl() {
        final Service service = new EmptyService();
        assertThat(service.getControlUrl(), is(not(nullValue())));
    }

    @Test
    public void getEventSubUrl() {
        final Service service = new EmptyService();
        assertThat(service.getEventSubUrl(), is(not(nullValue())));
    }

    @Test
    public void getDescription() {
        final Service service = new EmptyService();
        assertThat(service.getDescription(), is(not(nullValue())));
    }

    @Test
    public void getActionList() {
        final Service service = new EmptyService();
        assertThat(service.getActionList(), is(not(nullValue())));
    }

    @Test
    public void findAction() {
        final Service service = new EmptyService();
        assertThat(service.findAction(""), is(nullValue()));
    }

    @Test
    public void getStateVariableList() {
        final Service service = new EmptyService();
        assertThat(service.getStateVariableList(), is(not(nullValue())));
    }

    @Test
    public void findStateVariable() {
        final Service service = new EmptyService();
        assertThat(service.findStateVariable(""), is(nullValue()));
    }

    @Test
    public void subscribe() {
        final Service service = new EmptyService();
        assertThat(service.subscribeSync(), is(false));
    }

    @Test
    public void subscribe1() {
        final Service service = new EmptyService();
        assertThat(service.subscribeSync(true), is(false));
    }

    @Test
    public void renewSubscribe() {
        final Service service = new EmptyService();
        assertThat(service.renewSubscribeSync(), is(false));
    }

    @Test
    public void unsubscribe() {
        final Service service = new EmptyService();
        assertThat(service.unsubscribeSync(), is(false));
    }

    @Test
    public void getSubscriptionId() {
        final Service service = new EmptyService();
        assertThat(service.getSubscriptionId(), is(nullValue()));
    }
}
