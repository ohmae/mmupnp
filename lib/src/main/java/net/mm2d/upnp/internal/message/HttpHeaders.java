/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.util.TextUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPヘッダを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class HttpHeaders {
    /**
     * 名前をもとにKeyとして使用できる文字列を作成して返す。
     *
     * @param name 名前
     * @return Key
     */
    @Nonnull
    private static String toKey(@Nonnull final String name) {
        return TextUtils.toLowerCase(name);
    }

    /**
     * ヘッダのエントリー情報。
     *
     * <p>name/valueともにsetterがあるが、
     * nameは大文字小文字の差異のみがある場合に限る
     */
    public static class Entry {
        @Nonnull
        private String mName;
        @Nonnull
        private String mValue;

        /**
         * インスタンス作成。
         *
         * @param name  ヘッダ名
         * @param value 値
         */
        public Entry(
                @Nonnull final String name,
                @Nonnull final String value) {
            mName = name;
            mValue = value;
        }

        /**
         * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
         *
         * @param original コピー元
         */
        public Entry(@Nonnull final Entry original) {
            mName = original.mName;
            mValue = original.mValue;
        }

        /**
         * ヘッダ名を設定する。
         *
         * @param name ヘッダ名
         * @throws IllegalArgumentException keyとしての値が一致しないものに更新しようとした場合
         */
        // VisibleForTesting
        void setName(@Nonnull final String name) {
            if (!toKey(mName).equals(toKey(name))) {
                throw new IllegalArgumentException();
            }
            mName = name;
        }

        /**
         * ヘッダ名を取得する。
         *
         * @return ヘッダ名
         */
        @Nonnull
        public String getName() {
            return mName;
        }

        /**
         * 値を設定する。
         *
         * @param value 値
         */
        private void setValue(@Nonnull final String value) {
            mValue = value;
        }

        /**
         * 値を返す。
         *
         * @return 値
         */
        @Nonnull
        public String getValue() {
            return mValue;
        }

        @Override
        public int hashCode() {
            return mName.hashCode() + mValue.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            final Entry entry = (Entry) obj;
            return mName.equals(entry.mName)
                    && mValue.equals(entry.mValue);
        }

        @Override
        public String toString() {
            return mName + ": " + mValue;
        }
    }

    private final Map<String, Entry> mMap;

    /**
     * インスタンス初期化。
     */
    public HttpHeaders() {
        mMap = new LinkedHashMap<>();
    }

    /**
     * 引数のHeaderと同一の内容を持つHeaderを作成する。
     *
     * @param original コピー元
     */
    public HttpHeaders(@Nonnull final HttpHeaders original) {
        // EntryはmutableなのでDeep copyが必要
        mMap = new LinkedHashMap<>();
        for (final Entry entry : original.mMap.values()) {
            final String key = toKey(entry.getName());
            mMap.put(key, new Entry(entry.getName(), entry.getValue()));
        }
    }

    /**
     * ヘッダエントリー数を返す。
     *
     * @return ヘッダエントリー数
     */
    public int size() {
        return mMap.size();
    }

    /**
     * ヘッダ情報が空か否かを返す。
     *
     * @return ヘッダが空のときtrue
     */
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    /**
     * 指定されたヘッダ名の値を返す。
     *
     * <p>ヘッダの検索において大文字小文字の区別は行わない。
     *
     * @param name ヘッダ名
     * @return ヘッダの値
     */
    @Nullable
    public String get(@Nonnull final String name) {
        final Entry entry = mMap.get(toKey(name));
        return entry != null ? entry.getValue() : null;
    }

    /**
     * ヘッダの削除を行う。
     *
     * <p>ヘッダの検索において大文字小文字の区別は行わない。
     *
     * @param name ヘッダ名
     * @return 削除されたヘッダがあった場合、ヘッダの値、なかった場合null
     */
    @Nullable
    public String remove(@Nonnull final String name) {
        final Entry entry = mMap.remove(toKey(name));
        return entry != null ? entry.getValue() : null;
    }

    /**
     * ヘッダ情報を登録する。
     *
     * <p>ヘッダ名は登録においては大文字小文字の区別を保持して登録される。
     * 既に同一名のヘッダが登録されている場合置換される。
     * ヘッダの重複は大文字小文字の区別を行わない。
     * 置換された場合、ヘッダ名も引数のもので置き換えられる。
     *
     * @param name  ヘッダ名
     * @param value ヘッダの値
     * @return 重複があった場合、既に登録されていた値。
     */
    @Nullable
    public String put(
            @Nonnull final String name,
            @Nonnull final String value) {
        final String key = toKey(name);
        final Entry entry = mMap.get(key);
        if (entry != null) {
            final String oldValue = entry.getValue();
            entry.setName(name);
            entry.setValue(value);
            return oldValue;
        }
        mMap.put(key, new Entry(name, value));
        return null;
    }

    /**
     * 指定ヘッダに指定文字列が含まれるかを大文字小文字の区別なく判定する。
     *
     * <p>該当ヘッダ名の検索も大文字小文字の区別を行わない。
     *
     * @param name  ヘッダ名
     * @param value 含まれるか
     * @return 指定ヘッダにvalueが含まれる場合true
     */
    public boolean containsValue(
            @Nonnull final String name,
            @Nonnull final String value) {
        final String v = TextUtils.toLowerCase(get(name));
        return v != null && v.contains(TextUtils.toLowerCase(value));
    }

    /**
     * 登録情報のクリアを行う。
     */
    public void clear() {
        mMap.clear();
    }

    /**
     * 登録されているヘッダ情報へのCollectionビューを返す。
     *
     * @return 登録されているヘッダ情報へのCollectionビュー
     */
    @Nonnull
    public Collection<Entry> values() {
        return mMap.values();
    }

    @Override
    @Nonnull
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Entry entry : mMap.values()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append("\r\n");
        }
        return sb.toString();
    }
}
