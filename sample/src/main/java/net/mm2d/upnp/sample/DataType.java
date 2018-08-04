/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * DataTypeを表現するenum。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public enum DataType {

    /**
     * Unsigned 1 Byte int. Same format as int without leading sign.
     */
    UI1("ui1", "0", true, true, false),
    /**
     * Unsigned 2 Byte int. Same format as int without leading sign.
     */
    UI2("ui2", "0", true, true, false),
    /**
     * Unsigned 4 Byte int. Same format as int without leading sign.
     */
    UI4("ui4", "0", true, true, false),
    /**
     * Unsigned 8 Byte int. Same format as int without leading sign.
     */
    UI8("ui8", "0", true, true, false),
    /**
     * 1 Byte int. Same format as int.
     */
    I1("i1", "0", true, true, false),
    /**
     * 2 Byte int. Same format as int.
     */
    I2("i2", "0", true, true, false),
    /**
     * 4 Byte int. Same format as int. shall be between -2147483648 and 2147483647.
     */
    I4("i4", "0", true, true, false),
    /**
     * 8 Byte int. Same format as int. shall be between -9,223,372,036,854,775,808
     * and 9,223,372,036,854,775,807, from .(263) to 263 - 1.
     */
    I8("i8", "0", true, true, false),
    /**
     * Fixed point, integer number. Is allowed to have leading sign.
     * Is allowed to have leading zeros, which should be ignored by the recipient. (No currency symbol.)
     * (No grouping of digits to the left of the decimal, e.g., no commas.)
     */
    INT("int", "0", true, true, false),
    /**
     * 4 Byte float. Same format as float. shall be between 3.40282347E+38 to 1.17549435E-38.
     */
    R4("r4", "0.0", true, false, false),
    /**
     * 8 Byte float. Same format as float.
     * shall be between -1.79769313486232E308 and -4.94065645841247E-324 for negative values,
     * and between 4.94065645841247E-324 and 1.79769313486232E308 for positive values,
     * i.e., IEEE 64-bit (8-Byte) double.
     */
    R8("r8", "0.0", true, false, false),
    /**
     * Same as r8.
     */
    NUMBER("number", "0.0", true, false, false),
    /**
     * Same as r8 but no more than 14 digits to the left of the decimal point and no more than 4 to the right.
     */
    FIXED_14_4("fixed.14.4", "0.0", true, false, false),
    /**
     * Floating point number. Mantissa (left of the decimal) and/or exponent is allowed to have a leading sign.
     * Mantissa and/or exponent Is allowed to have leading zeros, which should be ignored by the recipient.
     * Decimal character in mantissa is a period, i.e.,
     * whole digits in mantissa separated from fractional digits by period (".").
     * Mantissa separated from exponent by "E". (No currency symbol.)
     * (No grouping of digits in the mantissa, e.g., no commas.)
     */
    FLOAT("float", "0.0", true, false, false),
    /**
     * Unicode string. One character long.
     */
    CHAR("char", "", false, false, false),
    /**
     * Unicode string. No limit on length.
     */
    STRING("string", "", false, false, true),
    /**
     * Date in a subset of ISO 8601 format without time data.
     */
    DATE("date", "", false, false, false),
    /**
     * Date in ISO 8601 format with allowed time but no time zone.
     */
    DATE_TIME("dateTime", "", false, false, false),
    /**
     * Date in ISO 8601 format with allowed time and allowed time zone.
     */
    DATE_TIME_TZ("dateTime.tz", "", false, false, false),
    /**
     * Time in a subset of ISO 8601 format with no date and no time zone.
     */
    TIME("time", "", false, false, false),
    /**
     * Time in a subset of ISO 8601 format with allowed time zone but no date.
     */
    TIME_TZ("time.tz", "", false, false, false),
    /**
     * "0" for false or "1" for true. The values "true", "yes", "false", or "no" are deprecated
     * and shall not be sent but shall be accepted when received. When received, the values "true"
     * and "yes" shall be interpreted as true and the values "false" and "no" shall be interpreted as false.
     */
    BOOLEAN("boolean", "", false, false, false),
    /**
     * MIME-style Base64 encoded binary BLOB. Takes 3 Bytes, splits them into 4 parts,
     * and maps each 6 bit piece to an octet. (3 octets are encoded as 4.) No limit on size.
     */
    BIN_BASE64("bin.base64", "", false, false, true),
    /**
     * Hexadecimal digits representing octets. Treats each nibble as a hex digit
     * and encodes as a separate Byte. (1 octet is encoded as 2.) No limit on size.
     */
    BIN_HEX("bin.hex", "", false, false, true),
    /**
     * Universal Resource Identifier.
     */
    URI("uri", "", false, false, false),
    /**
     * Universally Unique ID. See clause 1.1.4, "UUID format
     * and recommended generation algorithms" for the MANDATORY UUID format.
     */
    UUID("uuid", "", false, false, false),
    /**
     * Unknown Data Type.
     */
    UNKNOWN("unknown", "", false, false, true),
    ;

    private static final Map<String, DataType> mMap = new HashMap<>(values().length);

    static {
        for (final DataType type : values()) {
            mMap.put(type.mSymbol, type);
        }
    }

    public static DataType of(@Nonnull final String symbol) {
        final DataType dataType = mMap.get(symbol);
        return dataType != null ? dataType : UNKNOWN;
    }

    @Nonnull
    private final String mSymbol;
    @Nonnull
    private final String mDefault;
    private final boolean mNumber;
    private final boolean mInteger;
    private final boolean mMultiLine;

    DataType(
            @Nonnull final String symbol,
            @Nonnull final String defaultValue,
            final boolean number,
            final boolean integer,
            final boolean multiLine) {
        mSymbol = symbol;
        mDefault = defaultValue;
        mNumber = number;
        mInteger = integer;
        mMultiLine = multiLine;
    }

    @Nonnull
    public String getSymbol() {
        return mSymbol;
    }

    @Nonnull
    public String getDefault() {
        return mDefault;
    }

    public boolean isNumber() {
        return mNumber;
    }

    public boolean isInteger() {
        return mInteger;
    }

    public boolean isMultiLine() {
        return mMultiLine;
    }
}
