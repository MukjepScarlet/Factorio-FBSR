package moe.mukjep.fbsr.gui.feature

import com.demod.factorio.FactorioData
import moe.mukjep.fbsr.gui.GUIBox
import java.awt.image.BufferedImage

sealed class GUISourcedFeature(val filename: String, val source: GUIBox) {
    val image: BufferedImage = FactorioData.getModImage(filename)
}
