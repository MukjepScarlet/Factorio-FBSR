package moe.mukjep.fbsr.gui.part

import moe.mukjep.fbsr.gui.GUIBox
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class GUIImage @JvmOverloads constructor(
    box: GUIBox,
    val image: BufferedImage,
    private val scale: Double = 1.0,
    private val preScaled: Boolean = true
) : GUIPart(box) {

    override fun render(g: Graphics2D) {
        if (preScaled) {
            val xform = g.transform
            val dstWidth = (image.width / xform.scaleX).toInt()
            val dstHeight = (image.height / xform.scaleY).toInt()
            val dx1 = (box.x + box.width / 2 - scale * dstWidth / 2).toInt()
            val dy1 = (box.y + box.height / 2 - scale * dstHeight / 2).toInt()
            val dx2 = (dx1 + scale * dstWidth).toInt()
            val dy2 = (dy1 + scale * dstHeight).toInt()
            g.drawImage(image, dx1, dy1, dx2, dy2, 0, 0, image.width, image.height, null)
        } else {
            val x = (box.x + box.width / 2 - scale * image.width / 2).toInt()
            val y = (box.y + box.height / 2 - scale * image.height / 2).toInt()
            val w = (scale * image.width).toInt()
            val h = (scale * image.height).toInt()
            g.drawImage(image, x, y, w, h, null)
        }
    }
}
