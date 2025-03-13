package moe.mukjep.fbsr.gui.part

import moe.mukjep.fbsr.gui.GUIBox
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

class GUILabel(
    box: GUIBox,
    val text: String,
    val font: Font,
    val color: Color,
    val align: Align
) : GUIPart(box) {

    enum class Align(val horizontalFactor: Double, val verticalFactor: Double) {
        TOP_LEFT(0.0, 0.0),
        TOP_CENTER(0.5, 0.0),
        TOP_RIGHT(1.0, 0.0),
        CENTER_LEFT(0.0, 0.5),
        CENTER(0.5, 0.5),
        CENTER_RIGHT(1.0, 0.5),
        BOTTOM_LEFT(0.0, 1.0),
        BOTTOM_CENTER(0.5, 1.0),
        BOTTOM_RIGHT(1.0, 1.0)
    }

    fun getTextWidth(g: Graphics2D): Double {
        return g.getFontMetrics(font).stringWidth(text).toDouble()
    }

    override fun render(g: Graphics2D) {
        val prevFont = g.font
        val prevColor = g.color

        try {
            g.font = font
            g.color = color

            val textWidth = g.fontMetrics.stringWidth(text)
            val textAscent = g.fontMetrics.ascent
            val textDescent = g.fontMetrics.descent
            val textHeight = textAscent + textDescent

            val textX = box.x + ((box.width - textWidth) * align.horizontalFactor).toInt()
            val textY = box.y + textAscent + ((box.height - textHeight) * align.verticalFactor).toInt()

            g.drawString(text, textX, textY)
        } finally {
            g.font = prevFont
            g.color = prevColor
        }
    }
}
