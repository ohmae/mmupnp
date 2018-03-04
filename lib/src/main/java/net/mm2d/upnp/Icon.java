/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Iconを表すクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class Icon {
    /**
     * DeviceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser#loadDescription(HttpClient, Device.Builder)
     */
    public static class Builder {
        private Device mDevice;
        private String mMimeType;
        private int mHeight;
        private int mWidth;
        private int mDepth;
        private String mUrl;

        /**
         * インスタンス作成
         */
        public Builder() {
        }

        /**
         * このIconを保持するDeviceを登録する。
         *
         * @param device このIconを保持するDevice
         * @return Builder
         */
        @Nonnull
        public Builder setDevice(@Nonnull final Device device) {
            mDevice = device;
            return this;
        }

        /**
         * MimeTypeの値を登録する。
         *
         * @param mimeType MimeType
         * @return Builder
         */
        @Nonnull
        public Builder setMimeType(@Nonnull final String mimeType) {
            mMimeType = mimeType;
            return this;
        }

        /**
         * Heightの値を登録する
         *
         * @param height Height
         * @return Builder
         */
        @Nonnull
        public Builder setHeight(@Nonnull final String height) {
            try {
                mHeight = Integer.parseInt(height);
            } catch (final NumberFormatException e) {
                mHeight = 0;
            }
            return this;
        }

        /**
         * Widthの値を登録する。
         *
         * @param width Width
         * @return Builder
         */
        @Nonnull
        public Builder setWidth(@Nonnull final String width) {
            try {
                mWidth = Integer.parseInt(width);
            } catch (final NumberFormatException e) {
                mWidth = 0;
            }
            return this;
        }

        /**
         * Depthの値を登録する
         *
         * @param depth Depth
         * @return Builder
         */
        @Nonnull
        public Builder setDepth(@Nonnull final String depth) {
            try {
                mDepth = Integer.parseInt(depth);
            } catch (final NumberFormatException e) {
                mDepth = 0;
            }
            return this;
        }

        /**
         * URLの値を登録する。
         *
         * @param url URL
         * @return Builder
         */
        @Nonnull
        public Builder setUrl(@Nonnull final String url) {
            mUrl = url;
            return this;
        }

        /**
         * Iconのインスタンスを作成する。
         *
         * @return Iconのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Nonnull
        public Icon build() throws IllegalStateException {
            if (mDevice == null) {
                throw new IllegalStateException("device must be set.");
            }
            if (mMimeType == null) {
                throw new IllegalStateException("mimetype must be set.");
            }
            if (mWidth <= 0) {
                throw new IllegalStateException("width must be > 0.");
            }
            if (mHeight <= 0) {
                throw new IllegalStateException("height must be > 0.");
            }
            if (mDepth <= 0) {
                throw new IllegalStateException("depth must be > 0.");
            }
            if (mUrl == null) {
                throw new IllegalStateException("url must be set.");
            }
            return new Icon(this);
        }
    }

    @Nonnull
    private final Device mDevice;
    @Nonnull
    private final String mMimeType;
    private final int mHeight;
    private final int mWidth;
    private final int mDepth;
    @Nonnull
    private final String mUrl;
    @Nullable
    private byte[] mBinary;

    /**
     * インスタンスを作成する。
     *
     * @param builder Builder
     */
    private Icon(@Nonnull final Builder builder) {
        mDevice = builder.mDevice;
        mMimeType = builder.mMimeType;
        mHeight = builder.mHeight;
        mWidth = builder.mWidth;
        mDepth = builder.mDepth;
        mUrl = builder.mUrl;
    }

    /**
     * このIconを保持するDeviceを返す。
     *
     * @return このIconを保持するDevice
     */
    @Nonnull
    public Device getDevice() {
        return mDevice;
    }

    /**
     * MimeTypeの値を返す。
     *
     * <p>Required. Icon's MIME type (see RFC 2045, 2046, and 2387). Single MIME image type.
     * At least one icon should be of type “image/png” (Portable Network Graphics, see IETF RFC 2083).
     *
     * @return MimeType
     */
    @Nonnull
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Heightの値を返す。
     *
     * <p>Required. Vertical dimension of icon in pixels. Integer.
     *
     * @return Height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Widthの値を返す。
     *
     * <p>Required. Horizontal dimension of icon in pixels. Integer.
     *
     * @return Width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Depthの値を返す。
     *
     * <p>Required. Number of color bits per pixel. Integer.
     *
     * @return Depth
     */
    public int getDepth() {
        return mDepth;
    }

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
    public String getUrl() {
        return mUrl;
    }

    /**
     * URLからバイナリデータを読み込む。
     *
     * @param client 通信に使用する{@link HttpClient}
     * @throws IOException 通信エラー
     */
    public void loadBinary(@Nonnull final HttpClient client) throws IOException {
        final URL url = mDevice.getAbsoluteUrl(mUrl);
        mBinary = client.downloadBinary(url);
    }

    /**
     * バイナリデータを返す。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return バイナリデータ
     */
    @Nullable
    public byte[] getBinary() {
        return mBinary;
    }
}
