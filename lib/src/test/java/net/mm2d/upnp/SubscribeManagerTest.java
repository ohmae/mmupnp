/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.util.StringPair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SubscribeManagerTest {
    @Test
    public void onEventReceived_has_no_service() throws Exception {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        assertThat(manager.onEventReceived("", 0, Collections.<StringPair>emptyList()), is(false));
    }

    @Test
    public void onEventReceived() throws Exception {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = new ThreadPool();
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        final String sid = "sid";
        final Service service = mock(Service.class);
        doReturn(service).when(holder).getService(sid);

        assertThat(manager.onEventReceived(sid, 0, Collections.singletonList(new StringPair("", ""))), is(true));

        pool.terminate();
    }

    @Test
    public void initialize() throws Exception {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        manager.initialize();
        verify(holder).start();
    }

    @Test
    public void start() throws Exception {
        final EventReceiver receiver = mock(EventReceiver.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        doThrow(new IOException()).when(receiver).open();
        manager.start();
        verify(receiver).open();
    }

    @Test
    public void start_exception() throws Exception {
        final EventReceiver receiver = mock(EventReceiver.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        manager.start();
        verify(receiver).open();
    }

    @Test
    public void stop() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final EventReceiver receiver = mock(EventReceiver.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }

            @Nonnull
            @Override
            EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        doReturn(Collections.singletonList(mock(Service.class))).when(holder).getServiceList();
        manager.stop();

        verify(pool).executeInParallel(ArgumentMatchers.any(Runnable.class));
        verify(holder).clear();
        verify(receiver).close();
    }

    @Test
    public void terminate() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        manager.terminate();
        verify(holder).shutdownRequest();
    }

    @Test
    public void getEventPort() {
        final EventReceiver receiver = mock(EventReceiver.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        final int port = 80;
        doReturn(port).when(receiver).getLocalPort();

        assertThat(manager.getEventPort(), is(port));

        verify(receiver).getLocalPort();
    }

    @Test
    public void getSubscribeService() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(service).when(holder).getService(id);

        assertThat(manager.getSubscribeService(id), is(service));

        verify(holder).getService(id);
    }

    @Test
    public void registerSubscribeService() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        final Service service = mock(Service.class);

        manager.registerSubscribeService(service, true);

        verify(holder).add(service, true);
    }

    @Test
    public void unregisterSubscribeService() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final ThreadPool pool = mock(ThreadPool.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            SubscribeHolder createSubscribeHolder() {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(pool, listener, factory);
        final Service service = mock(Service.class);

        manager.unregisterSubscribeService(service);

        verify(holder).remove(service);
    }
}
