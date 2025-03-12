package moe.mukjep.fbsr.render

import com.demod.dcba.CommandReporting
import com.demod.fbsr.bs.BSBlueprint
import com.demod.fbsr.gui.GUIStyle
import java.awt.Color
import java.util.*

class RenderRequest(var blueprint: BSBlueprint, var reporting: CommandReporting) {
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

    var maxWidth: OptionalInt = OptionalInt.empty()
    var maxHeight: OptionalInt = OptionalInt.empty()
    var minWidth: OptionalInt = OptionalInt.empty()
    var minHeight: OptionalInt = OptionalInt.empty()
    var maxScale: OptionalDouble = OptionalDouble.empty()

    var background: Optional<Color> = Optional.of(GROUND_COLOR)
    var gridLines: Optional<Color> = Optional.of(GRID_COLOR)

    val debug: Debug = Debug()
    val show: Show = Show()

    companion object {
        private val GROUND_COLOR = Color(40, 40, 40)
        private val GRID_COLOR: Color = GUIStyle.FONT_BP_COLOR.darker().darker()
    }
}