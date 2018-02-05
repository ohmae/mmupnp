/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ActionTest {
    @Test(expected = IllegalStateException.class)
    public void build_ServiceをsetしていないとException() {
        new Action.Builder()
                .setName("name")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_NameをsetしていないとException() {
        new Action.Builder()
                .setService(mock(Service.class))
                .build();
    }

    @Test
    public void addArgumentBuilder_setした値が取得できる() {
        final Argument.Builder argumentBuilder = mock(Argument.Builder.class);
        final Action.Builder builder = new Action.Builder()
                .addArgumentBuilder(argumentBuilder);
        final List<Argument.Builder> list = builder.getArgumentBuilderList();
        assertThat(list.size(), is(1));
        assertThat(list, hasItem(argumentBuilder));
    }

    @Test
    public void getService_setした値が取得できる() {
        final Service service = mock(Service.class);
        final String name = "name";
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .build();
        assertThat(action.getService(), is(sameInstance(service)));
    }

    @Test
    public void getName_setした値が取得できる() {
        final Service service = mock(Service.class);
        final String name = "name";
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .build();
        assertThat(action.getName(), is(name));
    }

    @Test
    public void getArgumentList_Argumentがない場合はサイズ0() {
        final Service service = mock(Service.class);
        final String name = "name";
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .build();
        assertThat(action.getArgumentList().size(), is(0));
    }

    @Test
    public void getArgumentList_Builderで指定したArgumentが作成されている() {
        final String argumentName = "argumentName";
        final StateVariable stateVariable = mock(StateVariable.class);
        final String name = "name";
        final Service service = mock(Service.class);
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .addArgumentBuilder(new Argument.Builder()
                        .setName(argumentName)
                        .setDirection("in")
                        .setRelatedStateVariable(stateVariable))
                .build();

        assertThat(action.getArgumentList().size(), is(1));
        final Argument argument = action.getArgumentList().get(0);
        assertThat(argument.getAction(), is(action));
        assertThat(argument.getName(), is(argumentName));
        assertThat(argument.isInputDirection(), is(true));
        assertThat(argument.getRelatedStateVariable(), is(stateVariable));
    }

    @Test
    public void findArgument_名前指定でArugumentが取得でできる() {
        final String argumentName = "argumentName";
        final StateVariable stateVariable = mock(StateVariable.class);
        final String name = "name";
        final Service service = mock(Service.class);
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .addArgumentBuilder(new Argument.Builder()
                        .setName(argumentName)
                        .setDirection("in")
                        .setRelatedStateVariable(stateVariable))
                .build();
        final Argument argument = action.findArgument(argumentName);

        assertThat(argument.getAction(), is(action));
        assertThat(argument.getName(), is(argumentName));
        assertThat(argument.isInputDirection(), is(true));
        assertThat(argument.getRelatedStateVariable(), is(stateVariable));
    }

    @Test
    public void createHttpClient() {
        final Service service = mock(Service.class);
        final String name = "name";
        final Action action = new Action.Builder()
                .setService(service)
                .setName(name)
                .build();
        final HttpClient client = action.createHttpClient();
        assertThat(client.isKeepAlive(), is(false));
    }
}
