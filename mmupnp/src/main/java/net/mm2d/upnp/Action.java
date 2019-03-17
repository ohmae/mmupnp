/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Actionを表現するインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface Action {
    /**
     * エラーレスポンスの faultcode を格納するkey。
     *
     * <p>正常な応答であれば、SOAPのnamespaceがついた"Client"が格納されている。
     */
    String FAULT_CODE_KEY = "faultcode";
    /**
     * エラーレスポンスの faultstring を格納するkey。
     *
     * <p>正常な応答であれば、"UPnPError"が格納されている。
     */
    String FAULT_STRING_KEY = "faultstring";
    /**
     * エラーレスポンスの detail/UPnPError/errorCode を格納するkey。
     */
    String ERROR_CODE_KEY = "UPnPError/errorCode";
    /**
     * エラーレスポンスの detail/UPnPError/errorDescription を格納するkey.
     */
    String ERROR_DESCRIPTION_KEY = "UPnPError/errorDescription";

    /**
     * このActionを保持するServiceを返す。
     *
     * @return このActionを保持するService
     */
    @Nonnull
    Service getService();

    /**
     * Action名を返す。
     *
     * @return Action名
     */
    @Nonnull
    String getName();

    /**
     * Argumentリストを返す。
     *
     * <p>リストは変更不可であり、
     * 変更しようとするとUnsupportedOperationExceptionが発生する。
     *
     * @return Argumentリスト
     */
    @Nonnull
    List<Argument> getArgumentList();

    /**
     * 指定名に合致するArgumentを返す。
     *
     * @param name Argument名
     * @return Argument
     */
    @Nullable
    Argument findArgument(@Nonnull String name);

    /**
     * Actionを同期実行する。
     *
     * @param argumentValues 引数への入力値
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see #invokeSync(Map, boolean)
     * @see #invoke(Map, ActionCallback)
     */
    @Nonnull
    Map<String, String> invokeSync(@Nonnull Map<String, String> argumentValues) throws IOException;

    /**
     * Actionを同期実行する。
     *
     * @param argumentValues      引数への入力値
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see #invoke(Map, boolean, ActionCallback)
     * @see #FAULT_CODE_KEY
     * @see #FAULT_STRING_KEY
     * @see #ERROR_CODE_KEY
     * @see #ERROR_DESCRIPTION_KEY
     */
    @Nonnull
    Map<String, String> invokeSync(
            @Nonnull Map<String, String> argumentValues,
            boolean returnErrorResponse)
            throws IOException;

    /**
     * Actionを同期実行する。【試験的実装】
     *
     * @param argumentValues  引数への入力値
     * @param customNamespace カスタム引数のNamespace情報、不要な場合null
     * @param customArguments カスタム引数
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see #invokeCustomSync(Map, Map, Map, boolean)
     * @see #invokeCustom(Map, Map, Map, ActionCallback)
     */
    @Nonnull
    Map<String, String> invokeCustomSync(
            @Nonnull Map<String, String> argumentValues,
            @Nullable Map<String, String> customNamespace,
            @Nonnull Map<String, String> customArguments)
            throws IOException;

    /**
     * Actionを同期実行する。【試験的実装】
     *
     * @param argumentValues      引数への入力値
     * @param customNamespace     カスタム引数のNamespace情報、不要な場合null
     * @param customArguments     カスタム引数
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see #invokeCustom(Map, Map, Map, boolean, ActionCallback)
     * @see #FAULT_CODE_KEY
     * @see #FAULT_STRING_KEY
     * @see #ERROR_CODE_KEY
     * @see #ERROR_DESCRIPTION_KEY
     */
    @Nonnull
    Map<String, String> invokeCustomSync(
            @Nonnull Map<String, String> argumentValues,
            @Nullable Map<String, String> customNamespace,
            @Nonnull Map<String, String> customArguments,
            boolean returnErrorResponse)
            throws IOException;

    /**
     * Actionを実行する。
     *
     * <p>実行後エラー応答を受け取った場合は、IOExceptionを発生させる。
     * エラー応答の内容を取得する必要がある場合は{@link #invokeSync(Map, boolean)}を使用し、第二引数にtrueを指定する。
     * このメソッドは{@link #invokeSync(Map, boolean)}の第二引数にfalseを指定した場合と等価である。
     *
     * <p>実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     * Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われない。
     *
     * <p>引数として渡したMapの中にArgumentに記載のない値を設定していても無視される。
     *
     * <p>引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * <p>実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * @param argumentValues 引数への入力値
     * @param callback       実行結果のコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.Params#setCallbackExecutor(TaskExecutor)
     * @see #invoke(Map, boolean, ActionCallback)
     */
    void invoke(
            @Nonnull Map<String, String> argumentValues,
            @Nullable ActionCallback callback);

    /**
     * Actionを実行する。
     *
     * <p>実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     * Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われない。
     *
     * <p>引数として渡したMapの中にArgumentに記載のない値を設定していても無視される。
     *
     * <p>引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * <p>実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * <p>第二引数がfalseの場合、エラーレスポンスが返却された場合は、IOExceptionを発生させる。
     * trueを指定すると、エラーレスポンスもパースして戻り値として返却する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを発生させる。
     *
     * @param argumentValues      引数への入力値
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @param callback            実行結果のコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.Params#setCallbackExecutor(TaskExecutor)
     * @see #FAULT_CODE_KEY
     * @see #FAULT_STRING_KEY
     * @see #ERROR_CODE_KEY
     * @see #ERROR_DESCRIPTION_KEY
     */
    void invoke(
            @Nonnull Map<String, String> argumentValues,
            boolean returnErrorResponse,
            @Nullable ActionCallback callback);

    /**
     * Actionを実行する。【試験的実装】
     *
     * <p>※試験的実装であり、将来的に変更、削除される可能性が高い
     *
     * <p>実行後エラー応答を受け取った場合は、IOExceptionを発生させる。
     * エラー応答の内容を取得する必要がある場合は{@link #invokeCustomSync(Map, Map, Map, boolean)}を使用し、第四引数にtrueを指定する。
     * このメソッドは{@link #invokeCustomSync(Map, Map, Map, boolean)}の第四引数にfalseを指定した場合と等価である。
     *
     * <p>実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     * Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われない。
     *
     * <p>第一引数として渡したMapの中にArgumentに記載のない値を設定していても無視される。
     *
     * <p>引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * <p>第二引数として第三引数で使用するNamespaceを指定する。不要であればnullを指定する。
     * StringPairのリストであり、keyとしてprefixを、valueとしてURIを指定する。
     * key/valueともにnullを指定することはできない。
     * この引数によって与えたNamespaceはAction Elementに追加される。
     *
     * <p>第三引数として渡したStringPairのリストは純粋にSOAP XMLのAction Elementの子要素として追加される。
     * keyとして引数名、valueとして値を指定する。keyはnullであってはならない。valueがnullの場合は空の引数指定となる。
     * この際Argumentの値との関係性はチェックされずすべてがそのまま追加される。
     * ただし、Namespaceとして登録されないprefixを持っているなどXMLとして不正な引数を与えると失敗する。
     *
     * <p>実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * @param argumentValues  引数への入力値
     * @param customNamespace カスタム引数のNamespace情報、不要な場合null
     * @param customArguments カスタム引数
     * @param callback        実行結果のコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.Params#setCallbackExecutor(TaskExecutor)
     * @see #invokeCustom(Map, Map, Map, boolean, ActionCallback)
     */
    void invokeCustom(
            @Nonnull Map<String, String> argumentValues,
            @Nullable Map<String, String> customNamespace,
            @Nonnull Map<String, String> customArguments,
            @Nullable ActionCallback callback);

    /**
     * Actionを実行する。【試験的実装】
     *
     * <p>※試験的実装であり、将来的に変更、削除される可能性が高い
     *
     * <p>実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     * Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われない。
     *
     * <p>第一引数として渡したMapの中にArgumentに記載のない値を設定していても無視される。
     *
     * <p>引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * <p>第二引数として第三引数で使用するNamespaceを指定する。不要であればnullを指定する。
     * StringPairのリストであり、keyとしてprefixを、valueとしてURIを指定する。
     * key/valueともにnullを指定することはできない。
     * この引数によって与えたNamespaceはAction Elementに追加される。
     *
     * <p>第三引数として渡したStringPairのリストは純粋にSOAP XMLのAction Elementの子要素として追加される。
     * keyとして引数名、valueとして値を指定する。keyはnullであってはならない。valueがnullの場合は空の引数指定となる。
     * この際Argumentの値との関係性はチェックされずすべてがそのまま追加される。
     * ただし、Namespaceとして登録されないprefixを持っているなどXMLとして不正な引数を与えると失敗する。
     *
     * <p>実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * <p>第四引数がfalseの場合、エラーレスポンスが返却された場合は、IOExceptionを発生させる。
     * trueを指定すると、エラーレスポンスもパースして戻り値として返却する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを発生させる。
     *
     * @param argumentValues      引数への入力値
     * @param customNamespace     カスタム引数のNamespace情報、不要な場合null
     * @param customArguments     カスタム引数
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @param callback            実行結果のコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.Params#setCallbackExecutor(TaskExecutor)
     * @see #FAULT_CODE_KEY
     * @see #FAULT_STRING_KEY
     * @see #ERROR_CODE_KEY
     * @see #ERROR_DESCRIPTION_KEY
     */
    void invokeCustom(
            @Nonnull Map<String, String> argumentValues,
            @Nullable Map<String, String> customNamespace,
            @Nonnull Map<String, String> customArguments,
            boolean returnErrorResponse,
            @Nullable ActionCallback callback);

    interface ActionCallback {
        void onResult(@Nonnull Map<String, String> result);

        void onError(@Nonnull IOException e);
    }
}
