/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.internal.impl.StateVariableImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class StateVariableTest {
    @Test(expected = IllegalStateException.class)
    public void build_Nameを指定していなければException() {
        final String dataType = "ui4";
        new StateVariableImpl.Builder()
                .setDataType(dataType)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_DataTypeを指定していなければException() {
        final String name = "name";
        new StateVariableImpl.Builder()
                .setName(name)
                .build();
    }

    @Test
    public void getName() {
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .build();

        assertThat(stateVariable.getName(), is(name));
    }

    @Test
    public void getDataType() {
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .build();

        assertThat(stateVariable.getDataType(), is(dataType));
    }

    @Test
    public void isSendEvents() {
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setSendEvents("yes")
                .build();

        assertThat(stateVariable.isSendEvents(), is(true));
    }

    @Test
    public void isMulticast() {
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setMulticast("yes")
                .build();

        assertThat(stateVariable.isMulticast(), is(true));
    }

    @Test
    public void getAllowedValueList() {
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .addAllowedValue(value)
                .build();

        assertThat(stateVariable.getAllowedValueList(), hasItem(value));
    }

    @Test
    public void getDefaultValue() {
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setDefaultValue(value)
                .build();

        assertThat(stateVariable.getDefaultValue(), is(value));
    }

    @Test
    public void getMinimum() {
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setMinimum(value)
                .build();

        assertThat(stateVariable.getMinimum(), is(value));
    }

    @Test
    public void getMaximum() {
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setMaximum(value)
                .build();

        assertThat(stateVariable.getMaximum(), is(value));
    }

    @Test
    public void getStep() {
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .setStep(value)
                .build();

        assertThat(stateVariable.getStep(), is(value));
    }
}
