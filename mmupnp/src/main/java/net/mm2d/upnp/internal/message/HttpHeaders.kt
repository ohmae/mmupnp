/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import java.util.*

/**
 * HTTPヘッダを表現するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class HttpHeaders {
    /**
     * ヘッダのエントリー情報。
     *
     * name/valueともにsetterがあるが、
     * nameは大文字小文字の差異のみがある場合に限る
     */
    class Entry(
        private var _name: String,
        private var _value: String
    ) {
        constructor(original: Entry) : this(original._name, original._value)

        var value: String
            get() : String = _value
            internal set(value) {
                _value = value
            }

        var name: String
            get() = _name
            internal set(name) {
                if (_name.toKey() != name.toKey()) {
                    throw IllegalArgumentException()
                }
                _name = name
            }

        override fun hashCode(): Int {
            return _name.hashCode() + _value.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other !is Entry) return false
            return _name == other._name && _value == other._value
        }

        override fun toString(): String {
            return "$_name: $_value"
        }
    }

    private val map: MutableMap<String, Entry>

    /**
     * インスタンス初期化。
     */
    constructor() {
        map = mutableMapOf()
    }

    /**
     * 引数のHeaderと同一の内容を持つHeaderを作成する。
     *
     * @param original コピー元
     */
    constructor(original: HttpHeaders) {
        // EntryはmutableなのでDeep copyが必要
        map = original.map.entries.map {
            it.key to Entry(it.value)
        }.toMap().toMutableMap()
    }

    /**
     * ヘッダ情報が空か否かを返す。
     *
     * @return ヘッダが空のときtrue
     */
    val isEmpty: Boolean
        get() = map.isEmpty()

    /**
     * ヘッダエントリー数を返す。
     *
     * @return ヘッダエントリー数
     */
    fun size(): Int {
        return map.size
    }

    /**
     * 指定されたヘッダ名の値を返す。
     *
     * ヘッダの検索において大文字小文字の区別は行わない。
     *
     * @param name ヘッダ名
     * @return ヘッダの値
     */
    operator fun get(name: String): String? {
        return map[name.toKey()]?.value
    }

    /**
     * ヘッダの削除を行う。
     *
     * ヘッダの検索において大文字小文字の区別は行わない。
     *
     * @param name ヘッダ名
     * @return 削除されたヘッダがあった場合、ヘッダの値、なかった場合null
     */
    fun remove(name: String): String? {
        return map.remove(name.toKey())?.value
    }

    /**
     * ヘッダ情報を登録する。
     *
     * ヘッダ名は登録においては大文字小文字の区別を保持して登録される。
     * 既に同一名のヘッダが登録されている場合置換される。
     * ヘッダの重複は大文字小文字の区別を行わない。
     * 置換された場合、ヘッダ名も引数のもので置き換えられる。
     *
     * @param name  ヘッダ名
     * @param value ヘッダの値
     * @return 重複があった場合、既に登録されていた値。
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
     * 指定ヘッダに指定文字列が含まれるかを大文字小文字の区別なく判定する。
     *
     * 該当ヘッダ名の検索も大文字小文字の区別を行わない。
     *
     * @param name  ヘッダ名
     * @param value 含まれるか
     * @return 指定ヘッダにvalueが含まれる場合true
     */
    fun containsValue(name: String, value: String): Boolean {
        return get(name)?.contains(value, true) ?: false
    }

    /**
     * 登録情報のクリアを行う。
     */
    fun clear() {
        map.clear()
    }

    /**
     * 登録されているヘッダ情報へのCollectionビューを返す。
     *
     * @return 登録されているヘッダ情報へのCollectionビュー
     */
    fun values(): Collection<Entry> {
        return map.values
    }

    override fun toString(): String {
        return StringBuilder().let { sb ->
            map.values.forEach {
                sb.append(it.name)
                sb.append(": ")
                sb.append(it.value)
                sb.append("\r\n")
            }
            sb.toString()
        }
    }

    companion object {
        /**
         * 名前をもとにKeyとして使用できる文字列を作成して返す。
         *
         * @return Key
         */
        private fun String.toKey(): String {
            return toLowerCase(Locale.US)
        }
    }
}
