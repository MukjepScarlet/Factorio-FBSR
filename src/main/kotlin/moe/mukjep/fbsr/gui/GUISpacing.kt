package moe.mukjep.fbsr.gui

data class GUISpacing(val top: Int, val left: Int, val bottom: Int, val right: Int) {

    val horizontal: Int
        get() = left + right

    val vertical: Int
        get() = top + bottom

    operator fun plus(s: GUISpacing): GUISpacing {
        return GUISpacing(top + s.top, left + s.left, bottom + s.bottom, right + s.right)
    }

    operator fun minus(s: GUISpacing): GUISpacing {
        return GUISpacing(top + s.top, left + s.left, bottom + s.bottom, right + s.right)
    }

    companion object {
        @JvmField
        val NONE: GUISpacing = GUISpacing(0, 0, 0, 0)
    }
}