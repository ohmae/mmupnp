/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.XmlUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
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
    private ActionImpl mAction;
    private HttpClient mMockHttpClient;

    @Before
    public void setUp() throws Exception {
        mUrl = new URL("http://127.0.0.1:8888/test");
        final ServiceImpl service = mock(ServiceImpl.class);
        when(service.getServiceType()).thenReturn(SERVICE_TYPE);
        when(service.getControlUrl()).thenReturn("");
        mAction = (ActionImpl) spy(new ActionImpl.Builder()
                .setService(service)
                .setName(ACTION_NAME)
                .addArgumentBuilder(new ArgumentImpl.Builder()
                        .setName(IN_ARG_NAME_1)
                        .setDirection("in")
                        .setRelatedStateVariableName("1")
                        .setRelatedStateVariable(
                                new StateVariableImpl.Builder()
                                        .setDataType("string")
                                        .setName("1")
                                        .build()))
                .addArgumentBuilder(new ArgumentImpl.Builder()
                        .setName(IN_ARG_NAME_2)
                        .setDirection("in")
                        .setRelatedStateVariableName("2")
                        .setRelatedStateVariable(
                                new StateVariableImpl.Builder()
                                        .setDataType("string")
                                        .setName("2")
                                        .setDefaultValue(IN_ARG_DEFAULT_VALUE)
                                        .build()))
                .addArgumentBuilder(new ArgumentImpl.Builder()
                        .setName(OUT_ARG_NAME1)
                        .setDirection("out")
                        .setRelatedStateVariableName("3")
                        .setRelatedStateVariable(
                                new StateVariableImpl.Builder()
                                        .setDataType("string")
                                        .setName("3")
                                        .build()))
                .build());
        doReturn(mUrl).when(mAction).makeAbsoluteControlUrl();
        mMockHttpClient = spy(new HttpClient());
        mHttpResponse = new HttpResponse();
        mHttpResponse.setStatus(Http.Status.HTTP_OK);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "<s:Body>\n"
                + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">\n"
                + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">\n"
                + "</u:" + ACTION_NAME + "Response>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mMockHttpClient).when(mAction).createHttpClient();
    }

    @Test(expected = IOException.class)
    public void invoke_postでIOExceptionが発生() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        when(client.post(any())).thenThrow(IOException.class);
        doReturn(client).when(mAction).createHttpClient();
        mAction.invoke(new HashMap<>());
    }

    @Test
    public void invoke_リクエストヘッダの確認() throws Exception {
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        mAction.invoke(new HashMap<>());

        final HttpRequest request = captor.getValue();
        assertThat(request.getMethod(), is("POST"));
        assertThat(request.getUri(), is(mUrl.getPath()));
        assertThat(request.getVersion(), is("HTTP/1.1"));
        assertThat(request.getHeader(Http.HOST), is(mUrl.getHost() + ":" + mUrl.getPort()));
        assertThat(request.getHeader(Http.CONTENT_LENGTH),
                is(String.valueOf(request.getBodyBinary().length)));
    }

    private List<Element> createChildElementList(final Element parent) {
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
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        mAction.invoke(new HashMap<>());
        final HttpRequest request = captor.getValue();

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
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        mAction.invoke(new HashMap<>());
        final HttpRequest request = captor.getValue();

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
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        final Map<String, String> arg = new HashMap<>();
        arg.put(IN_ARG_NAME_1, value1);
        arg.put(IN_ARG_NAME_2, value2);

        mAction.invoke(arg);
        final HttpRequest request = captor.getValue();

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
    public void invoke_リクエストSOAPの引数確認_カスタム引数指定NSなし() throws Exception {
        final String value1 = "value1";
        final String value2 = "value2";
        final String name = "name";
        final String value = "value";
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        final Map<String, String> arg = new HashMap<>();
        arg.put(IN_ARG_NAME_1, value1);
        arg.put(IN_ARG_NAME_2, value2);

        mAction.invoke(arg, null, Collections.singletonMap(name, value));
        final HttpRequest request = captor.getValue();

        final Document doc = XmlUtils.newDocument(true, request.getBody());
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        final Element action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME);

        final List<Element> elements = createChildElementList(action);
        assertThat(elements.size(), is(3));
        assertThat(elements.get(0).getLocalName(), is(IN_ARG_NAME_1));
        assertThat(elements.get(0).getTextContent(), is(value1));

        assertThat(elements.get(1).getLocalName(), is(IN_ARG_NAME_2));
        assertThat(elements.get(1).getTextContent(), is(value2));

        assertThat(elements.get(2).getLocalName(), is(name));
        assertThat(elements.get(2).getTextContent(), is(value));
    }

    @Test
    public void invoke_リクエストSOAPの引数確認_カスタム引数指定NSあり() throws Exception {
        final String value1 = "value1";
        final String value2 = "value2";
        final String prefix = "custom";
        final String urn = "urn:schemas-custom-com:custom";
        final String name = "name";
        final String value = "value";
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        doReturn(mHttpResponse).when(mMockHttpClient).post(captor.capture());
        final Map<String, String> arg = new HashMap<>();
        arg.put(IN_ARG_NAME_1, value1);
        arg.put(IN_ARG_NAME_2, value2);

        mAction.invoke(arg,
                Collections.singletonMap(prefix, urn),
                Collections.singletonMap(prefix + ":" + name, value));
        final HttpRequest request = captor.getValue();

        final Document doc = XmlUtils.newDocument(true, request.getBody());
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        final Element action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME);

        final List<Element> elements = createChildElementList(action);
        assertThat(elements.size(), is(3));
        assertThat(elements.get(0).getLocalName(), is(IN_ARG_NAME_1));
        assertThat(elements.get(0).getTextContent(), is(value1));

        assertThat(elements.get(1).getLocalName(), is(IN_ARG_NAME_2));
        assertThat(elements.get(1).getTextContent(), is(value2));

        assertThat(elements.get(2).getLocalName(), is(name));
        assertThat(elements.get(2).getTextContent(), is(value));
    }

    @Test
    public void invoke_200以外のレスポンスでIOExceptionが発生() throws Exception {
        int statusCount = 0;
        int exceptionCount = 0;
        for (final Http.Status status : Http.Status.values()) {
            mHttpResponse.setStatus(status);
            doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
            if (status == Http.Status.HTTP_OK) {
                mAction.invoke(new HashMap<>());
                continue;
            }
            try {
                statusCount++;
                mAction.invoke(new HashMap<>());
            } catch (final IOException ignored) {
                exceptionCount++;
            }
        }
        assertThat(statusCount, is(exceptionCount));
    }

    @Test(expected = IOException.class)
    public void invoke_BodyタグがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

        mAction.invoke(Collections.emptyMap(), false);
    }

    @Test(expected = IOException.class)
    public void invoke_ActionタグがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "<s:Body>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

        mAction.invoke(Collections.emptyMap(), false);
    }

    @Test
    public void invoke_実行結果をパースしMapとして戻ること() throws Exception {
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        final Map<String, String> result = mAction.invoke(new HashMap<>());
        assertThat(result.get(OUT_ARG_NAME1), is(OUT_ARG_VALUE1));
    }

    @Test
    public void invoke_argumentListにない結果が含まれていても結果に含まれる() throws Exception {
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<s:Body>\n"
                + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">\n"
                + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">\n"
                + "<" + OUT_ARG_NAME2 + ">" + OUT_ARG_VALUE2 + "</" + OUT_ARG_NAME2 + ">\n"
                + "</u:" + ACTION_NAME + "Response>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        final Map<String, String> result = mAction.invoke(Collections.emptyMap());
        assertThat(result.get(OUT_ARG_NAME1), is(OUT_ARG_VALUE1));
        assertThat(result.containsKey(OUT_ARG_NAME2), is(true));
    }

    private static final String ERROR_RESPONSE =
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "<s:Fault>\n"
                    + "<faultcode>s:Client</faultcode>\n"
                    + "<faultstring>UPnPError</faultstring>\n"
                    + "<detail>\n"
                    + "<UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n"
                    + "<errorCode>711</errorCode>\n"
                    + "<errorDescription>Restricted object</errorDescription>\n"
                    + "</UPnPError>\n"
                    + "</detail>\n"
                    + "</s:Fault>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>";

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスのときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody(ERROR_RESPONSE);
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), false);
    }

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスにerrorCodeがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "<s:Body>\n"
                + "<s:Fault>\n"
                + "<faultcode>s:Client</faultcode>\n"
                + "<faultstring>UPnPError</faultstring>\n"
                + "<detail>\n"
                + "<UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n"
                + "<errorDescription>Restricted object</errorDescription>\n"
                + "</UPnPError>\n"
                + "</detail>\n"
                + "</s:Fault>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスにUPnPErrorがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "<s:Body>\n"
                + "<s:Fault>\n"
                + "<faultcode>s:Client</faultcode>\n"
                + "<faultstring>UPnPError</faultstring>\n"
                + "<detail>\n"
                + "</detail>\n"
                + "</s:Fault>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスにBodyがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスにFaultがないとIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                + "<s:Body>\n"
                + "</s:Body>\n"
                + "</s:Envelope>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test
    public void invoke_エラーレスポンスもパースできる() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody(ERROR_RESPONSE);
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        final Map<String, String> result = mAction.invoke(Collections.emptyMap(), true);
        assertThat(result.get(Action.FAULT_CODE_KEY), is("s:Client"));
        assertThat(result.get(Action.FAULT_STRING_KEY), is("UPnPError"));
        assertThat(result.get(Action.ERROR_CODE_KEY), is("711"));
        assertThat(result.get(Action.ERROR_DESCRIPTION_KEY), is("Restricted object"));
    }

    @Test(expected = IOException.class)
    public void invoke_エラーレスポンスのときIOExceptionが発生2() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody(ERROR_RESPONSE);
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false);
    }

    @Test
    public void invoke_エラーレスポンスもパースできる2() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody(ERROR_RESPONSE);
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        final Map<String, String> result = mAction.invoke(Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                true);
        assertThat(result.get(Action.FAULT_CODE_KEY), is("s:Client"));
        assertThat(result.get(Action.FAULT_STRING_KEY), is("UPnPError"));
        assertThat(result.get(Action.ERROR_CODE_KEY), is("711"));
        assertThat(result.get(Action.ERROR_DESCRIPTION_KEY), is("Restricted object"));
    }

    @Test(expected = IOException.class)
    public void invoke_ステータスコードがOKで中身が空のときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_OK);
        mHttpResponse.setBody("");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_ステータスコードがエラーで中身が空のときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_ステータスコードがその他で中身が空のときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_NOT_FOUND);
        mHttpResponse.setBody("");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_ステータスコードがOKで中身がxmlとして異常のときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_OK);
        mHttpResponse.setBody("<>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void invoke_ステータスコードがエラーで中身がxmlとして異常のときIOExceptionが発生() throws Exception {
        mHttpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
        mHttpResponse.setBody("<>");
        doReturn(mHttpResponse).when(mMockHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
        mAction.invoke(Collections.emptyMap(), true);
    }

    @Test(expected = IOException.class)
    public void makeSoap_xml作成でExceptionが発生したらIOException() throws Exception {
        doThrow(new TransformerException("")).when(mAction).formatXmlString(ArgumentMatchers.any(Document.class));
        mAction.makeSoap(null, Collections.emptyList());
    }
}
