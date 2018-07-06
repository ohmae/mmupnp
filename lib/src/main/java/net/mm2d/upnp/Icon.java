/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Iconを表すインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface Icon {
    /**
     * このIconを保持するDeviceを返す。
     *
     * @return このIconを保持するDevice
     */
    @Nonnull
    Device getDevice();

    /**
     * MimeTypeの値を返す。
     *
     * <p>Required. Icon's MIME type (see RFC 2045, 2046, and 2387). Single MIME image type.
     * At least one icon should be of type “image/png” (Portable Network Graphics, see IETF RFC 2083).
     *
     * @return MimeType
     */
    @Nonnull
    String getMimeType();

    /**
     * Heightの値を返す。
     *
     * <p>Required. Vertical dimension of icon in pixels. Integer.
     *
     * @return Height
     */
    int getHeight();

    /**
     * Widthの値を返す。
     *
     * <p>Required. Horizontal dimension of icon in pixels. Integer.
     *
     * @return Width
     */
    int getWidth();

    /**
     * Depthの値を返す。
     *
     * <p>Required. Number of color bits per pixel. Integer.
     *
     * @return Depth
     */
    int getDepth();

    /**
     * URLの値を返す。
     *
     * <p>Required. Pointer to icon image. (XML does not support direct embedding of binary data. See note below.)
     * Retrieved via HTTP. Shall be relative to the URL at which the device description is located in accordance with
     * clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     *
     * @return URL
     */
    @Nonnull
    String getUrl();

    /**
     * URLからバイナリデータを読み込む。
     *
     * @param client 通信に使用する{@link HttpClient}
     * @throws IOException 通信エラー
     */
    void loadBinary(@Nonnull HttpClient client) throws IOException;

    /**
     * バイナリデータを返す。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return バイナリデータ
     */
    @Nullable
    byte[] getBinary();
}
