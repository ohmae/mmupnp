/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;

import javax.annotation.Nonnull;

class MockHttpClient extends HttpClient {
    private HttpRequest mHttpRequest;
    private HttpResponse mHttpResponse;

    public void setResponse(final HttpResponse response) {
        mHttpResponse = response;
    }

    public HttpRequest getHttpRequest() {
        return mHttpRequest;
    }

    @Nonnull
    @Override
    public HttpResponse post(@Nonnull final HttpRequest request) throws IOException {
        mHttpRequest = request;
        return mHttpResponse;
    }
}
