/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Action;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class EmptyActionTest {

    @Test
    public void getService() {
        final Action action = new EmptyAction();
        assertThat(action.getService(), is(not(nullValue())));
    }

    @Test
    public void getName() {
        final Action action = new EmptyAction();
        assertThat(action.getName(), is(not(nullValue())));
    }

    @Test
    public void getArgumentList() {
        final Action action = new EmptyAction();
        assertThat(action.getArgumentList(), is(not(nullValue())));
    }

    @Test
    public void findArgument() {
        final Action action = new EmptyAction();
        assertThat(action.findArgument(""), is(nullValue()));
    }

    @Test(expected = IOException.class)
    public void invokeSync() throws Exception {
        final Action action = new EmptyAction();
        action.invokeSync(Collections.emptyMap());
    }

    @Test(expected = IOException.class)
    public void invokeSync1() throws Exception {
        final Action action = new EmptyAction();
        action.invokeSync(Collections.emptyMap(), false);
    }

    @Test(expected = IOException.class)
    public void invokeCustomSync() throws Exception {
        final Action action = new EmptyAction();
        action.invokeCustomSync(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test(expected = IOException.class)
    public void invokeCustomSync1() throws Exception {
        final Action action = new EmptyAction();
        action.invokeCustomSync(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
    }

    @Test
    public void invoke() {
        final Action action = new EmptyAction();
        action.invoke(Collections.emptyMap(), null);
    }

    @Test
    public void invoke1() {
        final Action action = new EmptyAction();
        action.invoke(Collections.emptyMap(), false, null);
    }

    @Test
    public void invokeCustom() {
        final Action action = new EmptyAction();
        action.invokeCustom(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
    }

    @Test
    public void invokeCustom1() {
        final Action action = new EmptyAction();
        action.invokeCustom(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false, null);
    }
}
