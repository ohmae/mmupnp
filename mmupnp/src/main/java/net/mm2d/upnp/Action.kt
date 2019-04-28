/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException

/**
 * Interface of UPnP Action.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Action {

    /**
     * Return the Service that is the owner of this Action.
     *
     * @return Service
     */
    val service: Service

    /**
     * Return the Action name.
     *
     * @return Action name
     */
    val name: String

    /**
     * Return the Argument list.
     *
     * List is the immutable.
     * When the modification method is called, UnsupportedOperationException will be thrown.
     *
     * @return Argument list
     */
    val argumentList: List<Argument>

    /**
     * Find the Argument by name
     *
     * @param name Argument name
     * @return Argument
     */
    fun findArgument(name: String): Argument?

    /**
     * Invoke this Action synchronously.
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * @param argumentValues      Input value to argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @return Invocation result
     * @throws IOException If any exception occurs while communication or there is an error response
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    @Throws(IOException::class)
    fun invokeSync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    /**
     * Invoke this Action synchronously. (Experimental function)
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * Use [customArguments] if you want to add non-existent arguments to [argumentValues].
     * This is added as a child element of SOAP XML Action Element.
     * If a prefix that is not in the existing namespace is specified, it fails and throws IOException.
     *
     * When using [customArguments], if there is an additional namespace required, specify it with [customNamespace]
     * Designate with `Map<String, String>`, specify prefix as key and URI as value.
     * The Namespace given by this argument is added to the Action Element.
     *
     * @param argumentValues      Input value to argument
     * @param customNamespace     Namespace of custom argument
     * @param customArguments     Custom argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @return Invocation result
     * @throws IOException If any exception occurs while communication or there is an error response
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    @Throws(IOException::class)
    fun invokeCustomSync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String> = emptyMap(),
        customArguments: Map<String, String> = emptyMap(),
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    /**
     * Invoke this Action asynchronously.
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * @param argumentValues      Input value to argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @param onResult            Callback to notify the result. It will be Executed in callback thread.
     * @param onError             Callback to notify error. It will be Executed in callback thread.
     * @throws IOException If any exception occurs while communication or there is an error response
     * @see ControlPointFactory.create
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    fun invoke(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean = false,
        onResult: ((Map<String, String>) -> Unit)? = null,
        onError: ((IOException) -> Unit)? = null
    )

    /**
     * Invoke this Action asynchronously. (Experimental function)
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * Use [customArguments] if you want to add non-existent arguments to [argumentValues].
     * This is added as a child element of SOAP XML Action Element.
     * If a prefix that is not in the existing namespace is specified, it fails and throws IOException.
     *
     * When using [customArguments], if there is an additional namespace required, specify it with [customNamespace]
     * Designate with `Map<String, String>`, specify prefix as key and URI as value.
     * The Namespace given by this argument is added to the Action Element.
     *
     * @param argumentValues      Input value to argument
     * @param customNamespace     Namespace of custom argument
     * @param customArguments     Custom argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @param onResult            Callback to notify the result. It will be Executed in callback thread.
     * @param onError             Callback to notify error. It will be Executed in callback thread.
     * @see ControlPointFactory.create
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String> = emptyMap(),
        customArguments: Map<String, String> = emptyMap(),
        returnErrorResponse: Boolean = false,
        onResult: ((Map<String, String>) -> Unit)? = null,
        onError: ((IOException) -> Unit)? = null
    )

    /**
     * Invoke this Action asynchronously.
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * @param argumentValues      Input value to argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @return Invocation result
     * @throws IOException If any exception occurs while communication or there is an error response
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    suspend fun invokeAsync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    /**
     * Invoke this Action asynchronously. (Experimental function)
     *
     * Execution arguments and execution results are represented by Map with argument name as key and value as value.
     * All values ​​are treat as String.
     *
     * Both arguments and return values ​​are not checked according to DataType and AllowedValue of Argument (StateVariable),
     * but the values that are not described in Argument are ignored even if it is in [argumentValues].
     *
     * When there are no required arguments, the value is set only if a default value is defined in StateVariable.
     * If not, it is treated as empty even if it violates DataType.
     *
     * If the execution result contains a value not described in Argument, it will not be ignored,
     * and its key / value will be set in the map of return values in the same way as the one described in Argument.
     *
     * If [returnErrorResponse] is false, IOException is thrown when an error response is returned.
     * If true, error response will be parsed and returned as return value.
     * In this case, if the error response conforms to the specification,
     * 'faultcode','faultstring','UPnPError/errorCode', are included as a key of the return value Map,
     * and 'UPnPError/errorDescription' may also be included.
     * In this method, if 'UPnPError/errorCode' is not included, It is treated as an error and IOException is thrown.
     *
     * Use [customArguments] if you want to add non-existent arguments to [argumentValues].
     * This is added as a child element of SOAP XML Action Element.
     * If a prefix that is not in the existing namespace is specified, it fails and throws IOException.
     *
     * When using [customArguments], if there is an additional namespace required, specify it with [customNamespace]
     * Designate with `Map<String, String>`, specify prefix as key and URI as value.
     * The Namespace given by this argument is added to the Action Element.
     *
     * @param argumentValues      Input value to argument
     * @param customNamespace     Namespace of custom argument
     * @param customArguments     Custom argument
     * @param returnErrorResponse When an error response is received, if true,
     * the error is also parsed and returned as a return value. If false, throws IOException.
     * @return Invocation result
     * @throws IOException If any exception occurs while communication or there is an error response
     * @see FAULT_CODE_KEY
     * @see FAULT_STRING_KEY
     * @see ERROR_CODE_KEY
     * @see ERROR_DESCRIPTION_KEY
     */
    suspend fun invokeCustomAsync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String> = emptyMap(),
        customArguments: Map<String, String> = emptyMap(),
        returnErrorResponse: Boolean = false
    ): Map<String, String>

    companion object {
        /**
         * The key used to store the error response of `faultcode`.
         *
         * If it is a normal error response, "Client" with a namespace of SOAP is stored.
         */
        const val FAULT_CODE_KEY = "faultcode"
        /**
         * The key used to store the error response of `faultstring`.
         *
         * If it is a normal error response, "UPnPError" is stored.
         */
        const val FAULT_STRING_KEY = "faultstring"
        /**
         * The key used to store the error response of `detail/UPnPError/errorCode`.
         */
        const val ERROR_CODE_KEY = "UPnPError/errorCode"
        /**
         * The key used to store the error response of `detail/UPnPError/errorDescription`.
         */
        const val ERROR_DESCRIPTION_KEY = "UPnPError/errorDescription"
    }
}
