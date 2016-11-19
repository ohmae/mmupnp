/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.util;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * IO関係のよく使用する機能を実装する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class IOUtils {
    private static final String TAG = IOUtils.class.getSimpleName();

    /**
     * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
     *
     * nullの場合は何も行わない、
     * closeでIOExceptionが発生した場合はログ出力をする。
     *
     * @param closeable close処理をするcloseable
     */
    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final IOException e) {
            Log.w(TAG, e);
        }
    }
}
