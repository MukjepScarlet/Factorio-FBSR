package moe.mukjep.fbsr.gui.feature

import moe.mukjep.fbsr.gui.GUIBox
import moe.mukjep.fbsr.gui.GUISpacing
import java.awt.Graphics2D

class GUISliceFeature(
    margin: GUISpacing,
    padding: GUISpacing,
    filename: String,
    source: GUIBox,
    slice: GUISpacing
) :
    GUISourcedFeature(filename, source) {
    val dx: IntArray = intArrayOf(-margin.left, padding.left, -padding.right, margin.right)
    val dy: IntArray = intArrayOf(-margin.top, padding.top, -padding.bottom, margin.bottom)
    val sx: IntArray = intArrayOf(
        source.x, source.x + slice.left, source.x + source.width - slice.right,
        source.x + source.width
    )
    val sy: IntArray = intArrayOf(
        source.y, source.y + slice.top, source.y + source.height - slice.bottom,
        source.y + source.height
    )

    fun render(g: Graphics2D, r: GUIBox) {
        val rx1 = r.x
        val rx2 = rx1 + r.width
        val ry1 = r.y
        val ry2 = ry1 + r.height
        val rx = intArrayOf(rx1, rx1, rx2, rx2)
        val ry = intArrayOf(ry1, ry1, ry2, ry2)

        for (row in 0..2) {
            for (col in 0..2) {
                val dx1 = rx[col] + dx[col]
                val dx2 = rx[col + 1] + dx[col + 1]
                val dy1 = ry[row] + dy[row]
                val dy2 = ry[row + 1] + dy[row + 1]
                val sx1 = sx[col]
                val sx2 = sx[col + 1]
                val sy1 = sy[row]
                val sy2 = sy[row + 1]
                g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
            }
        }
    }

    companion object {
        @JvmStatic
        fun inner(filename: String, source: GUIBox, slice: GUISpacing): GUISliceFeature {
            return GUISliceFeature(GUISpacing.NONE, slice, filename, source, slice)
        }

        @JvmStatic
        fun outer(filename: String, source: GUIBox, slice: GUISpacing): GUISliceFeature {
            return GUISliceFeature(slice, GUISpacing.NONE, filename, source, slice)
        }
    }
}