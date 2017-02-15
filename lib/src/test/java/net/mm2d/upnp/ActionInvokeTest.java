package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.XmlUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ActionInvokeTest {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";
    private static final String IN_ARG_NAME_1 = "InArgName1";
    private static final String IN_ARG_NAME_2 = "InArgName2";
    private static final String IN_ARG_DEFAULT_VALUE = "Default";
    private static final String OUT_ARG_NAME1 = "OutArgName1";
    private static final String OUT_ARG_VALUE1 = "OutArgValue1";
    private static final String OUT_ARG_NAME2 = "OutArgName2";
    private static final String OUT_ARG_VALUE2 = "OutArgValue2";
    private static final String ACTION_NAME = "TestAction";
    private static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:TestServiceType:1";
    private HttpResponse mHttpResponse;
    private URL mUrl;
    private Action mAction;
    private MockHttpClientFactory mMockFactory;

    @Before
    public void setUp() throws Exception {
        mUrl = new URL("http://127.0.0.1:8888/test");
        final Service service = mock(Service.class);
        when(service.getServiceType()).thenReturn(SERVICE_TYPE);
        when(service.getControlUrl()).thenReturn("");
        when(service.getAbsoluteUrl(anyString())).thenReturn(mUrl);
        mAction = new Action.Builder()
                .setService(service)
                .setName(ACTION_NAME)
                .addArgumentBuilder(new Argument.Builder()
                        .setName(IN_ARG_NAME_1)
                        .setDirection("in")
                        .setRelatedStateVariableName("1")
                        .setRelatedStateVariable(
                                new StateVariable.Builder()
                                        .setService(service)
                                        .setDataType("string")
                                        .setName("1")
                                        .build()))
                .addArgumentBuilder(new Argument.Builder()
                        .setName(IN_ARG_NAME_2)
                        .setDirection("in")
                        .setRelatedStateVariableName("2")
                        .setRelatedStateVariable(
                                new StateVariable.Builder()
                                        .setService(service)
                                        .setDataType("string")
                                        .setName("2")
                                        .setDefaultValue(IN_ARG_DEFAULT_VALUE)
                                        .build()))
                .addArgumentBuilder(new Argument.Builder()
                        .setName(OUT_ARG_NAME1)
                        .setDirection("out")
                        .setRelatedStateVariableName("3")
                        .setRelatedStateVariable(
                                new StateVariable.Builder()
                                        .setService(service)
                                        .setDataType("string")
                                        .setName("3")
                                        .build()))
                .build();
        mMockFactory = new MockHttpClientFactory();
        mHttpResponse = new HttpResponse();
        mHttpResponse.setStatus(Http.Status.HTTP_OK);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + "<s:Body>"
                + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">"
                + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">"
                + "</u:" + ACTION_NAME + "Response>" + "</s:Body>" + "</s:Envelope>");
        mAction.setHttpClientFactory(mMockFactory);
    }

    @Test(expected = IOException.class)
    public void invoke_postでIOExceptionが発生() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        when(client.post((HttpRequest) any())).thenThrow(IOException.class);
        mAction.setHttpClientFactory(new HttpClientFactory() {
            @Override
            @Nonnull
            public HttpClient createHttpClient(boolean keepAlive) {
                return client;
            }
        });
        mAction.invoke(new HashMap<String, String>());
    }

    @Test
    public void invoke_リクエストヘッダの確認() throws Exception {
        mMockFactory.setResponse(mHttpResponse);
        mAction.invoke(new HashMap<String, String>());

        final HttpRequest request = mMockFactory.getHttpRequest();
        assertThat(request.getMethod(), is("POST"));
        assertThat(request.getUri(), is(mUrl.getPath()));
        assertThat(request.getVersion(), is("HTTP/1.1"));
        assertThat(request.getHeader(Http.HOST), is(mUrl.getHost() + ":" + mUrl.getPort()));
        assertThat(request.getHeader(Http.CONTENT_LENGTH),
                is(String.valueOf(request.getBodyBinary().length)));
    }

    private List<Element> createChildElementList(Element parent) {
        final List<Element> elements = new ArrayList<>();
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) child);
            }
        }
        return elements;
    }

    @Test
    public void invoke_リクエストSOAPフォーマットの確認() throws Exception {
        mMockFactory.setResponse(mHttpResponse);
        mAction.invoke(new HashMap<String, String>());
        final HttpRequest request = mMockFactory.getHttpRequest();

        final Document doc = XmlUtils.newDocument(true, request.getBody());
        final Element envelope = doc.getDocumentElement();

        assertThat(envelope.getLocalName(), is("Envelope"));
        assertThat(envelope.getNamespaceURI(), is(SOAP_NS));
        assertThat(envelope.getAttributeNS(SOAP_NS, "encodingStyle"), is(SOAP_STYLE));

        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");

        assertThat(body, is(notNullValue()));
        assertThat(body.getNamespaceURI(), is(SOAP_NS));

        final Element action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME);

        assertThat(action, is(notNullValue()));
        assertThat(action.getNamespaceURI(), is(SERVICE_TYPE));
    }

    @Test
    public void invoke_リクエストSOAPの引数確認_指定なしでの実行() throws Exception {
        mMockFactory.setResponse(mHttpResponse);
        mAction.invoke(new HashMap<String, String>());
        final HttpRequest request = mMockFactory.getHttpRequest();

        final Document doc = XmlUtils.newDocument(true, request.getBody());
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        final Element action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME);

        final List<Element> elements = createChildElementList(action);
        assertThat(elements.size(), is(2));
        assertThat(elements.get(0).getLocalName(), is(IN_ARG_NAME_1));
        assertThat(elements.get(0).getTextContent(), is(""));

        assertThat(elements.get(1).getLocalName(), is(IN_ARG_NAME_2));
        assertThat(elements.get(1).getTextContent(), is(IN_ARG_DEFAULT_VALUE));
    }

    @Test
    public void invoke_リクエストSOAPの引数確認_指定ありでの実行() throws Exception {
        final String value1 = "value1";
        final String value2 = "value2";
        mMockFactory.setResponse(mHttpResponse);
        final Map<String, String> arg = new HashMap<>();
        arg.put(IN_ARG_NAME_1, value1);
        arg.put(IN_ARG_NAME_2, value2);
        mAction.invoke(arg);
        final HttpRequest request = mMockFactory.getHttpRequest();

        final Document doc = XmlUtils.newDocument(true, request.getBody());
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        final Element action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME);

        final List<Element> elements = createChildElementList(action);
        assertThat(elements.size(), is(2));
        assertThat(elements.get(0).getLocalName(), is(IN_ARG_NAME_1));
        assertThat(elements.get(0).getTextContent(), is(value1));

        assertThat(elements.get(1).getLocalName(), is(IN_ARG_NAME_2));
        assertThat(elements.get(1).getTextContent(), is(value2));
    }

    @Test
    public void invoke_200以外のレスポンスでIOExceptionが発生() throws Exception {
        Log.setLogLevel(Log.ASSERT);
        for (final Http.Status status : Http.Status.values()) {
            if (status == Http.Status.HTTP_OK) {
                continue;
            }
            mHttpResponse.setStatus(status);
            mMockFactory.setResponse(mHttpResponse);
            boolean fail = true;
            try {
                mAction.invoke(new HashMap<String, String>());
            } catch (final IOException e) {
                fail = false;
            }
            if (fail) {
                fail();
            }
        }
    }

    @Test
    public void invoke_実行結果をパースしMapとして戻ること() throws Exception {
        mMockFactory.setResponse(mHttpResponse);
        final Map<String, String> result = mAction.invoke(new HashMap<String, String>());
        assertThat(result.get(OUT_ARG_NAME1), is(OUT_ARG_VALUE1));
    }

    @Test
    public void invoke_argumentListにない結果が含まれていたら無視() throws Exception {
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + "<s:Body>"
                + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">"
                + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">"
                + "<" + OUT_ARG_NAME2 + ">" + OUT_ARG_VALUE2 + "</" + OUT_ARG_NAME2 + ">"
                + "</u:" + ACTION_NAME + "Response>" + "</s:Body>" + "</s:Envelope>");
        mMockFactory.setResponse(mHttpResponse);
        final Map<String, String> result = mAction.invoke(new HashMap<String, String>());
        assertThat(result.get(OUT_ARG_NAME1), is(OUT_ARG_VALUE1));
        assertThat(result.containsKey(OUT_ARG_NAME2), is(false));
    }
}
