package moe.mukjep.fbsr.gui.feature

import moe.mukjep.fbsr.gui.GUIBox
import java.awt.Graphics2D

class GUIStaticFeature(filename: String, source: GUIBox) : GUISourcedFeature(filename, source) {
    fun render(g: Graphics2D, rect: GUIBox) {
        g.drawImage(
            image, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,  //
            source.x, source.y, source.x + source.width, source.y + source.height, null
        )
    }
}