package moe.mukjep.fbsr.gui.part

import moe.mukjep.fbsr.gui.GUIBox
import java.awt.Graphics2D

sealed class GUIPart(val box: GUIBox) {
    abstract fun render(g: Graphics2D)
}
