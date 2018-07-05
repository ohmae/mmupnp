/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class StateVariableTest {
    @Test(expected = IllegalStateException.class)
    public void build_Serviceを指定していなければException() {
        final String name = "name";
        final String dataType = "ui4";
        new StateVariableImpl.Builder()
                .setName(name)
                .setDataType(dataType)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Nameを指定していなければException() {
        final Service service = mock(Service.class);
        final String dataType = "ui4";
        new StateVariableImpl.Builder()
                .setService(service)
                .setDataType(dataType)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_DataTypeを指定していなければException() {
        final Service service = mock(Service.class);
        final String name = "name";
        new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .build();
    }

    @Test
    public void getService() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .build();

        assertThat(stateVariable.getService(), is(service));
    }

    @Test
    public void getName() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .build();

        assertThat(stateVariable.getName(), is(name));
    }

    @Test
    public void getDataType() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .build();

        assertThat(stateVariable.getDataType(), is(dataType));
    }

    @Test
    public void isSendEvents() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setSendEvents("yes")
                .build();

        assertThat(stateVariable.isSendEvents(), is(true));
    }

    @Test
    public void isMulticast() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setMulticast("yes")
                .build();

        assertThat(stateVariable.isMulticast(), is(true));
    }

    @Test
    public void getAllowedValueList() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .addAllowedValue(value)
                .build();

        assertThat(stateVariable.getAllowedValueList(), hasItem(value));
    }

    @Test
    public void getDefaultValue() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setDefaultValue(value)
                .build();

        assertThat(stateVariable.getDefaultValue(), is(value));
    }

    @Test
    public void getMinimum() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setMinimum(value)
                .build();

        assertThat(stateVariable.getMinimum(), is(value));
    }

    @Test
    public void getMaximum() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setMaximum(value)
                .build();

        assertThat(stateVariable.getMaximum(), is(value));
    }

    @Test
    public void getStep() {
        final Service service = mock(Service.class);
        final String name = "name";
        final String dataType = "ui4";
        final String value = "1";
        final StateVariable stateVariable = new StateVariableImpl.Builder()
                .setService(service)
                .setName(name)
                .setDataType(dataType)
                .setStep(value)
                .build();

        assertThat(stateVariable.getStep(), is(value));
    }
}
