/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InterfaceAddress;

/**
 * SSDPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpRequestMessage extends SsdpMessage {
    /**
     * インスタンス作成
     */
    public SsdpRequestMessage() {
        super();
    }

    /**
     * 受信した情報からインスタンス作成
     *
     * @param ifa 受信したInterfaceAddress
     * @param dp 受信したDatagramPacket
     * @throws IOException 入出力エラー
     */
    public SsdpRequestMessage(InterfaceAddress ifa, DatagramPacket dp) throws IOException {
        super(ifa, dp);
    }

    @Override
    protected HttpMessage newMessage() {
        return new HttpRequest();
    }

    @Override
    protected HttpRequest getMessage() {
        return (HttpRequest) super.getMessage();
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    public String getMethod() {
        return getMessage().getMethod();
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    public void setMethod(String method) {
        getMessage().setMethod(method);
    }

    /**
     * URIを返す。
     *
     * @return URI文字列
     */
    public String getUri() {
        return getMessage().getUri();
    }

    /**
     * URIを設定する。
     *
     * @param uri URI文字列
     */
    public void setUri(String uri) {
        getMessage().setUri(uri);
    }
}
