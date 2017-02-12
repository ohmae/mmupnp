/*
 * Copyright(C) 2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

/**
 * Key/Valueのペアを保持するクラス。
 *
 * @param <K> Keyの型
 * @param <V> Valueの型
 */
public class Pair<K, V> {
    private final K mKey;
    private final V mValue;

    /**
     * 初期値を指定してインスタンス化する。
     *
     * @param key   Keyの値
     * @param value Valueの値
     */
    public Pair(K key, V value) {
        mKey = key;
        mValue = value;
    }

    /**
     * Keyの値を取り出す。
     *
     * @return Keyの値
     */
    public K getKey() {
        return mKey;
    }

    /**
     * Valueの値を取り出す。
     *
     * @return Valueの値
     */
    public V getValue() {
        return mValue;
    }

    @Override
    public int hashCode() {
        return (mKey != null ? mKey.hashCode() : 0)
                + (mValue != null ? mValue.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof Pair)) {
            return false;
        }
        final Pair pair = (Pair) obj;
        return (mKey != null && mKey.equals(pair.mKey)
                || mKey == null && pair.mKey == null)
                && (mValue != null && mValue.equals(pair.mValue)
                || mValue == null && pair.mValue == null);
    }

    @Override
    public String toString() {
        return "key:" + mKey.toString()
                + " value:" + mValue.toString();
    }
}
