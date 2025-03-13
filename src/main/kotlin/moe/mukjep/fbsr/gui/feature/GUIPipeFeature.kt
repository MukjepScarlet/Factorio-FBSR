package moe.mukjep.fbsr.gui.feature

import com.google.common.collect.Table
import moe.mukjep.fbsr.gui.GUIBox
import java.awt.Graphics2D
import java.awt.Rectangle
import java.util.function.ToIntBiFunction

class GUIPipeFeature(filename: String, source: GUIBox, val indices: IntArray) : GUISourcedFeature(filename, source) {
    val size: Int = source.height
    val sx: IntArray = IntArray(indices.size) {
        indices[it] * size + source.x
    }

    fun renderBox(g: Graphics2D, r: GUIBox) {
        val x1 = r.x
        val x2 = r.x + size
        val x3 = r.x + r.width - size
        val x4 = r.x + r.width
        val y1 = r.y
        val y2 = r.y + size
        val y3 = r.y + r.height - size
        val y4 = r.y + r.height

        val sy1 = source.y
        val sy2 = source.y + size

        // Top Left Corner
        g.drawImage(
            image, x1, y1, x2, y2,
            sx[SE], sy1, sx[SE] + size, sy2, null
        )
        // Top Side
        g.drawImage(
            image, x2, y1, x3, y2,
            sx[EW], sy1, sx[EW] + 1, sy2, null
        )
        // Top Right Corner
        g.drawImage(
            image, x3, y1, x4, y2,
            sx[SW], sy1, sx[SW] + size, sy2, null
        )
        // Right Side
        g.drawImage(
            image, x3, y2, x4, y3,
            sx[NS], sy1, sx[NS] + size, sy1 + 1, null
        )
        // Bottom Right Corner
        g.drawImage(
            image, x3, y3, x4, y4,
            sx[NW], sy1, sx[NW] + size, sy2, null
        )
        // Bottom Side
        g.drawImage(
            image, x2, y3, x3, y4,
            sx[EW], sy1, sx[EW] + 1, sy2, null
        )
        // Bottom Left Corner
        g.drawImage(
            image, x1, y3, x2, y4,
            sx[NE], sy1, sx[NE] + size, sy2, null
        )
        // Left Side
        g.drawImage(
            image, x1, y2, x2, y3,
            sx[NS], sy1, sx[NS] + size, sy1 + 1, null
        )
    }

    // Draws pipes based on matching cell grouping numbers in the grouping grid.
    fun renderDynamicGrid(
        g: Graphics2D, x: Int, y: Int, cellWidth: Double, cellHeight: Double, bounds: Rectangle,
        groupings: Table<Int, Int, Int>
    ) {
        val sy1 = source.y
        val sy2 = source.y + size

        val boundsX1 = bounds.x
        val boundsY1 = bounds.y
        val boundsX2 = boundsX1 + bounds.width
        val boundsY2 = boundsY1 + bounds.height

        val lookup = ToIntBiFunction { row: Int, col: Int ->
            if (col < boundsX1 || row < boundsY1 || col >= boundsX2 || row >= boundsY2) {
                return@ToIntBiFunction -1
            }
            groupings[row, col] ?: -1
        }

        for (row in boundsY1..boundsY2) {
            for (col in boundsX1..boundsX2) {
                // Quadrants

                val groupNW = lookup.applyAsInt(row - 1, col - 1)
                val groupNE = lookup.applyAsInt(row - 1, col)
                val groupSE = lookup.applyAsInt(row, col)
                val groupSW = lookup.applyAsInt(row, col - 1)

                // Comparing quadrants
                val diffN = groupNW != groupNE
                val diffE = groupSE != groupNE
                val diffS = groupSW != groupSE
                val diffW = groupNW != groupSW

                var id = 0
                id = id or if (diffN) 1 else 0
                id = id or if (diffE) 2 else 0
                id = id or if (diffS) 4 else 0
                id = id or if (diffW) 8 else 0
                val mode = DYNAMIC_GRID_MAP[id]

                var lineX = (x + (col - boundsX1) * cellWidth).toInt()
                var lineY = (y + (row - boundsY1) * cellHeight).toInt()

                if (mode != -1 && mode != NS && mode != EW) {
                    val x1 = lineX
                    val y1 = lineY
                    val x2 = lineX + size
                    val y2 = lineY + size
                    g.drawImage(image, x1, y1, x2, y2, sx[mode], sy1, sx[mode] + size, sy2, null)

                    lineX += size
                    lineY += size
                }

                if (diffS) { // vertical line
                    val x1 = (x + (col - boundsX1) * cellWidth).toInt()
                    val y1 = lineY
                    val x2 = x1 + size
                    val y2 = (y + (row + 1 - boundsY1) * cellHeight).toInt()
                    g.drawImage(
                        image, x1, y1, x2, y2,
                        sx[NS], sy1, sx[NS] + size, sy1 + 1, null
                    )
                }

                if (diffE) { // horizontal line
                    val x1 = lineX
                    val y1 = (y + (row - boundsY1) * cellHeight).toInt()
                    val x2 = (x + (col + 1 - boundsX1) * cellWidth).toInt()
                    val y2 = y1 + size
                    g.drawImage(
                        image, x1, y1, x2, y2,
                        sx[EW], sy1, sx[EW] + 1, sy2, null
                    )
                }
            }
        }
    }

    fun renderVertical(g: Graphics2D, x: Int, yStart: Int, yEnd: Int) {
        val x1 = x
        val x2 = x + size
        val y1 = yStart
        val y2 = yStart + size
        val y3 = yEnd - size
        val y4 = yEnd

        val sy1 = source.y
        val sy2 = source.y + size

        // Top
        g.drawImage(
            image, x1, y1, x2, y2,
            sx[S], sy1, sx[S] + size, sy2, null
        )
        // Middle
        g.drawImage(
            image, x1, y2, x2, y3,
            sx[NS], sy1, sx[NS] + size, sy1 + 1, null
        )
        // Bottom
        g.drawImage(
            image, x1, y3, x2, y4,
            sx[N], sy1, sx[N] + size, sy2, null
        )
    }

    companion object {
        private val INDICES_DRAGLINES = intArrayOf(
            //
            -1,  // ....
            2,  // ...N
            -1,  // ..E.
            -1,  // ..EN
            0,  // .S..
            1,  // .S.N
            -1,  // .SE.
            -1,  // .SEN
            -1,  // W...
            -1,  // W..N
            -1,  // W.E.
            -1,  // W.EN
            -1,  // WS..
            -1,  // WS.N
            -1,  // WSE.
            -1,  // WSEN
        )

        private val INDICES_FULL = intArrayOf(
            //
            -1,  // ....
            13,  // ...N
            14,  // ..E.
            4,  // ..EN
            11,  // .S..
            0,  // .S.N
            5,  // .SE.
            7,  // .SEN
            12,  // W...
            3,  // W..N
            1,  // W.E.
            6,  // W.EN
            2,  // WS..
            9,  // WS.N
            8,  // WSE.
            10,  // WSEN
        )

        const val N: Int = 1
        const val E: Int = 2
        const val NE: Int = 3
        const val S: Int = 4
        const val NS: Int = 5
        const val SE: Int = 6
        const val NSE: Int = 7
        const val W: Int = 8
        const val NW: Int = 9
        const val EW: Int = 10
        const val NEW: Int = 11
        const val SW: Int = 12
        const val NSW: Int = 13
        const val SEW: Int = 14
        const val NSEW: Int = 15

        // index = bitset of group change on the (W, S, E, W) side boundaries
        val DYNAMIC_GRID_MAP: IntArray = intArrayOf(
            //
            -1,  // 0
            -1,  // 1
            -1,  // 2
            NE,  // 3
            -1,  // 4
            NS,  // 5
            SE,  // 6
            NSE,  // 7
            -1,  // 8
            NW,  // 9
            EW,  // 10
            NEW,  // 11
            SW,  // 12
            NSW,  // 13
            SEW,  // 14
            NSEW,  // 15
        )

        @JvmStatic
        fun dragLines(filename: String, source: GUIBox): GUIPipeFeature {
            return GUIPipeFeature(filename, source, INDICES_DRAGLINES)
        }

        @JvmStatic
        fun full(filename: String, source: GUIBox): GUIPipeFeature {
            return GUIPipeFeature(filename, source, INDICES_FULL)
        }
    }
}