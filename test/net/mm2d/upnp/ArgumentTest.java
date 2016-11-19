/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArgumentTest {

    @Test(expected = IllegalStateException.class)
    public void build_Actionを設定していないとException() {
        final Argument.Builder builder = new Argument.Builder();
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Nameを設定していないとException() {
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(mock(Action.class));
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void Builder_build_RelatedStateVariableを設定していないとException() {
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(mock(Action.class));
        builder.setName("");
        builder.build();
    }

    @Test
    public void getRelatedStateVariableName_setした値が返る() {
        final String name = "test";
        final Argument.Builder builder = new Argument.Builder();
        builder.setRelatedStateVariableName(name);
        assertEquals(name, builder.getRelatedStateVariableName());
    }

    @Test
    public void getAction_Builderで指定した値が得られる() {
        final Action action = mock(Action.class);
        final String name = "test";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(action);
        builder.setName(name);
        builder.setDirection("in");
        builder.setRelatedStateVariable(stateVariable);
        final Argument argument = builder.build();
        assertEquals(action, argument.getAction());
    }

    @Test
    public void getName_Builderで指定した値が得られる() {
        final Action action = mock(Action.class);
        final String name = "test";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(action);
        builder.setName(name);
        builder.setDirection("in");
        builder.setRelatedStateVariable(stateVariable);
        final Argument argument = builder.build();
        assertEquals(name, argument.getName());
    }

    @Test
    public void isInputDirection_Builderでinを指定した場合true() {
        final Action action = mock(Action.class);
        final String name = "test";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(action);
        builder.setName(name);
        builder.setDirection("in");
        builder.setRelatedStateVariable(stateVariable);
        final Argument argument = builder.build();
        assertTrue(argument.isInputDirection());
    }

    @Test
    public void isInputDirection_Builderでoutを指定した場合false() {
        final Action action = mock(Action.class);
        final String name = "test";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(action);
        builder.setName(name);
        builder.setDirection("out");
        builder.setRelatedStateVariable(stateVariable);
        final Argument argument = builder.build();
        assertFalse(argument.isInputDirection());
    }

    @Test
    public void getRelatedStateVariable_Builderで指定した値が得られる() {
        final Action action = mock(Action.class);
        final String name = "test";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument.Builder builder = new Argument.Builder();
        builder.setAction(action);
        builder.setName(name);
        builder.setDirection("in");
        builder.setRelatedStateVariable(stateVariable);
        final Argument argument = builder.build();
        assertEquals(stateVariable, argument.getRelatedStateVariable());
    }
}
