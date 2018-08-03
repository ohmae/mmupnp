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

import java.io.IOException;

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

    @Test(expected = IOException.class)
    public void subscribe() throws Exception {
        final Service service = new EmptyService();
        service.subscribe();
    }

    @Test(expected = IOException.class)
    public void subscribe1() throws Exception {
        final Service service = new EmptyService();
        service.subscribe(true);
    }

    @Test(expected = IOException.class)
    public void renewSubscribe() throws Exception {
        final Service service = new EmptyService();
        service.renewSubscribe();
    }

    @Test(expected = IOException.class)
    public void unsubscribe() throws Exception {
        final Service service = new EmptyService();
        service.unsubscribe();
    }

    @Test
    public void expired() {
        final Service service = new EmptyService();
        service.expired();
    }

    @Test
    public void getSubscriptionId() {
        final Service service = new EmptyService();
        assertThat(service.getSubscriptionId(), is(nullValue()));
    }

    @Test
    public void getSubscriptionStart() {
        final Service service = new EmptyService();
        assertThat(service.getSubscriptionStart(), is(0L));
    }

    @Test
    public void getSubscriptionTimeout() {
        final Service service = new EmptyService();
        assertThat(service.getSubscriptionTimeout(), is(0L));
    }

    @Test
    public void getSubscriptionExpiryTime() {
        final Service service = new EmptyService();
        assertThat(service.getSubscriptionExpiryTime(), is(0L));
    }
}
