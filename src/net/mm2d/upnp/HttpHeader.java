/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class HttpHeader {
    public static class Entry {
        private String mName;
        private String mValue;

        public Entry(String name, String value) {
            mName = name;
            mValue = value;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public void setValue(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }
    }

    private class EntrySet extends AbstractSet<Entry> {
        @Override
        public Iterator<Entry> iterator() {
            return mList.iterator();
        }

        @Override
        public int size() {
            return mList.size();
        }
    }

    private EntrySet mEntrySet;
    private final List<Entry> mList = new LinkedList<>();

    public int size() {
        return mList.size();
    }

    public boolean isEmpty() {
        return mList.isEmpty();
    }

    public String get(String name) {
        for (final Entry entry : mList) {
            if (entry.getName().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String remove(String name) {
        final Iterator<Entry> i = mList.iterator();
        while (i.hasNext()) {
            final Entry entry = i.next();
            if (entry.getName().equalsIgnoreCase(name)) {
                i.remove();
                return entry.mValue;
            }
        }
        return null;
    }

    public String put(String name, String value) {
        for (final Entry entry : mList) {
            if (entry.getName().equalsIgnoreCase(name)) {
                final String old = entry.getValue();
                entry.setName(name);
                entry.setValue(value);
                return old;
            }
        }
        mList.add(new Entry(name, value));
        return null;
    }

    public boolean containsValue(String name, String value) {
        final String v = get(name);
        if (v == null) {
            return false;
        }
        return v.toLowerCase().contains(value.toLowerCase());
    }

    public void clear() {
        mList.clear();
    }

    public Set<Entry> entrySet() {
        if (mEntrySet == null) {
            mEntrySet = new EntrySet();
        }
        return mEntrySet;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Entry entry : mList) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append("\r\n");
        }
        return sb.toString();
    }
}
