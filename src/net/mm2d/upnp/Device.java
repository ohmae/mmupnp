/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * UPnP Deviceを表現するクラス
 *
 * SubDeviceには非対応
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Device {
    private static final String TAG = "Device";
    private final ControlPoint mControlPoint;
    private SsdpMessage mSsdp;
    private String mDescription;
    private String mUdn;
    private String mDeviceType;
    private String mFriendlyName;
    private String mManufacture;
    private String mManufactureUrl;
    private String mModelName;
    private String mModelUrl;
    private String mModelDescription;
    private String mModelNumber;
    private String mSerialNumber;
    private String mPresentationUrl;
    private final Map<String, Map<String, String>> mTagMap;
    private final List<Icon> mIconList;
    private final List<Service> mServiceList;

    /**
     * ControlPointに紐付いたインスタンスを作成。
     *
     * @param controlPoint 紐付けるControlPoint
     */
    public Device(@NotNull ControlPoint controlPoint) {
        mControlPoint = controlPoint;
        mIconList = new ArrayList<>();
        mServiceList = new ArrayList<>();
        mTagMap = new LinkedHashMap<>();
        mTagMap.put("", new HashMap<>());
    }

    /**
     * 紐付いたControlPointを返す。
     *
     * @return 紐付いたControlPoint
     */
    @NotNull
    public ControlPoint getControlPoint() {
        return mControlPoint;
    }

    /**
     * SSDPパケットを設定する。
     *
     * 同一性はSSDPパケットのUUIDで判断し
     * SSDPパケット受信ごとに更新される
     *
     * @param message SSDPパケット
     */
    void setSsdpMessage(@NotNull SsdpMessage message) {
        mSsdp = message;
    }

    /**
     * 最新のSSDPパケットを返す。
     *
     * @return 最新のSSDPパケット
     */
    @NotNull
    SsdpMessage getSsdpMessage() {
        return mSsdp;
    }

    /**
     * SSDPパケットに記述されたUUIDを返す。
     *
     * 本来はDeviceDescriptionに記述されたUDNと同一の値となるはずであるが
     * 異なる場合もエラーとしては扱っていないため、同一性は保証されない。
     *
     * @return SSDPパケットに記述されたUUID
     */
    @Nullable
    public String getUuid() {
        return mSsdp.getUuid();
    }

    /**
     * 更新がなければ無効となる時間[ms]を返す。
     *
     * @return 更新がなければ無効となる時間[ms]
     */
    public long getExpireTime() {
        return mSsdp.getExpireTime();
    }

    /**
     * DeviceDescriptionのXML文字列を返す。
     *
     * @return DeviceDescriptionのXML文字列
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * URL情報を正規化して返す。
     *
     * "http://"から始まっていればそのまま利用する。
     * "/"から始まっていればLocationホストの絶対パスとして
     * Locationの"://"以降の最初の"/"までと結合する。
     * それ以外の場合はLocationからの相対パスであり、
     * Locationからクエリーを除去し、最後の"/"までと結合する。
     *
     * @param url URLパス情報
     * @return 正規化したURL
     * @throws MalformedURLException
     */
    @NotNull
    URL getAbsoluteUrl(@NotNull String url) throws MalformedURLException {
        if (url.startsWith("http://")) {
            return new URL(url);
        }
        String baseUrl = getLocation();
        if (url.startsWith("/")) {
            int pos = baseUrl.indexOf("://");
            pos = baseUrl.indexOf("/", pos + 3);
            return new URL(baseUrl.substring(0, pos) + url);
        }
        int pos = baseUrl.indexOf("?");
        if (pos > 0) {
            baseUrl = baseUrl.substring(0, pos);
        }
        if (baseUrl.endsWith("/")) {
            return new URL(baseUrl + url);
        }
        pos = baseUrl.lastIndexOf("/");
        baseUrl = baseUrl.substring(0, pos + 1);
        return new URL(baseUrl + url);
    }

    /**
     * DeviceDescriptionを読み込む。
     *
     * Descriptionのパース後、そこに記述されている
     * icon/serviceのDescriptionの読み込みパースもまとめて行う。
     *
     * @throws IOException 通信上での何らかの問題
     * @throws SAXException XMLのパースに失敗
     * @throws ParserConfigurationException XMLパーサが利用できない場合
     */
    void loadDescription() throws IOException, SAXException, ParserConfigurationException {
        final HttpClient client = new HttpClient(true);
        final URL url = new URL(getLocation());
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.i(TAG, response.toString());
            client.close();
            throw new IOException(response.getStartLine());
        }
        mDescription = response.getBody();
        parseDescription(mDescription);
        if (Property.isGetIconOnLoadDescription()) {
            for (final Icon icon : mIconList) {
                icon.loadBinary(client);
            }
        }
        for (final Service service : mServiceList) {
            service.loadDescription(client);
        }
        client.close();
    }

    private void parseIconList(@NotNull Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("icon".equals(node.getLocalName())) {
                mIconList.add(parseIcon((Element) node));
            }
        }
    }

    private Icon parseIcon(@NotNull Element element) {
        final Icon.Builder icon = new Icon.Builder();
        icon.setDevice(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("mimetype".equals(tag)) {
                icon.setMimeType(node.getTextContent());
            } else if ("height".equals(tag)) {
                icon.setHeight(node.getTextContent());
            } else if ("width".equals(tag)) {
                icon.setWidth(node.getTextContent());
            } else if ("depth".equals(tag)) {
                icon.setDepth(node.getTextContent());
            } else if ("url".equals(tag)) {
                icon.setUrl(node.getTextContent());
            }
        }
        return icon.build();
    }

    private void parseServiceList(@NotNull Node listNode) throws IOException {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("service".equals(node.getLocalName())) {
                mServiceList.add(parseService((Element) node));
            }
        }
    }

    @NotNull
    private Service parseService(@NotNull Element element) throws IOException {
        final Service.Builder service = new Service.Builder();
        service.setDevice(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("serviceType".equals(tag)) {
                service.setServiceType(node.getTextContent());
            } else if ("serviceId".equals(tag)) {
                service.setServiceId(node.getTextContent());
            } else if ("SCPDURL".equals(tag)) {
                service.setScpdUrl(node.getTextContent());
            } else if ("eventSubURL".equals(tag)) {
                service.setEventSubUrl(node.getTextContent());
            } else if ("controlURL".equals(tag)) {
                service.setControlUrl(node.getTextContent());
            }
        }
        try {
            return service.build();
        } catch (IllegalStateException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void parseDescription(@NotNull String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        Node n = doc.getDocumentElement().getFirstChild();
        for (; n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("device".equals(n.getLocalName())) {
                break;
            }
        }
        if (n == null) {
            return;
        }
        Node node = n.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("iconList".equals(tag)) {
                parseIconList(node);
            } else if ("serviceList".equals(tag)) {
                parseServiceList(node);
            } else {
                String ns = node.getNamespaceURI();
                ns = ns == null ? "" : ns;
                final String text = node.getTextContent();
                Map<String, String> nsmap = mTagMap.get(ns);
                if (nsmap == null) {
                    nsmap = new HashMap<>();
                    mTagMap.put(ns, nsmap);
                }
                nsmap.put(tag, node.getTextContent());
                if ("UDN".equals(tag)) {
                    mUdn = text;
                } else if ("deviceType".equals(tag)) {
                    mDeviceType = text;
                } else if ("friendlyName".equals(tag)) {
                    mFriendlyName = text;
                } else if ("manufacturer".equals(tag)) {
                    mManufacture = text;
                } else if ("manufacturerURL".equals(tag)) {
                    mManufactureUrl = text;
                } else if ("modelName".equals(tag)) {
                    mModelName = text;
                } else if ("modelURL".equals(tag)) {
                    mModelUrl = text;
                } else if ("modelDescription".equals(tag)) {
                    mModelDescription = text;
                } else if ("modelNumber".equals(tag)) {
                    mModelNumber = text;
                } else if ("serialNumber".equals(tag)) {
                    mSerialNumber = text;
                } else if ("presentationURL".equals(tag)) {
                    mPresentationUrl = text;
                }
            }
        }
        if (mDeviceType == null) {
            throw new IOException("deviceType must be set.");
        }
        if (mFriendlyName == null) {
            throw new IOException("friendlyName must be set.");
        }
        if (mManufacture == null) {
            throw new IOException("manufacturer must be set.");
        }
        if (mModelName == null) {
            throw new IOException("modelName must be set.");
        }
        if (mUdn == null) {
            throw new IOException("UDN must be set.");
        }
    }

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * 個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない。
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * タグ名にネームスペースプレフィックスは含まない。
     * 複数のネームスペースがある場合は最初に見つかったネームスペースのタグが返される。
     * 値が存在しない場合nullが返る。
     *
     * @param name タグ名
     * @return タグの値
     */
    @Nullable
    public String getValue(@NotNull String name) {
        for (final Entry<String, Map<String, String>> entry : mTagMap.entrySet()) {
            final String value = entry.getValue().get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * 個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない、
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * ネームスペースはプレフィックスではなく、URIを指定する。
     * 値が存在しない場合nullが返る。
     *
     * @param name タグ名
     * @param namespace ネームスペース（URI）
     * @return タグの値
     */
    @Nullable
    public String getValue(@NotNull String name, @NotNull String namespace) {
        final Map<String, String> nsmap = mTagMap.get(namespace);
        if (nsmap == null) {
            return null;
        }
        return nsmap.get(name);
    }

    /**
     * SSDPパケットに記述されているLocationヘッダの値を返す。
     *
     * @return Locationヘッダの値
     */
    @Nullable
    public String getLocation() {
        return mSsdp.getLocation();
    }

    /**
     * Locationに記述のIPアドレスを返す。
     *
     * 記述に異常がある場合は空文字が返る。
     *
     * @return IPアドレス
     */
    @NotNull
    public String getIpAddress() {
        try {
            final URL url = new URL(getLocation());
            return url.getHost();
        } catch (final MalformedURLException e) {
            return "";
        }
    }

    /**
     * UDNタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return UDNタグの値
     */
    @Nullable
    public String getUdn() {
        return mUdn;
    }

    /**
     * deviceTypeタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return deviceTypeタグの値
     */
    @Nullable
    public String getDeviceType() {
        return mDeviceType;
    }

    /**
     * friendlyNameタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return friendlyNameタグの値
     */
    @Nullable
    public String getFriendlyName() {
        return mFriendlyName;
    }

    /**
     * manufacturerタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return manufacturerタグの値
     */
    @Nullable
    public String getManufacture() {
        return mManufacture;
    }

    /**
     * manufacturerURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return manufacturerURLタグの値
     */
    @Nullable
    public String getManufactureUrl() {
        return mManufactureUrl;
    }

    /**
     * modelNameタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return modelNameタグの値
     */
    @Nullable
    public String getModelName() {
        return mModelName;
    }

    /**
     * modelURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return modelURLタグの値
     */
    @Nullable
    public String getModelUrl() {
        return mModelUrl;
    }

    /**
     * modelDescriptionタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return modelDescriptionタグの値
     */
    @Nullable
    public String getModelDescription() {
        return mModelDescription;
    }

    /**
     * modelNumberタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return modelNumberタグの値
     */
    @Nullable
    public String getModelNumber() {
        return mModelNumber;
    }

    /**
     * serialNumberタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return serialNumberタグの値
     */
    @Nullable
    public String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * presentationURLタグの値を返す。
     *
     * 値が存在しない場合nullが返る。
     *
     * @return presentationURLタグの値
     */
    @Nullable
    public String getPresentationUrl() {
        return mPresentationUrl;
    }

    /**
     * Iconのリストを返す。
     *
     * @return Iconのリスト
     * @see Icon
     */
    @NotNull
    public List<Icon> getIconList() {
        return Collections.unmodifiableList(mIconList);
    }

    /**
     * Serviceのリストを返す。
     *
     * @return Serviceのリスト
     * @see Service
     */
    @NotNull
    public List<Service> getServiceList() {
        return Collections.unmodifiableList(mServiceList);
    }

    /**
     * 指定文字列とSerivceIDが合致するサービスを返す。
     *
     * 見つからない場合はnullを返す。
     *
     * @param id サーチするID
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    public Service findServiceById(@NotNull String id) {
        for (final Service service : mServiceList) {
            if (service.getServiceId().equals(id)) {
                return service;
            }
        }
        return null;
    }

    /**
     * 指定文字列とSerivceTypeが合致するサービスを返す。
     *
     * 見つからない場合はnullを返す。
     *
     * @param type サーチするType
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    public Service findServiceByType(@NotNull String type) {
        for (final Service service : mServiceList) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }
        return null;
    }

    /**
     * 指定文字列の名前を持つActionを返す。
     *
     * 全サービスに対して検索をかけ、最初に見つかったものを返す。
     * 見つからない場合はnullを返す。
     *
     * 特定のサービスのActionを取得したい場合は、
     * {@link #findServiceById(String)} もしくは
     * {@link #findServiceByType(String)} を使用してServiceを取得した後、
     * {@link Service#findAction(String)} を使用する。
     *
     * @param name Action名
     * @return 見つかったAction
     * @see Service
     * @see Action
     */
    @Nullable
    public Action findAction(@NotNull String name) {
        for (final Service service : mServiceList) {
            final Action action = service.findAction(name);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return mUdn != null ? mUdn.hashCode() : 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Device)) {
            return false;
        }
        final Device d = (Device) obj;
        return mUdn.equals(d.getUdn());
    }

    @Override
    @NotNull
    public String toString() {
        return mFriendlyName != null ? mFriendlyName : "";
    }
}
