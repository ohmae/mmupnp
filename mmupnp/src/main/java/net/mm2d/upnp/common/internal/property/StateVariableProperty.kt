/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

/**
 * Interface of UPnP StateVariable.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class StateVariableProperty(
    /**
     * Return the status of SendEvents.
     *
     * @return true: SendEvents is "yes", false: "no"
     */
    val isSendEvents: Boolean,

    /**
     * Return the status of Multicast.
     *
     * @return true: Multicast is "yes", false: "no"
     */
    val isMulticast: Boolean,

    /**
     * Return the name of StateVariable.
     *
     * @return StateVariable名
     */
    val name: String,

    /**
     * Return the value of DataType.
     *
     * The following data types are defined in UPnP.
     * If none of these apply, no check is made.
     *
     * DataType:
     * - ui1
     *     - Unsigned 1 Byte int. Same format as int without leading sign.
     * - ui2
     *     - Unsigned 2 Byte int. Same format as int without leading sign.
     * - ui4
     *     - Unsigned 4 Byte int. Same format as int without leading sign.
     * - ui8
     *     -  Unsigned 8 Byte int. Same format as int without leading sign.
     * - i1
     *     - 1 Byte int. Same format as int.
     * - i2
     *     - 2 Byte int. Same format as int.
     * - i4
     *     - 4 Byte int. Same format as int. shall be between -2147483648 and 2147483647.
     * - i8
     *     - 8 Byte int. Same format as int. shall be between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807,
     *     from -2^63 to 2^63 - 1.
     * - int
     *     - Fixed point, integer number.
     *     Is allowed to have leading sign.
     *     Is allowed to have leading zeros, which should be ignored by the recipient. (No currency symbol.)
     *     (No grouping of digits to the left of the decimal, e.g., no commas.)
     * - r4
     *     - 4 Byte float. Same format as float. shall be between 3.40282347E+38 to 1.17549435E-38.
     * - r8
     *     - 8 Byte float. Same format as float. shall be between -1.79769313486232E308 and -4.94065645841247E-324
     *     for negative values, and between 4.94065645841247E-324 and 1.79769313486232E308 for positive values,
     *     i.e., IEEE 64-bit (8-Byte) double.
     * - number
     *     - Same as r8.
     * - fixed.14.4
     *     - ame as r8 but no more than 14 digits to the left of the decimal point and no more than 4 to the right.
     * - float
     *     - Floating point number. Mantissa (left of the decimal) and/or exponent is allowed to have a leading sign.
     *     Mantissa and/or exponent Is allowed to have leading zeros, which should be ignored by the recipient.
     *     Decimal character in mantissa is a period,
     *     i.e., whole digits in mantissa separated from fractional digits by period (".").
     *     Mantissa separated from exponent by "E". (No currency symbol.)
     *     (No grouping of digits in the mantissa, e.g., no commas.)
     * - char
     *     - Unicode string. One character long.
     * - string
     *     - Unicode string. No limit on length.
     * - date
     *     - Date in a subset of ISO 8601 format without time data.
     * - dateTime
     *     - Date in ISO 8601 format with allowed time but no time zone.
     * - dateTime.tz
     *     - Date in ISO 8601 format with allowed time and allowed time zone.
     * - time
     *     - Time in a subset of ISO 8601 format with no date and no time zone.
     * - time.tz
     *     - Time in a subset of ISO 8601 format with allowed time zone but no date.
     * - boolean
     *     - "0" for false or "1" for true.
     *     The values "true", "yes", "false", or "no" are deprecated and shall not be sent but shall be accepted when received.
     *     When received, the values "true" and "yes" shall be interpreted as true and the values "false" and "no" shall be interpreted as false.
     * - bin.base64
     *     - MIME-style Base64 encoded binary BLOB.
     *     Takes 3 Bytes, splits them into 4 parts, and maps each 6 bit piece to an octet.
     *     (3 octets are encoded as 4.) No limit on size.
     * - bin.hex
     *     - Hexadecimal digits representing octets.
     *     Treats each nibble as a hex digit and encodes as a separate Byte.
     *     (1 octet is encoded as 2.) No limit on size.
     * - uri
     *     - Universal Resource Identifier.
     * - uuid
     *     - Universally Unique ID. See https://pubs.opengroup.org/onlinepubs/9629399/apdxa.htm
     *
     * @return DataType
     */
    val dataType: String,

    /**
     * Return the value of AllowedValueList.
     *
     * @return AllowedValueList
     */
    val allowedValueList: List<String>,

    /**
     * Return the value of DefaultValue.
     *
     * @return DefaultValue
     */
    val defaultValue: String?,

    /**
     * Return the value of Minimum.
     *
     * @return Minimum
     */
    val minimum: String?,

    /**
     * Return the value of Maximum.
     *
     * @return Maximum
     */
    val maximum: String?,

    /**
     * Return the value of Step.
     *
     * @return Step
     */
    val step: String?
) {
    class Builder {
        var isSendEvents: Boolean = false
        var isMulticast: Boolean = false
        var name: String? = null
        var dataType: String? = null
        val allowedValueList: MutableList<String> = mutableListOf()
        var defaultValue: String? = null
        var minimum: String? = null
        var maximum: String? = null
        var step: String? = null

        fun build(): StateVariableProperty {
            val name = name
                ?: throw IllegalStateException("name must be set.")
            val dataType = dataType
                ?: throw IllegalStateException("dataType must be set.")

            return StateVariableProperty(
                isSendEvents = isSendEvents,
                isMulticast = isMulticast,
                name = name,
                dataType = dataType,
                allowedValueList = allowedValueList,
                defaultValue = defaultValue,
                minimum = minimum,
                maximum = maximum,
                step = step
            )
        }
    }
}
