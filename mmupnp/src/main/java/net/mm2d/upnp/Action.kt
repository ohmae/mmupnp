/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException

/**
 * Actionを表現するインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Action {

    /**
     * このActionを保持するServiceを返す。
     *
     * @return このActionを保持するService
     */
    val service: Service

    /**
     * Action名を返す。
     *
     * @return Action名
     */
    val name: String

    /**
     * Argumentリストを返す。
     *
     * リストは変更不可であり、
     * 変更しようとするとUnsupportedOperationExceptionが発生する。
     *
     * @return Argumentリスト
     */
    val argumentList: List<Argument>

    /**
     * 指定名に合致するArgumentを返す。
     *
     * @param name Argument名
     * @return Argument
     */
    fun findArgument(name: String): Argument?

    /**
     * Actionを同期実行する。
     *
     * 実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     *
     * 引数、戻り値ともに、Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われないが、
     *
     * [argumentValues]にArgumentに記載のない値を設定していても無視される。
     * 引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * 実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * [returnErrorResponse]がfalseの場合、エラーレスポンスが返却された場合は、IOExceptionを発生させる。
     * trueを指定すると、エラーレスポンスもパースして戻り値として返却する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを発生させる。
     *
     * @param argumentValues      引数への入力値
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    @Throws(IOException::class)
    fun invokeSync(
        argumentValues: Map<String, String>,
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    /**
     * Actionを同期実行する。【試験的実装】
     *
     * 実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     *
     * 引数、戻り値ともに、Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われないが、
     *
     * [argumentValues]にArgumentに記載のない値を設定していても無視される。
     * 引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * 実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * [returnErrorResponse]がfalseの場合、エラーレスポンスが返却された場合は、IOExceptionを発生させる。
     * trueを指定すると、エラーレスポンスもパースして戻り値として返却する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを発生させる。
     *
     * [customNamespace]として[customArguments]で使用するNamespaceを指定する。
     * Map<String, String>で指定し、keyにprefixを、valueにURIを指定する。
     * この引数によって与えたNamespaceはAction Elementに追加される。
     *
     * [customArguments]として渡したMap<String, String>は純粋にSOAP XMLのAction Elementの子要素として追加される。
     * keyとして引数名、valueとして値を指定する。
     * Argumentの値との関係性はチェックされずすべてがそのまま追加される。
     * ただし、Namespaceとして登録されないprefixを持っているなどXMLとして不正な引数を与えると失敗し、
     * IOExceptionを発生させる。
     *
     * @param argumentValues      引数への入力値
     * @param customNamespace     カスタム引数のNamespace情報、不要な場合null
     * @param customArguments     カスタム引数
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    @Throws(IOException::class)
    fun invokeCustomSync(
        argumentValues: Map<String, String>,
        customNamespace: Map<String, String> = emptyMap(),
        customArguments: Map<String, String> = emptyMap(),
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    /**
     * Actionを非同期実行する。
     *
     * 実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     *
     * 引数、結果ともに、Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われないが、
     *
     * [argumentValues]にArgumentに記載のない値を設定していても無視される。
     * 引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * 実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * [returnErrorResponse]がfalseの場合で、エラーレスポンスが返却された場合は、IOExceptionを通知する。
     * trueを指定すると、エラーレスポンスもパースして通知する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを通知する。
     *
     * @param argumentValues      引数への入力値
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @param onResult            結果を通知するコールバック。callbackスレッドで実行される。
     * @param onError             エラー発生を通知するコールバック。callbackスレッドで実行される。
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    fun invoke(
        argumentValues: Map<String, String>,
        returnErrorResponse: Boolean = false,
        onResult: ((Map<String, String>) -> Unit)? = null,
        onError: ((IOException) -> Unit)? = null
    )

    /**
     * Actionを非同期実行する。【試験的実装】
     *
     * 実行引数及び実行結果は引数名をkeyとし、値をvalueとしたMapで表現する。
     * 値はすべてStringで表現する。
     *
     * 引数、結果ともに、Argument(StateVariable)のDataTypeやAllowedValueに応じた値チェックは行われないが、
     *
     * [argumentValues]にArgumentに記載のない値を設定していても無視される。
     * 引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が設定される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     *
     * 実行結果にArgumentに記載のない値が入っていた場合は無視することはなく、
     * Argumentに記載のあったものと同様にkey/valueの形で戻り値のMapに設定される。
     *
     * [returnErrorResponse]がfalseの場合で、エラーレスポンスが返却された場合は、IOExceptionを通知する。
     * trueを指定すると、エラーレスポンスもパースして通知する。
     * この場合、戻り値のMapのkeyとして
     * エラーレスポンスが仕様に従うなら'faultcode','faultstring','UPnPError/errorCode',が含まれ
     * 'UPnPError/errorDescription'も含まれている場合がある。
     * このメソッドでは'UPnPError/errorCode'が含まれていない場合は、
     * エラーレスポンスの異常として、IOExceptionを通知する。
     *
     * [customNamespace]として[customArguments]で使用するNamespaceを指定する。
     * Map<String, String>で指定し、keyにprefixを、valueにURIを指定する。
     * この引数によって与えたNamespaceはAction Elementに追加される。
     *
     * [customArguments]として渡したMap<String, String>は純粋にSOAP XMLのAction Elementの子要素として追加される。
     * keyとして引数名、valueとして値を指定する。
     * Argumentの値との関係性はチェックされずすべてがそのまま追加される。
     * ただし、Namespaceとして登録されないprefixを持っているなどXMLとして不正な引数を与えると失敗し、
     * IOExceptionを通知する。
     *
     * @param argumentValues      引数への入力値
     * @param customNamespace     カスタム引数のNamespace情報、不要な場合null
     * @param customArguments     カスタム引数
     * @param returnErrorResponse エラーレスポンス受信時の処理を指定、trueにするとエラーもパースして戻り値で返す。falseにするとIOExceptionを発生させる。
     * @param onResult            結果を通知するコールバック。callbackスレッドで実行される。
     * @param onError             エラー発生を通知するコールバック。callbackスレッドで実行される。
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    fun invokeCustom(
        argumentValues: Map<String, String>,
        customNamespace: Map<String, String> = emptyMap(),
        customArguments: Map<String, String> = emptyMap(),
        returnErrorResponse: Boolean = false,
        onResult: ((Map<String, String>) -> Unit)? = null,
        onError: ((IOException) -> Unit)? = null
    )

    companion object {
        /**
         * エラーレスポンスの faultcode を格納するkey。
         *
         * 正常な応答であれば、SOAPのnamespaceがついた"Client"が格納されている。
         */
        const val FAULT_CODE_KEY = "faultcode"
        /**
         * エラーレスポンスの faultstring を格納するkey。
         *
         * 正常な応答であれば、"UPnPError"が格納されている。
         */
        const val FAULT_STRING_KEY = "faultstring"
        /**
         * エラーレスポンスの detail/UPnPError/errorCode を格納するkey。
         */
        const val ERROR_CODE_KEY = "UPnPError/errorCode"
        /**
         * エラーレスポンスの detail/UPnPError/errorDescription を格納するkey.
         */
        const val ERROR_DESCRIPTION_KEY = "UPnPError/errorDescription"
    }
}
