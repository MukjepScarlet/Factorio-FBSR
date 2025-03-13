package moe.mukjep.fbsr.gui.layout

import com.demod.dcba.CommandReporting
import moe.mukjep.fbsr.gui.GUIStyle
import com.google.common.collect.ArrayTable
import com.google.common.collect.Table
import moe.mukjep.fbsr.FBSR.renderBlueprint
import moe.mukjep.fbsr.FBSR.version
import moe.mukjep.fbsr.bs.types.BSBlueprintBook
import moe.mukjep.fbsr.gui.GUIBox
import moe.mukjep.fbsr.gui.GUISize
import moe.mukjep.fbsr.gui.part.GUIImage
import moe.mukjep.fbsr.gui.part.GUILabel
import moe.mukjep.fbsr.gui.part.GUILabel.Align
import moe.mukjep.fbsr.gui.part.GUIPanel
import moe.mukjep.fbsr.render.RenderRequest
import moe.mukjep.fbsr.render.RenderResult
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

class GUILayoutBook {
    class ImageBlock(val rows: Int, val cols: Int, val label: Optional<String>, val image: BufferedImage) {
        val location: Rectangle = Rectangle(cols, rows)
    }

    private var book: BSBlueprintBook? = null
    private var reporting: CommandReporting? = null
    private val results = ArrayList<RenderResult>()
    private val blocks = ArrayList<ImageBlock>()
    private var packBounds: Rectangle? = null

    private fun drawFrame(g: Graphics2D, bounds: GUIBox) {
        val titleHeight = 50

        val panel = GUIPanel(bounds, GUIStyle.FRAME_INNER)
        panel.render(g)

        drawTitleBar(g, bounds.cutTop(titleHeight))
        drawImagePane(g, bounds.shrinkTop(titleHeight))

        val creditBounds = bounds.cutRight(190).cutBottom(24).expandTop(8).cutTop(16).cutLeft(160)
        val creditPanel = GUIPanel(creditBounds, GUIStyle.FRAME_TAB)
        creditPanel.render(g)
        val lblCredit = GUILabel(
            creditBounds, "BlueprintBot $version",
            GUIStyle.FONT_BP_BOLD.deriveFont(16f), Color.GRAY, Align.TOP_CENTER
        )
        lblCredit.render(g)
    }

    private fun drawImagePane(g: Graphics2D, bounds: GUIBox) {
        var bounds = bounds
        bounds = bounds.shrink(0, 24, 24, 24)

        val panel = GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER)
        panel.render(g)

        val xform = g.transform
        val renderScaleX = xform.scaleX
        val renderScaleY = xform.scaleY

        val cellOffsetX = -20 / renderScaleX
        val cellOffsetY = -20 / renderScaleY
        val cellWidth = (BP_CELL_SIZE.width + 20) / renderScaleX
        val cellHeight = (BP_CELL_SIZE.height + 20) / renderScaleY

        val centerShiftX = (20 / renderScaleX).toInt()
        val centerShiftY = (20 / renderScaleY).toInt()

        g.font = GUIStyle.FONT_BP_REGULAR.deriveFont(12f)
        g.color = Color.gray
        val prevClip = g.clip

        for (block in blocks!!) {
            val x =
                (bounds.x + bounds.width / 2 + (-packBounds!!.width / 2.0 - packBounds!!.x + block.location.x) * cellWidth + cellOffsetX).toInt()
            val y =
                (bounds.y + bounds.height / 2 + (-packBounds!!.height / 2.0 - packBounds!!.y + block.location.y) * cellHeight + cellOffsetY).toInt()
            val w = (block.location.width * cellWidth).toInt()
            val h = (block.location.height * cellHeight).toInt()
            g.clip = Rectangle(x, y, w, h)

            val centerX = x + w / 2 + centerShiftX
            val centerY = y + h / 2 + centerShiftY

            val image = GUIImage(GUIBox(centerX, centerY, 0, 0), block.image)
            image.render(g)

            if (block.label.isPresent) {
                val label = block.label.get()
                g.drawString(label, x + 25, y + 35)
            }
        }

        g.clip = prevClip

        val groupings = ArrayTable.create<Int, Int, Int>(
            (packBounds!!.y)..(packBounds!!.y + packBounds!!.height),
            (packBounds!!.x)..(packBounds!!.x + packBounds!!.width)
        ) as Table<Int, Int, Int> // ??? Unchecked cast ???

        blocks.forEachIndexed { i, block ->
            for (row in block.location.y until block.location.y + block.location.height) {
                for (col in block.location.x until block.location.x + block.location.width) {
                    groupings.put(row, col, i)
                }
            }
        }

        val pipeX = (bounds.x + bounds.width / 2 + (-packBounds!!.width / 2.0) * cellWidth).toInt() - 4
        val pipeY = (bounds.y + bounds.height / 2 + (-packBounds!!.height / 2.0) * cellHeight).toInt() - 4
        GUIStyle.PIPE.renderDynamicGrid(g, pipeX, pipeY, cellWidth, cellHeight, packBounds!!, groupings)
    }

    private fun drawTitleBar(g: Graphics2D, bounds: GUIBox) {
        val lblTitle = GUILabel(
            bounds.shrinkBottom(8).shrinkLeft(24),
            book!!.label.orElse("Untitled Blueprint Book"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
            GUIStyle.FONT_BP_COLOR, Align.CENTER_LEFT
        )
        lblTitle.render(g)

        val startX = bounds.x + (lblTitle.getTextWidth(g) + 44).toInt()
        val endX = bounds.x + bounds.width - 24
        val pipe = GUIStyle.DRAG_LINES
        var x = endX - pipe.size
        while (x >= startX) {
            pipe.renderVertical(g, x, bounds.y + 10, bounds.y + bounds.height - 10)
            x -= pipe.size
        }
    }

    fun generateDiscordImage(): BufferedImage {
        val renderScale = 0.5
        val uiScale = 1.0

        // Render Images
        blocks.clear()
        results.clear()
        for (blueprint in book!!.allBlueprints) {
            val minWidth = (BP_IMAGE_MIN.width * renderScale).toInt()
            val minHeight = (BP_IMAGE_MIN.height * renderScale).toInt()
            val maxWidth = (BP_IMAGE_MAX.width * renderScale).toInt()
            val maxHeight = (BP_IMAGE_MAX.height * renderScale).toInt()

            val request = RenderRequest(blueprint, reporting!!)
            request.minWidth = minWidth
            request.minHeight = minHeight
            request.maxWidth = maxWidth
            request.maxHeight = maxHeight
            request.background = null
            request.gridLines = null
            request.maxScale = 0.5

            val result = renderBlueprint(request)
            results.add(result)

            val rows = (result.image.height + BP_CELL_SIZE.height - 1) / BP_CELL_SIZE.height
            val cols = (result.image.width + BP_CELL_SIZE.width - 1) / BP_CELL_SIZE.width
            blocks.add(ImageBlock(rows, cols, blueprint.label, result.image))
        }

        packBounds = packBlocks(blocks, DISCORD_IMAGE_RATIO)

        var imageWidth = BP_CELL_SIZE.width * packBounds!!.width
        var imageHeight = BP_CELL_SIZE.height * packBounds!!.height

        // Pipe gaps between cells
        imageWidth = (imageWidth + (packBounds!!.width + 1) * 20 * uiScale).toInt()
        imageHeight = (imageHeight + (packBounds!!.height + 1) * 20 * uiScale).toInt()
        // Framing
        imageWidth = (imageWidth + 48 * uiScale).toInt()
        imageHeight = (imageHeight + 78 * uiScale).toInt()

        val ret = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

        val bounds = GUIBox(0, 0, (ret.width / uiScale).toInt(), (ret.height / uiScale).toInt())

        val g = ret.createGraphics()

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.scale(uiScale, uiScale)

            drawFrame(g, bounds)
        } finally {
            g.dispose()
        }

        return ret
    }

    companion object {
        @JvmField
        val BP_CELL_SIZE: GUISize = GUISize(200, 150)
        @JvmField
        val BP_IMAGE_MIN: GUISize = GUISize(BP_CELL_SIZE.width, BP_CELL_SIZE.height)
        @JvmField
        val BP_IMAGE_MAX: GUISize = GUISize(BP_CELL_SIZE.width * 8, BP_CELL_SIZE.height * 8)

        @JvmField
        val DISCORD_IMAGE_RATIO: Double = (GUILayoutBlueprint.DISCORD_IMAGE_SIZE.width
                / GUILayoutBlueprint.DISCORD_IMAGE_SIZE.height).toDouble()

        private fun computeBoundingBox(blocks: List<ImageBlock>): Rectangle {
            if (blocks.isEmpty()) return Rectangle(0, 0, 0, 0)

            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE

            for (block in blocks) {
                val r = block.location
                if (r.x < minX) minX = r.x
                if (r.y < minY) minY = r.y
                val rMaxX = r.x + r.width
                val rMaxY = r.y + r.height
                if (rMaxX > maxX) maxX = rMaxX
                if (rMaxY > maxY) maxY = rMaxY
            }

            return Rectangle(minX, minY, maxX - minX, maxY - minY)
        }

        private fun overlapsAny(blocks: List<ImageBlock>, i: Int, x: Int, y: Int): Boolean {
            val current = blocks[i].location
            val testRect = Rectangle(x, y, current.width, current.height)

            for (j in 0 until i) {
                val placed = blocks[j].location
                if (testRect.intersects(placed)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun packBlocks(blocks: List<ImageBlock>, targetRatio: Double): Rectangle {
            // Compute total area and largest rectangle dimensions
            var totalArea = 0
            var width = 0
            var height = 0
            for (block in blocks) {
                val r = block.location
                totalArea += r.width * r.height
                if (r.width > width) {
                    width = r.width
                    height = ceil(width / targetRatio).toInt()
                }
                if (r.height > height) {
                    width = ceil(height * targetRatio).toInt()
                    height = r.height
                }
            }

            val areaBasedWidth = ceil(sqrt(totalArea * targetRatio)).toInt()
            val areaBasedHeight = ceil(areaBasedWidth / targetRatio).toInt()
            width = max(width.toDouble(), areaBasedWidth.toDouble()).toInt()
            height = max(height.toDouble(), areaBasedHeight.toDouble()).toInt()

            val maxAttempts = 1000
            for (attempt in 0 until maxAttempts) {
                if (tryPlaceRectangles(blocks, width, height)) {
                    // Success
                    return computeBoundingBox(blocks)
                } else {
                    // Adjust dimensions slightly while trying to preserve ratio
                    val currentRatio = width.toDouble() / height.toDouble()
                    if (currentRatio < targetRatio) {
                        width++
                    } else {
                        height++
                    }
                }
            }

            throw RuntimeException("Could not pack rectangles within max attempts.")
        }

        /**
         * Attempts to place all rectangles in the given width and height. O(n²)
         * approach: - For each rectangle, try every (x, y) position until a valid
         * non-overlapping spot is found. - If no spot is found, return false.
         */
        private fun tryPlaceRectangles(blocks: List<ImageBlock>, width: Int, height: Int): Boolean {
            for (i in blocks.indices) {
                val r = blocks[i].location
                var placed = false
                var y = 0
                while (y <= height - r.height && !placed) {
                    var x = 0
                    while (x <= width - r.width && !placed) {
                        if (!overlapsAny(blocks, i, x, y)) {
                            r.setLocation(x, y)
                            placed = true
                        }
                        x++
                    }
                    y++
                }

                if (!placed) {
                    return false // could not place this rectangle
                }
            }
            return true
        }
    }
}