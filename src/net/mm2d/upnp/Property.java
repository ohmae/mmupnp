/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public final class Property {
    public static final String LIB_NAME = "mmupnp";
    public static final String VERSION = "1.0";
    public static final String OS_VERSION;
    public static final String UPNP_VERSION = "UPnP/1.0";
    public static final String LIB_VERSION = LIB_NAME + "/" + VERSION;
    public static final int DEFAULT_TIMEOUT = 30000;

    static {
        final String os = System.getProperty("os.name").split(" ")[0];
        final String ver = System.getProperty("os.version");
        OS_VERSION = os + "/" + ver;
    }

    private static boolean sGetIconOnLoadDescription = false;

    static boolean isGetIconOnLoadDescription() {
        return sGetIconOnLoadDescription;
    }

    static void setGetIconOnLoadDescription(boolean getIcon) {
        sGetIconOnLoadDescription = getIcon;
    }
}
