package build

object ProjectProperties {
    const val groupId: String = "net.mm2d.mmupnp"

    private const val versionMajor: Int = 3
    private const val versionMinor: Int = 1
    private const val versionPatch: Int = 1
    const val versionName: String = "$versionMajor.$versionMinor.$versionPatch"

    object Url {
        const val site: String = "https://github.com/ohmae/mmupnp"
        const val github: String = "https://github.com/ohmae/mmupnp"
        const val scm: String = "scm:git:https://github.com/ohmae/mmupnp.git"
    }
}
