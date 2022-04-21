package build

object ProjectProperties {
    const val groupId: String = "net.mm2d.mmupnp"
    const val name: String = "mmupnp"
    const val description: String = "Universal Plug and Play (UPnP) ControlPoint library for Java / Kotlin."
    const val developerId: String = "ryo"
    const val developerName: String = "ryosuke"

    private const val versionMajor: Int = 3
    private const val versionMinor: Int = 1
    private const val versionPatch: Int = 6
    const val versionName: String = "$versionMajor.$versionMinor.$versionPatch"

    object Url {
        const val site: String = "https://github.com/ohmae/mmupnp"
        const val github: String = "https://github.com/ohmae/mmupnp"
        const val scm: String = "scm:git@github.com:ohmae/mmupnp.git"
    }
}
