package moe.mukjep.fbsr.gui.layout

import com.demod.dcba.CommandReporting
import com.demod.factorio.FactorioData
import com.demod.factorio.TotalRawCalculator
import com.demod.factorio.prototype.DataPrototype
import com.demod.fbsr.EntityRendererFactory
import com.demod.fbsr.RenderUtils
import com.demod.fbsr.fp.FPSprite
import moe.mukjep.fbsr.gui.GUIStyle
import moe.mukjep.fbsr.FBSR.generateTotalItems
import moe.mukjep.fbsr.FBSR.generateTotalRawItems
import moe.mukjep.fbsr.FBSR.renderBlueprint
import moe.mukjep.fbsr.FBSR.version
import moe.mukjep.fbsr.bs.types.BSBlueprint
import moe.mukjep.fbsr.gui.GUIBox
import moe.mukjep.fbsr.gui.GUISize
import moe.mukjep.fbsr.gui.GUISpacing
import moe.mukjep.fbsr.gui.part.GUIImage
import moe.mukjep.fbsr.gui.part.GUILabel
import moe.mukjep.fbsr.gui.part.GUILabel.Align
import moe.mukjep.fbsr.gui.part.GUIPanel
import moe.mukjep.fbsr.render.RenderRequest
import moe.mukjep.fbsr.render.RenderResult
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.max

class GUILayoutBlueprint {
    private var blueprint: BSBlueprint? = null
    private var reporting: CommandReporting? = null
    var result: RenderResult? = null
        private set

    private var totalItems: Map<String, Double>? = null
    private var totalRawItems: Map<String, Double>? = null

    private var itemColumns = 0
    private var itemCellSize = 0

    private var itemIconScale = 0.0

    private var itemFontSize = 0f

    private var itemFontOffset = 0

    private fun drawFrame(g: Graphics2D, bounds: GUIBox) {
        val titleHeight = 50
        val infoPaneWidth = 76 + itemColumns * itemCellSize

        val panel = GUIPanel(bounds, GUIStyle.FRAME_INNER)
        panel.render(g)

        drawTitleBar(g, bounds.cutTop(titleHeight))
        drawInfoPane(g, bounds.shrinkTop(titleHeight).cutLeft(infoPaneWidth))
        drawImagePane(g, bounds.shrinkTop(titleHeight).shrinkLeft(infoPaneWidth))

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
        bounds = bounds.shrink(0, 12, 24, 24)

        // TODO description bar along top
        val panel = GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER)
        panel.render(g)

        val xform = g.transform
        val renderWidth = (bounds.width * xform.scaleX).toInt()
        val renderHeight = (bounds.height * xform.scaleY).toInt()

        val request = RenderRequest(blueprint!!, reporting!!)
        request.minWidth = renderWidth
        request.minHeight = renderHeight
        request.maxWidth = renderWidth
        request.maxHeight = renderHeight
        request.maxScale = 2.0
        request.background = null

        this.result = renderBlueprint(request)

        val image = GUIImage(bounds, result!!.image)
        image.render(g)
    }

    private fun drawInfoPane(g: Graphics2D, bounds: GUIBox) {
        var bounds = bounds
        val subPanelInset = GUISpacing(40, 12, 12, 12)
        val itemGridInset = GUISpacing(8, 8, 8, 8)
        val itemGridCell = GUISize(itemCellSize, itemCellSize)

        bounds = bounds.shrink(0, 24, 24, 12)

        val table = FactorioData.getTable()

        val backPanel = GUIPanel(bounds, GUIStyle.FRAME_DARK_INNER, GUIStyle.FRAME_OUTER)
        backPanel.render(g)

        abstract class SubPanel(var bounds: GUIBox, var title: String) {
            open fun render(g: Graphics2D) {
                GUIStyle.PIPE.renderBox(g, bounds.shrink(6, 6, 6, 6))

                val lblTitle = GUILabel(
                    bounds.cutTop(subPanelInset.top).shrink(20, 24, 8, 24), title,
                    GUIStyle.FONT_BP_BOLD.deriveFont(18f), GUIStyle.FONT_BP_COLOR, Align.CENTER
                )
                lblTitle.render(g)
            }
        }

        val subPanels: MutableList<SubPanel> = ArrayList()
        var cutY = 0

        // Components
        run {
            val itemRows = (totalItems!!.size + itemColumns - 1) / itemColumns
            val subPanelHeight = subPanelInset.plus(itemGridInset).vertical + itemGridCell.height * itemRows
            subPanels.add(object : SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Items") {
                override fun render(g: Graphics2D) {
                    super.render(g)

                    val itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset)
                    val itemGridPanel = GUIPanel(
                        itemGridBounds, GUIStyle.FRAME_DARK_INNER,
                        GUIStyle.FRAME_LIGHT_OUTER
                    )
                    itemGridPanel.render(g)

                    for (row in 0 until itemRows) {
                        for (col in 0 until itemColumns) {
                            val cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(
                                row,
                                col
                            )
                            val bump = itemCellSize / 4
                            GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(bump, bump, bump, bump))
                        }
                    }

                    val itemOrder = totalItems!!.entries.sortedBy { -it.value }

                    for (i in itemOrder.indices) {
                        val entry = itemOrder[i]
                        val item = entry.key
                        val quantity = entry.value
                        val col = i % itemColumns
                        val row = i / itemColumns
                        val cellBounds =
                            itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col)

                        GUIStyle.ITEM_SLOT.render(g, cellBounds)

                        val protoItem =
                            table.getItem(item)

                        if (protoItem.isPresent) {
                            val imgIcon = GUIImage(
                                cellBounds, FactorioData.getIcon(protoItem.get()),
                                itemIconScale, false
                            )
                            imgIcon.render(g)
                        } else {
                            g.color = EntityRendererFactory.getUnknownColor(item)
                            g.fillOval(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height)
                        }

                        val fmtQty = RenderUtils.fmtItemQuantity(quantity)
                        g.font = GUIStyle.FONT_BP_BOLD.deriveFont(itemFontSize)
                        val strW = g.fontMetrics.stringWidth(fmtQty)
                        val x = cellBounds.x + cellBounds.width - strW - itemFontOffset
                        val y = cellBounds.y + cellBounds.height - itemFontOffset
                        g.color = Color(0, 0, 0, 128)
                        g.drawString(fmtQty, x - 1, y - 1)
                        g.drawString(fmtQty, x + 1, y + 1)
                        g.color = Color.white
                        g.drawString(fmtQty, x, y)
                    }
                }
            })
            cutY += subPanelHeight
        }

        // Raw
        run {
            val itemRows = (totalRawItems!!.size + itemColumns - 1) / itemColumns
            val subPanelHeight = subPanelInset.plus(itemGridInset).vertical + itemGridCell.height * itemRows
            subPanels.add(object : SubPanel(bounds.shrinkTop(cutY).cutTop(subPanelHeight), "Raw") {
                override fun render(g: Graphics2D) {
                    super.render(g)

                    val itemGridBounds = bounds.shrink(subPanelInset).shrink(itemGridInset)
                    val itemGridPanel = GUIPanel(
                        itemGridBounds, GUIStyle.FRAME_DARK_INNER,
                        GUIStyle.FRAME_LIGHT_OUTER
                    )
                    itemGridPanel.render(g)

                    for (row in 0 until itemRows) {
                        for (col in 0 until itemColumns) {
                            val cellBounds = itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(
                                row,
                                col
                            )
                            val bump = itemCellSize / 4
                            GUIStyle.FRAME_DARK_BUMP_OUTER.render(g, cellBounds.shrink(bump, bump, bump, bump))
                        }
                    }

                    val itemOrder = totalRawItems!!.entries.sortedBy { -it.value }

                    for (i in itemOrder.indices) {
                        val entry = itemOrder[i]
                        val item = entry.key
                        val quantity = entry.value
                        val col = i % itemColumns
                        val row = i / itemColumns
                        val cellBounds =
                            itemGridCell.toBox(itemGridBounds.x, itemGridBounds.y).indexed(row, col)

                        GUIStyle.ITEM_SLOT.render(g, cellBounds)

                        var image: Optional<BufferedImage>
                        var scale: Double
                        if (item == TotalRawCalculator.RAW_TIME) {
                            image = Optional.ofNullable(timeIcon)
                            scale = itemIconScale * 2
                        } else {
                            var prototype: Optional<out DataPrototype?> = table.getItem(item)
                            if (!prototype.isPresent) {
                                prototype = table.getFluid(item)
                            }
                            image = prototype.map(FactorioData::getIcon)
                            scale = itemIconScale
                        }

                        if (image.isPresent) {
                            val imgIcon = GUIImage(cellBounds, image.get(), scale, false)
                            imgIcon.render(g)
                        } else {
                            g.color = EntityRendererFactory.getUnknownColor(item)
                            g.fillOval(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height)
                        }

                        val fmtQty = RenderUtils.fmtItemQuantity(quantity)
                        g.font = GUIStyle.FONT_BP_BOLD.deriveFont(itemFontSize)
                        val strW = g.fontMetrics.stringWidth(fmtQty)
                        val x = cellBounds.x + cellBounds.width - strW - itemFontOffset
                        val y = cellBounds.y + cellBounds.height - itemFontOffset
                        g.color = Color(0, 0, 0, 128)
                        g.drawString(fmtQty, x - 1, y - 1)
                        g.drawString(fmtQty, x + 1, y + 1)
                        g.color = Color.white
                        g.drawString(fmtQty, x, y)
                    }
                }
            })
            cutY += subPanelHeight
        }

        // Grid Settings
        run {}

        val frontPanel = GUIPanel(bounds.cutTop(cutY), GUIStyle.FRAME_LIGHT_INNER)
        frontPanel.render(g)

        subPanels.forEach { p: SubPanel -> p.render(g) }
    }

    private fun drawTitleBar(g: Graphics2D, bounds: GUIBox) {
        val lblTitle = GUILabel(
            bounds.shrinkBottom(8).shrinkLeft(24),
            blueprint!!.label.orElse("Untitled Blueprint"), GUIStyle.FONT_BP_BOLD.deriveFont(24f),
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
        totalItems = generateTotalItems(blueprint!!)
        totalRawItems = generateTotalRawItems(totalItems!!)

        val itemCount = totalItems!!.size + totalRawItems!!.size
        val itemRowMax: Int
        if (itemCount <= 32) {
            itemRowMax = 8
            itemCellSize = 40
            itemIconScale = 0.5
            itemFontSize = 12f
            itemFontOffset = 5
        } else if (itemCount <= 72) {
            itemRowMax = 12
            itemCellSize = 30
            itemIconScale = 0.375
            itemFontSize = 10f
            itemFontOffset = 4
        } else {
            itemRowMax = 16
            itemCellSize = 20
            itemIconScale = 0.25
            itemFontSize = 8f
            itemFontOffset = 3
        }

        itemColumns = max(1.0, ((itemCount + itemRowMax - 1) / itemRowMax).toDouble()).toInt()

        val scale = 2.0

        val imageWidth = (DISCORD_IMAGE_SIZE.width * scale).toInt()
        val imageHeight = (DISCORD_IMAGE_SIZE.height * scale).toInt()
        val ret = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

        val bounds = GUIBox(0, 0, (ret.width / scale).toInt(), (ret.height / scale).toInt())

        val g = ret.createGraphics()

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.scale(scale, scale)

            drawFrame(g, bounds)
        } finally {
            g.dispose()
        }

        return ret
    }

    companion object {
        // Discord messages at 100% scale embed images at 550x350
        // This is double so it has a nice zoom but also crisp in detail
        @JvmField
        val DISCORD_IMAGE_SIZE: GUISize = GUISize(1100, 700)

        // XXX this is a bad hack
        private var timeIcon: BufferedImage? = runCatching {
            FPSprite(FactorioData.getTable().getRaw("utility-sprites", "default", "clock").get())
                .createSprites()[0].image
        }.getOrNull()
    }
}