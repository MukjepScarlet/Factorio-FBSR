package moe.mukjep.fbsr.render

import com.demod.dcba.CommandReporting
import moe.mukjep.fbsr.bs.types.BSBlueprint
import moe.mukjep.fbsr.gui.GUIStyle
import java.awt.Color

data class RenderRequest(val blueprint: BSBlueprint, val reporting: CommandReporting) {
    data class Debug(
        var pathItems: Boolean = false,
        var pathRails: Boolean = false,
        var entityPlacement: Boolean = false
    )

    data class Show(
        var altMode: Boolean = true,
        var pathOutputs: Boolean = true,
        var pathInputs: Boolean = false,
        var pathRails: Boolean = true,
        var gridNumbers: Boolean = false,
        var gridAboveBelts: Boolean = false,
    )

    var maxWidth: Int = Int.MAX_VALUE
    var maxHeight: Int = Int.MAX_VALUE
    var minWidth: Int = 0
    var minHeight: Int = 0
    var maxScale: Double = 1.0

    var background: Color? = GROUND_COLOR
    var gridLines: Color? = GRID_COLOR

    val debug: Debug = Debug()
    val show: Show = Show()

    companion object {
        private val GROUND_COLOR = Color(40, 40, 40)
        private val GRID_COLOR: Color = GUIStyle.FONT_BP_COLOR.darker().darker()
    }
}