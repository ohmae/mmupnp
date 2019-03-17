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

@SuppressWarnings("NonAsciiCharacters")
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
    public void invoke() throws Exception {
        final Action action = new EmptyAction();
        action.invokeSync(Collections.emptyMap());
    }

    @Test(expected = IOException.class)
    public void invoke1() throws Exception {
        final Action action = new EmptyAction();
        action.invokeSync(Collections.emptyMap(), false);
    }

    @Test(expected = IOException.class)
    public void invokeCustom() throws Exception {
        final Action action = new EmptyAction();
        action.invokeCustomSync(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test(expected = IOException.class)
    public void invokeCustom1() throws Exception {
        final Action action = new EmptyAction();
        action.invokeCustomSync(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
    }
}
