/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpResponse extends HttpMessage {
    private Http.Status mStatus;
    private int mStatusCode;
    private String mReasonPhrase;

    @Override
    public void setStartLine(String line) {
        setStatusLine(line);
    }

    public void setStatusLine(String line) {
        final String[] params = line.split(" ");
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setVersion(params[0]);
        setStatusCode(Integer.parseInt(params[1]));
        setReasonPhrase(params[2]);
    }

    @Override
    public String getStartLine() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getVersion());
        sb.append(' ');
        if (mStatus != null) {
            sb.append(String.valueOf(mStatus.getCode()));
            sb.append(' ');
            sb.append(mStatus.getPhrase());
        } else {
            sb.append(String.valueOf(mStatusCode));
            sb.append(' ');
            sb.append(getReasonPhrase());
        }
        return sb.toString();
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public void setStatusCode(int code) {
        mStatusCode = code;
        mStatus = Http.Status.valueOf(code);
        if (mStatus != null) {
            mReasonPhrase = mStatus.getPhrase();
        }
    }

    public String getReasonPhrase() {
        return mReasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        mReasonPhrase = reasonPhrase;
    }

    public void setStatus(Http.Status status) {
        mStatus = status;
        mStatusCode = status.getCode();
        mReasonPhrase = status.getPhrase();
    }

    public Http.Status getStatus() {
        return mStatus;
    }
}
