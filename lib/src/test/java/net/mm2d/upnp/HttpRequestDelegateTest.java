/*
 * Copyright(C)  2018 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.HttpMessageDelegate.StartLineProcessor;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.*;

public class HttpRequestDelegateTest {
    private HttpMessageDelegate mDelegate;
    private HttpRequest mMessage;

    @Before
    public void setUp() {
        mDelegate = spy(new HttpMessageDelegate(new StartLineProcessor() {
            @Override
            public void setStartLine(@Nonnull final String line) {
            }

            @Nonnull
            @Override
            public String getStartLine() {
                return "";
            }
        }));
        mMessage = new HttpRequest(mDelegate);
    }

    @Test
    public void getVersion() {
        mMessage.getVersion();

        verify(mDelegate, times(1)).getVersion();
    }

    @Test
    public void setVersion() {
        final String version = Http.HTTP_1_1;
        mMessage.setVersion(version);

        verify(mDelegate, times(1)).setVersion(version);
    }

    @Test
    public void setHeader() {
        final String name = "name";
        final String value = "value";
        mMessage.setHeader(name, value);

        verify(mDelegate, times(1)).setHeader(name, value);
    }

    @Test
    public void setHeaderLine() {
        final String line = "name: value";
        mMessage.setHeaderLine(line);

        verify(mDelegate, times(1)).setHeaderLine(line);
    }

    @Test
    public void getHeader() {
        final String name = "name";
        mMessage.getHeader(name);

        verify(mDelegate, times(1)).getHeader(name);
    }

    @Test
    public void isChunked() {
        mMessage.isChunked();

        verify(mDelegate, times(1)).isChunked();
    }

    @Test
    public void isKeepAlive() {
        mMessage.isKeepAlive();

        verify(mDelegate, times(1)).isKeepAlive();
    }

    @Test
    public void getContentLength() {
        mMessage.getContentLength();

        verify(mDelegate, times(1)).getContentLength();
    }

    @Test
    public void setBody() {
        final String body = "body";
        mMessage.setBody(body);

        verify(mDelegate, times(1)).setBody(body);
    }

    @Test
    public void setBody1() {
        final String body = "body";
        mMessage.setBody(body, true);

        verify(mDelegate, times(1)).setBody(body, true);
    }

    @Test
    public void setBodyBinary() {
        final byte[] body = new byte[0];
        mMessage.setBodyBinary(body);

        verify(mDelegate, times(1)).setBodyBinary(body);
    }

    @Test
    public void setBodyBinary1() {
        final byte[] body = new byte[0];
        mMessage.setBodyBinary(body, true);

        verify(mDelegate, times(1)).setBodyBinary(body, true);
    }

    @Test
    public void getBody() {
        mMessage.getBody();

        verify(mDelegate, times(1)).getBody();
    }

    @Test
    public void getBodyBinary() {
        mMessage.getBodyBinary();

        verify(mDelegate, times(1)).getBodyBinary();
    }

    @Test
    public void getMessageString() {
        mMessage.getMessageString();

        verify(mDelegate, times(1)).getMessageString();
    }

    @Test
    public void writeData() throws Exception {
        final OutputStream os = mock(OutputStream.class);
        mMessage.writeData(os);

        verify(mDelegate, times(1)).writeData(os);
    }

    @Test
    public void readData() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[0]);
        try {
            mMessage.readData(is);
        } catch (final Exception ignored) {
        }

        verify(mDelegate, times(1)).readData(is);
    }

    @Test
    public void toString_() throws Exception {
        mMessage.toString();
    }
}