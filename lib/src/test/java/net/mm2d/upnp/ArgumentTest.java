/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ArgumentTest {
    @Test(expected = IllegalStateException.class)
    public void build_Actionを設定していないとException() {
        new ArgumentImpl.Builder()
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Nameを設定していないとException() {
        new ArgumentImpl.Builder()
                .setAction(mock(Action.class))
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_RelatedStateVariableを設定していないとException() {
        new ArgumentImpl.Builder()
                .setAction(mock(Action.class))
                .setName("")
                .build();
    }

    @Test
    public void getRelatedStateVariableName_setした値が返る() {
        final String name = "name";
        final ArgumentImpl.Builder builder = new ArgumentImpl.Builder()
                .setRelatedStateVariableName(name);
        assertThat(builder.getRelatedStateVariableName(), is(name));
    }

    @Test
    public void build_Builderで指定した値が得られる() {
        final Action action = mock(Action.class);
        final String name = "name";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument argument = new ArgumentImpl.Builder()
                .setAction(action)
                .setName(name)
                .setDirection("in")
                .setRelatedStateVariable(stateVariable)
                .build();
        assertThat(argument.getAction(), is(action));
        assertThat(argument.getRelatedStateVariable(), is(stateVariable));
        assertThat(argument.getName(), is(name));
        assertThat(argument.isInputDirection(), is(true));
    }

    @Test
    public void isInputDirection_Builderでoutを指定した場合false() {
        final Action action = mock(Action.class);
        final String name = "name";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument argument = new ArgumentImpl.Builder()
                .setAction(action)
                .setName(name)
                .setDirection("out")
                .setRelatedStateVariable(stateVariable)
                .build();
        assertThat(argument.isInputDirection(), is(false));
    }
}
