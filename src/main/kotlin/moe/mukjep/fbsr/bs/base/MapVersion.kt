package moe.mukjep.fbsr.bs.base

private const val UINT16_MASK: Long = 0xFFFF

data class MapVersion(
    val main: Int,
    val major: Int,
    val minor: Int,
    val dev: Int,
) : Comparable<MapVersion> {

    private val version: Long = dev.toLong() or (minor.toLong() shl 16) or (major.toLong() shl 32) or (main.toLong() shl 48)

    @JvmOverloads
    constructor(serialized: Long = 0) : this(
        main = ((serialized shr 48) and UINT16_MASK).toInt(),
        major = ((serialized shr 32) and UINT16_MASK).toInt(),
        minor = ((serialized shr 16) and UINT16_MASK).toInt(),
        dev = (serialized and UINT16_MASK).toInt(),
    )

    override fun compareTo(other: MapVersion): Int {
        return version.compareTo(other.version)
    }

    val isInvalid: Boolean
        get() = version == 0L

    override fun toString(): String = "($main.$major.$minor)"

}
