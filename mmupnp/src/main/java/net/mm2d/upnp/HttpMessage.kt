/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * HTTPのメッセージを表現するinterface。
 *
 * ResponseとRequestでStart Lineのフォーマットが異なるため
 * その部分については個別に実装する。
 *
 * UPnPの通信でよく利用される小さなデータのやり取りに特化したもので、
 * 長大なデータのやり取りは想定していない。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 * @see HttpResponse
 * @see HttpRequest
 */
interface HttpMessage {

    /**
     * Start Lineを返す。
     *
     * @return Start Line
     */
    val startLine: String?

    /**
     * HTTPバージョンの値を返す。
     *
     * @return HTTPバージョン
     */
    val version: String

    /**
     * ヘッダの値からチャンク伝送か否かを返す。
     *
     * @return チャンク伝送の場合true
     */
    val isChunked: Boolean

    /**
     * ヘッダの値からKeepAliveか否かを返す。
     *
     * HTTP/1.0の場合、Connection: keep-aliveの場合に、
     * HTTP/1.1の場合、Connection: closeでない場合に、
     * KeepAliveと判定し、trueを返す。
     *
     * @return KeepAliveの場合true
     */
    val isKeepAlive: Boolean

    /**
     * Content-Lengthの値を返す。
     *
     * 不明な場合0
     *
     * @return Content-Lengthの値
     */
    val contentLength: Int

    /**
     * メッセージボディを返す。
     *
     * @return メッセージボディ
     */
    val body: String?

    /**
     * メッセージボディを返す。
     *
     * 取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return メッセージボディ
     */
    val bodyBinary: ByteArray?

    /**
     * メッセージを文字列として返す。
     *
     * @return メッセージ文字列
     */
    fun getMessageString(): String

    /**
     * Start Lineを設定する。
     *
     * @param line Start Line
     */
    @Throws(IllegalArgumentException::class)
    fun setStartLine(line: String)

    /**
     * HTTPバージョンを設定する。
     *
     * @param version HTTPバージョン
     */
    fun setVersion(version: String)

    /**
     * ヘッダを設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    fun setHeader(name: String, value: String)

    /**
     * ヘッダの各行からヘッダの設定を行う
     *
     * @param line ヘッダの1行
     */
    fun setHeaderLine(line: String)

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return ヘッダの値
     */
    fun getHeader(name: String): String?

    /**
     * メッセージボディを設定する。
     *
     * @param body メッセージボディ
     */
    fun setBody(body: String?)

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     */
    fun setBody(body: String?, withContentLength: Boolean)

    /**
     * メッセージボディを設定する。
     *
     *
     * 取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @param body メッセージボディ
     */
    fun setBodyBinary(body: ByteArray?)

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     */
    fun setBodyBinary(
        body: ByteArray?,
        withContentLength: Boolean
    )

    /**
     * 指定されたOutputStreamにメッセージの内容を書き出す。
     *
     * @param outputStream 出力先
     * @throws IOException 入出力エラー
     */
    @Throws(IOException::class)
    fun writeData(outputStream: OutputStream)

    /**
     * 指定されたInputStreamからデータの読み出しを行う。
     *
     * @param inputStream 入力元
     * @throws IOException 入出力エラー
     */
    @Throws(IOException::class)
    fun readData(inputStream: InputStream)
}
