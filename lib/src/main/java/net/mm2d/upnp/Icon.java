/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.io.IOException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Iconを表すクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Icon {
    private static final String TAG = Icon.class.getSimpleName();

    /**
     * DeviceDescriptionのパース時に使用するビルダー
     *
     * @see Device#loadDescription(IconFilter)
     */
    public static class Builder {
        private Device mDevice;
        private String mMimeType;
        private int mHeight;
        private int mWidth;
        private int mDepth;
        private String mUrl;
        private byte[] mBinary;

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
        public Builder setDevice(@Nonnull Device device) {
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
        public Builder setMimeType(@Nonnull String mimeType) {
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
        public Builder setHeight(@Nonnull String height) {
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
        public Builder setWidth(@Nonnull String width) {
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
        public Builder setDepth(@Nonnull String depth) {
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
        public Builder setUrl(@Nonnull String url) {
            mUrl = url;
            return this;
        }

        /**
         * バイナリデータを登録する。
         *
         * <p>DeviceDescriptionからの読み込みの場合、
         * Iconのインスタンスを作成した後読み込みを実行するため。
         * このメソッドは使用しない。
         *
         * @param binary バイナリ
         * @return Builder
         */
        @Nonnull
        public Builder setBinary(@Nullable byte[] binary) {
            mBinary = binary;
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

    private Icon(@Nonnull Builder builder) {
        mDevice = builder.mDevice;
        mMimeType = builder.mMimeType;
        mHeight = builder.mHeight;
        mWidth = builder.mWidth;
        mDepth = builder.mDepth;
        mUrl = builder.mUrl;
        mBinary = builder.mBinary;
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
     * @return MimeType
     */
    @Nonnull
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Heightの値を返す。
     *
     * @return Height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Widthの値を返す。
     *
     * @return Width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Depthの値を返す。
     *
     * @return Depth
     */
    public int getDepth() {
        return mDepth;
    }

    /**
     * URLの値を返す。
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
    public void loadBinary(@Nonnull HttpClient client) throws IOException {
        final URL url = mDevice.getAbsoluteUrl(mUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.i(TAG, response.toString());
            throw new IOException(response.getStartLine());
        }
        mBinary = response.getBodyBinary();
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
