/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import java.util.*

/**
 * HTTP header.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SingleHttpHeaders {
    /**
     * Entry of header
     *
     * name can only be updated if there is only a case difference
     */
    class Entry(
        private var _name: String,
        private var _value: String
    ) {
        constructor(original: Entry) : this(original._name, original._value)

        var value: String
            get(): String = _value
            internal set(value) {
                _value = value
            }

        var name: String
            get() = _name
            internal set(name) {
                require(_name.toKey() == name.toKey())
                _name = name
            }

        override fun hashCode(): Int = _name.hashCode() + _value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other !is Entry) return false
            return _name == other._name && _value == other._value
        }

        override fun toString(): String = "$_name: $_value"
    }

    private val map: MutableMap<String, Entry>

    constructor() {
        map = mutableMapOf()
    }

    /**
     * Create a new instance with the same contents as the argument instance.
     *
     * @param original original header
     */
    constructor(original: SingleHttpHeaders) {
        // EntryはmutableなのでDeep copyが必要
        map = original.map.entries.map {
            it.key to Entry(it.value)
        }.toMap().toMutableMap()
    }

    /**
     * Returns whether the header information is empty.
     *
     * @return true: if empty
     */
    val isEmpty: Boolean
        get() = map.isEmpty()

    /**
     * Return the number of header entries.
     *
     * @return the number of header entries
     */
    fun size(): Int {
        return map.size
    }

    /**
     * Return the header value.
     *
     * Search header without case sensitivity.
     *
     * @param name header name
     * @return header value
     */
    operator fun get(name: String): String? = map[name.toKey()]?.value

    /**
     * remove header.
     *
     * Search header without case sensitivity.
     *
     * @param name header name
     * @return If deleted, header value, if not null
     */
    fun remove(name: String): String? = map.remove(name.toKey())?.value

    /**
     * Add header entry.
     *
     * Header names are registered with case sensitivity in registration.
     * If a header with the same name has already been registered, it will be replaced.
     * Header duplication is not case sensitive.
     * If replaced, the header name is also replaced with that of the argument.
     *
     * @param name header name
     * @param value header value
     * @return If there is a duplicate, the value already registered
     */
    fun put(name: String, value: String): String? {
        val key = name.toKey()
        map[key]?.let {
            val oldValue = it.value
            it.name = name
            it.value = value
            return oldValue
        }
        map[key] = Entry(name, value)
        return null
    }

    /**
     * Determines whether the specified header contains the specified value regardless of case.
     *
     * @param name header name
     * @param value header value
     * @return true: contains specified header value
     */
    fun containsValue(name: String, value: String): Boolean {
        return get(name)?.contains(value, true) ?: false
    }

    /**
     * clear all header
     */
    fun clear(): Unit = map.clear()

    /**
     * Returns a Collection view for registered header information.
     *
     * @return Collection view for registered header information.
     */
    fun values(): Collection<Entry> = map.values

    override fun toString(): String = buildString {
        map.values.forEach {
            append(it.name)
            append(": ")
            append(it.value)
            append("\r\n")
        }
    }

    companion object {
        private fun String.toKey(): String = lowercase(Locale.US)
    }
}
