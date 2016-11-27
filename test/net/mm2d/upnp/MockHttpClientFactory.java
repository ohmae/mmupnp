/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;

public class MockHttpClientFactory extends HttpClientFactory {
    private HttpRequest mHttpRequest;
    private HttpResponse mHttpResponse;

    public void setResponse(HttpResponse response) {
        mHttpResponse = response;
    }

    public HttpRequest getHttpRequest() {
        return mHttpRequest;
    }

    @Override
    public HttpClient createHttpClient(boolean keepAlive) {
        return new HttpClient() {
            @Override
            public HttpResponse post(HttpRequest request) throws IOException {
                mHttpRequest = request;
                return mHttpResponse;
            }
        };
    }
}
