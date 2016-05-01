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
 * SSDPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpResponseMessage extends SsdpMessage {
    /**
     * 受信した情報からインスタンス作成
     *
     * @param ifa 受信したInterfaceAddress
     * @param dp 受信したDatagramPacket
     * @throws IOException 入出力エラー
     */
    public SsdpResponseMessage(InterfaceAddress ifa, DatagramPacket dp) throws IOException {
        super(ifa, dp);
    }

    @Override
    protected HttpMessage newMessage() {
        return new HttpResponse();
    }

    @Override
    protected HttpResponse getMessage() {
        return (HttpResponse) super.getMessage();
    }

    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see #getStatus()
     */
    public int getStatusCode() {
        return getMessage().getStatusCode();
    }

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setStatusCode(int code) {
        getMessage().setStatusCode(code);
    }

    /**
     * レスポンスフレーズを取得する
     *
     * @return レスポンスフレーズ
     * @see #getStatus()
     */
    public String getReasonPhrase() {
        return getMessage().getReasonPhrase();
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setReasonPhrase(String reasonPhrase) {
        getMessage().setReasonPhrase(reasonPhrase);
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(Http.Status status) {
        getMessage().setStatus(status);
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    public Http.Status getStatus() {
        return getMessage().getStatus();
    }
}
