/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
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
 * Iconの実装。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class IconImpl implements Icon {
    /**
     * DeviceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser#loadDescription(HttpClient, DeviceImpl.Builder)
     */
    static class Builder {
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
            return new IconImpl(this);
        }
    }

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
    private IconImpl(@Nonnull final Builder builder) {
        mMimeType = builder.mMimeType;
        mHeight = builder.mHeight;
        mWidth = builder.mWidth;
        mDepth = builder.mDepth;
        mUrl = builder.mUrl;
    }

    @Override
    @Nonnull
    public String getMimeType() {
        return mMimeType;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getDepth() {
        return mDepth;
    }

    @Override
    @Nonnull
    public String getUrl() {
        return mUrl;
    }

    /**
     * URLからバイナリデータを読み込む。
     *
     * @param device DeviceImpl
     * @param client 通信に使用する{@link HttpClient}
     * @throws IOException 通信エラー
     */
    void loadBinary(
            @Nonnull final DeviceImpl device,
            @Nonnull final HttpClient client) throws IOException {
        final URL url = device.getAbsoluteUrl(mUrl);
        mBinary = client.downloadBinary(url);
    }

    @Override
    @Nullable
    public byte[] getBinary() {
        return mBinary;
    }
}
