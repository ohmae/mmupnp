/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.internal.message.HttpRequest;
import net.mm2d.upnp.internal.message.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPのメッセージを表現するinterface。
 *
 * <p>ResponseとRequestでStart Lineのフォーマットが異なるため
 * その部分については個別に実装する。
 *
 * <p>UPnPの通信でよく利用される小さなデータのやり取りに特化したもので、
 * 長大なデータのやり取りは想定していない。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 * @see HttpResponse
 * @see HttpRequest
 */
public interface HttpMessage {

    /**
     * Start Lineを返す。
     *
     * @return Start Line
     */
    @Nullable
    String getStartLine();

    /**
     * Start Lineを設定する。
     *
     * @param line Start Line
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setStartLine(@Nonnull String line) throws IllegalArgumentException;

    /**
     * HTTPバージョンの値を返す。
     *
     * @return HTTPバージョン
     */
    @Nonnull
    String getVersion();

    /**
     * HTTPバージョンを設定する。
     *
     * @param version HTTPバージョン
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setVersion(@Nonnull String version);

    /**
     * ヘッダを設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setHeader(
            @Nonnull String name,
            @Nonnull String value);

    /**
     * ヘッダの各行からヘッダの設定を行う
     *
     * @param line ヘッダの1行
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setHeaderLine(@Nonnull String line);

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return ヘッダの値
     */
    @Nullable
    String getHeader(@Nonnull String name);

    /**
     * ヘッダの値からチャンク伝送か否かを返す。
     *
     * @return チャンク伝送の場合true
     */
    boolean isChunked();

    /**
     * ヘッダの値からKeepAliveか否かを返す。
     *
     * <p>HTTP/1.0の場合、Connection: keep-aliveの場合に、
     * HTTP/1.1の場合、Connection: closeでない場合に、
     * KeepAliveと判定し、trueを返す。
     *
     * @return KeepAliveの場合true
     */
    boolean isKeepAlive();

    /**
     * Content-Lengthの値を返す。
     *
     * <p>不明な場合0
     *
     * @return Content-Lengthの値
     */
    int getContentLength();

    /**
     * メッセージボディを設定する。
     *
     * @param body メッセージボディ
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setBody(@Nullable String body);

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setBody(
            @Nullable String body,
            boolean withContentLength);

    /**
     * メッセージボディを設定する。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @param body メッセージボディ
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setBodyBinary(@Nullable byte[] body);

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     * @return HttpMessage
     */
    @Nonnull
    HttpMessage setBodyBinary(
            @Nullable byte[] body,
            boolean withContentLength);

    /**
     * メッセージボディを返す。
     *
     * @return メッセージボディ
     */
    @Nullable
    String getBody();

    /**
     * メッセージボディを返す。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return メッセージボディ
     */
    @Nullable
    byte[] getBodyBinary();

    /**
     * メッセージを文字列として返す。
     *
     * @return メッセージ文字列
     */
    @Nonnull
    String getMessageString();

    /**
     * 指定されたOutputStreamにメッセージの内容を書き出す。
     *
     * @param outputStream 出力先
     * @throws IOException 入出力エラー
     */
    void writeData(@Nonnull OutputStream outputStream) throws IOException;

    /**
     * 指定されたInputStreamからデータの読み出しを行う。
     *
     * @param inputStream 入力元
     * @return HttpMessage
     * @throws IOException 入出力エラー
     */
    HttpMessage readData(@Nonnull InputStream inputStream) throws IOException;
}
